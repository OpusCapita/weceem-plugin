package org.weceem.tags

class WorkaroundsTagLib {
    static namespace = "wcm"
    
    // This is a workaround for pluginContextPath being wrong in a layout supplied by a plugin and used by an app
    // We need this for the admin layout to work when embedded app admin pages are supplied
    def pluginCtxPath = { attrs ->
        throw new RuntimeException("pluginCtxPath tag is now obsolete with Grails 1.2RC2")
    }
}
