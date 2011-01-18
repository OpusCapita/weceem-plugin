
// The following properties have been added by the Upgrade process...
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

org.weceem.plugin.standalone="true"

//weceem.content.prefix="content"
//weceem.admin.prefix="cms/admin"
//weceem.tools.prefix="cms/tools"
//weceem.admin.layout="weceemadmin-alt"

log4j = {
    root {
        info 'stdout'
    }
    
    
    info   'grails.app'

    info   'grails.app.controller',
            'grails.app.service',
            'grails.app.task',
            'org.codehaus.groovy.grails.web.servlet',  //  controllers
    	    'org.codehaus.groovy.grails.web.pages', //  GSP
            'grails.app.domain'
//            'org.codehaus.groovy.grails.web.mapping' // URL mapping
    
}

environments {
    development {
        grails.serverURL = 'http://localhost:8080/weceem'
        
        weceem.upload.dir="/testing/"
    }
    
    test {
        log4j = {
            root {
                debug 'stdout'
            }

            warn  'org.codehaus.groovy.grails.web.servlet',  //  controllers
        	       'org.codehaus.groovy.grails.web.pages' //  GSP
            debug   'grails.app', 'org.weceem'

        }
    }
}