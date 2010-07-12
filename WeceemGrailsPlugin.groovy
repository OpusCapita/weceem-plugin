import org.apache.commons.logging.LogFactory

class WeceemGrailsPlugin {
    def _log = LogFactory.getLog('org.weceem.WeceemGrailsPlugin')

    // the plugin version
    def version = "0.9-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.2 > *"
    
    // the other plugins this plugin depends on
    def dependsOn = [
        searchable:'0.5.5 > *', 
        quartz:'0.4.2 > *', 
        navigation:'1.1.1 > *',
        fckeditor:'0.9.2 > *',
        feeds:'1.5 > *',
        beanFields:'1.0-RC3 > *',
        blueprint:'0.9.1.1 > *',
        jqueryUi:'1.8.2.4 > *',
        taggable:'0.6.2 > *'
    ]
    def observe = ["hibernate", 'services']
    
//    def loadAfter = ['logging']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "web-app/${org.weceem.files.WcmContentFile.DEFAULT_UPLOAD_DIR}/**/*"
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

        // Register our custom binding beans
        customPropertyEditorRegistrar(org.weceem.binding.CustomPropertyEditorRegistrar)
        
        cacheManager(net.sf.ehcache.CacheManager) { bean -> 
            bean.destroyMethod = 'shutdown'
        }
    }

    def doWithApplicationContext = { applicationContext ->

        _log.info "Weceem plugin running with data source ${applicationContext.dataSource.dump()}"
        _log.info "Weceem plugin running with grails configuration ${applicationContext.grailsApplication.config}"
        
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'wcmRepository', action:'treeTable', title:'content', path:'contentrepo', order:0])
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'wcmPortal', action:'administration', title:'administration', path:'admin',order:2])
        [
            [controller:'wcmSpace', action:'list', title:'spaces', path:'admin/spaces', order: 0],
            [controller:'wcmSynchronization', action:'list', title:'synchronize', path:'admin/files/synchronize', order: 1],
            [controller:'wcmPortal', action:'comingsoon', title:'plugins', path:'admin/plugins', order: 2],
            [controller:'wcmPortal', action:'licenses', title:'licenses', path:'admin/licenses', order: 3],
            [controller:'wcmPortal', action:'comingsoon', title:'linkcheck', path:'admin/linkchecker', order: 4] ].each { item ->
                applicationContext.navigationService.registerItem( 'weceem.plugin.admin', item)
        }

        applicationContext.wcmEditorService.cacheEditorInfo()
        applicationContext.wcmEditorService.configureFCKEditor()

        applicationContext.wcmContentRepositoryService.createDefaultStatuses()
        applicationContext.wcmContentRepositoryService.createDefaultSpace()
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = { ctx ->
    }

    def onChange = { event ->
        // Reload all if service / whole app reloaded
        applicationContext.wcmEditorService.cacheEditorInfo()
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
