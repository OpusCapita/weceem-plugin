import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.content.*
import org.weceem.html.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * wcmContentRepositoryService.
 *
 * These old tests BAD because they are not mocking the services, so they are testing the services and controller
 */

@TestMixin(IntegrationTestMixin)
class SearchTests extends AbstractWeceemIntegrationTest {
    def statusA
    def statusB
    def spaceA
    def spaceB
    def elasticSearchService
    
    static transactional = true

    void setUp() {
        super.setUp()
        elasticSearchService = grailsApplication.mainContext.elasticSearchService

        statusA = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert statusA.save(flush:true)
        statusB = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert statusB.save(flush:true)

        spaceA = new WcmSpace(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)
        spaceB = new WcmSpace(name: 'b', aliasURI: 'b')
        assert spaceB.save(flush: true)

        def folder = wcmContentRepositoryService.createNode(WcmFolder, [title:'folder', aliasURI:'folder1', space:spaceA, status:statusA])

        50.times {
            wcmContentRepositoryService.createNode(WcmHTMLContent, [
                title: "Acontent-$it", aliasURI: "acontent-$it",
                content: "content number #$it", status: it % 2 == 0 ? statusA : statusB,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA,
                orderIndex: 1+it])
        }

        10.times {
            wcmContentRepositoryService.createNode(WcmHTMLContent, [
                title: "Child-$it", aliasURI: "child-$it",
                content: "child number #$it", status: statusA,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA,
                orderIndex: 1+it, 'parent.id':folder.ident()])
        }

        10.times {
            wcmContentRepositoryService.createNode(WcmHTMLContent, [
                title: "Bcontent-$it", aliasURI: "bcontent-$it",
                content: "content number #$it", status: statusA,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceB,
                orderIndex: 1+it])
        }

        System.out.println "spaceA: \n"
        wcmContentRepositoryService.findAllRootContent(spaceA).each { n ->
            System.out.println n.debugDescription()
        }
        System.out.println "spaceB: \n"
        wcmContentRepositoryService.findAllRootContent(spaceB).each { n ->
            System.out.println n.debugDescription()
        }
        elasticSearchService.index()
    }
    
    void tearDown() {
        super.tearDown()
    }
    
    void testSearchForContentAsInRepository() {
        def pageSize = 20
        
        def resultData = wcmContentRepositoryService.searchForContent('content', spaceA, null, [max:pageSize])
        
        assert pageSize.equals(resultData.searchResults.size())
        assert resultData.searchResults.every { it.space.id == spaceA.id }

        resultData = wcmContentRepositoryService.searchForContent('content', spaceB, null, [max:pageSize])
        
        assert resultData.searchResults.size().equals(10)
        assert resultData.searchResults.every { it.space.id == spaceB.id }
    }


    void testSearchForPublicContentPaging() {
        def pagesize = 10
        def resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize])

        assert pagesize.equals(resultData.searchResults.size())
        assert resultData.searchResults.every { n -> n.status.publicContent == true }

        def resultData2 = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize, offset:pagesize])

        assert pagesize.equals(resultData2.searchResults.size())
        assert resultData2.searchResults.every { r ->
            r.status.publicContent && !resultData.searchResults.find { n -> n.id == r.id }
        }
    }

    void testSearchForPublicContentExcludesUnpublishedContent() {
        def pageSize = 50
        def resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])

        int size = Math.round(pageSize / 2)
        assert resultData.searchResults.size().equals(size)
        assert resultData.searchResults.every { it.status.publicContent == true }
    }
}