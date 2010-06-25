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
        
        // Now call the code
        println "In handleRequest, delegate is: ${delegate}"
        
        Closure code = getWcmScriptInstance(content.script)
        code.delegate = delegate
        code.resolveStrategy = Closure.DELEGATE_FIRST
        return code()
        
        
    }
}
