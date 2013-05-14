package org.weceem

import org.springframework.web.context.WebApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.FileSystemResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.services.*
import org.weceem.content.*

abstract class AbstractServletContextMockingTest extends GroovyTestCase {
    def oldServletContext

    void tearDown() {
        super.tearDown()

        if (oldServletContext) {
            ServletContextHolder.servletContext = oldServletContext
        }
    }

    void initFakeServletContextPath(path) {
        oldServletContext = ServletContextHolder.servletContext 

        ServletContextHolder.servletContext = new MockServletContext(
            path, new FileSystemResourceLoader())
        ServletContextHolder.servletContext.setAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
            grailsApplication.mainContext)
    }
}
