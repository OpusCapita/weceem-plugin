package org.weceem.tags

class AdminTagLib { 
    static namespace = "wcm"
 
    def userProfileEditUrl = { attrs ->
        def appUrl = grailsApplication.config.weceem.profile.url
        if (!appUrl) appUrl = [controller:'repository']
        out << g.createLink(url:appUrl)
    }

    def userLogOutUrl = { attrs ->
        def appUrl = grailsApplication.config.weceem.logout.url
        if (!appUrl) appUrl = [controller:'repository']
        out << g.createLink(url:appUrl)
    }
}