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
    def updateFingerprintFor(WcmContent content, Boolean updateDependents = true) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for content ${content.absoluteURI}"
        }
        def currentETag
        WcmTemplate template = wcmContentRepositoryService.getTemplateForContent(content)

        // See if the content node has any dependencies itself, these need to be included
        def contentURI = content.absoluteURI
        def templateURI = template?.absoluteURI
        def contentDepPaths = wcmContentDependencyService.getDependencyPathsOf(content, true)
        // Get all deps *except* template, we handle that in a special way
        def contentDepFingerprints = contentDepPaths.findAll({ uri -> uri != templateURI}).collect { uri ->
            if (uri.endsWith('/**')) {
                return getTreeHashForDescendentsOf(uri - '/**', content.space)
            } else {
                return calculateFingerprintFor(uri, content.space)
            }
        }
        boolean isTemplate = content instanceof WcmTemplate
        
        def nodesNeedingRecalculation = []

        def aggregatedContentDepFingerPrints = contentDepFingerprints.join(':')

        if (template) {
            // tag is tag of template plus content - template changes = reload the HTML content
            def templDepPaths = wcmContentDependencyService.getDependencyPathsOf(template, true)
            def templDepFingerprints = templDepPaths.collect { uri ->
                if (log.debugEnabled) {
                    log.debug "Checking template dependency $uri"
                }
                if (uri.endsWith('/**')) {
                    if (log.debugEnabled) {
                        log.debug "Template dependency $uri is recursive"
                    }
                    def u = uri - '**'
                    // If dep includes this current node we're processing, skip it to avoid recursion
                    if (!contentURI.startsWith(u)) {
                        if (log.debugEnabled) {
                            log.debug "Template dependency $uri does not encompass the node ${contentURI} so we can calculate tree fingerprint"
                        }
                        return calculateFingerprintForDescendentsOf(uri-'/**', content.space)
                    } else {
                        if (log.debugEnabled) {
                            log.debug "Template dependency $uri encompasses the node ${contentURI} so we will skip, we know part of it has changed as this node is under there"
                        }
                        return ''
                    }
                } else {
                    if (log.debugEnabled) {
                        log.debug "Template dependency $uri is not recursive"
                    }
                    // If dep includes this current node we're processing, skip it to avoid recursion
                    if (uri != contentURI) {
                        if (log.debugEnabled) {
                            log.debug "Template dependency $uri is not current node ${contentURI} so getting fingerprint"
                        }
                        return calculateFingerprintFor(uri, content.space)
                    } else {
                        if (log.debugEnabled) {
                            log.debug "Template dependency $uri is current node ${contentURI} so skipping"
                        }
                        return ''
                    }
                }
            }
            def templFp = templDepFingerprints.join('') + template.calculateFingerprint()
            
            if (log.debugEnabled) {
                log.debug "Building fingerprint for templated content ${content.absoluteURI} using dependency fingerprints: "+
                    "${aggregatedContentDepFingerPrints} and template fingerprint: $templFp"
            }
            currentETag = (aggregatedContentDepFingerPrints + templFp + content.calculateFingerprint()).encodeAsSHA256()

        } else {
            // No template, content controls it
            def fp = content.calculateFingerprint()
            if (log.debugEnabled) {
                log.debug "Building fingerprint for non-templated content ${content.absoluteURI} using dep fingerprints: ${aggregatedContentDepFingerPrints} and node FP $fp"
            }
            currentETag = (aggregatedContentDepFingerPrints+fp).encodeAsSHA256()
        }

        wcmCacheService.putToCache(contentFingerprintCache, content.ident(), currentETag)

        // **** @todo Now we must invalidate the tree fingerprints on all our ancestors
        if (content.parent) {
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
        if (updateDependents) {
            updateFingerprintsForAllDependencies(nodesNeedingRecalculation)
        }
        return currentETag
    }

    /**
     * Update fingerprints for a dependency list, without recursing into their dependencies
     * The dependency list will usually include all the transitive depenencies, and this prevents stack overflow
     */
    def updateFingerprintsForAllDependencies(nodesNeedingRecalculation) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprints for all of ${nodesNeedingRecalculation*.absoluteURI} "
        }
        nodesNeedingRecalculation.each { n ->   
            updateFingerprintFor(n, false)
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
    def calculateFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return c.calculateFingerprint()
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