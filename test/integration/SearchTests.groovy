
import org.weceem.content.*
import org.weceem.html.*

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * wcmContentRepositoryService.
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
        
        statusA = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert statusA.save(flush:true)
        statusB = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert statusB.save(flush:true)

        spaceA = new WcmSpace(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)
        spaceB = new WcmSpace(name: 'b', aliasURI: 'b')
        assert spaceB.save(flush: true)

        def folder = new WcmFolder(title:'folder', aliasURI:'folder1', space:spaceA, status:statusA)
        assert folder.save()
        
        50.times {
            assert new WcmHTMLContent(title: "Acontent-$it", aliasURI: "acontent-$it",
                content: 'content number #$it', status: it % 2 == 0 ? statusA : statusB,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA,
                orderIndex: 1+it).save()
        }

        10.times {
            def n = new WcmHTMLContent(title: "Child-$it", aliasURI: "child-$it",
                content: 'child number #$it', status: statusA,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA,
                orderIndex: 1+it)
            folder.addToChildren(n)
            n.validate()
            println "Errors: ${n.errors}"
            assert n.save() 
        }

        10.times {
            assert new WcmHTMLContent(title: "Bcontent-$it", aliasURI: "bcontent-$it",
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
        
        def resultData = wcmContentRepositoryService.searchForContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize, resultData.results.size()
        assertTrue resultData.results.every { it.space.id == spaceA.id }

        resultData = wcmContentRepositoryService.searchForContent('content', spaceB, null, [max:pageSize])
        
        assertEquals 10, resultData.results.size()
        assertTrue resultData.results.every { it.space.id == spaceB.id }
    }
    
/* commented out because for now this doesn't work and we're deferring this until a later version
    void testSearchForContentUnderURI() {
        def pageSize = 20
        
        def resultData = wcmContentRepositoryService.searchForContent('content', spaceA, 'folder1', [max:pageSize])
        
        println "Results: ${resultData.results}" 
        assertEquals 10, resultData.results.size()
        def f = WcmFolder.findByAliasURI('folder1')
        
        assertTrue resultData.results.every { n->
            (n.space.id == spaceA.id) && (n.parent.id == f.id)
        }
    }
*/
    
    void testSearchForPublicContentPaging() {
        def pagesize = 10
        def resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize])

        assertEquals pagesize, resultData.results.size()
        assertTrue resultData.results.every { n -> n.status.publicContent == true }

        def resultData2 = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pagesize, offset:pagesize])

        assertEquals pagesize, resultData2.results.size()
        assertTrue resultData2.results.every { r ->
            r.status.publicContent && !resultData.results.find { n -> n.id == r.id }
        }
    }

    void testSearchForPublicContentExcludesUnpublishedContent() {
        def pageSize = 50
        def resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize / 2, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
    }

/* commented out as we can't get this implementation to work at all, Searchable/Compass issues
    void testSearchCascadesChangesToStatusPublicContentProperty() {
        def pageSize = 50
        def resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize/2, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
 
        def status = WcmStatus.findByCode(100)
        status.publicContent = true
        status.save(flush:true)
        
        searchableService.stopMirroring()
        searchableService.reindex()
        searchableService.startMirroring()
        
        resultData = wcmContentRepositoryService.searchForPublicContent('content', spaceA, null, [max:pageSize])
        
        assertEquals pageSize, resultData.results.size()
        assertTrue resultData.results.every { it.status.publicContent == true }
    }
*/
}