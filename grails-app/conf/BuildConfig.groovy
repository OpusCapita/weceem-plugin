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
     }

	plugins {
        // plugins for the build system only
        build   ':tomcat:7.0.54'
        runtime(':hibernate4:4.3.5.5') {
            export = false
        }
        runtime ':database-migration:1.4.0'
        runtime ':elasticsearch:0.0.3.6'

        compile ':scaffolding:2.1.2'
        compile ':cache:1.1.8'
        compile ':cache-headers:1.1.7'
        compile ':fields:1.4'
        compile ':ckeditor:4.4.1.0'
        compile ':feeds:1.6'

        runtime ':jquery:1.11.1'
        compile ':jquery-ui:1.10.4'
        compile ':platform-core:1.0.0'
        compile ':quartz:1.0.2'
        compile ':taggable:1.1.0'
        compile ':twitter-bootstrap:3.2.0.2'

        test(':functional-test:2.0.RC2-SNAPSHOT') {
            excludes "xerces, xml-apis"
            export = false
        }
        build ':release:3.0.1'
        // For serlvet filter ordering
        provided ':webxml:1.4.1'
 	}
}

