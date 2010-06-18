grails.project.dependency.resolution = {
    inherits "global" // inherit Grails default dependencies

	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

	repositories {        
		grailsPlugins()
		grailsHome()
		grailsCentral()
    }
    
    dependencies {
        flatDir dirs:"lib" // need this for textilej
        
        // Workarounds for Grails 1.2 not shipping ant in WAR
        compile 'org.apache.ant:ant:1.7.1'
        compile 'org.apache.ant:ant-launcher:1.7.1'

        // Our specific dependencies
        compile 'com.lowagie:iText:2.1.3'
        compile 'net.java.textilej:net.java.textilej:2.2.854'
        compile 'xstream:xstream:1.2.1'
    }

	plugins { 
		runtime ':hibernate:1.3.1'
		runtime ':searchable:0.5.5'
		runtime ':navigation:1.1.1'
		runtime ':bean-fields:1.0-RC3'
		runtime ':jquery-ui:1.8.2.1'
		runtime ':quartz:0.4.2'
		runtime ':feeds:1.5'
		runtime ':taggable:0.6.1'
		runtime ':fckeditor:0.9.5'
	}
}

