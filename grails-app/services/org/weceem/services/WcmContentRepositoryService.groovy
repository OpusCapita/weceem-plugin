package org.weceem.services

import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.springframework.beans.factory.InitializingBean
import grails.util.Environment

// This is for a hack, remove later
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.hibernate.exception.ConstraintViolationException

import org.weceem.content.*

//@todo design smell!
import org.weceem.files.*
import org.weceem.script.WcmScript
import org.weceem.event.WeceemEvent

import org.weceem.security.*
import org.codehaus.groovy.grails.web.context.ServletContextHolder

/**
 * WcmContentRepositoryService class provides methods for WcmContent Repository tree
 * manipulations.
 * The service deals with WcmContent and subclasses of WcmContent classes.
 *
 * @author Sergei Shushkevich
 */
class WcmContentRepositoryService implements InitializingBean {

    static final String EMPTY_ALIAS_URI = "_ROOT"
    
    static final CONTENT_CLASS = WcmContent.class.name
    static final STATUS_ANY_PUBLISHED = 'published'
    
    static CACHE_NAME_GSP_CACHE = "gspCache"
    static CACHE_NAME_URI_TO_CONTENT_ID = "uriToContentCache"
    
    static transactional = true

    def uriToIdCache
    def gspClassCache
    
    def grailsApplication
    def wcmImportExportService
    def wcmCacheService
    def groovyPagesTemplateEngine
    def wcmSecurityService
    def wcmEventService
    
    def archivedStatusCode
    def unmoderatedStatusCode
    
    static uploadDir
    static uploadUrl
    static uploadInWebapp
    
    static DEFAULT_ARCHIVED_STATUS_CODE = 500
    static DEFAULT_UNMODERATED_STATUS_CODE = 150
    
    static DEFAULT_STATUSES = [
        [code:100, description:'draft', publicContent:false].asImmutable(),
        [code:DEFAULT_UNMODERATED_STATUS_CODE, description:'unmoderated', publicContent:false].asImmutable(),
        [code:200, description:'reviewed', publicContent:false].asImmutable(),
        [code:300, description:'approved', publicContent:false].asImmutable(),
        [code:400, description:'published', publicContent:true].asImmutable(),
        [code:DEFAULT_ARCHIVED_STATUS_CODE, description:'archived', publicContent:false].asImmutable()
    ].asImmutable()
    
    void afterPropertiesSet() {
        uriToIdCache = wcmCacheService.getCache(CACHE_NAME_URI_TO_CONTENT_ID)
        assert uriToIdCache

        gspClassCache = wcmCacheService.getCache(CACHE_NAME_GSP_CACHE)
        assert gspClassCache

        def wcmconf = grailsApplication.config.weceem
        def code = wcmconf?.archived.status
        archivedStatusCode = code instanceof Number ? code : DEFAULT_ARCHIVED_STATUS_CODE
        code = wcmconf?.umoderated.status
        unmoderatedStatusCode = code instanceof Number ? code : DEFAULT_UNMODERATED_STATUS_CODE
            
        loadConfig()
    }
    
    /**
     * Workaround for replaceAll problems with \ in Java
     */
    static String makeFileSystemPathFromURI(String uri) {
        if (uri == null) {
            return ''
        }
        def chars = uri.chars
        chars.eachWithIndex { c, i ->
            if (c == '/') {
                chars[i] = File.separatorChar
            }
        }
        new String(chars)
    }

    static File getUploadPath(WcmSpace space, path = null) {
        if (File.separatorChar != '/') {
            path = makeFileSystemPathFromURI(path)
        }
        def spcf = new File(WcmContentRepositoryService.uploadDir, 
            space.makeUploadName() ?: EMPTY_ALIAS_URI)
        return path ? new File(spcf, path) : spcf
    }

    static getUploadDirFromConfig(configObject) {
        def uploadDirConf = configObject.weceem.upload.dir
        (uploadDirConf instanceof String) && uploadDirConf ? uploadDirConf : "/WeceemFiles/"
    }
    
    static getUploadUrlFromConfig(configObject) {
        def uploadDirConf = getUploadDirFromConfig(configObject)

        if (!uploadDirConf.startsWith('file:')) {
            if (!uploadDirConf.startsWith('/')) uploadDirConf = '/'+uploadDirConf
            if (!uploadDirConf.endsWith('/')) uploadDirConf += '/'
            
            return uploadDirConf
        } else {
            return '/uploads/'
        }
    }

    void loadConfig() {
        def uploadDirConf = getUploadDirFromConfig(grailsApplication.config)

        if (!uploadDirConf.startsWith('file:')) {
            uploadInWebapp = true
            uploadDir = grailsApplication.mainContext.getResource("$uploadDirConf").file
        } else {
            def f = new File(new URI(uploadDirConf))
            if (!f.exists()) {
                def ok = f.mkdirs()
                if (!ok) {
                    throw new RuntimeException("Cannot start Weceem - upload directory is set to [${uploadDirConf}] but cannot make the directory and it doesn't exist")
                }
            }
            uploadInWebapp = false
            uploadDir = f
        }

        uploadUrl = WcmContentRepositoryService.getUploadUrlFromConfig(grailsApplication.config)

        log?.info "Weceem will use [${uploadDir}] as the directory for static uploaded files, and the url [${uploadUrl}] to serve them, files are inside webapp? [${uploadInWebapp}]"
    }
    
    void createDefaultSpace() {
        if (Environment.current != Environment.TEST) {
            if (WcmSpace.count() == 0) {
                createSpace([name:'Default'])
            }
        }
    }
    
    void createDefaultStatuses() {
        if (WcmStatus.count() == 0) {
            DEFAULT_STATUSES.each {
                assert new WcmStatus(it).save()
            }
        }

        // Make sure unmoderated and archived status codes, as defined by user config, exist
        [ 
            ['archivedStatusCode', DEFAULT_ARCHIVED_STATUS_CODE], 
            ['unmoderatedStatusCode', DEFAULT_UNMODERATED_STATUS_CODE]
        ].each { info -> 
            def defaultStatusPropertyName = info[0]
            if (!WcmStatus.findByCode(this[defaultStatusPropertyName])) {
                // The default info for this status
                def newStatus = [:] + DEFAULT_STATUSES.find { it.code == info[1] } 

                // Write it out as user-supplied status code, or default...
                newStatus.code = this[defaultStatusPropertyName] // In case user has overriden it but deleted the row
                def s = new WcmStatus(newStatus)
                if (!s.save()) {
                    log.error "Couldn't create missing status for ${info[0]}: ${s.errors}"
                    assert s
                }
            }
        }
    }
    
    List getAllPublicStatuses() {
        WcmStatus.findAllByPublicContent(true, [cache:true])
    }
    
    WcmSpace findDefaultSpace() {
        def space
        def spaces = WcmSpace.list([cache:true])
        if (spaces) {
            space = spaces[0]
        }        
        return space
    }
    
    WcmSpace findSpaceByURI(String uri) {
        WcmSpace.findByAliasURI(uri, [cache:true])
    }
    
    Map resolveSpaceAndURIOfUploadedFile(String url) {
        def u = url - uploadUrl
        if (u.startsWith(EMPTY_ALIAS_URI)) {
            u -= EMPTY_ALIAS_URI
        }
        resolveSpaceAndURI( u)
        
    }
    
