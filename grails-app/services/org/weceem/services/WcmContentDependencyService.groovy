package org.weceem.services

import java.util.concurrent.ConcurrentHashMap

import org.weceem.content.WcmSpace
import org.weceem.content.WcmContent
import org.weceem.content.WcmTemplate

class WcmContentDependencyService {
    
    static transactional = true

    static CACHE_NAME_CONTENT_FINGERPRINT_CACHE = "contentFingerprintCache"
    static CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE = "contentTreeFingerprintCache"

    def wcmCacheService
    def grailsApplication
    
    /* We populate this ourselves to work around circular dependencies */
    @Lazy
    def wcmContentRepositoryService = { 
        def s = grailsApplication.mainContext.wcmContentRepositoryService
        return s
    }()

    def contentFingerprintCache
    def contentTreeFingerprintCache
    Map contentDependencyInfo = new ConcurrentHashMap()

    void init() {
        contentFingerprintCache = wcmCacheService.getCache(CACHE_NAME_CONTENT_FINGERPRINT_CACHE)
        assert contentFingerprintCache
        contentFingerprintCache.removeAll()
        
        contentTreeFingerprintCache = wcmCacheService.getCache(CACHE_NAME_CONTENT_TREE_FINGERPRINT_CACHE)
        assert contentTreeFingerprintCache
        contentTreeFingerprintCache.removeAll()

        contentDependencyInfo.clear()
        
        reload()
    }

    void reload() {
        // Find all nodes with contentDependencies
        List<Class> classesWithDeps = grailsApplication.domainClasses.findAll { d -> d.clazz.metaClass.hasProperty(d, 'contentDependencies') }
        if (log.debugEnabled) {
            log.debug "Content classes with contentDependencies: ${classesWithDeps*.clazz}"
        }
        // Find all nodes with templates
        List<Class> classesWithTemplates = grailsApplication.domainClasses.findAll { d -> d.clazz.metaClass.hasProperty(d, 'template') }
        if (log.debugEnabled) {
            log.debug "Content classes with template: ${classesWithTemplates*.clazz}"
        }
        (classesWithDeps + classesWithTemplates).unique().clazz.each { dc ->
            if (log.debugEnabled) {
                log.debug "Loading content instances for ${dc} to load dependency info..."
            }
            dc.list().each { content ->
                if (log.debugEnabled) {
                    log.debug "Content instance ${content.absoluteURI} dependency info being loaded..."
                }
                updateDependencyInfoFor(content)
            }
        }
    }
    
    List<String> getDependencyPathsOf(WcmContent content) {
        WcmTemplate template = wcmContentRepositoryService.getTemplateForContent(content)
        // A template is an implicit dependency for the node, any changes to the template or its deps
        // means we have to change too.
        def results = template ? [template.absoluteURI] : []
    
        if (content.metaClass.hasProperty(content, 'contentDependencies')) {
            def deps = content.contentDependencies?.split(',')*.trim()
            if (deps) {
                results.addAll(deps)
            }
        }
        return results
    }
    
    List<WcmContent> getDependenciesOf(WcmContent content) {
        def deps = getDependencyPathsOf(content)
        if (deps) {
            def results = []
            deps.each { uri -> 
                def u = uri.trim()
                if (u.endsWith('/**')) {
                    def c = wcmContentRepositoryService.findContentForPath(u - '/**', content.space) 
                    if (c?.content) {
                        results.addAll(wcmContentRepositoryService.findDescendents(c.content))
                    } else {
                        println "BOOF! $c"
                        log.warn "Content ${content.absoluteURI} depends on ${u} which describes no nodes"
                    }
                } else {
                    def c = wcmContentRepositoryService.findContentForPath(u, content.space) 
                    if (c?.content) {
                        results << c.content
                    }
                }
            }
            if (log.debugEnabled) {
                log.debug "Returning dependencies of ${content.absoluteURI}: ${results*.absoluteURI}"
            }
            return results
        }
        return Collections.EMPTY_LIST
    }
    
    def removeDependencyInfoFor(WcmContent content) {
        def id = content.ident()
        synchronized (contentDependencyInfo) {
            def keys = contentDependencyInfo.keySet()
            keys.each { k ->
                contentDependencyInfo[k] = contentDependencyInfo[k].findAll { n -> n != id }
            }
        }
    }
    
    def updateDependencyInfoFor(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Updating dependency info for: ${content.absoluteURI}"
        }
        removeDependencyInfoFor(content)
        def deps = getDependencyPathsOf(content)
        
