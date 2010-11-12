import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.services.*
import org.weceem.content.*

abstract class AbstractWeceemIntegrationTest extends GroovyTestCase {
    def wcmContentRepositoryService
    def application
    
    void setUp() {
        super.setUp()
        
        wcmContentRepositoryService = new WcmContentRepositoryService()
        wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        wcmContentRepositoryService.wcmCacheService.weceemCacheManager = new net.sf.ehcache.CacheManager()
        wcmContentRepositoryService.wcmSecurityService = new WcmSecurityService()
        wcmContentRepositoryService.wcmSecurityService.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ],
                        archived: [ status: ''],
                        unmoderated: [ status: '']
                    ]
                ]
            ]
            afterPropertiesSet()
        }
        application = ApplicationHolder.application
        wcmContentRepositoryService.wcmEventService = new WcmEventService()
        
        wcmContentRepositoryService.grailsApplication = application
        wcmContentRepositoryService.afterPropertiesSet()
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
        def deleg = [:]
        dsl.delegate = deleg
        dsl.call()
        def inst = wcmContentRepositoryService.newContentInstance(type, deleg.remove('space'))
        deleg.each {
            inst[it.key] = it.value
        }
        if (!inst.aliasURI) {
            inst.aliasURI = "node-"+System.currentTimeMillis()+"|"+(nodeCount++)
        }
        if (!inst.validate()) {
            println "Errors creating content: ${inst.errors}"
        }
        assert inst.save(flush:true)
        return inst
    }
}
