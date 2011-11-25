package org.weceem.blog

import org.weceem.content.*

/**
 * Placeholder for blog settings etc, has WcmBlogEntry children
 */
class WcmBlog extends WcmContent {

    WcmTemplate template
    Integer maxEntriesToDisplay
    String commentMarkup = ""
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "blog-32.png"]

    @Override
    String getHardDependencies() {
        // A template is an implicit dependency for the node, any changes to the template or its deps
        // means we have to change too.
        def t = TemplateUtils.getTemplateForContent(this)
        return t ? t.absoluteURI : ''
    }

    static searchable = {
        alias WcmBlog.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }
    
    Map getVersioningProperties() { 
        def r = super.getVersioningProperties() + [ 
            maxEntriesToDisplay:maxEntriesToDisplay,
            commentMarkup:commentMarkup,
            template:template?.ident() // Is this right?
        ] 
        return r
    }

    static constraints = {
        template(nullable: true)
        maxEntriesToDisplay(min:1, max:50)
        commentMarkup(inList:["", "html", "wiki"])
    }
    
    static mapping = {
        template cascade: 'all', lazy: false // we never want proxies for this
    }

    static transients = WcmContent.transients

    static editors = {
        commentMarkup(editor:'SelectInList')
        template(group:'extra')
    }

}