import org.springframework.context.ApplicationContext
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest

import org.weceem.controllers.*
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*
import org.weceem.services.*

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * contentRepositoryService.
 *
 * These old tests BAD because they are not mocking the services, so they are testing the services and controller
 */
class ContentControllerTests extends GroovyTestCase {
    def template
    def nodeA
    def nodeB
    def applicationContext

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
        def defStatus = new Status(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        def spaceA = new Space(name: 'jcatalog', aliasURI: 'jcatalog').save(flush: true)
        assert spaceA
        
        println "Saved space: ${spaceA.dump()}"
        
        template = new Template(title: 'template', aliasURI: 'template',
                    space: spaceA, status: defStatus,
                    createdBy: 'admin', createdOn: new Date(),
                    changedBy: 'admin', changedOn: new Date(),
                    content: 'TEMPLATE START[<wcm:content/>]TEMPLATE END', orderIndex: 0).save(flush: true)
        assert template
        
        nodeA = new HTMLContent(title: 'contentA', aliasURI: 'contentA',
                content: 'sample A content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert nodeA.save(flush: true)

        nodeB = new HTMLContent(title: 'contentB', aliasURI: 'contentB',
                parent: nodeA, status: defStatus,
                content: 'sample B content',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeB.save(flush:true)

        nodeA.addToChildren(nodeB)
        nodeA.save(flush: true)

        def nodeLang = new HTMLContent(title: 'Translations', aliasURI: 'lang',
                parent: null, status: defStatus,
                content: 'Language list',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeLang.save(flush:true)

        def nodeLangDE = new HTMLContent(title: 'Deutsch', aliasURI: 'de',
                parent: nodeLang, status: defStatus,
                content: 'Deutsch Home',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software', orderIndex: 2)
        assert nodeLangDE.save(flush:true)

        nodeLang.addToChildren(nodeLangDE)
        nodeLang.save(flush: true)

        def virtContent1 = new VirtualContent(title: 'virtContent1', aliasURI: 'virtContent1',
                                              parent: null, target: nodeA, status: defStatus,
                                              space: spaceA, orderIndex: 3)
        assert virtContent1.save(flush:true)

        def virtContent2 = new VirtualContent(title: 'virtContent2', aliasURI: 'haus',
                                              parent: nodeLangDE, target: nodeB, status: defStatus,
                                              space: spaceA, orderIndex: 3)
        assert virtContent2.save(flush:true)
    }
    
    void testVirtualContentRenderRoot() {
        def con = new ContentController()
        con.contentRepositoryService = new ContentRepositoryService()
        con.contentRepositoryService.cacheService = new CacheService()
        con.contentRepositoryService.afterPropertiesSet()
        con.weceemSecurityService = new WeceemSecurityService()
        
        con.params.uri = "/jcatalog/virtContent1"
        con.show()
        
        println "${con.response.errorMessage}"
        assertEquals 200, con.response.status
        println "Content was: ${con.response.contentAsString}"
        assertTrue con.response.contentAsString.contains(nodeA.content)
    }
    
    void testVirtualContentRenderDeepChild() {
        def con = new ContentController()
        con.contentRepositoryService = new ContentRepositoryService()
        con.contentRepositoryService.cacheService = new CacheService()
        con.contentRepositoryService.afterPropertiesSet()
        con.weceemSecurityService = new WeceemSecurityService()
        con.params.uri = "/jcatalog/lang/de/haus"
        con.show()
        
        println "${con.response.errorMessage}"
        assertEquals 200, con.response.status
        println "Content was: ${con.response.contentAsString}"
        assertTrue con.response.contentAsString.contains(nodeB.content)
    }
}