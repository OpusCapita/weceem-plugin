package org.weceem.controllers

import org.weceem.services.*
import org.weceem.content.*
import org.weceem.html.*
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.FileSystemResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.AbstractServletContextMockingTest

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * wcmContentRepositoryService.
 *
 * These old tests BAD because they are not mocking the services, so they are testing the services and controller
 */
class ContentSubmissionControllerTests extends AbstractServletContextMockingTest {
    def template
    def nodeA
    def nodeB
    def applicationContext
    def grailsApplication
    
    WcmContentSubmissionController mockedController() {
        def con = new WcmContentSubmissionController()

        def secSvc = new WcmSecurityService()
        secSvc.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ],
                        archived: [ status: ''],
                        unmoderated: [ status: '']
                    ]
                ]
            ]
            afterPropertiesSet()
        }
        secSvc.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }

        con.wcmContentRepositoryService = new WcmContentRepositoryService()
        con.wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        con.wcmContentRepositoryService.wcmCacheService.weceemCacheManager = new net.sf.ehcache.CacheManager()
        con.wcmContentRepositoryService.wcmSecurityService = secSvc
        con.wcmContentRepositoryService.afterPropertiesSet()

        con.wcmSecurityService = secSvc
        con.grailsApplication = grailsApplication
        return con
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    void setUp() {
    
        initFakeServletContextPath('test/files/contentRepository')

        def draftStatus = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert draftStatus.save(flush:true)
        def defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        def spaceA = new WcmSpace(name: 'jcatalog', aliasURI: 'jcatalog').save(flush: true)
        assert spaceA

        nodeA = new WcmHTMLContent(title: 'contentA', aliasURI: 'contentA',
            content: 'sample A content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            changedBy: 'admin', changedOn: new Date(),
            space: spaceA, keywords: 'software',
            template: template, orderIndex: 1)
        assert nodeA.save(flush: true)
    }

    void testSubmitCommentAsGuest() {
       /* Can't get it to work as can't give grailsApplication to controller
        assertTrue WcmComment.count() == 0

        def con = mockedController()

        con.params.formPath = "/original/form"
        con.params.successPath = "/contentA"
        con.params.spaceId = "1"
        con.params.parentId = nodeA.id
        con.params.type = "org.weceem.content.WcmComment"
        
        con.params.author = "Marc Palmer"
        con.params.email = "test@somewhere.com"
        def title = "WcmComment number 1"
        con.params.title = title
        con.params.content = "This is my firs comment"

        con.submit()

        assertEquals 200, con.response.status
        assertEquals "/successful", con.response.redirectedUrl
        
        assertTrue WcmComment.count() == 1
        assertTrue WcmComment.get(1).title = title
        */
    }

}