package org.weceem.domain

import org.weceem.AbstractWeceemIntegrationTest

import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*

class ContentConstraintTests extends AbstractWeceemIntegrationTest {
    void testAllContentPropertyConstraintsAreCorrect() {
        grailsApplication.domainClasses.each { dca ->
            def con = dca.clazz.constraints.content
            if (con) {
                assertEquals "${dca.clazz} maxSize is not set to MAX_CONTENT_SIZE", WcmContent.MAX_CONTENT_SIZE, con.maxSize
            }
        }
    }
}