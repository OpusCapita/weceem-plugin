package org.weceem.script

import org.weceem.content.WcmContent

class WcmAction extends WcmContent {
    
    WcmScript script
    String description // Human explanation of it
    String allowedMethods
    
    static searchable = {
        alias WcmAction.name.replaceAll("\\.", '_')
        only = ['description', 'title', 'status']
    }

    static mapping = {
        cache usage: 'nonstrict-read-write' 
    }
    
    static transients = WcmContent.transients
    
    static constraints = {
        description(maxSize:200)
        allowedMethods(maxSize:40)
    }
    
    static handleRequest = { content ->
        // @todo enforceAllowedMethods here
        
        // Now call the codo
        def code = getScriptInstance(content.script)
        def controller = delegate
        code.metaClass.methodMissing = { String name, Object args ->
            // Have to call invoke method here as we have a single args object
            controller.invokeMethod("$name", args)   
        }

        code.metaClass.getProperty = { String name ->
            controller[name]
        }

        def r = code.run()
        return r
    }
}
