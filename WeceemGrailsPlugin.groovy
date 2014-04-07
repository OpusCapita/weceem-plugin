import org.apache.commons.logging.LogFactory

import org.weceem.services.WcmContentRepositoryService

import grails.util.Environment

class WeceemGrailsPlugin {
    def _log = LogFactory.getLog('org.weceem.WeceemGrailsPlugin')

    // the plugin version
    def version = "1.2"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.1 > *"

    def observe = ["hibernate", 'services']

    def loadAfter = ['logging', 'searchable']
    def loadBefore = ['controllers', 'ckeditor'] // Make sure taglib sees configured service
    
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "web-app/WeceemFiles/**/*",
        "web-app/testing/**/*"
    ]

    def author = "jCatalog AG"
    def authorEmail = "info@weceem.org"
    def title = "Weceem CMS"
    def description = '''Weceem CMS is a free, open source content management system.'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/weceem"
    def license = "APACHE"
    def organization = [ name: "jCatalog AG", url: "http://weceem.org/" ]
    def developers = [
            [ name: "Marc Palmer", email: "marc@grailsrocks.com" ]
    ]
    def issueManagement = [ system: "JIRA", url: "http://jira.jcatalog.com/browse/WCM" ]
    def scm = [ url: "https://github.com/jCatalog/weceem-plugin" ]

    def getWebXmlFilterOrder() {
        def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
        [   WeceemFileFilter: FilterManager.URL_MAPPING_POSITION + 10000 ]
    }

    def doWithSpring = {

        simpleSpaceExporter(org.weceem.export.SimpleSpaceExporter) {
            grailsApplication = ref('grailsApplication')
            proxyHandler = ref('proxyHandler')
        }
        simpleSpaceImporter(org.weceem.export.SimpleSpaceImporter) {
            grailsApplication = ref('grailsApplication')
            proxyHandler = ref('proxyHandler')
        }
        confluenceSpaceImporter(org.weceem.export.ConfluenceSpaceImporter) {
            grailsApplication = ref('grailsApplication')
        }

        wcmRenderEngine(org.weceem.content.RenderEngine) {
            // We can't wire up here, dependency pain
            proxyHandler = ref('proxyHandler')
        }

        // Register our custom binding beans
        customPropertyEditorRegistrar(org.weceem.binding.CustomPropertyEditorRegistrar)
        
        // Configure caching
        boolean hasEhCacheConfigXML = application.parentContext.getResource('classpath:/ehcache.xml').exists()
        if (hasEhCacheConfigXML) {
            // We assume app dev is managing cache with their own ehcache.xml
            println "Weceem: Initializing ehcache with default ehcache.xml from application"
            weceemCacheManager(net.sf.ehcache.CacheManager) { bean -> 
                bean.destroyMethod = 'shutdown'
            }
        } else {
            // init with default Weceem caching
            def configRes = application.parentContext.getResource('classpath:/weceem-default-ehcache.xml')
            println "Weceem: Initializing ehcache with default weceem ehcache.xml from plugin resource: ${configRes}"
            weceemCacheManager(net.sf.ehcache.CacheManager, configRes.URL) { bean -> 
                bean.destroyMethod = 'shutdown'
            }
        }
        //configure defaults for searchable plugin here
        if (!application.config.searchable) {
            def searchableConfig = new ConfigObject()
            def userHome = System.getProperty("user.home")
            searchableConfig.searchable.compassConnection = new File("${userHome}/.weceem/searchable-index").absolutePath
            searchableConfig.searchable.defaultExcludedProperties = ["password"]
            searchableConfig.searchable.defaultMethodOptions = [
                    search: [reload: false, escape: false, offset: 0, max: 10, defaultOperator: "and"],
                    suggestQuery: [userFriendly: true]
            ]
            searchableConfig.searchable.mirrorChanges = false
            searchableConfig.searchable.bulkIndexOnStartup = true
            searchableConfig.searchable.releaseLocksOnStartup = true
            application.config.merge(searchableConfig)

        } else if (!application.config.searchable.compassConnection) {
            def searchableConfig = new ConfigObject()
            def userHome = System.getProperty("user.home")
            searchableConfig.searchable.compassConnection = new File("${userHome}/.weceem/searchable-index").absolutePath
            application.config.merge(searchableConfig)
        }
    }

    def doWithApplicationContext = { applicationContext ->

        _log.info "Weceem plugin running with data source ${applicationContext.dataSource.dump()}"
        _log.info "Weceem plugin running with grails configuration ${applicationContext.grailsApplication.config}"
        
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'wcmRepository', action:'treeTable', title:'content', path:'contentrepo', order:0])

        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'wcmPortal', action:'administration', title:'administration', path:'admin',order:2])

        // Register the standard admin sections
        [
            [controller:'wcmSpace', action:'list', title:'spaces', path:'admin/spaces', order: 0],
            [controller:'wcmSynchronization', action:'list', title:'synchronize', path:'admin/files/synchronize', order: 1],
            [controller:'wcmPortal', action:'comingsoon', title:'plugins', path:'admin/plugins', order: 2],
            [controller:'wcmPortal', action:'licenses', title:'licenses', path:'admin/licenses', order: 3],
            [controller:'wcmPortal', action:'comingsoon', title:'linkcheck', path:'admin/linkchecker', order: 4] ].each { item ->
                applicationContext.navigationService.registerItem( 'weceem.plugin.admin', item)
        }

        def repSvc = applicationContext.wcmContentRepositoryService
        repSvc.loadConfig()
        applicationContext.wcmEditorService.cacheEditorInfo()
        configureCKEditor(repSvc.uploadDir, repSvc.uploadUrl, application)

        repSvc.createDefaultStatuses()
        
        def createDefSpace = application.config.weceem.create.default.space
        if (createDefSpace instanceof ConfigObject) {
            createDefSpace = true
        } else {
            createDefSpace = createDefSpace instanceof Boolean ? createDefSpace : createDefSpace.asBoolean()
        }
        
        if (createDefSpace) {
            if (Environment.current != Environment.TEST) {
                repSvc.createDefaultSpace()
            }
        }

        applicationContext.wcmContentDependencyService.reset()
        
        applicationContext.wcmRenderEngine.wcmSecurityService = applicationContext.wcmSecurityService
        applicationContext.wcmRenderEngine.wcmContentRepositoryService = applicationContext.wcmContentRepositoryService
        
    }

    def configureCKEditor(dir, url, application) {
        def settings = application.config
        def co = new ConfigObject()
        co.ckeditor.upload.basedir = dir.toString()
        co.ckeditor.upload.baseurl = url.toString()

        co.ckeditor.upload.overwrite = false
        co.ckeditor.defaultFileBrowser = "ofm"
        co.ckeditor.upload.image.browser = true
        co.ckeditor.upload.image.upload = true
        co.ckeditor.upload.image.allowed = ['jpg', 'gif', 'jpeg', 'png']
        co.ckeditor.upload.image.denied = []
        co.ckeditor.upload.link.browser = true
        co.ckeditor.upload.link.upload  = true
        co.ckeditor.upload.link.allowed = ['pdf', 'doc', 'docx', 'zip', 'jpg', 'jpeg', 'png']
        co.ckeditor.upload.media.upload = true
        co.ckeditor.upload.media.allowed = ['mpg','mpeg','avi','wmv','asf','mov']
        co.ckeditor.upload.media.denied = []
        co.ckeditor.upload.flash.upload = true
        co.ckeditor.upload.flash.allowed = ['swf']
        co.ckeditor.upload.flash.denied = []
        settings.merge(co)
    }
    
    def doWithWebDescriptor = { webXml ->
        // TODO Implement additions to web.xml (optional)
        
        // Install filter for /$uploadUrl that
        // extracts space URI part, converts back to space
        // asks sec svc if current user can view that uri
        // returns file if so
        
        log.info("Adding servlet filter")
                
        def listeners = webXml.listener[0]
        listeners + {
            'listener' {
                'listener-class'("org.weceem.servlet.SessionChangeListener")
            }
        }

        def filters = webXml.filter[0]
        filters + {
            'filter' {
                'filter-name'("WeceemFileFilter")
                'filter-class'("org.weceem.filter.UploadedFileFilter")
            }
        }
        def filterMappings = webXml."filter-mapping"
        def lastMapping = filterMappings[filterMappings.size() - 1]
        lastMapping + {
            'filter-mapping' {
                'filter-name'("WeceemFileFilter")
                'url-pattern'("/*")
            }
        }
        
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
