package org.weceem.css

import org.weceem.content.*

/**
 * StyleSheet class describes the content node of type 'CSS'.
 *
 * @author Sergei Shushkevich
 */
class StyleSheet extends Content {

    // 64Kb Unicode text with HTML/Wiki Markup
    String content

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { content }
    
    String getMimeType() { "text/css" }

    static constraints = {
        content(nullable: false, maxSize: 65536)
        status(nullable: false) // Workaround for Grails 1.1.1 constraint inheritance bug
    }
    
    static editors = {
        content(editor:'CssCode')
    }

    static transients = Content.transients

    static mapping = {
        cache usage: 'read-write'
        columns {
            content type:'text'
        }
    }

}
