grails.tomcat.jvmArgs = ["-Xmx1024m", "-XX:MaxPermSize=100m", '-verbose:class'] 
grails.project.work.dir="target/work"

grails.project.dependency.resolution = {
    inherits "global" // inherit Grails default dependencies

	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

	repositories {        
		grailsPlugins()
		grailsHome()
		grailsCentral()
        flatDir dirs:"lib" // need this for iText
    }
    
    dependencies {
        // Workarounds for Grails 1.2 not shipping ant in WAR
        compile 'org.apache.ant:ant:1.7.1'
        compile 'org.apache.ant:ant-launcher:1.7.1'

        // Our specific dependencies
        compile 'net.java.dev.textile-j:textile-j:2.2.864'
        compile 'xstream:xstream:1.2.1'
    }

	plugins { 
        compile ":bean-fields:1.0.RC5" //newest: 1.0; replacement fields-plugin
        compile ":blueprint:0.9.1.1" // newest: 1.0.2
        compile ":cache-headers:1.1.5"
        compile ":ckeditor:3.6.0.0" // newest: 3.6.3.0
        compile ":feeds:1.5"
        test(":functional-test:1.3-RC1") { // neweset : 2.0.RC1
            export = false 
        }
        compile ":jquery:1.8.3"
        compile ":jquery-ui:1.8.24"
        compile ":navigation:1.3.2"
        compile ":quartz:0.4.2" // newest : 1.0-RC5
        compile ":searchable:0.6.4" 
        compile ":taggable:1.0.1"

        // For serlvet filter ordering
        provided ":webxml:1.4.1"

        compile ":hibernate:$grailsVersion"
        compile(":release:2.2.1") {
            export = false
        }
        build(":tomcat:$grailsVersion") {
            export = false
        }            
 	}
}

