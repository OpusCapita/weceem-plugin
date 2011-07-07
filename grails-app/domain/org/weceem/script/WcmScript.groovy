package org.weceem.script

import org.weceem.content.WcmContent

class WcmScript extends WcmContent {

    static searchable = {
        alias WcmScript.name.replaceAll("\\.", '_')
        only = ['content', 'description', 'title', 'status']
    }

    static standaloneContent = false

    String content
    
    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { '' /* don't return script contents for search */ }

    static mapping = {
        cache usage: 'nonstrict-read-write' 
    }
    
    static editors = {
        content(editor:'GroovyCode')
    }

    static transients = WcmContent.transients
    
    static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE)
        description(maxSize:200, nullable: true, blank: true)
    }
}
