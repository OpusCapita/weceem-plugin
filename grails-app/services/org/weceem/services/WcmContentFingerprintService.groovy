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
     * This invalidates dependent fingerprints so that they can be recalculated without stack overflow
     */
    synchronized updateFingerprintFor(WcmContent content, Set<WcmContent> alreadyVisited = []) {
        // Setup a cache for the duration of this processing
        startFingerprinting(content)
        try {
            def result = doUpdateFingerprintFor(content, alreadyVisited)
            return result
        } finally {
            stopFingerprinting()
        }
    }

    /**
     * This method must ONLY update the fingerprint for this one node. This means:
     * - no side effects should cause other nodes to update i.e. no getFingerprintFor
     * - any dependent nodes FPs must only be updated at the end of the method
     */
    synchronized doUpdateFingerprintFor(WcmContent content, Set<WcmContent> alreadyVisited = []) {
        // Setup a cache for the duration of this processing
        startFingerprinting(content)
        try {
            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI}"
            }
            def currentETag = wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())

            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI} - current is: ${currentETag}"
            }
            def newETag = calculateDeepFingerprintFor(content)
            if (log.debugEnabled) {
                log.debug "Updating fingerprint for content ${content.absoluteURI} - new is: ${newETag}"
            }

            if (currentETag == newETag) {
                return [ETag:currentETag, changed: false] // nothing to do, let the caller know nothing changed
            }
        
            currentETag = newETag
            // Need to recalculate for this and all nodes each of   these depend on (recursively)
            alreadyVisited << content
        
            // Update the cache
            wcmCacheService.putToCache(contentFingerprintCache, content.ident(), currentETag)

            invalidateFingerPrintsForAll(findNodesDependentOn(content))

            return [ETag:currentETag, changed: true]
        } finally {
            stopFingerprinting()
        }
    }

    List findNodesDependentOn(WcmContent content) {
        def nodes = []

        wcmContentDependencyService.getContentDependentOn(content).each { c -> 
            def id = c.ident()
            if (id == content.ident()) {
                    return // no stack overflow thanks. We can't depend on self, and we don't want to process the template either
            }

            if (log.debugEnabled) {
                log.debug "Found content ${c.absoluteURI} which needs a new fingerprint due to recalculation of fingerprint of ${content.absoluteURI}"
            }
            nodes << c
        }
        
        return nodes
    }

    void invalidateFingerPrintsForAll(List<WcmContent> content) {
        if (log.debugEnabled) {
            log.debug "Invalidating hashes for all of of ${content*.absoluteURI}"
        }
        content.each { n ->
            invalidateFingerprintFor(n)
        }
    }

    void updateAllFingerprintsDependentOn(WcmContent content, List<WcmContent> alreadyVisited = []) {
    
        // Also must recalculate etags on all nodes that use nodes that depend on THIS node
        if (log.debugEnabled) {
            log.debug "Checking to see if this content ${content.absoluteURI} has content dependent on it, for which we need to update their fingerprints..."
        }
        // See if the content node has any dependencies itself, these need to be included
        def nodesNeedingRecalculation = findNodesDependentOn(content)
        
        updateFingerprintsForAllDependencies(nodesNeedingRecalculation, alreadyVisited)
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
     * Public method to get the fingerprint for a given node
     */
    synchronized getFingerprintFor(WcmContent content, boolean updateIfMissing = true) {        
        doGetFingerprintFor(content, updateIfMissing, true)
    }
    
    synchronized protected doGetFingerprintFor(WcmContent content, boolean updateIfMissing, boolean invalidateDependents) {        
        startFingerprinting(content)
        try {
            if (log.debugEnabled) {
                log.debug "Getting fingerprint for content ${content.absoluteURI}"
            }
            def v = wcmCacheService.getObjectValue(contentFingerprintCache, content.ident())
            if (!v && updateIfMissing) {
                def res = invalidateDependents ? updateFingerprintFor(content) : doUpdateFingerprintFor(content)
                return res.ETag
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
            
}