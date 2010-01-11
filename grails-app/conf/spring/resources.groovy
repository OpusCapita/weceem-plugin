beans = {
    cacheManager(net.sf.ehcache.CacheManager) { bean -> 
        bean.destroyMethod = 'shutdown'
    }
}