package org.weceem.tags

import grails.util.Environment

import org.weceem.controllers.ContentController

class CacheTagLib {
    static namespace = "wcm"

    static MACRO_PATTERN = ~/\$\{(\S+)\}/
    
    static REQUEST_ATTRIBUTE_CURRENT_CACHE_SECTION = "org.weceem.tag.cache.current.section"
    
    static transactional = false
    
    def cacheService
    
    def cache = { attrs, body ->
        def cacheName = attrs.name ?: 'contentCache'
        if ((Environment.current != Environment.PRODUCTION) && params.refresh) {
            cacheService.clearCache(cacheName)
        } 
        def key = attrs.key
        if (!key) {
            def n = request[REQUEST_ATTRIBUTE_CURRENT_CACHE_SECTION]
            if (n == null) {
                request[REQUEST_ATTRIBUTE_CURRENT_CACHE_SECTION] = 0
            } else {
                request[REQUEST_ATTRIBUTE_CURRENT_CACHE_SECTION] += 1
            }
            key = request[ContentController.REQUEST_ATTRIBUTE_PAGE].URI+n
            if (log.warnEnabled) {
                log.warn "Auto-generating cache key for ${request[ContentController.REQUEST_ATTRIBUTE_PAGE].URI}"
            }
        }
        def content = cacheService.getOrPutValue(cacheName, key, body)
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