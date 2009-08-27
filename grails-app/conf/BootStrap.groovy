/**
 * Development-mode only bootstrap
 */
import org.weceem.content.Space
import org.weceem.content.Status
import org.codehaus.groovy.grails.commons.ApplicationHolder

class BootStrap {
    def grailsApplication
    
    def init = { servletContext ->
        
        
        ApplicationHolder.application.mainContext['contentRepositoryService'].createSpace([name:'Weceem'])
    }
    
    def destroy = {
        
    }
}