    Map resolveSpaceAndURI(String uri) {
        def spaceName
        def space
        
        // This is pretty horrible stuff. Beware.
        if (uri?.startsWith('/')) {
            if (uri.length() > 1) {
                uri = uri[1..uri.length()-1]
            } else {
                uri = ''
            }
        }
        def n = uri?.indexOf('/')
        if (n >= 0) {
            spaceName = uri[0..n-1]
            if (n < uri.size()-1) {
                uri = uri[n+1..-1]
            }
        }
        
        // Let's try to find the space, or page in the root space
        if (!spaceName) {
            if (log.debugEnabled) {
                log.debug "WcmContent request for no space, looking for space with blank aliasURI"
            }
            space = findSpaceByURI('')
            if (!space) {
                if (log.debugEnabled) {
                    log.debug "WcmContent request for no space, looking for any space, none with blank aliasURI"
                }
                space = findDefaultSpace()
            }
        } else {
            if (log.debugEnabled) {
                log.debug "Content request for space with alias: ${spaceName}"
            }
            space = findSpaceByURI(spaceName)

            // Check for case where requesting a doc that is in a space mapped to uri ""
            if (space == null) {
                if (log.debugEnabled) {
                    log.debug "WcmContent request has no space found in database, looking for space with blank aliasURI to see if doc is there"
                }
                space = findSpaceByURI('')
                if (space) {
                    uri = uri ? spaceName + '/' + uri : spaceName
                    if (log.debugEnabled) {
                        log.debug "Content request has found space with blank aliasURI, amending uri to include the space name: ${uri}"
                    }
                }
            }
        }        

        // If the URI is just for the space uri with no doc, default to "index" node in root of spacer
        if ((uri == null) || (uri == space?.aliasURI) || (uri == space?.aliasURI+'/')) { 
            uri = 'index'
        }

        [space:space, uri:uri]
    }
    
    WcmSpace createSpace(params, templateName = 'default') {
        def s
        WcmContent.withTransaction { txn ->
            s = new WcmSpace(params)
            if (s.save()) {
                // Create the filesystem folder for the space
                def spaceDir = getUploadPath(s)
                if (!spaceDir.exists()) {
                    spaceDir.mkdirs()
                }

                if (templateName) {
                    importSpaceTemplate(templateName, s)
                }
            } else {
                log.error "Unable to create space with properties: ${params} - errors occurred: ${s.errors}"
            }
        }
        return s // If this fails we still return the original space so we can see errors
    }
    
    /**
     * Import a named space template (import zip) into the specified space
     */
    void importSpaceTemplate(String templateName, WcmSpace space) {
        log.info "Importing space template [${templateName}] into space [${space.name}]"
        // For now we only load files, in future we may get them as blobs from DB
        def f = File.createTempFile("default-space-import", null)
        def resourceName = "classpath:/org/weceem/resources/${templateName}-space-template.zip"
        def res = ApplicationHolder.application.parentContext.getResource(resourceName).inputStream
        if (!res) {
            log.error "Unable to import space template [${templateName}] into space [${space.name}], space template not found at resource ${resourceName}"
            return
        }
        f.withOutputStream { os ->
            os << res
        }
        try {
            wcmImportExportService.importSpace(space, 'simpleSpaceImporter', f)
        } catch (Throwable t) {
            log.error "Unable to import space template [${templateName}] into space [${space.name}]", t
            throw t // rethrow, this is sort of fatal
        }
        log.info "Successfully imported space template [${templateName}] into space [${space.name}]"
    }
    
    void requirePermissions(WcmSpace space, permissionList, Class<WcmContent> type = null) throws AccessDeniedException {
        if (!wcmSecurityService.hasPermissions(space, permissionList, type)) {
            throw new AccessDeniedException("User [${wcmSecurityService.userName}] with roles [${wcmSecurityService.userRoles}] does not have the permissions [$permissionList] to access space [${space.name}]")
        }
    }       
    
    void requirePermissions(WcmContent content, permissionList, Class<WcmContent> type = null) throws AccessDeniedException {
        if (!wcmSecurityService.hasPermissions(content, permissionList, type)) {
            throw new AccessDeniedException("User [${wcmSecurityService.userName}] with roles [${wcmSecurityService.userRoles}] does not have the permissions [$permissionList] to access content at [${content.absoluteURI}] in space [${content.space.name}]")
        }
    }       

    void deleteSpaceContent(WcmSpace space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        log.info "Deleting content from space [$space]"
        // Let's brute-force this
        // @todo remove/rework this for 0.2
        def contentList = WcmContent.findAllBySpace(space)
        for (content in contentList){
            // Invalidate the caches before parent is changedBy
            invalidateCachingForURI(content.space, content.absoluteURI)

            content.parent = null
            content.save()
        }
        def wasDelete = true
        while (wasDelete){
            contentList = WcmContent.findAllBySpace(space)
            wasDelete = false
            for (content in contentList){
                def refs = findReferencesTo(content)
                if (refs.size() == 0){
                    deleteNode(content)
                    wasDelete = true
                }
            }
        }
        log.info "Finished Deleting content from space [$space]"
    }
    
    void deleteSpace(WcmSpace space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        // Delete space content
        deleteSpaceContent(space)
        // Delete space
        space.delete(flush: true)
    }

    def getGSPTemplate(content) {
        def absURI = content.absoluteURI
        def k = makeURICacheKey(content.space, absURI)
        wcmCacheService.getOrPutObject(CACHE_NAME_GSP_CACHE, k) {
            if (log.debugEnabled) {
                log.debug "Creating GSP template class for $absURI"
            }
            // Workaround for Grails 1.2.0 bug where page name must be a valid local system file path!
            // Was dying on Windows with / in uris. http://jira.codehaus.org/browse/GRAILS-5772
            // @todo This is VERY nasty, assumes GSP content is in a "content" property
            groovyPagesTemplateEngine.createTemplate(content.content, ('WcmContent:'+absURI).replaceAll(/[^a-zA-Z0-9\-]/, '_') )
        }
    }

    /**
     * Take a string or Class or null and turn it into a content Class
     */
    // @todo cache the list of known type ans mappings that are assignable to a WcmContent variable
    // so that we can skip the isAssignableFrom which will affect performance a lot, as this function may be
    // called a lot
    Class getContentClassForType(def type) {
        if (type == null) {
            return WcmContent.class
        }        
        
        def cls = (type instanceof Class) ? type : grailsApplication.getClassForName(type)
        if (cls) {
            if (!WcmContent.isAssignableFrom(cls)) {
                throw new IllegalArgumentException("The class $clazz does not extend Content")
            } else {
                return cls
            }
        } else {
            throw new IllegalArgumentException("There is no content class with name $type")
        }
    }

    List listContentClassNames(Closure precondition = null) {
        return listContentClasses(precondition).collect { it.name }
    }
    
    List listContentClasses(Closure precondition = null) {
        def results = []
        grailsApplication.domainClasses.each { dc ->
            def cls = dc.clazz
            if (WcmContent.isAssignableFrom(cls) && (cls != WcmContent)) {
                if ((precondition == null) || precondition(cls)) {
                    results << cls
                }
            }
        }
        return results
    }
    
    WcmContent newContentInstance(type, WcmSpace space = null) {
        def cls = getContentClassForType(type)
        def c = cls.newInstance()
        if (space) {
            c.space = space
        }
        return c
    }
    
    /**
     * Prepares map of content properties.
     * In the future, we may need more information for the nodes,
     * eg. incoming links
     */
    Map getContentDetails(WcmContent content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        return [id: content.id, className: content.class.name,
                title: content.title, createdBy: content.createdBy,
                createdOn: content.createdOn, changedBy: content.changedBy,
                changedOn: content.changedOn,
                // @todo replace this ugliness with polymorphic calls
                summary: content.metaClass.hasProperty(content, 'summary') ? content.summary : null,
                contentType: content.class.name]
    }

    /**
     * Returns map of related content properties.
     *
     * @param content
     */
    Map getRelatedContent(WcmContent content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        def result = [:]
        // @todo change to criteria/select
        result.parents = WcmVirtualContent.findAllByTarget(content)*.parent
        if (content.parent) result.parents << content.parent
        result.children = content.children
        
        def relatedContents = []
        // @todo replace with more efficient select/criteria
        relatedContents.addAll(WcmRelatedContent.findAllWhere(targetContent: content).collect {
            it.sourceContent
        })
        // @todo replace with more efficient select/criteria
        relatedContents.addAll(WcmRelatedContent.findAllWhere(sourceContent: content).collect {
            it.targetContent
        })
        result.related = relatedContents.unique()

        return result
    }

