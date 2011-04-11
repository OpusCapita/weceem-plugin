package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*

class ContentRepositoryDefaultSpaceTests extends AbstractWeceemIntegrationTest {

    static transactional = true

    def servletContext 
    
    protected void setUp() {
        ServletContextHolder.servletContext = new MockServletContext(
                'test/files/default-space-tests', new FileSystemResourceLoader())
        ServletContextHolder.servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                grailsApplication.mainContext)
        servletContext = grailsApplication.mainContext.servletContext = ServletContextHolder.servletContext

        super.setUp()
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
}