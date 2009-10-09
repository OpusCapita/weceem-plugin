log4j = {
    appenders {
        rollingFile name: 'fileLog', 
        fileName: 'application.log', 
        maxFileSize: 26214400, 
        maxBackupIndex: 10, 
        layout: pattern(conversionPattern: '%d{yyyy-MM-dd HH:mm:ss,SSS} %p %c{2} %m%n')
    }
    root {
         error()
         additivity = true
    }
    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
	       'org.codehaus.groovy.grails.web.pages' //  GSP
    debug 'grails.app'

    warn   'org.mortbay.log'
}

// The following properties have been added by the Upgrade process...
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
org.weceem.plugin.standalone="true"
