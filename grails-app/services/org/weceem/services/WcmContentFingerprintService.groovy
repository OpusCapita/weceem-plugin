package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.ConcurrentHashMap

import org.weceem.content.WcmSpace
import org.weceem.content.WcmContent
import org.weceem.content.WcmTemplate

class WcmContentFingerprintService implements InitializingBean {
    
    static transactional = true

    static CACHE_NAME_CONTENT_FINGERPRINT_CACHE = "contentFingerprintCache"
    static CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE = "contentTreeFingerprintCache"

    def wcmCacheService
    def wcmContentDependencyService
    def grailsApplication
    
    /* We populate this ourselves to work around circular dependencies */
    @Lazy
    def wcmContentRepositoryService = { 
        def s = grailsApplication.mainContext.wcmContentRepositoryService
        return s
    }()

    def contentFingerprintCache
    def contentTreeFingerprintCache

    void afterPropertiesSet() {
        contentFingerprintCache = wcmCacheService.getCache(CACHE_NAME_CONTENT_FINGERPRINT_CACHE)
        assert contentFingerprintCache
        
        contentTreeFingerprintCache = wcmCacheService.getCache(CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE)
        assert contentTreeFingerprintCache
    }
    
    void reset() {
        contentFingerprintCache.removeAll()
        contentTreeFingerprintCache.removeAll()
    }

    void dumpFingerprintInfo(boolean stdout = false) {
        def out = stdout ? { println it } : { log.debug it }
        out "Content node fingerprints:"
        contentFingerprintCache.keys.each { k ->
            def n = WcmContent.get(k).absoluteURI
            out "$n ---> ${wcmCacheService.getObjectValue(contentFingerprintCache, k)}"
        }
        out "Content tree fingerprints:"
        contentTreeFingerprintCache.keys.each { k ->
            def n = WcmContent.get(k).absoluteURI
            out "$n ---> ${wcmCacheService.getObjectValue(contentTreeFingerprintCache, k)}"
        }
    }
    
