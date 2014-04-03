class WeceemPluginUrlMappings {

    static mappings = { appContext ->
        def ctx
        // Look for Grails 2.0 context arg
        if (appContext) {
            ctx = appContext
        } else {
            // Static holders are our only choice pre-2.0
            ctx = org.codehaus.groovy.grails.commons.ApplicationHolder.application.mainContext
        }
        def config = ctx.grailsApplication.config
        
        final CONTENT_PREFIX = (config.weceem.content.prefix instanceof String) ? 
            config.weceem.content.prefix : ''
        final TOOLS_PREFIX = (config.weceem.tools.prefix instanceof String) ? 
            config.weceem.tools.prefix : 'wcm'
        final ADMIN_PREFIX = (config.weceem.admin.prefix instanceof String) ?
            config.weceem.admin.prefix : 'wcm/admin'

        final FORBIDDEN_SPACE_URIS = [
            // Internal/app resources
            "_weceem/",
            "plugins/",
            "WEB-INF/",
            "ck",
            // Admin links
            "${ADMIN_PREFIX}/",
            "${TOOLS_PREFIX}/"
        ]


        def adminURI = "/${ADMIN_PREFIX}"
        
        delegate.(adminURI+"/$action?")(controller: 'wcmPortal')
                
        delegate.(adminURI+"/repository/$action?")(controller: 'wcmRepository')

        delegate.(adminURI+"/repository/preview/$id")(controller: 'wcmRepository', action:'preview')

        delegate.(adminURI+"/repository/$space/$action?/$id?")(controller: 'wcmRepository')
                
        delegate.(adminURI+"/editor/$action?/$id?")(controller: 'wcmEditor')
        
        delegate.(adminURI+"/editor/preview")(controller: 'wcmEditor', action:'update')

        delegate.(adminURI+"/administration/synchronization/$action?/$id?")(controller: 'wcmSynchronization')

        delegate.(adminURI+"/versions/$action?/$id?")(controller: 'wcmVersion')

        delegate.(adminURI+"/space/$action?/$id?")(controller: 'wcmSpace')

        def toolFunctionsPrefix = (TOOLS_PREFIX ? '/' : '')+"${TOOLS_PREFIX}"

        name contentSubmission: delegate.(toolFunctionsPrefix+"/submit/$action?") {
            controller = "wcmContentSubmission"
        }
        
        name feed: delegate.(toolFunctionsPrefix+"/feed/$action/$uri**") {
            controller = "wcmFeeds"
        }
        
        name archive: delegate.(toolFunctionsPrefix+"/archive/$uri**") {
            controller = "wcmArchive"
            action = "list"
        }
        
        name search: delegate.(toolFunctionsPrefix+"/search") {
            controller = "wcmSearch"
            action = "search"
        }
        
/*
        def u = wcmContentRepositoryService.uploadUrl
        delegate.(u+'$uri**') {
            controller = "wcmContent"
            action = "showUploadedFile"
        }
*/
        // This is tricky
        def contentURI = (CONTENT_PREFIX ? '/' : '')+"${CONTENT_PREFIX}/$uri**"
        
        invokeMethod(contentURI, {
            controller = "wcmContent"
            action = "show"
            constraints {
                // @todo this is very ugly, clean up
                uri(validator: { v ->
                    def uploadsPath = ctx.wcmContentRepositoryService.uploadUrl - '/'
                    return !v?.startsWith(uploadsPath) && !FORBIDDEN_SPACE_URIS.find { p -> 
                        return v?.startsWith(p) 
                    }
                })
            }
        })
        
        def rootURI = "/${CONTENT_PREFIX}"
        invokeMethod(rootURI, {
            controller = "wcmContent"
            action = "show"
        })

        final page404 = (config.weceem.page404 instanceof String) ?
            config.weceem.page404 : '404'
        "404"(view: "/${page404}")

        "403"(view:'/denied')
        "500"(view:'/error')

    }
}
