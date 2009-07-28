class WeceemPluginUrlMappings {
    static INTERNAL_URI_PREFIXES = [
        'css/',
        'js/',
        'images/',
        'admin/',
        'WEB-INF/',
        'content/',
        'plugins/',
        'uploads/',
        'login/',
        'logout/',
        'register/',
        'j_spring_',
        'fck',
        'WeceemFiles/'
    ]
    
    static mappings = {

        "/admin/$action?"(controller: 'portal')

        "/admin/view/$action?/$id?"(controller: 'preview')
                
        "/admin/repository/$action?"(controller: 'repository')

        "/admin/repository/preview/$id"(controller: 'repository', action:'preview')

        "/admin/repository/$space/$action?/$id?"(controller: 'repository')
                
        "/admin/editor/$action?/$id?"(controller: 'editor')
        
        "/admin/administration/synchronization/$action?/$id?"(controller: 'synchronization')

        "/admin/versions/$action?/$id?"(controller: 'version')

        "/admin/space/$action?/$id?"(controller: 'space')

        "/content/$action?/$id?"(controller:'content')
        
        "/$uri**?" {
            controller = "content"
            action = "show"
            constraints {
                // @todo this is very ugly, clean up
                uri(validator: { v ->
                    !WeceemPluginUrlMappings.INTERNAL_URI_PREFIXES.find { pref -> return v?.startsWith(pref) }
                })
            }
        }
        
        "/search/$space" {
            controller = "weceem"
            action = "search"
        }


        "500"(view:'/error')
    }
}
