package org.weceem.script

import org.weceem.content.Content

class WcmScript extends Content {

    static standaloneContent = false

    String content
    String description // Human explanation of it
    
    public String getVersioningContent() { content }
    
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
