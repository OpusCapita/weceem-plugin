
// The following properties have been added by the Upgrade process...
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

org.weceem.plugin.standalone="true"

//weceem.content.prefix="mycontent"
//weceem.admin.prefix="cms"
//weceem.admin.layout="weceemadmin-alt"

log4j = {
    root {
        info 'stdout'
    }
    
    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
	       'org.codehaus.groovy.grails.web.pages' //  GSP
    info   'grails.app'

    debug   'grails.app.controller',
            'grails.app.service',
            'org.codehaus.groovy.grails.web.mapping' // URL mapping
    
}

environments {
    test {
        log4j = {
            root {
                info 'stdout'
            }

            warn  'org.codehaus.groovy.grails.web.servlet',  //  controllers
        	       'org.codehaus.groovy.grails.web.pages' //  GSP
            debug   'grails.app'

        }
    }
}