package org.weceem.script

import org.weceem.content.Content

class WcmScript extends Content {

    static standaloneContent = false

    String content
    String description // Human explanation of it
    
    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { content }

    static mapping = {
        cache usage: 'nonstrict-read-write' 
    }
    
    static editors = {
        content(editor:'GroovyCode')
    }

    static transients = Content.transients
    
    static constraints = {
        content(nullable: false, maxSize: 65536)
        description(maxSize:200)
    }
}
