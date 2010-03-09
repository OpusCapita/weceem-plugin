import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.services.*

class AbstractWeceemIntegrationTest extends GroovyTestCase {
    def wcmContentRepositoryService
    def application
    
    void setUp() {
        super.setUp()
        
        wcmContentRepositoryService = new WcmContentRepositoryService()
        wcmContentRepositoryService.wcmCacheService = new WcmCacheService()
        wcmContentRepositoryService.wcmCacheService.cacheManager = new net.sf.ehcache.CacheManager()
        wcmContentRepositoryService.wcmSecurityService = new WcmSecurityService()
        wcmContentRepositoryService.wcmSecurityService.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ]
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
}