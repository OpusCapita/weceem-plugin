package org.weceem.services

import java.util.concurrent.ConcurrentHashMap

import org.weceem.content.WcmSpace
import org.weceem.content.WcmContent
import org.weceem.content.WcmTemplate

class WcmContentDependencyService {
    
    static transactional = true

    def grailsApplication
    
    /* We populate this ourselves to work around circular dependencies */
    @Lazy
    def wcmContentRepositoryService = { 
        def s = grailsApplication.mainContext.wcmContentRepositoryService
        return s
    }()

    // This is keyed on URIs, so needs space id namespacing or multiple caches
    Map contentDependenciesBySpace = new ConcurrentHashMap()

    void reset() {
        contentDependenciesBySpace.clear()
        
        reload()
    }

    /**
     * Reload all the depenency info for one or all spaces
     */
    void reload(WcmSpace space = null) {
        if (space) {
            contentDependenciesBySpace.remove(space.ident())
        } else {
            contentDependenciesBySpace.clear()
        }
        
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
            def instances = space ? dc.findAllBySpace(space) : dc.list()
            instances.each { content ->
                if (log.debugEnabled) {
                    log.debug "Content instance ${content.absoluteURI} dependency info being loaded..."
                }
                updateDependencyInfoFor(content)
            }
        }
    }
    
    /**
     * Get the list of string node paths that the specified node explicitly depends on, as well as any special
     * implicit dependencies e.g. Template. This does not recurse, it gather the info for this node only, unless
     * you pass in "true" for "recurse"
     *
     */
    List<String> getDependencyPathsOf(WcmContent content, Boolean recurse = false) {
        if (recurse) {
            HashSet<String> results = []
            HashSet<String> visitedNodes = []
            
            recurseDependencyPathsOf(content, results, visitedNodes)
            return results as List<WcmContent>
        } else {
            return extractDependencyPathsOf(content) as List<WcmContent>
        }
    }
    
    void recurseDependencyPathsOf(WcmContent content, Set<String> results, Set<String> alreadyVisited) {
        def contentURI = content.absoluteURI
        alreadyVisited << contentURI

        def deps = extractDependencyPathsOf(content)
        deps.each {
            if (!alreadyVisited.contains(it)) {
                results << it
            }
        }
        
        deps.each { d ->
            def nodes = resolveDependencyPathToNodes(d, content.space)
            nodes.each { n ->
                // Prevent stackoverflow
                def nURI = n.absoluteURI
                if (!alreadyVisited.contains(nURI)) {
                    recurseDependencyPathsOf( n, results, alreadyVisited)
                }
            }
        }
    }
    
    protected List<String> extractDependencyPathsOf(WcmContent content) {
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
        List l = []
        gatherDependenciesOf(content, l)
        // Remove us
        l.removeAll(content)        
        return l
    }
    
    protected List<WcmContent> resolveDependencyPathToNodes(String path, WcmSpace space) {
        def u = path.trim()
        if (u.endsWith('/**')) {
            def c = wcmContentRepositoryService.findContentForPath(u - '/**', space) 
            if (c?.content) {
                return wcmContentRepositoryService.findDescendents(c.content)
            } else {
                if (log.debugEnabled) {
                    log.debug "Attempt to resolve dependencies ${path} which describes no nodes"
                }
            }
        } else {
            def c = wcmContentRepositoryService.findContentForPath(u, space) 
            if (c?.content) {
                return [c.content]
            }
        }
        return []
    }

    protected void gatherDependenciesOf(WcmContent content, List results) {
        def deps = getDependencyPathsOf(content)
        if (deps) {
            deps.each { uri -> 
                def nodes = resolveDependencyPathToNodes(uri, content.space)

                // Filter out results we already have
                def localResults = nodes.findAll { n -> 
                    !results.find { c -> 
                        c.ident() == n.ident()
                    }
                }
                // De-dupe any to prevent extra work / loops
                localResults = localResults.unique()
                
                results.addAll( localResults)
                localResults.each { n ->
                    gatherDependenciesOf(n, results)
                }

            }
            if (log.debugEnabled) {
                log.debug "Gathered dependencies of ${content.absoluteURI} - results ${results*.absoluteURI}"
            }
        }
    }
    
    def removeDependencyInfoFor(WcmContent content) {
        def id = content.ident()
        def depInfo = getDependencyCacheForSpace(content.space)
        synchronized (depInfo) {
            def keys = depInfo.keySet()
            keys.each { k ->
                depInfo[k] = depInfo[k].findAll { n -> n != id } // can do minus?
            }
        }
    }
    
    def updateDependencyInfoFor(WcmContent content) {
        if (log.debugEnabled) {
            log.debug "Updating dependency info for: ${content.absoluteURI}"
        }
        removeDependencyInfoFor(content)
        def deps = getDependencyPathsOf(content, true)
        
        def depInfo = getDependencyCacheForSpace(content.space)
        def dependerId = content.ident()
        deps.each { uri ->
            if (log.debugEnabled) {
                log.debug "Storing dependency: ${content.absoluteURI} depends on $uri"
            }
            // We treat /** and normal uris the same
            def depsList = depInfo[uri]
            if (depsList == null) {
                depsList = []
                depInfo[uri] = depsList
            }
            if (!depsList.contains(dependerId)) {
                depsList << dependerId
            }
        }
    }
    
    void dumpDependencyInfo(boolean stdout = false) {
        def out = stdout ? { println it } : { log.debug it }
        out "Content dependencies:" 
        contentDependenciesBySpace.each { spaceId, depInfo ->
            out "Space: $spaceId" 
            depInfo.each { k, v ->
                def ns = v.collect { id -> 
                    def c = WcmContent.get(id)
                    return c ? c.absoluteURI : '?not found?'
                }
                out "$k --- is dependency of nodes with ids ---> ${ns}"
            }
        }
    }
    
    /** 
     * Get a flattened list of all the content nodes dependent on "content"
     * This includes implicit dependencies e.g. inherited templates on deeply nested nodes
     */
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
        def depInfo = getDependencyCacheForSpace(content.space)
        def dependents = depInfo[u]
        if (dependents == null) {
            dependents = []
        }
        
        // go up parent uris and check for /** at any depth
        // dependents of X = sum of all explicit and ancestral dependents on X
        if (u.indexOf('/') >= 0) {
            def lastSlash
            while ((lastSlash = u.lastIndexOf('/')) > 0) {
                def wildcardURL = u[0..lastSlash]+'**'
                def deps = depInfo[wildcardURL]
                if (deps) {
                    dependents.addAll(deps)
                }
                u = u[0..lastSlash-1]
            }
        }
        if (dependents) {
            // Make sure we are not circular
            dependents.remove(content.ident())
            return WcmContent.getAll(dependents)
        } else return []
    }
    
    Map<String, List> getDependencyCacheForSpace(WcmSpace space) {
        def id = space.ident()
        contentDependenciesBySpace.putIfAbsent(id, new HashMap<String, List>())
        return contentDependenciesBySpace[id]
    }
       
}
