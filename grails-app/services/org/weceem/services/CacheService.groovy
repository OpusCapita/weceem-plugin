package org.weceem.services

import org.springframework.beans.factory.InitializingBean
import net.sf.ehcache.Element
import org.apache.commons.logging.LogFactory

class CacheService implements InitializingBean {

    static log = LogFactory.getLog("grails.app.service."+CacheService.class.name)
    
    static transactional = false
    
    def cacheManager // From resources.groovy

    void afterPropertiesSet() {
        log.info "Caches configured: ${cacheManager.cacheNames}"
    }
    
    def clearCache(String name, boolean dontTellReplicators = false) {
        getCache(name)?.removeAll(dontTellReplicators)
    }
    
    def removeValue(String cacheName, key, boolean dontTellReplicators = false) {
        getCache(cacheName)?.remove(key, dontTellReplicators)
    }
    
    def getCache(String name) {
        def c = cacheManager.getCache(name)
        if (!c) {
            log.warn "Tried to get cache with name [$name] but wasn't found - check ehcache.xml config"
        }
        return c
    }
    
    def getValue(cacheName, key) {
        getCache(cacheName).get(key)?.value
    }
    
    def getElement(cacheName, key) {
        getCache(cacheName).get(key)
    }
    
    def putValue(cacheName, key, value) {
        getCache(cacheName).put( new Element(key, value) )
    }

    def getOrPutObject(cacheName, key, objectCallable) {
        getOrPut(cacheName, true, key, objectCallable)
    }

    def getOrPutValue(cacheName, key, valueCallable) {
        getOrPut(cacheName, false, key, objectCallable)
    }
    
    def getOrPut(cacheName, boolean isObject, key, valueCallable) {
        if (log.debugEnabled) {
            log.debug "getOrPutValue to $cacheName with key $key"
        }
        def c = getCache(cacheName)
        def elem = c.get(key)
        def v
        if (elem) {
            v = isObject ? elem.objectValue : elem.value
        }
        if (!v) {
            if (log.debugEnabled) {
                log.debug "getOrPut did not find key in the cache so generating value for $key in cache $cacheName..."
            }
            v = valueCallable.call() // Call whatever we were given
            c.put( new Element(key, v) )
            if (log.debugEnabled) {
                log.debug "getOrPut stored generated value for $key in cache $cacheName."
            }
        }
        return v
    }
}