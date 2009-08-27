package org.weceem.blog

import org.weceem.content.*

/**
 * Placeholder for blog settings etc, has BlogEntry children
 */
class Blog extends Content {

    Template template
    Integer maxEntriesToDisplay
    String commentMarkup = ""
    
    static constraints = {
        template(nullable: true)
        maxEntriesToDisplay(inList:[3, 5, 10, 20])
        commentMarkup(inList:["", "html", "wiki"])
    }
    
    static transients = Content.transients
    
    Map getVersioningProperties() { 
       def r = super.getVersioningProperties() + [ 
           template:template?.ident() // Is this right?
       ] 
       return r
    }
}