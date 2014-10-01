grails.servlet.version = "3.0"
grails.tomcat.jvmArgs = ["-Xmx1024m", "-XX:MaxPermSize=100m", '-verbose:class']

grails.project.work.dir="target/work"

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    inherits "global" // inherit Grails default dependencies

	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'

	repositories {        
		grailsPlugins()
		grailsHome()
		grailsCentral()
        mavenRepo "http://repo.grails.org/grails/core"
        mavenCentral() // need this to resolve junit 3.8.1 (with grails 2.2.1+)
    }

    def gebVersion = "0.9.0-RC-1"
    def seleniumVersion = "2.26.0"

    dependencies {
        // Workarounds for Grails 1.2 not shipping ant in WAR
        compile 'org.apache.ant:ant:1.7.1'
        compile 'org.apache.ant:ant-launcher:1.7.1'

        // Our specific dependencies
        compile 'net.java.dev.textile-j:textile-j:2.2.864'
        compile 'com.lowagie:itext:2.1.3'

        compile 'xstream:xstream:1.2.1'
        compile "org.compass-project:compass:2.2.1"
    }

	plugins {
        // plugins for the build system only
        build ":tomcat:7.0.52.1"
        build (":release:3.0.1") {
            export = false
        }
        // plugins for the compile step
        compile ":scaffolding:2.0.1"
        compile ':cache:1.1.1'

        // plugins needed at runtime but not for compilation
        runtime ":hibernate:3.6.10.9" // or ":hibernate4:4.1.11.2"
        runtime ":database-migration:1.3.8"

        compile ":fields:1.4"
        compile ":cache-headers:1.1.5"
        compile ":ckeditor:3.6.6.1.0"
        compile ":feeds:1.6"

//        test ":geb:$gebVersion"
        test(":functional-test:2.0.RC2-SNAPSHOT") { 
            excludes "xerces, xml-apis"
            export = false
        }

        runtime ":jquery:1.11.1"
        compile ":jquery-ui:1.10.3"
        compile ":platform-core:1.0.0"
        compile ":quartz:1.0-RC7"
        compile ":searchable:0.6.8"
        compile ":taggable:1.0.1"
        compile ":twitter-bootstrap:3.2.0.2"

        // For serlvet filter ordering
        provided ":webxml:1.4.1"
 	}
}

