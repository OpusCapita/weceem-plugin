/**
 * Development-mode only bootstrap
 */
import org.weceem.content.Space
import org.weceem.content.Status
import org.codehaus.groovy.grails.commons.ApplicationHolder
import grails.util.Environment

class BootStrap {
    def grailsApplication
    
    def init = { servletContext ->
        
        if (Environment.current != Environment.TEST) {
            ApplicationHolder.application.mainContext['contentRepositoryService'].createSpace([name:'Weceem'])
        }
    }
    
    def destroy = {
        
    }
}