/**
 * Development-mode only bootstrap
 */
import org.weceem.content.Space
import org.weceem.content.Status
import org.codehaus.groovy.grails.commons.ApplicationHolder

class BootStrap {
    def grailsApplication
    
    def init = { servletContext ->
        
        def space = new Space(name:'Test', aliasURI:'').save()
        assert space
        
        def space2 = new Space(name:'Other Test', aliasURI:'other').save()
        assert space2
        
        def node = ApplicationHolder.application.mainContext['contentRepositoryService'].newContentInstance(
            'org.weceem.html.HTMLContent', space)
        node.title = "Test document"
        node.aliasURI = "index"
        node.content = """
<h1>Welcome to Weceem CMS Plugin</h1>
<p>This shows you are seeing content from the CMS in development mode.</p>
Please go to the <a href="/weceem/admin">admin</a> interface.
"""
        node.status = Status.findByPublicContent(true)
        assert node.save()
    }
    
    def destroy = {
        
    }
}