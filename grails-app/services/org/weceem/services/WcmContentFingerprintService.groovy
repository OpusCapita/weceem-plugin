package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.ConcurrentHashMap

import org.weceem.content.WcmSpace
import org.weceem.content.WcmContent
import org.weceem.content.WcmTemplate

/**
 * Service that is responsible for caching and calculating fingerprints of individual content nodes
 * and also "tree" fingerprints representing the fingerprint of all the descendents of a node.
 * This information is used in the ETag handling and relies heavily on the dependency service.
 *
 * NOTE: This is really complicated. The issues around cyclic node dependencies and avoiding 
 * calculating hashes that change during their own calculation (e.g. tree hashes) hurt your brain.
 * Do not change any of this without fully understanding it.
 * 
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class WcmContentFingerprintService implements InitializingBean {
    
    static transactional = true

    static CACHE_NAME_CONTENT_FINGERPRINT_CACHE = "contentFingerprintCache"
    static CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE = "contentTreeFingerprintCache"

    def wcmCacheService
    def wcmContentDependencyService
    def grailsApplication

    ThreadLocal fingerprintingContext = new ThreadLocal()
    
    /* We populate this ourselves to work around circular dependencies */
    @Lazy
    def wcmContentRepositoryService = { 
        def s = grailsApplication.mainContext.wcmContentRepositoryService
        return s
    }()

    // Both these caches are space-safe, cached on id
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

    private startFingerprinting(contentOrURI) {
        def ctx = fingerprintingContext.get()
        if (!ctx) {
            fingerprintingContext.set([
                uri:contentOrURI instanceof WcmContent ? contentOrURI.absoluteURI : contentOrURI,  
                hashes:[:], 
                treeHashes:[:], 
                refCount:1,
                hashCallCounter:0,
                treeHashCallCounter:0])
        } else {
            ctx.refCount++
        }
    }

    private stopFingerprinting() {
        def ctx = fingerprintingContext.get()
        if (ctx.refCount == 1) {
            if (log.infoEnabled) {
                log.info "Finished fingerprinting run for ${ctx.uri}, "+
                    "${ctx.hashes.size()} (of ${ctx.hashCallCounter} calls) node hashes "+
                    "and ${ctx.treeHashes.size()} (of ${ctx.treeHashCallCounter} calls) tree hashes calculated"
            }
            if (log.debugEnabled) {
                log.debug "The fingerprinting run for ${ctx.uri} hashed the following nodes: "+
                    "${ctx.hashes.keySet()} and the following trees ${ctx.treeHashes.keySet()}."
            }
            fingerprintingContext.set(null)
        } else {
            ctx.refCount--
        }
    }

    /**
     * This method must ONLY update the fingerprint for this one node. This means:
     * - no side effects should cause other nodes to update i.e. no getFingerprintFor
     * - any dependent nodes FPs must only be updated at the end of the method
     */
    synchronized updateFingerprintFor(WcmContent content, Set<WcmContent> alreadyVisited = []) {
        // Setup a cache for the duration of this processing
        startFingerprinting(content)
        try {
            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI}"
            }
            def currentETag = wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())

            // See if the content node has any dependencies itself, these need to be included
            def nodesNeedingRecalculation = []

            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI} - current is: ${currentETag}"
            }
            def newETag = calculateDeepFingerprintFor(content)
            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI} - new is: ${newETag}"
            }

            if (currentETag == newETag) {
                return currentETag // nothing to do
            }
        
            currentETag = newETag
        
            // Update the cache
            wcmCacheService.putToCache(contentFingerprintCache, content.ident(), currentETag)

            // **** @todo Now we must invalidate the tree fingerprints on all our ancestors
            // @todo we must not update if the etag has not changed
            if (content.parent) {
                def p = content
                while (p = p.parent) {
                    // This causes a lot of processing of nodes that haven't changed, but is necessary as we know
                    // as least some part of the tree has.
                    updateTreeHashForDescendentsOf(p)
                }
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
            alreadyVisited << content
            updateFingerprintsForAllDependencies(nodesNeedingRecalculation, alreadyVisited)
            return currentETag
        } finally {
            stopFingerprinting()
        }
    }

    /**
     * Update fingerprints for a dependency list, without recursing into their dependencies
     * The dependency list will usually include all the transitive depenencies, and this prevents stack overflow
     */
    protected synchronized updateFingerprintsForAllDependencies(nodesNeedingRecalculation, Set<WcmContent> alreadyVisited) {
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
    
    protected def invalidateTreeHashForDescendentsOf(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Invalidating tree hash for descendents of ${content.absoluteURI}"
        }
        wcmCacheService.removeValue(CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE, content.ident())
    }
    
    protected updateTreeHashForDescendentsOf(WcmContent content) {
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
    protected calculateShallowFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.withPermissionsBypass {
            wcmContentRepositoryService.findContentForPath(uri, space)?.content
        }
        return c.calculateFingerprint()
    }
    
    /**
     * Locate a node and ask it for its individual content fingerprint
     */
    protected calculateShallowFingerprintFor(WcmContent content) {
        def ctx = fingerprintingContext.get()
        ctx.hashCallCounter++
        def fp = ctx.hashes[content.absoluteURI]
        if (!fp) {
            fp = content.calculateFingerprint()            
            ctx.hashes[content.absoluteURI] = fp
        }
        return fp
    }
    
    /**
     * Locate a node and ask it for its dependency's content fingerprint, handling any cyclic refs
     */
    protected calculateDeepFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.withPermissionsBypass {
            wcmContentRepositoryService.findContentForPath(uri, space)?.content
        }
        return calculateDeepFingerprintFor(c)
    }
    
    /**
     * Locate a node and ask it for its dependency's content fingerprint, handling any cyclic refs
     */
    protected calculateDeepFingerprintFor(WcmContent content, List<WcmContent> alreadyVisited = []) {
        if (log.debugEnabled) {
            log.debug "Calculating deep fingerprint for content ${content.absoluteURI}"
        }
        
        // Prevent circulars
        alreadyVisited << content

        // Get deps
        def deps = wcmContentDependencyService.getDependenciesOf(content) - alreadyVisited
        
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
    protected calculateFingerprintForDescendentsOf(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.withPermissionsBypass {
            wcmContentRepositoryService.findContentForPath(uri, space)?.content
        }
        return calculateFingerprintForDescendentsOf(c)
    }
    
    /** 
     * Calculate the concatenated fingerprint for all descendents of the specified node
     */
    protected calculateFingerprintForDescendentsOf(WcmContent content) {
        def ctx = fingerprintingContext.get()
        ctx.treeHashCallCounter++
        def fp = ctx.treeHashes[content.absoluteURI]
        if (!fp) {
            def hashes = wcmContentRepositoryService.withPermissionsBypass {
                wcmContentRepositoryService.findDescendents(content).collect { c -> getFingerprintFor(c) }
            }
            fp = hashes.join(':').encodeAsSHA256()
            if (log.debugEnabled) {
                log.debug "Updating fingerprint for descendents of ${content.absoluteURI}, new value from $hashes is ${fp}"
            }
            ctx.treeHashes[content.absoluteURI] = fp
        }
        return fp
    }
    
    /** 
     * Public method to get the fingerprint for a given node
     */
    synchronized getFingerprintFor(WcmContent content, boolean updateIfMissing = true) {        
        startFingerprinting(content)
        try {
            if (log.debugEnabled) {
                log.debug "Getting fingerprint for content ${content.absoluteURI}"
            }
            def v = wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())
            if (!v && updateIfMissing) {
                return updateFingerprintFor(content)
            } else {
                return v
            }
        } finally {
            stopFingerprinting()
        }
    }
    
    protected invalidateFingerprintFor(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Invalidating fingerprint for content ${content.absoluteURI}"
        }
        wcmCacheService.removeValue(CACHE_NAME_CONTENT_FINGERPRINT_CACHE, content.ident())
    }
    
    synchronized getTreeHashForDescendentsOf(String uri, WcmSpace space) {
        startFingerprinting(uri)
        try {
            def c = wcmContentRepositoryService.withPermissionsBypass {
                wcmContentRepositoryService.findContentForPath(uri, space)?.content
            }
            return c ? getTreeHashForDescendentsOf(c) : ''
        } finally {
            stopFingerprinting()
        }
    }
    
    synchronized getTreeHashForDescendentsOf(WcmContent content, boolean updateIfMissing = true) {
        startFingerprinting(content)
        try {
            if (log.debugEnabled) {
                log.debug "Getting fingerprint for descendents of content ${content.absoluteURI}"
            }
            def v = wcmCacheService.getObjectValue(contentTreeFingerprintCache, content.ident())
            if (!v && updateIfMissing) {
                return updateTreeHashForDescendentsOf(content)
            } else {
                return v
            }
        } finally {
            stopFingerprinting()
        }
    }
    
    
}