package org.weceem.blog

import org.weceem.content.*

/**
 * Placeholder for blog settings etc, has BlogEntry children
 */
class Blog extends Content {

    Template template
    Integer maxEntriesToDisplay
    String commentMarkup = ""
    
    static searchable = {
        alias Blog.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }
    
    static constraints = {
        template(nullable: true)
        maxEntriesToDisplay(inList:[3, 5, 10, 20])
        commentMarkup(inList:["", "html", "wiki"])
    }
    
    static mapping = {
        template cascade: 'all', lazy: false // we never want proxies for this
    }

    static transients = Content.transients

    static editors = {
        template(group:'extra')
    }
    
    Map getVersioningProperties() { 
       def r = super.getVersioningProperties() + [ 
           template:template?.ident() // Is this right?
       ] 
       return r
    }
}