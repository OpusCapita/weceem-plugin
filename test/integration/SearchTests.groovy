
import org.weceem.controllers.*
import org.weceem.services.*
import org.weceem.content.*
import org.weceem.html.*

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * contentRepositoryService.
 *
 * These old tests BAD because they are not mocking the services, so they are testing the services and controller
 */
class SearchTests extends AbstractWeceemIntegrationTest {
    def statusA
    def statusB
    def spaceA
    def spaceB
    
    static transactional = true
    
    def searchableService
    
    void setUp() {
        super.setUp()
        
        searchableService = application.mainContext.searchableService
        searchableService.stopMirroring()
        
        statusA = new Status(code: 400, description: "published", publicContent: true)
        assert statusA.save(flush:true)
        statusB = new Status(code: 100, description: "draft", publicContent: false)
        assert statusB.save(flush:true)

        spaceA = new Space(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)
        spaceB = new Space(name: 'b', aliasURI: 'b')
        assert spaceB.save(flush: true)

        50.times {
            assert new HTMLContent(title: 'Acontent-$it', aliasURI: 'content-$it',
            content: 'content number #$it', status: it % 2 == 0 ? statusA : statusB,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA,
            orderIndex: 1+it).save(flush:true)
        }

        10.times {
            assert new HTMLContent(title: 'Bcontent-$it', aliasURI: 'content-$it',
            content: 'content number #$it', status: statusA,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceB,
            orderIndex: 1+it).save(flush:true)
        }

        searchableService.reindex()
        searchableService.startMirroring()
    }
    
    void tearDown() {
        
    }
    
    void testSearchForContentAsInRepository() {
        def pageSize = 20
        
        def resultData = contentRepositoryService.searchForContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize, resultData.results.size()
        assertTrue resultData.results.every { it.space.id == spaceA.id }

        resultData = contentRepositoryService.searchForContent('content', spaceB, null, [max:pageSize])
        
        assertEquals 10, resultData.results.size()
        assertTrue resultData.results.every { it.space.id == spaceB.id }
    }
    
    void testSearchForPublicContentPaging() {
        def pagesize = 10
        def resultData = contentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize])

        assertEquals pagesize, resultData.results.size()
        assertTrue resultData.results.every { n -> n.status.publicContent == true }

        def resultData2 = contentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize, offset:pagesize])

        assertEquals pagesize, resultData2.results.size()
        assertTrue resultData2.results.every { r ->
            r.status.publicContent && !resultData.results.find { n -> n.id == r.id }
        }
    }

    void testSearchForPublicContentExcludesUnpublishedContent() {
        def pageSize = 50
        def resultData = contentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize / 2, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
    }

    void testSearchCascadesChangesToStatusPublicContentProperty() {
        def pageSize = 50
        def resultData = contentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize/2, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
 
        def status = Status.findByCode(100)
        status.publicContent = true
        status.save(flush:true)
        
        searchableService.stopMirroring()
        searchableService.reindex()
        searchableService.startMirroring()
        
        resultData = contentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
    }

}