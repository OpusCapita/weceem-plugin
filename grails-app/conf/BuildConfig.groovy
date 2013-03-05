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
        compile ":bean-fields:1.0.RC5"
        compile ":blueprint:0.9.1.1"
        compile ":cache-headers:1.1.5"
        compile ":ckeditor:3.6.0.0"
        compile ":feeds:1.5"
        test(":functional-test:2.0.RC2") { 
            export = false 
        }
        compile ":jquery:1.4.4.1"
        compile ":jquery-ui:1.8.6.1"
        compile ":navigation:1.3.2"
        compile ":quartz:1.0-RC5"
        compile ":searchable:0.6.4" 
        compile ":taggable:1.0.1"

        // For serlvet filter ordering
        provided ":webxml:1.4.1"

        compile ":hibernate:$grailsVersion"
        // compile(":release:2.2.1") {
        //     excludes "nekohtml", "xercesMinimal"
        //     export = false
        // }
        build(":tomcat:$grailsVersion") {
            export = false
        }            
 	}
}

