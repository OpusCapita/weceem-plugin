import org.codehaus.groovy.grails.commons.GrailsDomainClass

import org.weceem.content.*

class WeceemGrailsPlugin {
    // the plugin version
    def version = "0.3-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [
        searchable:'0.5.4 > *', 
        quartz:'0.4.1-SNAPSHOT > *', 
        navigation:'1.1 > *',
        fckeditor:'0.9.2 > *',
        beanFields:'0.1 > *'
    ]
    def observe = ["hibernate"]
    
    def loadAfter = ['logging']

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
    }

    def doWithApplicationContext = { applicationContext ->
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'repository', action:'treeTable', title:'content', path:'contentrepo', order:0])
        applicationContext.navigationService.registerItem( 'weceem', 
            [controller:'portal', action:'administration', title:'administration', path:'admin',order:2])
        [
            [controller:'space', action:'list', title:'spaces', path:'admin/spaces', order: 0],
            [controller:'synchronization', action:'synchronizationList', title:'synchronize', path:'admin/files/synchronize', order: 1],
            [controller:'portal', action:'comingsoon', title:'plugins', path:'admin/plugins', order: 2],
            [controller:'portal', action:'licenses', title:'licenses', path:'admin/licenses', order: 3],
            [controller:'portal', action:'comingsoon', title:'linkcheck', path:'admin/linkchecker', order: 4] ].each { item ->
                applicationContext.navigationService.registerItem( 'weceem.plugin.admin', item)
        }
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = { ctx ->
        ctx.editorService.cacheEditorInfo()
        ctx.contentRepositoryService.createDefaultStatuses()
        ctx.editorService.configureFCKEditor()
    }

    def onChange = { event ->
        if (event.source instanceof GrailsDomainClass) {
            applicationContext.editorService.cacheEditorInfo(event.source.clazz)
        }
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
