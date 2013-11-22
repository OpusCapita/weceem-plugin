package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import grails.util.Holders
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class ContentRepositoryDefaultSpaceTests extends AbstractWeceemIntegrationTest {

    static transactional = true

    def servletContext 

    public void setUp() {
        initFakeServletContextPath('test/files/default-space-tests')

        servletContext = Holders.grailsApplication.mainContext.servletContext = ServletContextHolder.servletContext
        Holders.grailsApplication.mainContext.simpleSpaceImporter.proxyHandler = [unwrapIfProxy: { o -> o}]

        // Reset to defaults!
        Holders.grailsApplication.config.weceem.default.space.template = null
        Holders.grailsApplication.config.weceem.space.templates = [:]

        super.setUp()
    }
            
    void testDefaultSpaceCreateWorksWithoutLoggedInUser() {
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> [] }
        
        wcmContentRepositoryService.createDefaultSpace()
        
        assert WcmSpace.count().equals(1)

        // Need perms to view the content!
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }
        
        def spc = WcmSpace.findByName('Default')
        assert spc != null
        
        def contentInfo = wcmContentRepositoryService.findContentForPath('about', spc)
        
        assert contentInfo != null
        assert contentInfo.content != null
        assert contentInfo.content.title.indexOf('bout') >= 0
    }

    void testDefaultSpaceCreatedWithAppTemplate() {
        def f = new File(servletContext.getRealPath('/Alternative.zip'))

        grailsApplication.config.weceem.default.space.template = f.toURL().toString()

        wcmContentRepositoryService.createDefaultSpace()
        
        assert WcmSpace.count().equals(1)
        def contentInfo = wcmContentRepositoryService.findContentForPath('alternative-test', WcmSpace.findByName('Default'))
        
        assert contentInfo != null
        assert contentInfo.content != null
        assert contentInfo.content.title.indexOf('elcome') >= 0
    }

    void testSpaceCreatedCustomTemplate() {
        def f = new File(servletContext.getRealPath('/Alternative.zip'))

        Holders.grailsApplication.config.weceem.space.templates.DUMMY = f.toURL().toString()

        wcmContentRepositoryService.createSpace([name:'testing', aliasURI:'testing'], 'DUMMY')
        
        assert WcmSpace.count().equals(1)
        def contentInfo = wcmContentRepositoryService.findContentForPath('alternative-test', WcmSpace.findByName('testing'))
        
        assert contentInfo != null
        assert contentInfo.content != null
        assert contentInfo.content.title.indexOf('elcome') >= 0
    }
}