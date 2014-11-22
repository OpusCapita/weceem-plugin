package org.weceem.content

/**
 * A node representing a link to an external URL
 *
 * @author Marc Palmer
 */
class WcmExternalLink extends WcmContent {

    String url

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "ext-link-32.png"]

    static searchable = {
        only = ["description", 'title', 'status', 'space', 'aliasURI', 'parent']
    }
    
    static handleRequest = { content ->
        redirect(url:content.url)
    }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { url }

    /**
     * Should be overriden by content types that can represent their content as HTML.
     * Used for wcm:content tag (content rendering)
     */
    public String getContentAsHTML() { contentAsText }
    
    Map getVersioningProperties() { 
        def r = super.getVersioningProperties() + [ 
            url:url
        ] 
        return r
    }

    static constraints = {
        url(url:true, nullable: false, maxSize: 1000)
    }
    
    static transients = WcmContent.transients

    static mapping = {
        cache usage: 'read-write'
    }

}
