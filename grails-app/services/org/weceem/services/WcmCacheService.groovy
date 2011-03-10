package org.weceem.services

import org.springframework.beans.factory.InitializingBean
import net.sf.ehcache.Element
import net.sf.ehcache.Cache
import org.apache.commons.logging.LogFactory

class WcmCacheService implements InitializingBean {

    static log = LogFactory.getLog("grails.app.service."+WcmCacheService.class.name)
    
    static transactional = false
    
    def weceemCacheManager // From resources.groovy

    void afterPropertiesSet() {
        log.info "Caches configured: ${weceemCacheManager.cacheNames}"
    }
    
    def clearCache(String name, boolean dontTellReplicators = false) {
        getCache(name)?.removeAll(dontTellReplicators)
    }
    
    def removeValue(String cacheName, key, boolean dontTellReplicators = false) {
        getCache(cacheName)?.remove(key, dontTellReplicators)
    }
    
    def getCache(cacheOrName) {
        if (cacheOrName instanceof Cache) {
            return cacheOrName
        }
        
        def c = weceemCacheManager.getCache('weceem.'+cacheOrName)
        if (!c) {
            log.warn "Tried to get cache with name [$cacheOrName] but wasn't found - check ehcache.xml config"
        }
        return c
    }
    
    def getValue(cacheName, key) {
        getCache(cacheName).get(key)?.value
    }
    
    def getObjectValue(cacheName, key) {
        getCache(cacheName).get(key)?.objectValue
    }
    
    def getElement(cacheName, key) {
        getCache(cacheName).get(key)
    }
    
    def putValue(cacheName, key, value) {
        getCache(cacheName).put( new Element(key, value) )
        return value
    }

    def putToCache(Cache cache, key, value) {
        cache.put( new Element(key, value) )
        return value
    }
    
    def getOrPutObject(cache, key, objectCallable) {
        getOrPut(cache, true, key, objectCallable)
    }

    def getOrPutValue(cache, key, valueCallable) {
        getOrPut(cache, false, key, valueCallable)
    }
    
    def getOrPut(cache, boolean isObject, key, valueCallable) {
        def c = cache instanceof Cache ? cache : getCache(cache)
        if (log.debugEnabled) {
            log.debug "getOrPutValue to ${c.name} with key $key"
        }
        def elem = c.get(key)
        def v
        if (elem) {
            v = isObject ? elem.objectValue : elem.value
        }
        if (!v) {
            if (log.debugEnabled) {
                log.debug "getOrPut did not find key in the cache so generating value for $key in cache ${c.name}..."
            }
            v = valueCallable.call() // Call whatever we were given
            c.put( new Element(key, v) )
            if (log.debugEnabled) {
                log.debug "getOrPut stored generated value for $key in cache ${c.name}."
            }
        }
        return v
    }
}