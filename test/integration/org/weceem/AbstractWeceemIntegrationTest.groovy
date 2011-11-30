package org.weceem

import org.weceem.services.*
import org.weceem.content.*

abstract class AbstractWeceemIntegrationTest extends GroovyTestCase {
    def wcmContentRepositoryService
    def wcmRenderEngine
    def grailsApplication
    
    void setUp() {
        super.setUp()
        
        WcmContentRepositoryService.metaClass.getLog = { ->
            [
                debugEnabled: true, debug: { s -> println s },
                errorEnabled: true, error: { s -> println s },
                warnEnabled: true, warn: { s -> println s },
                infoEnabled: true, info: { s -> println s }
            ]
        }
        
        wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        wcmRenderEngine = new RenderEngine()
        def configURL = grailsApplication.class.getResource('/weceem-default-ehcache.xml')
        wcmContentRepositoryService.wcmCacheService.weceemCacheManager = new net.sf.ehcache.CacheManager(configURL)

        wcmContentRepositoryService.wcmSecurityService.securityDelegate.getUserRoles = { -> ['ROLE_ADMIN'] }

        wcmContentRepositoryService.loadConfig()

        // Need to kill everything between tests
        wcmContentRepositoryService.resetAllCaches()

        wcmRenderEngine.wcmContentRepositoryService = wcmContentRepositoryService
        wcmRenderEngine.wcmSecurityService = wcmContentRepositoryService.wcmSecurityService

        wcmContentRepositoryService.wcmSecurityService.proxyHandler = [unwrapIfProxy: { o -> o}]
        wcmRenderEngine.proxyHandler = [unwrapIfProxy: { o -> o}]

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
