import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import grails.util.Environment

import org.weceem.content.*

class WeceemGrailsPlugin {
    def _log = LogFactory.getLog('org.weceem.WeceemGrailsPlugin')

    // the plugin version
    def version = "0.8-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2-RC2 > *"
    
    // the other plugins this plugin depends on
    def dependsOn = [
        searchable:'0.5.4 > *', 
        quartz:'0.4.1 > *', 
        navigation:'1.1.1 > *',
        fckeditor:'0.9.2 > *',
        beanFields:'0.4 > *'
    ]
    def observe = ["hibernate"]
    
//    def loadAfter = ['logging']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "web-app/WeceemFiles/**/*"
    ]

    // TODO Fill in these fields
    def author = "jCatalog AG"
    def authorEmail = "info@weceem.org"
    def title = "Weceem CMS"
    def description = '''\\
A CMS that you can install into your own applications, as used by the Weceem CMS application
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/weceem"

    def doWithSpring = {
        simpleSpaceExporter(org.weceem.export.SimpleSpaceExporter)
        simpleSpaceImporter(org.weceem.export.SimpleSpaceImporter)
        defaultSpaceImporter(org.weceem.export.DefaultSpaceImporter)
        confluenceSpaceImporter(org.weceem.export.ConfluenceSpaceImporter)
        
        cacheManager(net.sf.ehcache.CacheManager) { bean -> 
            bean.destroyMethod = 'shutdown'
        }
    }

    def doWithApplicationContext = { applicationContext ->
        _log.info "Weceem plugin running with data source ${applicationContext.dataSource.dump()}"
        _log.info "Weceem plugin running with grails configuration ${applicationContext.grailsApplication.config}"
        
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'repository', action:'treeTable', title:'content', path:'contentrepo', order:0])
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'portal', action:'administration', title:'administration', path:'admin',order:2])
        [
            [controller:'space', action:'list', title:'spaces', path:'admin/spaces', order: 0],
            [controller:'synchronization', action:'list', title:'synchronize', path:'admin/files/synchronize', order: 1],
            [controller:'portal', action:'comingsoon', title:'plugins', path:'admin/plugins', order: 2],
            [controller:'portal', action:'licenses', title:'licenses', path:'admin/licenses', order: 3],
            [controller:'portal', action:'comingsoon', title:'linkcheck', path:'admin/linkchecker', order: 4] ].each { item ->
                applicationContext.navigationService.registerItem( 'weceem.plugin.admin', item)
        }

        applicationContext.editorService.cacheEditorInfo()
        applicationContext.editorService.configureFCKEditor()

        applicationContext.contentRepositoryService.createDefaultStatuses()
        applicationContext.contentRepositoryService.createDefaultSpace()
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = { ctx ->
    }

    def onChange = { event ->
        println "ON CHANGE EVENT OCCURED: ${event}"
        if (event.source instanceof GrailsDomainClass) {
            applicationContext.editorService.cacheEditorInfo(event.source.clazz)
        }
        if (event.source instanceof GrailsServiceClass) {
            // Reload all if service / whole app reloaded
            applicationContext.editorService.cacheEditorInfo()
        }
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
