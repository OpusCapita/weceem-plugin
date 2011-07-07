package org.weceem.css

import org.weceem.content.*

/**
 * WcmStyleSheet class describes the content node of type 'CSS'.
 *
 * @author Sergei Shushkevich
 */
class WcmStyleSheet extends WcmContent {

    // 64Kb Unicode text with HTML/Wiki Markup
    String content

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "css-file-32.png"]

    static searchable = {
        alias WcmStyleSheet.name.replaceAll("\\.", '_')
        only = ['content', 'title', 'status']
    }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { content }
    
    String getMimeType() { "text/css" }

    static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE)
        status(nullable: false) // Workaround for Grails 1.1.1 constraint inheritance bug
    }
    
    static editors = {
        content(editor:'CssCode')
    }

    static transients = WcmContent.transients

    static mapping = {
        cache usage: 'read-write'
        columns {
            content type:'text'
        }
    }

}
