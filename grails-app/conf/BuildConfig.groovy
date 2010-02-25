grails.project.dependency.resolution = {
    inherits "global" // inherit Grails default dependencies
    
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
}