    /**
     * Returns map of recent changes for specified WcmContent.
     *
     * @param content
     */
    List getChangeHistory(WcmContent content, queryArgs = [:]) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        def args = [:] + queryArgs
        if (!args.sort) {
            args += [sort:'createdOn', order: 'desc']
        }
        def changes = WcmContentVersion.findAllByObjectKeyAndObjectClassName(content.ident(), content.class.name, args)
        return changes
    }

    /** 
     * Get a specific change history item
     */
    def getChangeHistoryItem(id) {
        WcmContentVersion.get(id)
    }
    
    /**
     * Creates new WcmContent node and it's relation from request parameters
     *
     * @param content
     */
    def createNode(String type, def params, Closure postInit = null) {
        def content = newContentInstance(type)
        def tags = params.remove('tags')
        hackedBindData(content, params)
        if (postInit) {
            postInit(content)
        }
        // Ignore result here, we need the content's errors
        createNode(content, content.parent)
        // Set these after create, even if error - can't set them before instance exists
        // and need them on the object in the case of error or they get lost
        if (tags != null) {
            content.setTags(tags.tokenize(',').collect { it.trim().toLowerCase()} )
        }
        return content // has the errors set on it
    }

    /**
     * Trigger an event on the domain class and also on the events service.
     *
     * Not suitable for events that are confirmation "can this happen" callbacks, this if for
     * things that have already happened.
     */
    def triggerEvent(WcmContent content, WeceemEvent event, argumentTypes = null, arguments = []) {
        def outcome = triggerDomainEvent(content, event, argumentTypes, arguments)
        if (outcome) {
            wcmEventService.event(event, content)
        }
        return outcome
    }
    
    /**
     * Trigger an event on the domain class only.
     *
     */
    def triggerDomainEvent(WcmContent content, WeceemEvent event, argumentTypes = null, arguments = []) {
        if (log.debugEnabled) {
            log.debug "Attempting to trigger domain event [$eventName] on node ${content.dump()}"
        }
        def eventName = event.toString()
        if (content.metaClass.respondsTo(content, eventName, *argumentTypes)) {
            if (log.debugEnabled) {
                log.debug "Trigger domain event [$eventName] as it is supported on node ${content.dump()}"
            }
            // Call the event so that nodes can perform post-creation tasks
            return content."${eventName}"(*arguments)
        } else {
            if (log.debugEnabled) {
                log.debug "Node does not support [$eventName] event, skipping"
            }
            return true
        }
    }
    
    /**
     * Creates new WcmContent node and it's relation
     *
     * @param content
     * @param parentContent
     */
    Boolean createNode(WcmContent content, WcmContent parentContent = null) {
        if (parentContent) { 
            requirePermissions(parentContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        } else {
            assert content.space
            requirePermissions(content.space, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        }

        if (parentContent == null) parentContent = content.parent

        if (log.debugEnabled) {
            log.debug "Creating node: ${content.dump()} with parent [$parentContent]"
        }
        
        def result = true

        if (parentContent) {
            result = parentContent.canAcceptChild(content)
        }
        
        if (result) {
            result = triggerDomainEvent(content, WeceemEvent.contentShouldBeCreated, [WcmContent], [parentContent])
        }

        if (result) {
            // @todo This is not safe in concurrent environments, you can end up with 2 
            // nodes with same orderIndex - which is not prevented by constraints but may be annoying for users
            // Try to update this to use executeUpdate to set the index, at some point
            def orderIndex = -1
            WcmContent.withNewSession {
                def criteria = WcmContent.createCriteria()
                def nodes = criteria {
                    if (parentContent) {
                        eq("parent", parentContent)
                    } else {
                        isNull("parent")
                    }
                    maxResults(1)
                    order("orderIndex", "desc")
                }
                orderIndex = nodes ? nodes[0].orderIndex + 1 : 0
            }
            content.orderIndex = orderIndex

            if (parentContent) {
                parentContent.addToChildren(content)
            }

            // Short circuit out of here if not valid now
            def valid = content.validate()
            if (!valid) {
                // If its not just a blank aliasURI error, get out now
                if (!(content.errors.errorCount == 1 && content.errors.getFieldErrors('aliasURI').size() == 1)) {
                    result = false
                }
            }

            if (result) {
                // We complete the AliasURI, AFTER handling the create() event which may need to affect title/aliasURI
                if (!content.aliasURI) {
                    content.createAliasURI(parentContent)
                }
            
                // Auto-set publishFrom to now if content is created as public but no publishFrom specified
                // Required for blogs and sort by publishFrom to work
                if (content.status.publicContent && (content.publishFrom == null)) {
                    content.publishFrom = new Date()
                }
            
                // We must have generated aliasURI and set parent here to be sure that the uri is unique
                boolean saved = false
                int attempts = 0
                while (!saved && (attempts++ < 100)) {
                    try {
                        if (content.save(flush:true)) {
                            saved = true
                        }
                    } catch (ConstraintViolationException cve) {
                        // See if we get a new aliasURI from the content, and if so try again
                        def oldAliasURI = content.aliasURI
                        content.createAliasURI(parentContent)
                        if (oldAliasURI != content.aliasURI) {
                            if (log.warnEnabled) {
                                log.warn "Failed to create new content ${content.dump()} due to constraint violation, trying again with a new aliasURI"
                            }
                        } else {
                            log.error "Failed to create new content ${content.dump()} due to constraint violation, giving up as aliasURI is invariant"
                            result = false
                            break;
                        }
                    }
                }
            }
            
            if (result) {
                triggerEvent(content, WeceemEvent.contentDidGetCreated)
            }

            if (!result) {
                parentContent?.discard() // revert the changes we made to parent
            }
            
            invalidateCachingForURI(content.space, content.absoluteURI)
        }
        
        return result
    }

    /**
     * Creates new virtual copy of a content node.
     *
     * @todo rename to createNodeReference? virtualCopyNode?
     *
     * @param sourceContent
     * @param targetContent
     * @return new instance of VirtualContentNode or null if there were errors
     */
    WcmVirtualContent linkNode(WcmContent sourceContent, WcmContent targetContent, orderIndex) {
        // Check they can create under the target
        requirePermissions(targetContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        if (sourceContent == null){
            return null
        }
        if (sourceContent && (sourceContent instanceof WcmVirtualContent)) {
            sourceContent = sourceContent.target
        }
        if (WcmContent.findWhere(parent: targetContent, aliasURI: sourceContent.aliasURI + "-copy") != null){
            return null
        }
        WcmVirtualContent vcont = new WcmVirtualContent(title: sourceContent.title,
                                          aliasURI: sourceContent.aliasURI + "-copy",
                                          target: sourceContent, status: sourceContent.status, 
                                          space: sourceContent.space)
        WcmContent inPoint = WcmContent.findByOrderIndexAndParent(orderIndex, targetContent)
        if (inPoint != null){
            shiftNodeChildrenOrderIndex(targetContent.space ?: sourceContent.space, targetContent, orderIndex)
        }
        vcont.orderIndex = orderIndex
        if (targetContent) {
            if (WcmVirtualContent.findWhere(parent: targetContent, target: sourceContent)){
                return null
            }
            targetContent.addToChildren(vcont)
            if (!vcont.save()){
                return null
            }
            if (!targetContent.save(flush:true)){
                return null
            }
        }else{
            if (!vcont.save(flush: true)){
                return null
            }
        }
        return vcont
    }

    /**
     * Changes content node reference.
     *
     * @param sourceContent
     * @param targetContent
     */
    Boolean moveNode(WcmContent sourceContent, WcmContent targetContent, orderIndex) {
        if (log.debugEnabled) {
            log.debug "Moving content ${sourceContent} to ${targetContent} at order index $orderIndex"
        }
        if (targetContent) {
            requirePermissions(targetContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        }
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_EDIT,WeceemSecurityPolicy.PERMISSION_VIEW])        

        if (!sourceContent) return false

        // Do an ugly check for unique uris at root
        if (!targetContent){
            def criteria = WcmContent.createCriteria()
            def nodes = criteria {
                if (targetContent){
                    eq("parent.id", targetContent.id)
                }else{
                    isNull("parent")
                }
                eq("aliasURI", sourceContent.aliasURI)
                not{
                    eq("id", sourceContent.id)
                }
            }
            
            if (nodes.size() > 0){
                return false
            } 
        }
        def success = true

        // We need this to invalidate caches
        def originalURI = sourceContent.absoluteURI

        def parentChanged = targetContent != sourceContent.parent
        if (targetContent && parentChanged) {
            success = targetContent.canAcceptChild(sourceContent)
        }

        if (success && parentChanged) {
            success = triggerDomainEvent(sourceContent, WeceemEvent.contentShouldMove, [WcmContent], [targetContent])
        }
        
        if (success) {
            if (parentChanged) {
                // Transpose to new parent
                def parent = sourceContent.parent
                if (parent) {
                    parent.children.remove(sourceContent)
                    sourceContent.parent = null
                    assert parent.save()
                }
            }
            
            // Update the orderIndexes of target's children
            WcmContent inPoint = WcmContent.findByOrderIndexAndParent(orderIndex, targetContent)
            if (inPoint != null) {
                shiftNodeChildrenOrderIndex(sourceContent.space, targetContent, orderIndex)
            }
            
            // Update ourself to new orderIndex
            sourceContent.orderIndex = orderIndex
            
            // Add us to the child list
            if (targetContent && parentChanged) {
                if (!targetContent.children) targetContent.children = new TreeSet()
                targetContent.addToChildren(sourceContent)
                assert targetContent.save()
            }

            // Invalidate the caches 
            invalidateCachingForURI(sourceContent.space, originalURI)

            success = sourceContent.save(flush: true)
            if (success) {
                triggerEvent(sourceContent, WeceemEvent.contentDidMove)
            }
            return success
        } else {
            return false
        }
     }
     
    def shiftNodeChildrenOrderIndex(WcmSpace space, parent, shiftedOrderIndex){
        if (log.debugEnabled) {
            log.debug "Updating node order indexes in space ${space} for parent ${parent}"
        }
        // Can't do this until space is supplied
        //requirePermissions(parent, [WeceemSecurityPolicy.PERMISSION_EDIT])        
        // @todo this is probably flushing the session with incomplete changes - use withNewSession?
        def criteria = WcmContent.createCriteria()
        def nodes = criteria {
            eq('space', space)
            if (parent){
                eq("parent.id", parent.id)
            }else{
                isNull("parent")
            }
            ge("orderIndex", shiftedOrderIndex)
            order("orderIndex", "asc")
        }
        def orderIndex = shiftedOrderIndex
        nodes.each{it->
            it.orderIndex = ++orderIndex
            it.save()
        }
    }
    
    
    /**
     * Use introspection to find all references to the specified content. Requires finding all
     * associations/relationships to other WcmContent and querying them all individually. Hideous but
     * less ugly than forcing all references to be ContentRef(s) we decided.
     */
    ContentReference[] findReferencesTo(WcmContent content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        def results = [] 
        // @todo this will perform rather poorly. We should find all assocation properties FIRST
        // and then run a query for each association, which - with caching - should run a lot faster than
        // checking every property on every node
        for (cont in WcmContent.list()){
            def perProps = grailsApplication.getDomainClass(cont.class.name).persistentProperties.findAll { p -> 
                p.isAssociation() && WcmContent.isAssignableFrom(p.referencedPropertyType)
            }
            for (p in perProps){
                if (cont."${p.name}" instanceof Collection){
                    for (inst in cont."${p.name}"){
                        if (inst.equals(content)){
                            results << new ContentReference(referringProperty: p.name, referencingContent: cont, targetContent: content)
                        }
                    }
                }else{
                    if (content.equals(cont."${p.name}")){
                        results << new ContentReference(referringProperty: p.name, referencingContent: cont, targetContent: content)
                    }
                }
            }
        }
        return results as ContentReference[]
    }
    
    void removeAllTagsFrom(WcmContent content) {
        content.tags.collect({it}).each { t -> content.removeTag(t) }
    }
    
    /**
     * Deletes content node and all it's references.
     * All children of sourceContent will be assigned to all its parents.
     *
     * @param sourceContent
     */
    Boolean deleteNode(WcmContent sourceContent) {
        if (!sourceContent) return Boolean.FALSE
        
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_DELETE])        
        
        // Create a versioning entry
        sourceContent.saveRevision(sourceContent.title, sourceContent.space.name)
        
        if (!triggerDomainEvent(sourceContent, WeceemEvent.contentShouldBeDeleted)) {
            return false
        }

        triggerEvent(sourceContent, WeceemEvent.contentWillBeDeleted)

        // Do this now before absoluteURI gets trashed by changing the parent
        invalidateCachingForURI(sourceContent.space, sourceContent.absoluteURI)

        def parent = sourceContent.parent

        // if there is a parent  - we delete node from its association
        if (parent) {
            parent.children = parent.children.findAll{it-> it.id != sourceContent.id}
            assert parent.save()
        }

        // we need to delete all virtual contents that reference sourceContent
        def copies = WcmVirtualContent.findAllWhere(target: sourceContent)
        copies?.each() {
           if (it.parent) {
               parent = WcmContent.get(it.parent.id)
               parent.children.remove(it)
           }
           removeAllTagsFrom(it)
           it.delete()
        }

        // delete node
        
        // @todo replace this with code that looks at all the properties for relationships
        if (sourceContent.metaClass.hasProperty(sourceContent, 'template')?.type == WcmTemplate) {
            sourceContent.template = null
        }
        if (sourceContent.metaClass.hasProperty(sourceContent, 'target')?.type == WcmContent) {
            sourceContent.target = null
        }

        removeAllTagsFrom(sourceContent)
        sourceContent.delete(flush: true)

        triggerEvent(sourceContent, WeceemEvent.contentDidGetDeleted)

        return true
    }

    /**
     * Deletes content reference 
     *
     * @todo Update the naming of this, "link" is not correct terminology. 
     *
     * @param child
     * @param parent
     */
    void deleteLink(WcmContent child, WcmContent parent) {
        requirePermissions(parent, [WeceemSecurityPolicy.PERMISSION_EDIT])        
        requirePermissions(child, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        // remove child from association
        parent.children?.remove(child)
        parent.save()
        
        // change reference to parent
        def parentRef = parent.parent
        child.parent = parentRef

        // update orderIndex for new associatio
        def newIndex = parentRef?.children?.last()?.orderIndex ?
                           parentRef?.children?.last()?.orderIndex + 1 : 0
        child.orderIndex = newIndex        

        if (parentRef) {
            parentRef.children << child
            parentRef.save()
        }

    }
    
    def updateSpace(def id, def params){
        def space = WcmSpace.get(id)
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        if (space){
            def oldAliasURI = space.makeUploadName()
            hackedBindData(space, params)
            if (!space.hasErrors() && space.save()) {
                def oldFile = WcmContentRepositoryService.getUploadPath(space, oldAliasURI)
                if (oldFile.exists()) {
                    def newFile = WcmContentRepositoryService.getUploadPath(space)
                    oldFile.renameTo(newFile)
                }
                return [space: space]
            } else {
                return [errors:space.errors, space:space]
            }
        }else{
            return [notFound: true]
        }
    }
    
    /**
     * Update a node with the new properties supplied, binding them in using Grails binding
     * @return a map containing an optional "errors" list property and optional notFound boolean property
     */
    def updateNode(String id, def params) {
        WcmContent content = WcmContent.get(id)
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        if (content) {
            return updateNode(content, params)
        } else {
            return [notFound:true]
        }        
    }
    
    // @todo This is a hack so we can bind without x.properties = y which is broken in production on Grails 1.2-M2
    public hackedBindData(obj, params) {
        new BindDynamicMethod().invoke(this, 'bindData', obj, params)
    }

    String makeURICacheKey(WcmSpace space, uri) {
        space.aliasURI+':'+uri
    }

    /**
     * Flush all uri caches for given space
     */
    void invalidateCachingForSpace(WcmSpace space) {
        def uri = space.aliasURI+':'
        gspClassCache.keys.each { k ->
            if (k.startsWith(uri)) {
                gspClassCache.remove(k)
            }
        }
        uriToIdCache.keys.each { k ->
            if (k.startsWith(uri)) {
                uriToIdCache.remove(k)
            }
        }
    }

    /**
     * Flush all uri caches for given space and uri prefix
     */
    void invalidateCachingForURI( WcmSpace space, uri) {
        // If this was content that created a cached GSP class, clear it now
        def key = makeURICacheKey(space,uri)
        log.debug "Removing cached info for cache key [$key]"
        gspClassCache.remove(key) // even if its not a GSP/script lets just assume so, quicker than checking & remove
        uriToIdCache.remove(key)
        
        // Now remove the caches of all child nodes too, as the parent may have moved and all URIs changed
        def parentKey = makeURICacheKey(space,uri+'/')
        gspClassCache.keys.each { k ->
            if (k.startsWith(parentKey)) {
                gspClassCache.remove(k)
            }
        }
        uriToIdCache.keys.each { k ->
            if (k.startsWith(parentKey)) {
                uriToIdCache.remove(k)
            }
        }
    }
    
    /**
     * Update a content node in the database, binding new properties in from "params"
     *
     * @return an object with properties "content", "errors" and "notFound" - set as appropriate
     */
    def updateNode(WcmContent content, def params) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        // firstly we save revision: to prevent errors that we have 2 objects
        // in session with the same identifiers
        if (log.debugEnabled) {
            log.debug("Updating node with id ${content.id}, with parameters: $params")
        }
        def oldAbsURI = content.absoluteURI
        def oldSpaceName = params.space ? WcmSpace.get(params.'space.id')?.name : content.space.name
        
        // Get read-only instance now, for persisting revision info after we bind and successfully update
        def contentForRevisionSave = content.class.read(content.id)

        def oldTitle = content.title
        // map in new values
        hackedBindData(content, params)
        
        if (!content.hasErrors()) {
        
            if (params.tags != null) {
                content.setTags(params.tags.tokenize(',').collect { it.trim().toLowerCase()} )
            }
            triggerEvent(content, WeceemEvent.contentDidChangeTitle, [String], [oldTitle])
            
            if (log.debugEnabled) {
                log.debug("Updated node with id ${content.id}, properties are now: ${content.dump()}")
            }
            if (!content.aliasURI && content.title) {
                content.createAliasURI(content.parent)
            }

            // Auto-set publishFrom to now if content is created as public but no publishFrom specified
            // Required for blogs and sort by publishFrom to work
            if (content.status.publicContent && (content.publishFrom == null)) {
                content.publishFrom = new Date()
            }

            def ok = content.validate()
            if (content.save()) {
                // Save the revision now
                contentForRevisionSave.saveRevision(params.title ?: oldTitle, oldSpaceName)

                if (log.debugEnabled) {
                    log.debug("Update node with id ${content.id} saved OK")
                }
            
                invalidateCachingForURI(content.space, oldAbsURI)

                triggerEvent(content, WeceemEvent.contentDidGetUpdated)

                return [content:content]
            }
        }
        if (log.debugEnabled) {
            log.debug("Update node with id ${content.id} failed with errors: ${content.errors}")
        }
        return [errors:content.errors, content:content]
    }
    
    /**
     * Count child nodes of a given node, where nodes match the type and status (if any) supplied in args
     * Very useful for rendering the number of published comments on an item, for example in blogs.
     */
    def countChildren(WcmContent sourceNode, Map args = null) {
        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        // for WcmVirtualContent - the children list is a list of target children
        if (sourceNode instanceof WcmVirtualContent) {
            sourceNode = sourceNode.target
        }
        
        def clz = args?.type ? getContentClassForType(args.type) : WcmContent
        return (doCriteria(clz, args?.status, args?.params) {
            projections {
                count('id')
            }
            if (sourceNode) {
                eq('parent', sourceNode)
            } else {
                isNull('parent')
            }
        })[0]
    }
    
    /**
     * Returns the number of content nodes in the given space and matching the indicated parameters.
     * @param space The space to search for content in
     * @param args A map of query parameters (type, status)
     */
    def countContent(WcmSpace space, Map args = null) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        def clz = args?.type ? getContentClassForType(args.type) : WcmContent
        return (doCriteria(clz, args?.status, args?.params) {
            projections {
                count('id')
            }
            eq('space', space)
        })[0]
    }
    
    /**
     * Change a criteria closure so that it includes the restrictions specified in the params as per
     * normal grails controller mechanisms - max, offset, sort and order
     */
    private Closure criteriaWithParams(Map params, Closure originalCriteria) {
        return { ->
            originalCriteria.delegate = delegate
            originalCriteria()
            if (params?.max != null) {
                maxResults(params.max)
            }
            if (params?.offset != null) {
                firstResult(params.offset)
            }
            if (params?.sort != null) {
                order(params.sort, params.order ?: 'asc')
            }
        }
    }
    
    /**
     * Wrap a criteria query, adding filtering by status
     * where status can be:
     * - null for 'any' 
     * - a WcmStatus instance eg WcmStatus.get(1)
     * - an integer for a status code eg 500
     * - a list of status codes eg [100, 200, 500]
     * - a range of integer status codes eg (1..500)
     * - a string integer status code eg "500"
     * 
     */
    private def criteriaWithStatus(status, Closure originalCriteria) {
        return { ->
            originalCriteria.delegate = delegate
            originalCriteria()

            if (status != null) {
                if (status == WcmContentRepositoryService.STATUS_ANY_PUBLISHED) {
                    inList('status', allPublicStatuses)
                } else if (status instanceof Collection) {
                    // NOTE: This assumes collection is a collection of codes, not WcmStatus objects
                    inList('status', WcmStatus.findAllByCodeInList(status))
                } else if (status instanceof WcmStatus) {
                    eq('status', status)
                } else if (status instanceof Integer) {
                    eq('status', WcmStatus.findByCode(status) )
                } else if (status instanceof IntRange) {
                    between('status', status.fromInt, status.toInt)
                } else {
                    def s = status.toString()
                    if (s.isInteger()) {
                        eq('status', WcmStatus.findByCode(s.toInteger()) )
                    } else throw new IllegalArgumentException(
                        "The [status] argument must be null (for 'any'), or '${WcmContentRepositoryService.STATUS_ANY_PUBLISHED}',  an integer (or integer string), a collection of codes (numbers), a Status instance or an IntRange. You supplied [$status]")
                }
            }
        }
    }
    
    protected def doCriteria(clz, status, params, Closure c) {
        clz.withCriteria( criteriaWithParams( params, criteriaWithStatus(status, c) ) )
    }
    
    /**
     * Find all the children of the specified node, within the content hierarchy, optionally filtering by a content type class
     * @todo we can probably improve performance by applying the typeRestriction using some HQL
     */ 
    def findChildren(WcmContent sourceNode, Map args = Collections.EMPTY_MAP) {
        if (log.debugEnabled) {
            log.debug "Finding children of ${sourceNode.absoluteURI} with args $args"
        }
        // @todo we also need to filter the result list by VIEW permission too!
        assert sourceNode != null

        // for WcmVirtualContent - the children list is a list of target children
        if (sourceNode instanceof WcmVirtualContent) {
            sourceNode = sourceNode.target
        }

        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        
        // @todo replace this with smarter queries on children instead of requiring loading of all child objects
        def typeRestriction = getContentClassForType(args.type)
        if (log.debugEnabled) {
            log.debug "Finding children of ${sourceNode.absoluteURI} restricting type to ${typeRestriction}"
        }
        def children = doCriteria(typeRestriction, args.status, args.params) {
            if (sourceNode == null) {
                isNull('parent')
            } else {
                eq('parent', sourceNode)
            }
            cache true
        }
        
        return children
    }


    /**
     * Sort function for queries that cannot be sorted in the database, i.e. aggregated data
     */
    def sortNodes(nodes, sortProperty, sortDirection = "asc") {
        if (sortProperty) {
            if (sortDirection == "asc") {
                return nodes.sort { a, b -> 
                    if (a[sortProperty]) 
                        return a[sortProperty].compareTo(b[sortProperty]) 
                    else 
                        return -1 
                }
            } else if (sortDirection == "desc") {
                return nodes.sort { a, b -> 
                    if (b[sortProperty]) 
                        return b[sortProperty].compareTo(a[sortProperty]) 
                    else 
                        return +1 
                }
            } else throw new IllegalArgumentException("Sort order must be one of [asc] or [desc], was: [$sortDirection]")
        } else return nodes
    }
    
    /**
     * Returns true if the node has a status that matches the supplied status
     * See findWithStatus() for rules and possible values of "status"
     */
    boolean contentMatchesStatus(status, WcmContent node) {
        if (status == null) {
            return true
        } else if (status == WcmContentRepositoryService.STATUS_ANY_PUBLISHED) {
            return WcmStatus.findAllByPublicContent(true).find { it == node.status }
        } else if (status instanceof Collection) {
            // NOTE: This assumes collection is a collection of codes, not WcmStatus objects
            return status.find { it == node.status.code }  
        } else if (status instanceof WcmStatus) {
            return node.status == status
        } else if (status instanceof Integer) {
            return node.status.code == status.code
        } else if (status instanceof IntRange) {
            return status.containsWithBounds(node.status.code)
        } else {
            def s = status.toString()
            if (s.isInteger()) {
                return s.toNumber() == node.status.code
            } else throw new IllegalArgumentException(
                "The [status] argument must be null (for 'any'), or '${WcmContentRepositoryService.STATUS_ANY_PUBLISHED}', an integer (or integer string), a collection of codes (numbers), a Status instance or an IntRange. You supplied [$status]")
        }        
    }
    
    /**
     * Find all the parents of the specified node, within the content hierarchy, optionally filtering by status and a content type class
     * @todo we can probably improve performance by applying the typeRestriction using some HQL
     */ 
    def findParents(WcmContent sourceNode, Map args = Collections.EMPTY_MAP) {
        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        // @todo change to criteria/select
        def references = (doCriteria(WcmVirtualContent, args.status, Collections.EMPTY_MAP) {
            eq('target', sourceNode) 
        })*.parent
         
        if (sourceNode.parent && contentMatchesStatus(args.status, sourceNode.parent)) {
            references << sourceNode.parent
        }
        // Allow null here if there's no restriction
        def typeRestriction = args.type ? getContentClassForType(args.type) : null
        def parents = []
        references?.unique()?.each { 
            if (typeRestriction ? typeRestriction.isAssignableFrom(it.class) : true) {
                parents << it
            }
        }
        return sortNodes(parents, args.params?.sort, args.params?.order)
    }

    /**
     * Locate a root node by uri, type, status and space
     */ 
    def findRootContentByURI(String aliasURI, WcmSpace space, Map args = Collections.EMPTY_MAP) {
        if (log.debugEnabled) {
            log.debug "findRootContentByURI: aliasURI $aliasURI, space ${space?.name}, args ${args}"
        }
        def r = doCriteria(getContentClassForType(args.type), args.status, args.params) {
            isNull('parent')
            eq('aliasURI', aliasURI)
            eq('space', space)
            maxResults(1)
            cache true
        }
        WcmContent node = r ? r[0] : null
        if (node) {
            requirePermissions(node, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        }
        return node
    }
    
    /**
     * find all root nodes by type and space
     */ 
    def findAllRootContent(WcmSpace space, Map args = Collections.EMPTY_MAP) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        if (log.debugEnabled) {
            log.debug "findAllRootContent $space, $args"
        }
        doCriteria(getContentClassForType(args.type), args.status, args.params) {
            isNull('parent')
            eq('space', space)
            cache true
        }
    }
    
    /**
     * find all nodes by type and space
     */ 
    def findAllContent(WcmSpace space, Map args = Collections.EMPTY_MAP) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        if (log.debugEnabled) {
            log.debug "findAllContent $space, $args"
        }
        doCriteria(getContentClassForType(args.type), args.status, args.params) {
            eq('space', space)
            cache true
        }
    }
    
    /**
     *
     * Find the content node that is identified by the specified uri path. This always finds a single WcmContent node
     * or none at all. Each node can have multiple URI paths, so this code returns the node AND the uri to its parent
     * so that you can tell where it is in the hierarchy
     *
     * This call does NOT filter by path.
     *
     * @param uriPath
     * @param space
     *
     * @return a map of 'content' (the node), 'lineage' (list of parent WcmContent nodes to reach the node)
     * and 'parentURI' (the uri to the parent of this instance of the node)
     */
    def findContentForPath(String uriPath, WcmSpace space, boolean useCache = true) {
        if (log.debugEnabled) {
            log.debug "findContentForPath uri: ${uriPath} space: ${space}"
        }

        def cacheKey = makeURICacheKey(space, uriPath)
        
        if (useCache) {
            // This looks up the uriPath in the cache to see if we can get a Map of the content id and parentURI
            // If we call getValue on the cache hit, we lose 50% of our performance. Just retrieving
            // the cache hit is not expensive.
            def cachedElement = uriToIdCache.get(cacheKey)
            def cachedContentInfo = cachedElement?.getValue()
            if (cachedContentInfo) {
                if (log.debugEnabled) {
                    log.debug "Found content info in cache for uri $uriPath: ${cachedContentInfo}"
                }
                // @todo will this break with different table mapping strategy eg multiple ids of "1" with separate tables?
                WcmContent c = WcmContent.get(cachedContentInfo.id)
                // @todo re-load the lineage objects here, currently they are ids!
                def reloadedLineage = cachedContentInfo.lineage?.collect { l_id ->
                    WcmContent.get(l_id)
                }
                if (log.debugEnabled) {
                    log.debug "Reconstituted lineage from cache for uri $uriPath: ${reloadedLineage}"
                }
                if (c) {
                    requirePermissions(c, [WeceemSecurityPolicy.PERMISSION_VIEW])        
                }
            
                return c ? [content:c, parentURI:cachedContentInfo.parentURI, lineage:reloadedLineage] : null
            }   
        }
        
        def tokens = uriPath.split('/')

        // @todo: optimize query 
        WcmContent content = findRootContentByURI(tokens[0], space)
        if (!content) content = findFileRootContentByURI(tokens[0], space)
        if (log.debugEnabled) {
            log.debug "findContentForPath $uriPath - root content node is $content"
        }
        
        def lineage = [content]
        if (content && (tokens.size() > 1)) {
            for (n in 1..tokens.size()-1) {
                def child = WcmContent.find("""from WcmContent c \
                        where c.parent = ? and c.aliasURI = ?""",
                        [content, tokens[n]])
                if (log.debugEnabled) {
                    log.debug "findContentForPath $uriPath - found child $child for path token ${tokens[n]}"
                }
                if (child) {
                    lineage << child
                    content = child
                } else {
                    // We hit a URI part that does not resolve to a content node
                    content = null
                    break
                }
            }
        }
    
        // Get all the URI parts except the last
        def parentURIParts = []
        if (tokens.size() > 1) {
            parentURIParts = tokens[0..tokens.size()-2]
        }

        def parentURI = parentURIParts.join('/')

        // Cache this resolution - found or not
        // This MUST be a new map otherwise we have immutability problems
        // Don't writer lineage if the result was null
        def cacheValue = [id:content?.id, 
          parentURI:parentURI, lineage: content ? (lineage.collect { l -> l.id }).toArray() : null]
        
        if (log.debugEnabled) {
            log.debug "Caching content info for uri $uriPath: $cacheValue"
        }
        if (useCache) {
            wcmCacheService.putToCache(uriToIdCache, cacheKey, cacheValue)
        }
        
        if (content) {
            requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        

            [content:content, parentURI:parentURI, lineage:lineage]
        } else {
            return null
        }
    }
    
    def findFileRootContentByURI(String aliasURI, WcmSpace space, Map args = Collections.EMPTY_MAP) {
        if (log.debugEnabled) {
            log.debug "findFileRootContentByURI: aliasURI $aliasURI, space ${space?.name}, args ${args}"
        }
        def r = doCriteria(WcmContentFile, args.status, args.params) {
            eq('aliasURI', aliasURI)
            eq('space', space)
        }
        def res = r?.findAll(){it-> (it.parent == null) || !(it.parent instanceof WcmContentFile)}
        WcmContent result = res ? res[0] : null
        if (result) {
            requirePermissions(result, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        }
        return result
    }
    
    def getAncestors(uri, sourceNode) {
        // Can't impl this yet
    }
    
    def getTemplateForContent(def content){
        def template = (content.metaClass.hasProperty(content, 'template')) ? content.template : null
        if ((template == null) && (content.parent != null)){
            return getTemplateForContent(content.parent)
        }else{
            return template
        }
    }
    
    /** 
     * Determine if the content node is able to be rendered to visitors.
     * @return false if this content is not meant to be rendered, and is instead a component of other content
     */
    boolean contentIsRenderable(WcmContent content) {
        // See if it is renderable directly - eg WcmWidget and WcmTemplate are not renderable on their own
        if (content.metaClass.hasProperty(content.class, 'standaloneContent')) {
            return content.class.standaloneContent
        } else { 
            return true
        }
    }
     
    /**
     * Synchronize given space with file system
     * 
     * @param space - space to synchronize
    **/
    def synchronizeSpace(space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        def existingFiles = new TreeSet()
        def createdContent = []
        def spaceDir = WcmContentRepositoryService.getUploadPath(space)
        if (!spaceDir.exists()) spaceDir.mkdirs()
        log.info "Synchronizing filesystem with repository for space /${space.aliasURI} (server dir: ${spaceDir.absolutePath})"
        spaceDir.eachFileRecurse { file ->
            def relativePath = file.absolutePath.substring(spaceDir.absolutePath.size() + 1)
            log.info "Checking for existing content node in space /${space.aliasURI} for server file ${relativePath}"
            def content = findContentForPath(relativePath, space, false)?.content
            //if content wasn't found then create new
            if (!content) {
                log.info "Creating new content node in space /${space.aliasURI} for server file ${relativePath}"
                def newFileContent = createContentFile(space, relativePath)
                createdContent << newFileContent
                existingFiles.add(newFileContent)
                content = newFileContent.parent
                while (content) {
                    if (!existingFiles.find { c -> c.absoluteURI == content.absoluteURI } ) {
                        existingFiles.add(content)
                    }
                    content = content.parent
                }
            }else{
                log.info "Skipping server file ${relativePath}, already has content node"
                existingFiles.add(content)
            }
        }
        def allFiles = WcmContentFile.findAllBySpace(space);
        def existingIds = existingFiles*.id
        def missedFiles = allFiles.findAll { f ->
            def adding = !existingIds.contains(f.id)
            if (adding) {
                log.info "Adding ${f.absoluteURI} to list of orphaned files who have no content node"
            }
            return adding
        }
        
        return ["created": createdContent, "missed": missedFiles]
    }
    
    /**
     * Creates WcmContentFile/WcmContentDirectory from specified <code>path</code>
     * on the file system.
     *
     * @param space
     * @param path
     * @return the new content node, if any
     */
    def createContentFile(space, path) {
        if (log.debugEnabled) {
            log.debug "Creating content node for server file at [$path]"
        }
        
        def publicStatus = allPublicStatuses[0]
        assert publicStatus
        
        List tokens = path.replace('\\', '/').split('/')
        if (tokens.size()) {
            def parents = tokens
            def ancestor
            def content
            def createdContent
            parents.eachWithIndex(){ obj, i ->
                def parentPath = parents[0..i].join('/')
                log.debug "Creating content for file path $path, checking to see if node exists for ${parentPath}"
                content = findContentForPath(parentPath, space)?.content
                if (!content){
                    def file = WcmContentRepositoryService.getUploadPath(space, parentPath)
                    if (file.isDirectory()){
                        content = new WcmContentDirectory(title: file.name,
                            content: '', filesCount: 0, space: space, orderIndex: 0,
                            mimeType: '', fileSize: 0, status: publicStatus)
                    }else{
                        // @todo This is bugged if x.y.z in filename use our MimeUtils
                        def mimeType = ServletContextHolder.servletContext.getMimeType(file.name)
                        content = new WcmContentFile(title: file.name,
                            content: '', space: space, orderIndex: 0, 
                            mimeType: (mimeType ? mimeType : ''), fileSize: file.length(),
                            status: publicStatus)
                    }

                    createNode(content, ancestor)
                    
                    if (!content.save()) {
                        log.error "Failed to save content ${content} - errors: ${content.errors}"
                        assert false
                    } else {
                        createdContent = content
                    }
                }
                ancestor = content
            }
            return createdContent
        }
        return null
    }
    
    /**
     * Create new content supplied by site visitors
     */
    WcmContent createUserSubmittedContent(space, parent, type, data, request) throws AccessDeniedException {
        if (!(space instanceof WcmSpace)) {
            space = WcmSpace.get(space.toLong())
        }
        assert space
        if (parent) {
            if (!(parent instanceof WcmContent)) {
                parent = WcmContent.get(parent.toLong())
            }
        } else {
            parent = null
        }

        // Need to get the status before we query, or we may force flush
        def unmoderatedStat = getStatusByCode(unmoderatedStatusCode)
        
        Class contentClass = getContentClassForType(type)
        // check CREATE permission on the uri & user
        def n = parent ?: space
        requirePermissions(n, [WeceemSecurityPolicy.PERMISSION_CREATE], contentClass)
        // create content and populate

        // Prevent setting status and other internal values
        def publicProperties = contentClass.publicSubmitProperties 
        def dataKeys = data.collect { k, v -> k }
        // We eliminate all properties that are not in this list, so status etc cannot be set
        if (publicProperties) {
            dataKeys.each { k ->
                if (!publicProperties.contains(k)) {
                    data.remove(k)
                }
            }
        }
        
        // Now create it
        def newContent = createNode(type, data) { c ->
            c.space = space
            c.parent = parent
            c.status = unmoderatedStat
            // We should convention-ize this so they can have different fields
            c.ipAddress = request.remoteAddr
        }
        // Check for binding errors
        return newContent.hasErrors() ? newContent : newContent.save() // it might not work, but hasErrors will be set if not
    }
    
    /**
     * Return a list of month/year pairs for all months where there is content under the parent (children) of the specified type
     * Results are in descending year and month order
     * @todo Implement permissions use here, or is it ok to ignore this, it only leaks dates?
     */
    def findMonthsWithContent(parentOrSpace, contentType) {
        def type = getContentClassForType(contentType)
        def parentClause = parentOrSpace instanceof WcmContent ? "parent = :parent" : "space = :parent"
        def monthsYears = type.executeQuery("""select distinct month(publishFrom), year(publishFrom) from 
${type.name} where $parentClause and status.publicContent = true and publishFrom < current_timestamp() 
order by year(publishFrom) desc, month(publishFrom) desc""", [parent:parentOrSpace])
        return monthsYears?.collect() {
            [month: it[0], year: it[1]]
        }      
    }
        
    /** 
     * Get all the content within a given time period, inclusive at both ends
     * 
     * 
     */
    def findContentForTimePeriod(parentOrSpace, startDate, endDate, args = [:]) {
        if (log.debugEnabled) {
            log.debug "Finding children of ${parentOrSpace} with args $args"
        }
        assert parentOrSpace != null

        // for WcmVirtualContent - the children list is a list of target children
        if (parentOrSpace instanceof WcmVirtualContent) {
            parentOrSpace = parentOrSpace.target
        }

        requirePermissions(parentOrSpace, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        // @todo replace this with smarter queries on children instead of requiring loading of all child objects
        def typeRestriction = getContentClassForType(args.type)
        if (log.debugEnabled) {
            log.debug "Finding children of ${parentOrSpace} restricting type to ${typeRestriction}"
        }
        def children = doCriteria(typeRestriction, args.status, args.params) {
            if (parentOrSpace instanceof WcmSpace) {
                isNull('parent')
                eq('space', parentOrSpace)
            } else {
                eq('parent', parentOrSpace)
            }

            or {
                isNull('publishFrom')
                ge('publishFrom', startDate)
            }
            
            or {
                isNull('publishUntil')
                le('publishUntil', endDate)
            }

            order('publishFrom', 'desc')
            cache true
        }
        
        return children
    }
    
    /**
     * Calculates start and end timestamps for first and last second in given month and year, 
     * returning object containing:
     * start - Date of 00:00:00 at start of month
     * end - Date of 23:59:59 at end of month
     * @param month Number in range 1-12
     * @param year Number 
     */
    def calculateMonthStartEndDates(month, year) {
        def t = new GregorianCalendar()
        t.set(Calendar.DAY_OF_MONTH, 1)
        t.set(Calendar.MILLISECOND, 0)
        t.set(Calendar.HOUR_OF_DAY, 0)
        t.set(Calendar.MINUTE, 0)
        t.set(Calendar.SECOND, 0)
        t.set(Calendar.MONTH, month-1)
        t.set(Calendar.YEAR, year)
        
        def result = [:]
        result.start = t.time
        
        t.set(Calendar.DAY_OF_MONTH, t.getActualMaximum(Calendar.DAY_OF_MONTH))
        t.set(Calendar.HOUR_OF_DAY, t.getActualMaximum(Calendar.HOUR_OF_DAY))
        t.set(Calendar.MINUTE, t.getActualMaximum(Calendar.MINUTE))
        t.set(Calendar.SECOND, t.getActualMaximum(Calendar.SECOND))
        t.set(Calendar.MILLISECOND, t.getActualMaximum(Calendar.MILLISECOND))
        result.end = t.time
        
        return result
    }
    
    /**
     * Calculates start and end timestamps for first and last second in given day, month and year, 
     * returning object containing:
     * start - Date of 00:00:00 at start of the day
     * end - Date of 23:59:59 at end of the day
     * @param day Number in range 1-31
     * @param month Number in range 1-12
     * @param year Number 
     */
    def calculateDayStartEndDates(day, month, year) {
        def t = new GregorianCalendar()
        t.set(Calendar.DAY_OF_MONTH, day)
        t.set(Calendar.MILLISECOND, 0)
        t.set(Calendar.HOUR_OF_DAY, 0)
        t.set(Calendar.MINUTE, 0)
        t.set(Calendar.SECOND, 0)
        t.set(Calendar.MONTH, month-1)
        t.set(Calendar.YEAR, year)
        
        def result = [:]
        result.start = t.time
        
        t.set(Calendar.HOUR_OF_DAY, t.getActualMaximum(Calendar.HOUR_OF_DAY))
        t.set(Calendar.MINUTE, t.getActualMaximum(Calendar.MINUTE))
        t.set(Calendar.SECOND, t.getActualMaximum(Calendar.SECOND))
        t.set(Calendar.MILLISECOND, t.getActualMaximum(Calendar.MILLISECOND))
        result.end = t.time
        
        return result
    }
    
    /**
     * Get a new instance of the Groovy Closure object defined by the WcmScript content node.
     * Uses caching of compiled classes to prevent permgen explosion, and unique classloaders
     */ 
    Closure getWcmScriptInstance(WcmScript s) {
        def absURI = s.absoluteURI
        if (log.debugEnabled) {
            log.debug "Getting Groovy script class for $absURI"
        }
        
        def cls = wcmCacheService.getOrPutObject(CACHE_NAME_GSP_CACHE, makeURICacheKey(s.space, absURI)) {
            if (log.debugEnabled) {
                log.debug "Compiling Groovy script class for $absURI"
            }
            def code = s.content
            def cls = new GroovyClassLoader().parseClass("""codeClosure = { $code }""")
            def script = cls.newInstance()
            script.run()
            return script.binding.codeClosure
        }
        cls.clone()
    }
    
    /**
     * Look for any content pending publication, and move statys to published
     */
    def publishPendingContent() {
        if (log.debugEnabled) {
            log.debug "Looking for content that needs to become published now"
        }
        def now = new Date()
        // Find all content with publication date less than now
        
        // @todo this can be improved by flattening the query, using inList criteria to specify only the list
        // of possible statuses we can process (i.e. all - unmoderated - archived)
        def pendingContent = WcmContent.withCriteria {
            isNotNull('publishFrom')
            lt('publishFrom', now)
            status {
                and {
                    eq('publicContent', false)
                    ne('code', unmoderatedStatusCode)
                    ne('code', archivedStatusCode)
                }
            }
        }
        def count = 0
        pendingContent?.each { content ->
            // Find the next status (in code order) that is public content, after the content's current status
            def status = WcmStatus.findByPublicContentAndCodeGreaterThan(true, content.status.code)
            if (!status) {
                log.error "Tried to publish content ${content} with status [${content.status.dump()}] but found no public status with a code higher than it"
            } else if (log.debugEnabled) {
                log.debug "Transitioning content ${content} from status [${content.status.code}] to [${status.code}]"
            }
            content.status = status
            count++
        }
        return count
    }
    
    WcmStatus getStatusByCode(code) {
        WcmStatus.findByCode(code.toInteger(), [cache: true])
    }
    
    /**
     * Look for any content that has gone past its publishUntil date, and move status to archived
     */
    def archiveStaleContent() {
        def now = new Date()
        // Find all content with publication date less than now
        def staleContent = WcmContent.withCriteria {
            isNotNull('publishUntil')
            le('publishUntil', now)
        }
        def count = 0
        staleContent?.each { content ->
            // Find the next status (in code order) that is public content, after the content's current status
            content.status = getStatusByCode(archivedStatusCode)
            count++
        }
        return count
    }
    
    def searchForContent(String query, WcmSpace space,contentOrPath = null, args = null) {
        if (log.debugEnabled) {
            log.debug "Searching for content with query [$query] in space [$space] under path [$contentOrPath] with args [$args]"
        }
        def baseURI
        if (contentOrPath) {
            if (contentOrPath instanceof WcmContent) {
                baseURI = contentOrPath.absoluteURI
            } else {
                baseURI = contentOrPath.toString()
            }
        }

        def domCls = args.type ?: WcmContent
        
        domCls.search([reload:true, offset:args?.offset ?:0, max:args?.max ?: 25]){
            must(queryString(query))

/* This doesn't work yet
            // Restrict to base URI
            if (baseURI) {
                must {
                    term('absoluteURI', baseURI+'/')
                }
            }
*/
         
                // Restrict to space
            must {
                listContentClassNames().each { n ->
                    def t = '$/'+n.replaceAll('\\.', '_')+'/space/id'
                    term(t, space.id)
                }
            }
        }
    }

    def filterToTypes(listOfContent, String typeNames) {
        def types = typeNames.tokenize(',').collect { n -> grailsApplication.getClassForName(n.trim()) }
        listOfContent.findAll { c -> types.any { t -> t.isAssignableFrom(c.class) } }
    }

    def searchForPublicContentByTag(String tag, WcmSpace space, contentOrPath = null, args = [:]) {
        if (log.debugEnabled) {
            log.debug "Searching for content by tag $tag"
        }
        def baseURI
        if (contentOrPath) {
            if (contentOrPath instanceof WcmContent) {
                baseURI = contentOrPath.absoluteURI
            } else {
                baseURI = contentOrPath.toString()
            }
        }

        def hits = WcmContent.findAllByTagWithCriteria(tag) {
            eq('space', space)

            // @todo apply baseURI
            
            firstResult(args.offset?.toInteger() ?:0)
            maxResults(args.max?.toInteger() ?: 25) 
            // Default to newest content first
            order(args.sort ?: 'createdOn', args.order ?: 'desc')
        }
        // Filter by type if required - probably do this inside the criteria?
        if (args.types) {
            hits = filterToTypes(hits, args.types)
        }
        [results:hits, total:hits.size()]
    }

    def searchForPublicContent(String query, WcmSpace space, contentOrPath = null, args = null) {
        def baseURI
        if (contentOrPath) {
            if (contentOrPath instanceof WcmContent) {
                baseURI = contentOrPath.absoluteURI
            } else {
                baseURI = contentOrPath.toString()
            }
        }
        
        def results = WcmContent.search([reload:true, offset:args?.offset ?:0, max:args?.max ?: 25]){
            must(queryString(query))

            // @todo apply baseURI

            // Restrict to public
            must(term('status_publicContent', true))

            // Restrict to space
            must {
                listContentClassNames( { 
                    def hasSCProp = it.metaClass.hasProperty(it.class, 'standaloneContent')
                    !hasSCProp || it.standaloneContent
                } ).each { n ->
                    def t = '$/'+n.replaceAll('\\.', '_')+'/space/id'
                    term(t, space.id)
                }
            }
        }

        // Filter by type if required
        if (args.types) {
            results.results = filterToTypes(results.results, args.types)
        }
        
        return results
    }
}

