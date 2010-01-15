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

    String getVersioningContent() { url }
    
    static handleRequest = { content ->
        redirect(url:content.url)
    }

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