    /**
     * This method must ONLY update the fingerprint for this one node. This means:
     * - no side effects should cause other nodes to update i.e. no getFingerprintFor
     * - any dependent nodes FPs must only be updated at the end of the method
     */
    def updateFingerprintFor(WcmContent content, Set<WcmContent> alreadyVisited = []) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for content ${content.absoluteURI}"
        }
        def currentETag

        // See if the content node has any dependencies itself, these need to be included
        def nodesNeedingRecalculation = []

        if (log.debugEnabled) {
            log.debug "Updating fingerprint for content ${content.absoluteURI} - current is: ${wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())}"
        }
        currentETag = calculateDeepFingerprintFor(content)
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for content ${content.absoluteURI} - new is: ${currentETag}"
        }

        // Update the cache
        wcmCacheService.putToCache(contentFingerprintCache, content.ident(), currentETag)

        // **** @todo Now we must invalidate the tree fingerprints on all our ancestors
        // @todo we must not update if the etag has not changed
        if (content.parent) {
            // This causes a lot of processing of nodes that haven't changed, but is necessary as we know
            // as least some part of the tree has.
            updateTreeHashForDescendentsOf(content.parent)
        }
        
        // **** Now we are past this point, we can update nodes that DEPEND on this node *****
        
        // Also must recalculate etags on all nodes that use nodes that depend on THIS node
        if (log.debugEnabled) {
            log.debug "Checking to see if this content ${content.absoluteURI} has content dependent on it, for which we need to update their fingerprints..."
        }
        wcmContentDependencyService.getContentDependentOn(content).each { c -> 
            def id = c.ident()
            if (id == content.ident()) {
                    return // no stack overflow thanks. We can't depend on self, and we don't want to process the template either
            }

            if (log.debugEnabled) {
                log.debug "Found content ${c.absoluteURI} which needs a new fingerprint due to recalculation of fingerprint of ${content.absoluteURI}"
            }
            nodesNeedingRecalculation << c
        }
        
        // Need to recalculate for this and all nodes each of these depend on (recursively)
        println "Nodes that need to be recalculated now: "+nodesNeedingRecalculation
        alreadyVisited << content
        updateFingerprintsForAllDependencies(nodesNeedingRecalculation, alreadyVisited)
        return currentETag
    }

    /**
     * Update fingerprints for a dependency list, without recursing into their dependencies
     * The dependency list will usually include all the transitive depenencies, and this prevents stack overflow
     */
    def updateFingerprintsForAllDependencies(nodesNeedingRecalculation, Set<WcmContent> alreadyVisited) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprints for all of ${nodesNeedingRecalculation*.absoluteURI} "
        }
        nodesNeedingRecalculation.each { n ->   
            if (!alreadyVisited.contains(n)) {
                updateFingerprintFor(n, alreadyVisited)
            } else {
                alreadyVisited << n
            }
        }
    }
    
    def invalidateTreeHashForDescendentsOf(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Invalidating tree hash for descendents of ${content.absoluteURI}"
        }
        wcmCacheService.removeValue(CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE, content.ident())
    }
    
    def updateTreeHashForDescendentsOf(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for descendents of ${content.absoluteURI}, current is ${getTreeHashForDescendentsOf(content, false)}"
        }
        def fingerprint = calculateFingerprintForDescendentsOf(content)
        wcmCacheService.putToCache(contentTreeFingerprintCache, content.ident(), fingerprint )
        return fingerprint
    }

    /**
     * Locate a node and ask it for its individual content fingerprint
     */
    def calculateShallowFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return c.calculateFingerprint()
    }
    
    /**
     * Locate a node and ask it for its individual content fingerprint
     */
    def calculateShallowFingerprintFor(WcmContent content) {
        return content.calculateFingerprint()
    }
    
    /**
     * Locate a node and ask it for its dependency's content fingerprint, handling any cyclic refs
     */
    def calculateDeepFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return calculateDeepFingerprintFor(c)
    }
    
    /**
     * Locate a node and ask it for its dependency's content fingerprint, handling any cyclic refs
     */
    def calculateDeepFingerprintFor(WcmContent content, List<WcmContent> alreadyVisited = []) {
        if (log.debugEnabled) {
            log.debug "Calculating deep fingerprint for content ${content.absoluteURI}"
        }
        
        // Prevent circulars
        alreadyVisited << content

        // Get deps
        def deps = wcmContentDependencyService.getDependenciesOf(content)
        
        def nonCyclicDeps = deps.findAll { 
            // Non-cylic dependencies are those that do not have any dependency on any of the nodes we've already processed
            wcmContentDependencyService.getDependenciesOf(it).disjoint(alreadyVisited) 
        }
        def cyclicDeps = deps - nonCyclicDeps
        if (log.debugEnabled) {
            log.debug "Calculating deep fingerprint for content ${content.absoluteURI}, noncylic: ${nonCyclicDeps*.absoluteURI} cyclic: ${cyclicDeps*.absoluteURI}"
        }

        
        // Get DFP of each dep that does not depend on content
        def nonCyclicFP = nonCyclicDeps.collect({ calculateDeepFingerprintFor(it, alreadyVisited) }).join(':')

        // Get SFP of nodes that depend on content
        def cyclicFP = cyclicDeps.collect({ calculateShallowFingerprintFor(it) }).join(':')
        def nodeFP = calculateShallowFingerprintFor(content)
        if (log.debugEnabled) {
            log.debug "Calculating deep fingerprint for content ${content.absoluteURI}, noncylic FP: ${nonCyclicFP} cyclic FP: ${cyclicFP} node FP: ${nodeFP}"
        }
        return [nodeFP, nonCyclicFP, cyclicFP].join(':').encodeAsSHA256()
    }
    
    /** 
     * Calculate the concatenated fingerprint for all descendents of the specified node
     */
    def calculateFingerprintForDescendentsOf(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return calculateFingerprintForDescendentsOf(c)
    }
    
    /** 
     * Calculate the concatenated fingerprint for all descendents of the specified node
     */
    def calculateFingerprintForDescendentsOf(WcmContent content) {
        def hashes = wcmContentRepositoryService.findDescendents(content).collect { c -> getFingerprintFor(c) }
        def fp = hashes.join('').encodeAsSHA256()
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for descendents of ${content.absoluteURI}, new value from $hashes is ${fp}"
        }
        return fp
    }
    
    def getFingerprintFor(WcmContent content, boolean updateIfMissing = true) {
        if (log.debugEnabled) {
            log.debug "Getting fingerprint for content ${content.absoluteURI}"
        }
        def v = wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())
        if (!v && updateIfMissing) {
            return updateFingerprintFor(content)
        } else {
            return v
        }
    }
    
    def invalidateFingerprintFor(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Invalidating fingerprint for content ${content.absoluteURI}"
        }
        wcmCacheService.removeValue(CACHE_NAME_CONTENT_FINGERPRINT_CACHE, content.ident())
    }
    
    def getTreeHashForDescendentsOf(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return c ? getTreeHashForDescendentsOf(c) : ''
    }
    
    def getTreeHashForDescendentsOf(WcmContent content, boolean updateIfMissing = true) {
        if (log.debugEnabled) {
            log.debug "Getting fingerprint for descendents of content ${content.absoluteURI}"
        }
        def v = wcmCacheService.getObjectValue(contentTreeFingerprintCache, content.ident())
        if (!v && updateIfMissing) {
            return updateTreeHashForDescendentsOf(content)
        } else {
            return v
        }
    }
    
    
}