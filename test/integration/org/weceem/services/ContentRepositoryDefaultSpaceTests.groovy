package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*

class ContentRepositoryDefaultSpaceTests extends AbstractWeceemIntegrationTest {

    static transactional = true

    def servletContext 

    public void setUp() {
        ServletContextHolder.servletContext = new MockServletContext(
                'test/files/default-space-tests', new FileSystemResourceLoader())
        ServletContextHolder.servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                grailsApplication.mainContext)
        servletContext = grailsApplication.mainContext.servletContext = ServletContextHolder.servletContext
        grailsApplication.mainContext.simpleSpaceImporter.proxyHandler = [unwrapIfProxy: { o -> o}]

        // Reset to defaults!
        grailsApplication.config.weceem.default.space.template = null
        grailsApplication.config.weceem.space.templates = [:]

        super.setUp()
    }
    
    void testDefaultSpaceCreateWorksWithoutLoggedInUser() {
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> [] }
        
        wcmContentRepositoryService.createDefaultSpace()
        
        assertEquals 1, WcmSpace.count()

        // Need perms to view the content!
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }
        
        def spc = WcmSpace.findByName('Default')
        assertNotNull spc
        
        def contentInfo = wcmContentRepositoryService.findContentForPath('about', spc)
        
        assertNotNull contentInfo
        assertNotNull contentInfo.content
        assertTrue contentInfo.content.title.indexOf('bout') >= 0
    }

    void testDefaultSpaceCreatedWithAppTemplate() {
        def f = new File(servletContext.getRealPath('/Alternative.zip'))

        grailsApplication.config.weceem.default.space.template = f.toURL().toString()

        wcmContentRepositoryService.createDefaultSpace()
        
        assertEquals 1, WcmSpace.count()
        def contentInfo = wcmContentRepositoryService.findContentForPath('alternative-test', WcmSpace.findByName('Default'))
        
        assertNotNull contentInfo
        assertNotNull contentInfo.content
        assertTrue contentInfo.content.title.indexOf('elcome') >= 0
    }

    void testSpaceCreatedCustomTemplate() {
        def f = new File(servletContext.getRealPath('/Alternative.zip'))

        grailsApplication.config.weceem.space.templates.DUMMY = f.toURL().toString()

        wcmContentRepositoryService.createSpace([name:'testing', aliasURI:'testing'], 'DUMMY')
        
        assertEquals 1, WcmSpace.count()
        def contentInfo = wcmContentRepositoryService.findContentForPath('alternative-test', WcmSpace.findByName('testing'))
        
        assertNotNull contentInfo
        assertNotNull contentInfo.content
        assertTrue contentInfo.content.title.indexOf('elcome') >= 0
    }
}