        println "Deps: ${contentDependencyInfo}"
        def dependerId = content.ident()
        deps.each { uri ->
            if (log.debugEnabled) {
                log.debug "Storing dependency: ${content.absoluteURI} depends on $uri"
            }
            // We treat /** and normal uris the same
            def depsList = contentDependencyInfo[uri]
            if (depsList == null) {
                depsList = []
                contentDependencyInfo[uri] = depsList
            }
            if (!depsList.contains(dependerId)) {
                depsList << dependerId
            }
        }
    }
    
    void dumpDependencyInfo(boolean stdout = false) {
        def out = stdout ? { println it } : { log.debug it }
        out "Content dependencies:" 
        contentDependencyInfo.each { k, v ->
            def ns = v.collect { id -> 
                def c = WcmContent.get(id)
                return c ? c.absoluteURI : '?not found?'
            }
            out "$k --- is dependency of nodes with ids ---> ${ns}"
        }
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
    
    def getContentDependentOn(WcmContent content) {
        /*
        
        index
        products
        blog/**
        faq/**
        widget/
        widget/news
        
        For widget/news >>> deps blog/**
        
        So /blog or /blog/xxxx or /blog/xxxx/comment-4 would return widget/news
        
        blog/** -> [widget/news, templates/homepage]
        blog/ -> [widget/news, templates/homepage]
        // imaginary entry/..
        blog -> []
        
        */
        def u = content.absoluteURI
        def dependents = contentDependencyInfo[u]
        if (dependents == null) {
            dependents = []
        }
        
        // go up parent uris and check for /** at any depth
        // dependents of X = sum of all explicit and ancestral dependents on X
        if (u.indexOf('/') >= 0) {
            def lastSlash = u.lastIndexOf('/')
            if (lastSlash > 0) {
                def deps = contentDependencyInfo[u[0..lastSlash]+'**']
                if (deps) {
                    dependents.addAll(deps)
                }
            }
        }
        if (dependents) {
            return WcmContent.getAll(dependents)
        } else return dependents
    }
    
    def updateFingerprintFor(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for content ${content.absoluteURI}"
        }
        def currentETag
        WcmTemplate template = wcmContentRepositoryService.getTemplateForContent(content)

        // See if the content node has any dependencies itself, these need to be included
        def contentURI = content.absoluteURI
        def templateURI = template?.absoluteURI
        def contentDepPaths = getDependencyPathsOf(content)
        // Get all deps *except* template, we handle that in a special way
        def contentDepFingerprints = contentDepPaths.findAll({ uri -> uri != templateURI}).collect { uri ->
            if (uri.endsWith('/**')) {
                return getFingerprintForDescendentsOf(uri - '/**', content.space)
            } else {
                return getFingerprintFor(uri, content.space)
            }
        }
        boolean isTemplate = content instanceof WcmTemplate
        
        def aggregatedContentDepFingerPrints = contentDepFingerprints.join('')

        if (template) {
            // tag is tag of template plus content - template changes = reload the HTML content
            def templDepPaths = getDependencyPathsOf(template)
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
                        return getFingerprintForDescendentsOf(uri-'/**', content.space)
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
                        return getFingerprintFor(uri, content.space)
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
            
            // The template implicitly depends on us, so it needs to be recalculated, and that needs to ripple out to all nodes
            // that use the template
            invalidateFingerprintFor(template)
        } else {
            // No template, content controls it
            if (log.debugEnabled) {
                log.debug "Building fingerprint for non-templated content ${content.absoluteURI} using dep fingerprints: ${aggregatedContentDepFingerPrints}"
            }
            currentETag = (aggregatedContentDepFingerPrints+content.calculateFingerprint()).encodeAsSHA256()
        }

        wcmCacheService.putToCache(contentFingerprintCache, content.ident(), currentETag)

        // **** Now we must invalidate the tree fingerprints on all our ancestors
        if (content.parent) {
            // This may cause recursion
            updateFingerprintForDescendentsOf(content.parent)
        }
        
        // **** Now we are past this point, we can update nodes that DEPEND on this node *****
        
        // Also must recalculate etags on all nodes that use nodes that depend on THIS node
        if (log.debugEnabled) {
            log.debug "Checking to see if this content ${content.absoluteURI} has content dependent on it, for which we need to update their fingerprints..."
        }
        getContentDependentOn(content).each { c -> 
            def id = c.ident()
            if ((id == content.ident()) || 
                (id == template?.ident()) ) {
                    return // no stack overflow thanks. We can't depend on self, and we don't want to process the template either
            }
            if (log.debugEnabled) {
                log.debug "Found content ${c.absoluteURI} which needs a new fingerprint due to recalculation of fingerprint of ${content.absoluteURI}"
            }
            updateFingerprintFor(c) 
        }
        
        return currentETag
    }

    def updateFingerprintForDescendentsOf(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for descendents of ${content.absoluteURI}, current is ${getFingerprintForDescendentsOf(content, false)}"
        }
        def fingerprints = wcmContentRepositoryService.findDescendents(content).collect { c -> getFingerprintFor(c) }
        def fp = fingerprints.join('').encodeAsSHA256()
        if (log.debugEnabled) {
            log.debug "Updating fingerprint for descendents of ${content.absoluteURI}, new value from $fingerprints is ${fp}"
        }
        wcmCacheService.putToCache(contentTreeFingerprintCache, content.ident(), fp )
        return fp
    }

    def getFingerprintFor(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return c ? getFingerprintFor(c) : ''
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
    
    def getFingerprintForDescendentsOf(String uri, WcmSpace space) {
        def c = wcmContentRepositoryService.findContentForPath(uri, space)?.content
        return c ? getFingerprintForDescendentsOf(c) : ''
    }
    
    def getFingerprintForDescendentsOf(WcmContent content, boolean updateIfMissing = true) {
        if (log.debugEnabled) {
            log.debug "Getting fingerprint for descendents of content ${content.absoluteURI}"
        }
        def v = wcmCacheService.getObjectValue(contentTreeFingerprintCache, content.ident())
        if (!v && updateIfMissing) {
            return updateFingerprintForDescendentsOf(content)
        } else {
            return v
        }
    }
    
    
}