package org.weceem.controllers

import org.springframework.context.ApplicationContext

import groovy.mock.interceptor.*

import org.weceem.controllers.*
import org.weceem.content.*
import org.weceem.html.*

import org.weceem.services.*
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.FileSystemResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder

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
    def grailsApplication
    
    WcmContentController mockedController() {
        def con = new WcmContentController()

        def app = grailsApplication
        
        con.wcmContentRepositoryService = app.mainContext.wcmContentRepositoryService
        con.wcmContentFingerprintService = app.mainContext.wcmContentFingerprintService
        con.wcmSecurityService = app.mainContext.wcmSecurityService
        return con
    }

    void setUp() {
        def servletContext = new MockServletContext(
                'test/files/contentRepository', new FileSystemResourceLoader())
/*        servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                applicationContext)
*/
        ServletContextHolder.servletContext = servletContext
        def defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        def draftStatus = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert draftStatus.save(flush:true)

        def spaceA = new WcmSpace(name: 'jcatalog', aliasURI: 'jcatalog').save(flush: true)
        assert spaceA
        
        template = new WcmTemplate(title: 'template', aliasURI: 'template',
                    space: spaceA, status: defStatus,
                    createdBy: 'admin', createdOn: new Date(),
                    changedBy: 'admin', changedOn: new Date(),
                    content: 'TEMPLATE START[<wcm:content/>]TEMPLATE END', orderIndex: 0).save(flush: true)
        assert template
        
        nodeA = new WcmHTMLContent(title: 'contentA', aliasURI: 'contentA',
                content: 'sample A content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert nodeA.save(flush: true)

        nodeB = new WcmHTMLContent(title: 'contentB', aliasURI: 'contentB',
                parent: nodeA, status: draftStatus,
                content: 'sample B content',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeB.save(flush:true)

        nodeA.addToChildren(nodeB)
        nodeA.save(flush: true)

        def nodeLang = new WcmHTMLContent(title: 'Translations', aliasURI: 'lang',
                parent: null, status: defStatus,
                content: 'Language list',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeLang.save(flush:true)

        def nodeLangDE = new WcmHTMLContent(title: 'Deutsch', aliasURI: 'de',
                parent: nodeLang, status: defStatus,
                content: 'Deutsch Home',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software', orderIndex: 2)
        assert nodeLangDE.save(flush:true)

        nodeLang.addToChildren(nodeLangDE)
        nodeLang.save(flush: true)

        def virtContent1 = new WcmVirtualContent(title: 'virtContent1', aliasURI: 'virtContent1',
                                              parent: null, target: nodeA, status: defStatus,
                                              space: spaceA, orderIndex: 3)
        assert virtContent1.save(flush:true)

        def virtContent2 = new WcmVirtualContent(title: 'virtContent2', aliasURI: 'haus',
                                              parent: nodeLangDE, target: nodeB, status: defStatus,
                                              space: spaceA, orderIndex: 3)
        assert virtContent2.save(flush:true)
    }
    
    void testDraftContentNotViewableByGuest() {
        def con = mockedController()

        // Clear roles
        con.wcmSecurityService.securityDelegate.getUserRoles = { -> [] }
        
        con.params.uri = "/jcatalog/contentA/contentB"
        con.show()
        
        println con.response.contentAsString
        assertEquals 403, con.response.status
    
    }
    
    void testDraftContentIsViewableByAdmin() {
        def con = mockedController()

        // Set admin
        con.wcmSecurityService.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }
        
        con.params.uri = "/jcatalog/contentA/contentB"
        con.show()
        
        println con.response.contentAsString
        assertEquals 200, con.response.status
    
    }

    void testVirtualContentRenderRoot() {
        /*

         This should all be a functional test, it is too painful to integ/unit test

        def con = mockedController()
        
        con.params.uri = "/jcatalog/virtContent1"
        con.show()
        
        println "${con.response.errorMessage}"
        assertEquals 200, con.response.status
        println "WcmContent was: ${con.response.contentAsString}"
        assertTrue con.response.contentAsString.contains(nodeA.content)
        */
    }
    
    void testVirtualContentRenderDeepChild() {
        /*

         This should all be a functional test, it is too painful to integ/unit test

        def con = mockedController()
        
        con.params.uri = "/jcatalog/lang/de/haus"
        con.show()
        
        println "${con.response.errorMessage}"
        assertEquals 200, con.response.status
        println "WcmContent was: ${con.response.contentAsString}"
        assertTrue con.response.contentAsString.contains(nodeB.content)
        */
    }
}