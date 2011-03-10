package org.weceem

import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.services.*
import org.weceem.content.*

abstract class AbstractWeceemIntegrationTest extends GroovyTestCase {
    def wcmContentRepositoryService
    def wcmContentDependencyService
    def grailsApplication
    
    void setUp() {
        super.setUp()
        
        wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        wcmContentRepositoryService.wcmCacheService.weceemCacheManager = new net.sf.ehcache.CacheManager()
        wcmContentRepositoryService.loadConfig()

        wcmContentDependencyService.init()

        wcmContentRepositoryService.resetAllCaches()
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
