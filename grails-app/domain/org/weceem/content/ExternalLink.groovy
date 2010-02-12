package org.weceem.content

import org.weceem.content.*

/**
 * A node representing a link to an external URL
 *
 * @author Marc Palmer
 */
class ExternalLink extends Content {

    String url
    String description

    static searchable = {
        alias ExternalLink.name.replaceAll("\\.", '_')
        
        only = ["description", 'title', 'status']
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
            description:description
        ] 
        return r
    }

    static constraints = {
        url(url:true, nullable: false, maxSize: 1000)
        description(nullable: true, blank: true, maxSize: 200)
    }
    
    static transients = Content.transients

    static mapping = {
        cache usage: 'read-write'
    }

}
