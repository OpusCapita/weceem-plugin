package org.weceem.domain

import org.weceem.AbstractWeceemIntegrationTest

import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*

import org.weceem.AbstractServletContextMockingTest
import grails.util.Holders

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class ContentConstraintTests extends AbstractWeceemIntegrationTest {
    void testAllContentPropertyConstraintsAreCorrect() {
        Holders.grailsApplication.domainClasses.each { dca ->
            def con = dca.clazz.constraints.content
            if (con) {
                assert WcmContent.MAX_CONTENT_SIZE.equals(con.maxSize)
            }
        }
    }
}