
// The following properties have been added by the Upgrade process...
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

grails.mime.file.extensions = false // enables the parsing of file extensions from URLs into the request format

org.weceem.plugin.standalone="true"

//weceem.content.prefix="content"
//weceem.admin.prefix="wcm-admin"
//weceem.tools.prefix="cms/tools"
//weceem.admin.layout="weceemadmin-alt"
//weceem.create.default.space = false
//weceem.default.space.template = "file:/.....zip"
/*
weceem.space.templates = [
    default: "classpath:/org/weceem/resources/default-space-template.zip", 
    basic:"classpath:/org/weceem/resources/basic-space-template.zip"]
*/

log4j = {
    root {
        info 'stdout'
    }


    info   'grails.app'

    info   'org.weceem',
            'grails.app.controller',
            'grails.app.service',
            'grails.app.task',
            'grails.app.domain',
            'net.sf.ehcache'

}


environments {
    development {

        grails.serverURL = 'http://localhost:8080/weceem'
        
        weceem.upload.dir="/testing/"
    }
    
    test {
/*        log4j = {
            appenders {
               file name:'file', file: '/Users/marc/Desktop/Pending/testlog.log'
            }
            root {
               debug 'stdout', 'file'
            }
            
            warn  'org.codehaus.groovy.grails.web.servlet',  //  controllers
        	       'org.codehaus.groovy.grails.web.pages' //  GSP
            debug   'grails.app', 'org.weceem'

        }
*/
    }
}