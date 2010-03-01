import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.services.*

class AbstractWeceemIntegrationTest extends GroovyTestCase {
    def contentRepositoryService
    def application
    
    void setUp() {
        super.setUp()
        
        contentRepositoryService = new ContentRepositoryService()
        contentRepositoryService.cacheService = new CacheService()
        contentRepositoryService.cacheService.cacheManager = new net.sf.ehcache.CacheManager()
        contentRepositoryService.weceemSecurityService = new WeceemSecurityService()
        contentRepositoryService.weceemSecurityService.with {
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
        contentRepositoryService.eventService = new EventService()
        
        contentRepositoryService.grailsApplication = application
        contentRepositoryService.afterPropertiesSet()
    }
}