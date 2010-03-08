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
class ContentSubmissionControllerTests extends GroovyTestCase {
    def template
    def nodeA
    def nodeB
    def applicationContext
    
    WcmContentSubmissionController mockedController() {
        def con = new WcmContentSubmissionController()

        def secSvc = new WeceemSecurityService()
        secSvc.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ]
                    ]
                ]
            ]
            afterPropertiesSet()
        }
        con.contentRepositoryService = new ContentRepositoryService()
        con.contentRepositoryService.cacheService = new CacheService()
        con.contentRepositoryService.cacheService.cacheManager = new net.sf.ehcache.CacheManager()
        con.contentRepositoryService.weceemSecurityService = secSvc
        con.contentRepositoryService.afterPropertiesSet()

        con.weceemSecurityService = secSvc
        con.grailsApplication = ApplicationHolder.application
        return con
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    void setUp() {
        def servletContext = new MockServletContext(
            'test/files/contentRepository', new FileSystemResourceLoader())
        servletContext.setAttribute(
            GrailsApplicationAttributes.APPLICATION_CONTEXT,
            applicationContext)
        ServletContextHolder.servletContext = servletContext

        def draftStatus = new Status(code: 100, description: "draft", publicContent: false)
        assert draftStatus.save(flush:true)
        def defStatus = new Status(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        def spaceA = new Space(name: 'jcatalog', aliasURI: 'jcatalog').save(flush: true)
        assert spaceA

        nodeA = new HTMLContent(title: 'contentA', aliasURI: 'contentA',
            content: 'sample A content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            changedBy: 'admin', changedOn: new Date(),
            space: spaceA, keywords: 'software',
            template: template, orderIndex: 1)
        assert nodeA.save(flush: true)
    }
   
    void testSubmitCommentAsGuest() {
       /* Can't get it to work as can't give grailsApplication to controller
        assertTrue Comment.count() == 0

        def con = mockedController()

        con.params.formPath = "/original/form"
        con.params.successPath = "/contentA"
        con.params.spaceId = "1"
        con.params.parentId = nodeA.id
        con.params.type = "org.weceem.content.Comment"
        
        con.params.author = "Marc Palmer"
        con.params.email = "test@somewhere.com"
        def title = "Comment number 1"
        con.params.title = title
        con.params.content = "This is my firs comment"

        con.submit()

        assertEquals 200, con.response.status
        assertEquals "/successful", con.response.redirectedUrl
        
        assertTrue Comment.count() == 1
        assertTrue Comment.get(1).title = title
        */
    }

}