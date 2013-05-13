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

    //     test "org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion"
    //     // test "org.seleniumhq.selenium:selenium-ie-driver:$seleniumVersion"
    //     // test "org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion"
    //     test "org.seleniumhq.selenium:selenium-support:$seleniumVersion"
    //     test "org.gebish:geb-spock:$gebVersion"
    //     test "org.gebish:geb-junit4:$gebVersion"
    }

	plugins {
        build(":tomcat:$grailsVersion", ":release:2.2.0", ":hibernate:$grailsVersion") {
            export = false
        }

        compile ":bean-fields:1.0"
        compile ":blueprint:0.9.1.1"
        compile ":cache-headers:1.1.5"
        compile ":ckeditor:3.6.0.0"
        compile ":feeds:1.6"

        compile ":platform-core:1.0.RC5"
//        test ":geb:$gebVersion"
        test(":functional-test:2.0.RC2-SNAPSHOT") { 
            excludes "xerces, xml-apis"
            export = false
        }
        compile ":jquery:1.8.3"
        compile ":jquery-ui:1.8.24"
        compile ":navigation:1.3.2"
        compile ":quartz:1.0-RC7"
        compile ":searchable:0.6.4" 
        compile ":taggable:1.0.1"

        // For serlvet filter ordering
        provided ":webxml:1.4.1"
 	}
}

