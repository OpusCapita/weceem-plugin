package org.weceem.tags

import grails.util.Environment

class CacheTagLib {
    static namespace = "wcm"

    static MACRO_PATTERN = ~/\$\{(\S+)\}/
    
    static transactional = false
    
    def cacheService
    
    def cache = { attrs, body ->
        def cacheName = attrs.name ?: 'contentCache'
        if ((Environment.current != Environment.PRODUCTION) && params.refresh) {
            cacheService.clearCache(cacheName)
        } 
        def content = cacheService.getOrPutValue(cacheName, attrs.key, body)
        def model = attrs.model
        if (model) {
             content = expandModel(content, model)
        }
        out << content
    }
    
    def expandModel(content, model) {
        def m = content =~ MACRO_PATTERN
        def sb = new StringBuffer()
        while (m.find()) {
            m.appendReplacement(sb, (model[m.group(1)] ?: '').encodeAsHTML() )
        }
        m.appendTail(sb)
        return sb
    }
}