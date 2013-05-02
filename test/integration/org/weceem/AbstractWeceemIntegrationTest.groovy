package org.weceem

import org.springframework.web.context.WebApplicationContext
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.FileSystemResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.services.*
import org.weceem.content.*

abstract class AbstractWeceemIntegrationTest extends AbstractServletContextMockingTest {
    def wcmContentRepositoryService
    def _wcmRenderEngine
    def grailsApplication
    
    def oldCacheService
    def oldSecurityDelegateUserRoles
    def oldSecurityServiceProxyHandler

    void setUp() {
        super.setUp()
        
//        WcmContentRepositoryService.metaClass.getLog = { ->
//            [
//                debugEnabled: true, debug: { s -> println s },
//                errorEnabled: true, error: { s -> println s },
//                warnEnabled: true, warn: { s -> println s },
//                infoEnabled: true, info: { s -> println s }
//            ]
//        }
//
        // This feels really, really bad. Why are we modifying these beans!!!!
        oldCacheService = wcmContentRepositoryService.wcmCacheService
        wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        _wcmRenderEngine = new RenderEngine()

        def configURL = grailsApplication.class.getResource('/weceem-default-ehcache.xml')
        wcmContentRepositoryService.wcmCacheService.weceemCacheManager = new net.sf.ehcache.CacheManager(configURL)

        oldSecurityDelegateUserRoles = wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }

        wcmContentRepositoryService.loadConfig()

        // Need to kill everything between tests
        wcmContentRepositoryService.resetAllCaches()

        _wcmRenderEngine.wcmContentRepositoryService = wcmContentRepositoryService
        _wcmRenderEngine.wcmSecurityService = wcmContentRepositoryService.wcmSecurityService

        oldSecurityServiceProxyHandler = wcmContentRepositoryService.wcmSecurityService.proxyHandler
        wcmContentRepositoryService.wcmSecurityService.proxyHandler = [unwrapIfProxy: { o -> o}]
        _wcmRenderEngine.proxyHandler = [unwrapIfProxy: { o -> o}]

    }

    void tearDown() {
        // WcmSpace.list().each { s ->
        //     wcmContentRepositoryService.deleteSpaceContent(s)
        // }
        wcmContentRepositoryService.wcmCacheService = oldCacheService
        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = oldSecurityDelegateUserRoles

        wcmContentRepositoryService.wcmSecurityService.proxyHandler = oldSecurityServiceProxyHandler

        super.tearDown()

    }

    void createContent(Closure c) {
        c.delegate = new ContentCreatorDelegate(wcmContentRepositoryService:wcmContentRepositoryService)
        c.call()
    }

}


class ContentCreatorDelegate {
    def wcmContentRepositoryService
    
    def nodeCount = 0
    
    def status(args) {
        def s = new WcmStatus(code: args.code, description: args.description ?: "Status-"+args.code, 
            publicContent: args.publicContent == null ? true : args.publicContent)
        assert s.save(flush:true)        
        return s
    }
    def content(type, Closure dsl) {
        println "Creating content of type $type..."
        def deleg = [:]
        dsl.delegate = deleg
        dsl.call()
        def res = wcmContentRepositoryService.createNode(type, deleg)
        if (res.hasErrors()) {
            println "Errors creating content: ${res.errors}"
        } else {
            println "Created content: ${res.absoluteURI}"
        }
        assert !res.hasErrors()
        return res
    }
}
