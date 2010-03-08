package org.weceem.tags

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class AdminTagLib { 
    static namespace = "wcm"
 
    def userProfileEditUrl = { attrs ->
        def appUrl = grailsApplication.config.weceem.profile.url
        if (!appUrl) appUrl = [controller:'wcmRepository']
        out << g.createLink(url:appUrl)
    }

    def userLogOutUrl = { attrs ->
        def appUrl = grailsApplication.config.weceem.logout.url
        if (!appUrl) appUrl = [controller:'wcmRepository']
        out << g.createLink(url:appUrl)
    }
    
    def adminLayout = { attrs ->
        def layoutSetting = ConfigurationHolder.config.weceem.admin.layout
        def layoutName = layoutSetting instanceof String ? layoutSetting : 'weceemadmin'
        out << layoutName
    }
}