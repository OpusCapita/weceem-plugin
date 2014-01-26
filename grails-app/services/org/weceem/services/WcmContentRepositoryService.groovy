package org.weceem.services

import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.logging.LogFactory

import org.springframework.beans.factory.InitializingBean
import grails.util.Environment
import grails.util.GrailsNameUtils

// This is for a hack, remove later
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.hibernate.exception.ConstraintViolationException

import org.weceem.content.*

//@todo design smell!
import org.weceem.files.*
import org.weceem.script.WcmScript

import org.weceem.event.EventMethod
import org.weceem.event.WeceemEvents
import org.weceem.event.WeceemDomainEvents
import org.weceem.security.*
import org.weceem.content.TemplateUtils

/**
 * WcmContentRepositoryService class provides methods for WcmContent Repository tree
 * manipulations.
 * The service deals with WcmContent and subclasses of WcmContent classes.
 *
 * @author Sergei Shushkevich
 */
class WcmContentRepositoryService implements InitializingBean {

    static log = LogFactory.getLog("grails.app.service."+WcmContentRepositoryService.class.name)

    static final String EMPTY_ALIAS_URI = "_ROOT"
    
    static final CONTENT_CLASS = WcmContent.class.name
    static final STATUS_ANY_PUBLISHED = 'published'
    
    static CACHE_NAME_GSP_CACHE = "gspCache"
    static CACHE_NAME_URI_TO_CONTENT_ID = "uriToContentCache"
    
    static DEFAULT_DOCUMENT_NAMES = ['index', 'index.html']

    static DEFAULT_SPACE_TEMPLATE_ZIP = "classpath:/org/weceem/resources/default-space-template.zip"
    static BASIC_SPACE_TEMPLATE_ZIP = "classpath:/org/weceem/resources/basic-space-template.zip"
    
    static transactional = true

    def uriToIdCache
    def gspClassCache

    
    def grailsApplication
    def wcmImportExportService
    def wcmCacheService
    def groovyPagesTemplateEngine

    def wcmSecurityService
    def wcmEventService
    def wcmContentDependencyService
    def wcmContentFingerprintService
    
    def archivedStatusCode
    def unmoderatedStatusCode
    
    def proxyHandler
    
    ThreadLocal permissionsBypass = new ThreadLocal()
    
    static uploadDir
    static uploadUrl

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
        
        checkDomainModel()
    }
    
    void checkDomainModel() {
        grailsApplication.domainClasses.each { artefact ->
            if ( WcmContent.isAssignableFrom(artefact.clazz) ) {
                def assocProps = findDomainClassContentAssociations(artefact)
                if (!assocProps.every( { it.optional } )) {
                    throw new IllegalArgumentException( 
                        "Content class ${artefact.clazz} has an association to other content that is not nullable. All such references must be nullable")
                }
            }
        }
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
        uploadDirConf ? uploadDirConf.toString() : "/WeceemFiles/"
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
        def config = grailsApplication.config
        def uploadDirConf = getUploadDirFromConfig(config)

        if (!uploadDirConf.startsWith('file:')) {
            def homeDir = new File(System.getProperty("user.home"))
            def weceemHomeDir = new File(homeDir, 'weceem-uploads')
            uploadDir = new File(weceemHomeDir, uploadDirConf) 
            if (!uploadDir.exists()) {
                uploadDir.mkdirs()
            }
        } else {
            def f = new File(new URI(uploadDirConf))
            if (!f.exists()) {
                def ok = f.mkdirs()
                if (!ok) {
                    throw new RuntimeException("Cannot start Weceem - upload directory is set to [${uploadDirConf}] but cannot make the directory and it doesn't exist")
                }
            }
            uploadDir = f
        }

        uploadUrl = WcmContentRepositoryService.getUploadUrlFromConfig(grailsApplication.config)
        // In tests we don't have log
        logOrPrint('info', "Weceem will use [${uploadDir}] as the directory for static uploaded files, and the url [${uploadUrl}] to serve them")
        
        if ( !(config.grails.mime.file.extensions instanceof Boolean) ||
            ((config.grails.mime.file.extensions instanceof Boolean) && (config.grails.mime.file.extensions == true)) ) {
            throw new IllegalArgumentException(
                "Cannot start Weceem - You must change the Config setting 'grails.mime.file.extensions' to false for Weceem to work correctly")
        }
    }
    
    void logOrPrint(String level, String s) {
        if (metaClass.hasProperty(this, 'log')) {
            log?."$level"(s)
        } else {
            println s
        }
    }
    
    void createDefaultSpace() {
        if (WcmSpace.count() == 0) {
            def configValue = grailsApplication.config.weceem.default.space.template
            def templateName = configValue instanceof String ? configValue : 'default'
            withPermissionsBypass {
                createSpace([name:'Default'], templateName)
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

    WcmSpace findSpaceByBlankUri() {
        def results =  WcmSpace.withCriteria() {
            or {
                isNull("aliasURI")
                eq("aliasURI",'')
            }
        }
        if (results instanceof ArrayList) {
            return results.getAt(0)
        } else {
            return results
        }
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
    
    /**
     * Take a URI and work out what space it refers to, and what the remaining URI is
     *
     * Note, this is perhaps one of our most evil pieces of logic. Enter at your peril.
     * @return A map with "space" and "uri" values, with the space resolved to a WcmSpace and uri amended
     */
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
            } else {
                uri = ''
            }
        }
        
        // Let's try to find the space, or page in the root space
        if (!spaceName) {
            if (log.debugEnabled) {
                log.debug "WcmContent request for no space, looking for space with blank aliasURI"
            }
            space = findSpaceByBlankUri()
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
                space = findSpaceByBlankUri()
                if (space) {
                    // put the space name back into uri
                    if (spaceName) {
                        uri = spaceName + '/' + uri
                    }
                    spaceName = ''
                    uri = uri ? (spaceName ? spaceName + '/' : '') + uri : spaceName
                    if (log.debugEnabled) {
                        log.debug "Content request has found space with blank aliasURI, amending uri to include the space name: ${uri}"
                    }
                }
            }
        }        

        if (uri == null) {
            uri = ''
        }
        
        [space:space, uri:uri]
    }
    
    String getSpaceTemplateLocationByName(String name) {
        spaceTemplates[name]
    }
    
    WcmSpace createSpace(params, templateURLOrName = 'default') throws IllegalArgumentException {
        // @todo need to enforce ADMIN permission here, but not per-space, its general admin
        
        def s
        WcmContent.withTransaction { txn ->
            s = new WcmSpace(params)
            if (s.save()) {
                // Create the filesystem folder for the space
                def spaceDir = getUploadPath(s)
                if (!spaceDir.exists()) {
                    spaceDir.mkdirs()
                }

                if (templateURLOrName) {
                    def templateLocation
                    // Only map template name via config if not already given a path
                    if (templateURLOrName.startsWith('file:') || templateURLOrName.startsWith('classpath:')) {
                        templateLocation = templateURLOrName
                    } else {
                        templateLocation = getSpaceTemplateLocationByName(templateURLOrName)
                    }
                    if (templateLocation instanceof ConfigObject) {
                        throw new IllegalArgumentException("No space template defined with name [${templateURLOrName}]")
                    }
                    importSpaceTemplate(templateLocation, s)
                }
            } else {
                log.error "Unable to create space with properties: ${params} - errors occurred: ${s.errors}"
            }
        }
        return s // If this fails we still return the original space so we can see errors
    }
    
    def getSpaceTemplates() {
        def temps = grailsApplication.config.weceem.space.templates 
        def data = [:]
        if (temps.size()) {
            data.putAll(temps)
        } else {
            data.'default' = DEFAULT_SPACE_TEMPLATE_ZIP
            data.'basic' = BASIC_SPACE_TEMPLATE_ZIP
        }
        return data
    }
    
    /**
     * Import a named space template (import zip) into the specified space
     * @param templateLocationOrName Name of a template in classpath:/org/weceem/resources/ or file: or classpath: url to ZIP file
     * @param space The space into which the import is to be performed
     */
    void importSpaceTemplate(String templateLocationOrName, WcmSpace space) {
        // @todo enforce ADMIN permission here
        
        log.info "Importing space template [${templateLocationOrName}] into space [${space.name}]"
        // For now we only load files, in future we may get them as blobs from DB
        def f = File.createTempFile("weceem-space-import", null)
    
        def resourceName = templateLocationOrName.startsWith('file:') || templateLocationOrName.startsWith('classpath:') ?
            templateLocationOrName :
            "classpath:/org/weceem/resources/${templateLocationOrName}-space-template.zip"
            
        def res = grailsApplication.parentContext.getResource(resourceName).inputStream
        if (!res) {
            log.error "Unable to import space template [${templateLocationOrName}] into space [${space.name}], space template not found at resource ${resourceName}"
            return
        }
        f.withOutputStream { os ->
            os << res
        }
        try {
            wcmImportExportService.importSpace(space, 'simpleSpaceImporter', f)
        } catch (Throwable t) {
            log.error "Unable to import space template [${templateLocationOrName}] into space [${space.name}]", t
            throw t // rethrow, this is sort of fatal
        }

        log.info "Successfully imported space template [${templateLocationOrName}] into space [${space.name}]"
        
        invalidateCachingForSpace(space)
    }
    
    void requirePermissions(WcmSpace space, List permissionList, Class<WcmContent> type = null) throws AccessDeniedException {
        if (!permissionsBypass.get()) {
            if (!wcmSecurityService.hasPermissions(space, permissionList, type)) {
                throw new AccessDeniedException("User [${wcmSecurityService.userName}] with roles [${wcmSecurityService.userRoles}] does not have the permissions [$permissionList] to access space [${space.name}]")
            }
        }
    }       
    
    void requirePermissions(WcmContent content, permissionList) throws AccessDeniedException {
        if (!permissionsBypass.get()) {
            if (!wcmSecurityService.hasPermissions(content, permissionList)) {
                throw new AccessDeniedException("User [${wcmSecurityService.userName}] with roles [${wcmSecurityService.userRoles}] does not have the permissions [$permissionList] to access content at [${content.absoluteURI}] in space [${content.space.name}]")
            }
        }
    }       

    void requirePermissionToCreateContent(WcmContent parentContent, WcmContent content) throws AccessDeniedException {
        if (!permissionsBypass.get()) {
            if (!wcmSecurityService.isUserAllowedToCreateContent(content.space, parentContent, content)) {
                throw new AccessDeniedException("User [${wcmSecurityService.userName}] with roles [${wcmSecurityService.userRoles}] does not have the permissions to create content under [${parentContent ? parentContent.absoluteURI + '/' + content.aliasURI : content.aliasURI}] in space [${content.space.name}]")
            }
        }
    }

    // Workaround for removeFromChildren breaking for us
    private void removeContentFromParent(WcmContent content) {
        if (content.parent) {
            content.parent.children = content.parent.children.findAll { it.ident() != content.ident() }
            content.parent = null
        }
    }

    void deleteSpaceContent(WcmSpace space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        log.info "Deleting content from space [$space]"
        // Let's brute-force this
        // @todo remove/rework this for 0.2
		// purge all parent-children relationships essentially flattening out the hierarchy
        def contentList = WcmContent.findAllBySpace(space)
        for (content in contentList){
            // Invalidate the caches before parent is changed
            invalidateCachingForURI(content.space, content.absoluteURI)

            removeContentFromParent(content)
            content.parent = null
            content.unindex()
            content.save()
        }
		
		// loop and delete unreferenced content
		int size = contentList.size()
		while (size > 0) {
			log.debug "Deleting unreferenced content in space [$space]; ${size} items remain to be deleted"
			contentList = deleteUnreferencedContent(contentList)
			if (contentList.size() == size) {
				throw new RuntimeException("Could not find any unreferenced content. So, cannot continue deleting.")
			}
			size = contentList.size()
		}
		
		// old code which has issues: http://jira.jcatalog.com/browse/WCM-251
//        // @todo This code is very naïve and probably broken
//        // It needs leaf-first dependency ordering and/or brute force clearing of all refs
//        eachContentDepthFirst(space) { node ->
//            log.debug "Finding references to content [${node.aliasURI}] in space [$space]"
//            removeAllReferencesTo([node])
//            deleteNode(node)
//        }
        log.info "Finished deleting content from space [$space]"
    }
    
	/**
	 * Deletes all items in a list which are not referenced by other items in the same list
	 * and returns a list of those items which are referenced and which could not be deleted.
	 * @param contentList the list of items to delete
	 * @return the list of items which could not be deleted
	 */
	List<WcmContent> deleteUnreferencedContent(List<WcmContent> contentList) {
		def result = []
		for (content in contentList) {
			if (isReferred(content, contentList)) {
				result += content
			}
			else {
				deleteNode(content)
			}
		}
		result
	}
	
	/**
	 * Checks if a content item is referenced by some other content item in a list.
	 * @param content the item to check
	 * @param contentList the list of items within which we search for referers
	 * @return true, iff the item is referenced by some other content item in the list
	 */
	boolean isReferred(WcmContent content, List<WcmContent> contentList) {
		requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])
		
		// @todo this will perform rather poorly. We should find all assocation properties FIRST
		// and then run a query for each association, which - with caching - should run a lot faster than
		// checking every property on every node
		for (cont in contentList) {
			def perProps = findDomainClassContentAssociations(getDomainClassArtefact(cont))
			for (p in perProps){
				if (cont."${p.name}") {
					if (cont."${p.name}" instanceof Collection){
						for (inst in cont."${p.name}"){
							if (inst.equals(content)) {
								return true
							}    
						}
					} else {
						if (content.equals(cont."${p.name}")){
							return true
						}
					}
				}
			}
		}
		
        false
	}
	
    void eachContentDepthFirst(WcmSpace space, Closure code) {
        def rootNodes = findAllRootContent(space, [sort:'orderIndex'])
        rootNodes.each { root ->
            findDescendentsDepthFirst(root).each { d ->
                code(d)
            }
            code(root)
        }
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
        return [id: content.id, className: proxyHandler.unwrapIfProxy(content).class.name,
                title: content.title, createdBy: content.createdBy,
                createdOn: content.createdOn, changedBy: content.changedBy,
                changedOn: content.changedOn,
                // @todo replace this ugliness with polymorphic calls
                summary: content.metaClass.hasProperty(content, 'summary') ? content.summary : null,
                contentType: proxyHandler.unwrapIfProxy(content).class.name]
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
        def changes = WcmContentVersion.findAllByObjectKeyAndObjectClassName(content.ident(), 
            proxyHandler.unwrapIfProxy(content).class.name, args)
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
    def createNode(type, params, Closure postInit = null) {
        
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
    def triggerEvent(WcmContent content, EventMethod event, arguments = null) {
        assert event.definedIn(WeceemEvents)

        def outcome = triggerDomainEvent(content, WeceemDomainEvents[event.name], arguments)
        if (outcome) {
            wcmEventService.event(event, content)
        }
        return outcome
    }
    
    /**
     * Trigger an event on the domain class only.
     *
     */
    def triggerDomainEvent(WcmContent content, EventMethod event, arguments = null) {
        assert event.definedIn(WeceemDomainEvents)
        
        if (log.debugEnabled) {
            log.debug "Attempting to trigger domain event [$event] on node ${content.dump()}"
        }
        def eventName = event.toString()
        if (content.conformsTo(event)) {
            if (log.debugEnabled) {
                log.debug "Trigger domain event [$event] as it is supported on node ${content.dump()}"
            }
            // Call the event so that nodes can perform post-creation tasks
            return event.invokeOn(content, arguments)
        } else {
            if (log.debugEnabled) {
                log.debug "Node does not support [$event] event, skipping"
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
        requirePermissionToCreateContent(parentContent, content)
        
        if (parentContent == null) parentContent = content.parent

        if (log.debugEnabled) {
            log.debug "Creating node: ${content.dump()} with parent [$parentContent]"
        }
        
        def result = true

        if (parentContent) {
            result = triggerDomainEvent(parentContent, WeceemDomainEvents.contentShouldAcceptChildren)
            if (result) {
                result = triggerDomainEvent(parentContent, WeceemDomainEvents.contentShouldAcceptChild, [content])
            } else if (log.infoEnabled) {
                log.info "Cancelled creating node, shouldAcceptChild returned false for: ${content.dump()}"
            }
        }
        
        if (result) {
            result = triggerDomainEvent(content, WeceemDomainEvents.contentShouldBeCreated, [parentContent])
            if (!result && log.infoEnabled) {
                log.info "Cancelled creating node, shouldBeCreated returned false for: ${content.dump()} with parent [$parentContent]"
            }
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
                orderIndex = nodes ? (nodes[0].orderIndex ?: 0) + 1 : 0
            }
            content.orderIndex = orderIndex

            if (parentContent) {
                parentContent.addToChildren(content)
            }
            
            if (result) {
                // We complete the AliasURI, AFTER handling the create() event which may need to affect title/aliasURI
                if (!content.aliasURI) {
                    content.createAliasURI(parentContent)
                }
            
                // Short circuit out of here if not valid now
                def valid = content.validate()
                if (!valid) {
                    // If its not just a blank aliasURI error, get out now
                    if (!(content.errors.errorCount == 1 && content.errors.getFieldErrors('aliasURI').size() == 1)) {
                        result = false
                    }
                }
            }
            
            if (result) {
                // Auto-set publishFrom to now if content is created as public but no publishFrom specified
                // Required for blogs and sort by publishFrom to work
                if (content.status?.publicContent && (content.publishFrom == null)) {
                    content.publishFrom = new Date()
                }
            
            }
            
            if (result) {

                triggerEvent(content, WeceemEvents.contentWillBeCreated, [parentContent])

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
                content.index()

                triggerEvent(content, WeceemEvents.contentDidGetCreated)
            }

            if (!result) {
                parentContent?.discard() // revert the changes we made to parent
            } else {
                parentContent?.save(flush:true)
            }
            
            invalidateCachingForURI(content.space, content.absoluteURI)
            updateCachingMetadataFor(content)
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
        vcont.index()
        return vcont
    }

    /**
     * Changes content node reference.
     *
     * @param sourceContent
     * @param targetContent
     */
    void moveNode(WcmContent sourceContent, WcmContent targetContent, orderIndex) throws ContentRepositoryException {
        if (log.debugEnabled) {
            log.debug "Moving content ${sourceContent} to ${targetContent} at order index $orderIndex"
        }
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_EDIT,WeceemSecurityPolicy.PERMISSION_VIEW])        

        if (!sourceContent) {
            throw new IllegalArgumentException("sourceContent cannot be null")
        }

        // Do an ugly check for unique uris at root
        def criteria = WcmContent.createCriteria()
        def nodes = criteria {
            eq("space", sourceContent.space)
            if (!targetContent) {
                isNull("parent")
            } else {
                eq('parent.id', targetContent.ident())
            }
            eq("aliasURI", sourceContent.aliasURI)
            not {
                idEq(sourceContent.ident())
            }
        }
        
        if (nodes.size() > 0){
            throw new IllegalArgumentException("Another node at the root of your repository has the same aliasURI")
        } 

        // We need this to invalidate caches
        def originalURI = sourceContent.absoluteURI
        def originalParent = sourceContent.parent
        
        def parentChanged = targetContent != originalParent
        if (targetContent && parentChanged) {
            if (!triggerDomainEvent(targetContent, WeceemDomainEvents.contentShouldAcceptChild, [sourceContent])) {
                throw new InvalidDestinationException("This node is not accepted by the target '${targetContent.absoluteURI}'")
            }
        }

        if (parentChanged) {
            if (!triggerDomainEvent(sourceContent, WeceemDomainEvents.contentShouldMove, [targetContent])) {
                throw new InvalidDestinationException("This node cannot move to the target '${targetContent?.absoluteURI}'")
            }
        }
        
        if (parentChanged) {
            // Transpose to new parent
            def parent = sourceContent.parent
            if (parent) {
                parent.children.remove(sourceContent)
                sourceContent.parent = null
                assert parent.save()
                parent.reindex()
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

            // Check we can be saved there
            requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
            
            assert targetContent.save()
            targetContent.reindex()
        } else {
            // Check we can be saved there
            requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        }

        // Invalidate the caches 
        invalidateCachingForURI(sourceContent.space, originalURI)

        if (sourceContent.save(flush: true)) {
            updateCachingMetadataFor(sourceContent)

            sourceContent.reindex()
            triggerEvent(sourceContent, WeceemEvents.contentDidMove, [originalURI, originalParent])
        } else {
            log.error "Couldn't save node: ${sourceContent.errors}"
            if (sourceContent.errors.hasFieldErrors('aliasURI')) {
                throw new IllegalArgumentException("Another child of the target has the same alias URI. Change the alias URI first and then move again.")
            } else {
                throw new UpdateFailedException("The node could not be saved")
            }
        }
     }
     
    def shiftNodeChildrenOrderIndex(WcmSpace space, WcmContent parent, shiftedOrderIndex){
        if (log.debugEnabled) {
            log.debug "Updating node order indexes in space ${space} for parent ${parent}"
        }
        requirePermissions(parent ?: space, [WeceemSecurityPolicy.PERMISSION_EDIT])        
        
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
            it.reindex()
        }
    }
    
    
    def findDomainClassContentAssociations(domainArtefact) {
        domainArtefact.persistentProperties.findAll { p -> 
            p.isAssociation() && WcmContent.isAssignableFrom(p.referencedPropertyType)
        }        
    }

    def getDomainClassArtefact(domainInstance) {
        def realClass = proxyHandler.unwrapIfProxy(domainInstance).class
        grailsApplication.getDomainClass(realClass.name)
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
            def perProps = findDomainClassContentAssociations(getDomainClassArtefact(cont))
            for (p in perProps){
                if (cont."${p.name}") {
                    if (cont."${p.name}" instanceof Collection){
                        for (inst in cont."${p.name}"){
                            if (inst.equals(content)){
                                results << new ContentReference(referringProperty: p.name, referencingContent: cont, targetContent: content)
                            }
                        }
                    } else {
                        if (content.equals(cont."${p.name}")){
                            results << new ContentReference(referringProperty: p.name, referencingContent: cont, targetContent: content)
                        }
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
     * Null all non-hierarchy references to the content nodes listed.
     * This is expensive. Parent and children associations are NOT affected
     */
    void removeAllReferencesTo(List<WcmContent> nodes) {
        if (log.debugEnabled) {
            log.debug "Removing all references to the following nodes: ${nodes.absoluteURI}"
        }
        for (node in nodes) {
            def refs = findReferencesTo(node)
            for (ref in refs) {
                if ( !(ref.referringProperty in ['parent', 'children']) ) {
                    if (log.debugEnabled) {
                        log.debug "Removing references ${ref.referencingContent.absoluteURI}.${ref.referringProperty} to the node: ${node.absoluteURI}"
                    }
                    ref.referencingContent[ref.referringProperty] = null
                    // Need to update this so that deps of this referencing node do not continue to reference this
                    wcmContentDependencyService.updateDependencyInfoFor(ref.referencingContent)
                }
            }
            wcmContentDependencyService.updateDependencyInfoFor(node)
        }
    }
    
    /**
     * Delete multiple nodes at once, avoiding problems where parent is deleted before descendents
     * If you try to delete X and X/P/Z, you have to do it in the right order and don't bother deleting children
     * if already deleted.
     */
    void deleteNodes(List<WcmContent> nodesToDelete, boolean deleteChildren = false) throws DeleteNotPossibleException {
        if (log.infoEnabled) {
            log.info "Bulk deleting nodes: ${nodesToDelete.absoluteURI}"
        }
        
        def deletionInAscendingURIOrder = nodesToDelete.collect({ [uri:it.absoluteURI, node:it] }).sort { it.uri }
        def alreadyDeletedAncestorURIs = []
        
        for (nodeInfo in deletionInAscendingURIOrder) {
            def node = nodeInfo.node
            def nodeURI = nodeInfo.uri
            if (!deleteChildren || !(alreadyDeletedAncestorURIs.any { uri -> nodeURI.startsWith(uri) })) {
                deleteNode(node, deleteChildren)
                alreadyDeletedAncestorURIs << (nodeURI + '/')
            }
        }
    }
    
    /**
     * Deletes content node and all it's references.
     * All children of sourceContent will be assigned to all its parents.
     *  
     * @param sourceContent
     */
    void deleteNode(WcmContent sourceContent, boolean deleteChildren = false) throws DeleteNotPossibleException {
        if (log.debugEnabled) {
            log.debug "Deleting ${sourceContent?.absoluteURI} (including children? $deleteChildren)"
        }

        if (!sourceContent) {
            throw new DeleteNotPossibleException("Could not delete content, it is null")
        }
        
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_DELETE])        
        
        // We can't delete if we have children
        if (!deleteChildren && sourceContent.children?.size() > 0) {
            log.error "Could not delete content, it has children"
            throw new DeleteNotPossibleException("Could not delete content, it has children")
        }

        // Get the dependencies before we delete it
        def deps = wcmContentDependencyService.getContentDependentOn(sourceContent)
        if (log.debugEnabled) {
            wcmContentDependencyService.dumpDependencyInfo()
        }
        if (deps) {
            log.error "Could not delete content, it has other nodes dependent on it: ${deps*.absoluteURI}"
            throw new DeleteNotPossibleException("Could not delete content, it has content dependent on it: ${deps*.absoluteURI.join(', ')}")
        }

        if (!triggerDomainEvent(sourceContent, WeceemDomainEvents.contentShouldBeDeleted)) {
            log.error "Could not delete content, it has vetoed it"
            throw new DeleteNotPossibleException("Could not delete content, it has vetoed it")
        }

        // Create a versioning entry
        sourceContent.saveRevision(sourceContent.title, sourceContent.space.name)
        
        triggerEvent(sourceContent, WeceemEvents.contentWillBeDeleted)

        if (deleteChildren) {
            if (log.debugEnabled) {
                log.debug "Deleting children of ${sourceContent.absoluteURI}"
            }
            def children = sourceContent.children.collect { it }
            children.each { deleteNode(it, true) }
        }
        
        // Do this now before absoluteURI gets trashed by changing the parent
        invalidateCachingForURI(sourceContent.space, sourceContent.absoluteURI)

        def parent = sourceContent.parent

        removeAllTagsFrom(sourceContent)
        // if there is a parent  - we delete node from its association
        if (parent) {
            removeContentFromParent(sourceContent)
            assert parent.save(flush:true)
            parent.reindex()
        }

        // We don't want any dep info relating to us to persist
        wcmContentFingerprintService.invalidateFingerprintFor(sourceContent)
        wcmContentDependencyService.removeDependencyInfoFor(sourceContent)

        // Strip off associations
        def artef = grailsApplication.getArtefact(org.codehaus.groovy.grails.commons.DomainClassArtefactHandler.TYPE, 
                proxyHandler.unwrapIfProxy(sourceContent).class.name)
                
        // Nuke all the references from this node to other content
        artef.persistentProperties.each { p ->
            if (p.association && !p.owningSide) {
                if (p.manyToMany || p.oneToMany) {
                    sourceContent[p.name]?.clear()
                } else {
                    sourceContent[p.name] = null // detach association
                }
            }
        }

        sourceContent.delete(flush: true)
        sourceContent.unindex()


        triggerEvent(sourceContent, WeceemEvents.contentDidGetDeleted)
    }

    /**
     * Deletes content reference 
     *
     * @param child
     * @param parent
     * @deprecated This is old tat
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
            parentRef.reindex()
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
        // Eager fetch to avoid problems with lazy loading if errors occur
        WcmContent content = WcmContent.findById(id, [fetch:[children:'eager']])
        
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        if (content) {
            return updateNode(content, params)
        } else {
            return [notFound:true]
        }        
    }
    
    // @todo This is a hack so we can bind without x.properties = y which is broken in production on Grails 1.2-M2
    public hackedBindData(obj, params) {
//        def transientProps = obj.metaClass.hasProperty(null, 'transients') ? obj.class.transients : []
        def excludes
        new BindDynamicMethod().invoke(this /* dummy value */, 'bindData', obj, params, [exclude:excludes])
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

        wcmContentDependencyService.reload(space)        
        wcmContentFingerprintService.reset() // @todo prefer to do this per-space...        
    }

    /**
     * Flush all uri caches for given space and uri prefix
     */
    void invalidateCachingForURI( WcmSpace space, uri) {
        // If this was content that created a cached GSP class, clear it now
        def key = makeURICacheKey(space,uri)
        if (log.debugEnabled) {
            log.debug "Removing cached info for cache key [$key]"
        }
        
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
        
        // Get read-only instance now, for persisting revision info after we bind and successfully update
        def contentForRevisionSave = proxyHandler.unwrapIfProxy(content).class.read(content.id)

        def oldTitle = content.title
        // map in new values
        hackedBindData(content, params)
        
        if (!content.hasErrors()) {
        
            if (params.tags != null) {
                content.setTags(params.tags.tokenize(',').collect { it.trim().toLowerCase()} )
            }
            triggerDomainEvent(content, WeceemDomainEvents.contentDidChangeTitle, [oldTitle])
            
            if (log.debugEnabled) {
                log.debug("Updated node with id ${content.id}, properties are now: ${content.dump()}")
            }
            if (!content.aliasURI && content.title) {
                content.createAliasURI(content.parent)
            }

            if (content.status.publicContent && (content.publishFrom == null)) {
                // Auto-set publishFrom to now if content is created as public but no publishFrom specified
                // Required for blogs and sort by publishFrom to work
                content.publishFrom = new Date()
            } else if (!content.status.publicContent && content.publishFrom && (content.publishFrom < new Date())) {
                // Automatically clear the publishFrom if it was in the past, as it does not make sense and will
                // be automatically re-published. Future publishFrom must have been manually set so 
                // its ok to keep it. Doing this stops content with auto publishFrom being pushed
                // back to published soon after edit to non-published without manually clearing publishFrom
                content.publishFrom = null
            }

            def ok = content.validate()
            if (content.save()) {
                // Save the revision now
                contentForRevisionSave.saveRevision(params.title ?: oldTitle, content.space.name)

                if (log.debugEnabled) {
                    log.debug("Update node with id ${content.id} saved OK")
                }
            
                // Invalidate the old URI's info
                invalidateCachingForURI(content.space, oldAbsURI)
                // Invalidate the new URI's info - it might exist already but with no data (not found)
                invalidateCachingForURI(content.space, content.absoluteURI)
                updateCachingMetadataFor(content)

                content.reindex()

                triggerEvent(content, WeceemEvents.contentDidGetUpdated)

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
     * Run all repo service methods inside the closure without any permissions checks.
     * Inherently dangerous but needed to build up correct fingerprint and dependency info.
     * @todo Block access to this and security policy beans from Groovy script actions
     */
    protected withPermissionsBypass(Closure code) {
        try {
            permissionsBypass.set(true)

            return code()
            
        } finally {
            permissionsBypass.set(false)
        }
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

        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        // Short circuit out of here without querying if content cannot have children
        if (!triggerDomainEvent(sourceNode, WeceemDomainEvents.contentShouldAcceptChildren)) {
            return []
        }
        
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
                    
        return onlyNodesWithPermissions(children, [WeceemSecurityPolicy.PERMISSION_VIEW])        
    }

    def onlyNodesWithPermissions(srcList, List permissionsNeeded) {
        srcList?.findAll { c -> wcmSecurityService.hasPermissions(c, permissionsNeeded) }
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
            return WcmStatus.findAllByPublicContent(true, [cache:true]).find { it == node.status }
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
            if (typeRestriction ? typeRestriction.isAssignableFrom(proxyHandler.unwrapIfProxy(it).class) : true) {
                parents << it
            }
        }
        return onlyNodesWithPermissions(sortNodes(parents, args.params?.sort, args.params?.order), 
            [WeceemSecurityPolicy.PERMISSION_VIEW])        
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
        def res = doCriteria(getContentClassForType(args.type), args.status, args.params) {
            isNull('parent')
            eq('space', space)
            cache true
        }
        return onlyNodesWithPermissions(res, [WeceemSecurityPolicy.PERMISSION_VIEW])        
    }
    
    /**
     * find all nodes by type and space
     */ 
    def findAllContent(WcmSpace space, Map args = Collections.EMPTY_MAP) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        if (log.debugEnabled) {
            log.debug "findAllContent $space, $args"
        }
        def res = doCriteria(getContentClassForType(args.type), args.status, args.params) {
            eq('space', space)
            cache true
        }
        return onlyNodesWithPermissions(res, [WeceemSecurityPolicy.PERMISSION_VIEW])        
    }

    def getCachedContentInfoFor(space, uriPath) {
        def cacheKey = makeURICacheKey(space, uriPath)
        def cachedElement = uriToIdCache.get(cacheKey)
        cachedElement?.getValue()
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
        if (uriPath == null) {
            uriPath = ''
        }
        
        def c
        def isFolderURL = uriPath?.endsWith('/')
        // If it doesn't end in / and its not blank, try to find it
        if (!isFolderURL && uriPath) {
            c = doFindContentForPath(uriPath,space,useCache)
        }
            
        // If it is a folder ending in / or not yet found, try default documents
        if (isFolderURL || !c?.content) {
            // Add slash to end if required (not for root)
            if (!isFolderURL && uriPath) {
                uriPath += '/'
            }

            // Look for default document aliasURIs
            for (n in DEFAULT_DOCUMENT_NAMES) {
                def u = uriPath+n
                c = doFindContentForPath(u,space,useCache)
                if (c?.content) {
                    break;
                }
            }
        }
        
        return c
    }
    
    protected def doFindContentForPath(String uriPath, WcmSpace space, boolean useCache) {
        if (log.debugEnabled) {
            log.debug "findContentForPath uri: ${uriPath} space: ${space}"
        }

        if (useCache) {
            // This looks up the uriPath in the cache to see if we can get a Map of the content id and parentURI
            // If we call getValue on the cache hit, we lose 50% of our performance. Just retrieving
            // the cache hit is not expensive.
            def cachedContentInfo = getCachedContentInfoFor(space, uriPath)
            if (cachedContentInfo) {
                if (log.debugEnabled) {
                    log.debug "Found content info in cache for uri $uriPath: ${cachedContentInfo}"
                }
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
            def cacheKey = makeURICacheKey(space, uriPath)

            wcmCacheService.putToCache(uriToIdCache, cacheKey, cacheValue)
        }
        
        if (content) {
            requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        

            [content:content, parentURI:parentURI, lineage:lineage]
        } else {
            return null
        }
    }
    
    /**
     * @deprecated Use normal content methods since 0.9
     */
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

/*    
    def getAncestors(uri, sourceNode) {
        // Can't impl this yet
    }
*/
    /**
     * Return a flattened list of all the descendents of this node
     */
    def findDescendentsDepthFirst(WcmContent parent, Set<WcmContent> alreadyVisited = []) {
        alreadyVisited << parent
        def r = findChildren(parent, [sort:'orderIndex'])
        def result = []
        r.each { c ->
            if (!alreadyVisited.contains(c)) {
                result.addAll(findDescendentsDepthFirst(c, alreadyVisited))
            }
            result << c
        }
        return result
    }

    /**
     * Return a flattened list of all the descendents of this node
     */
    def findDescendents(WcmContent parent, Set<WcmContent> alreadyVisited = []) {
        alreadyVisited << parent
        def r = findChildren(parent, [sort:'orderIndex'])
        def result = r.clone()
        r.each { c ->
            if (!alreadyVisited.contains(c)) {
                result.addAll(findDescendents(c, alreadyVisited))
            }
        }
        return result
    }

    def updateCachingMetadataFor(WcmContent content) {
        wcmContentDependencyService.updateDependencyInfoFor(content)
        wcmContentFingerprintService.updateFingerprintFor(content)

        if (log.debugEnabled) {
            wcmContentDependencyService.dumpDependencyInfo()
            wcmContentFingerprintService.dumpFingerprintInfo()
        }
    }
    
    def getTemplateForContent(def content) {
        TemplateUtils.getTemplateForContent(content)
    }
    
    def getLastModifiedDateFor(WcmContent content) {
        def templ = getTemplateForContent(content)
        // @todo Do we also need to check dependencies' (and their dependencies') last mod dates?
        def templLastMod = templ?.lastModified
        def contentLastMod = content.lastModified
        return (templLastMod > contentLastMod) ? templLastMod : contentLastMod
    }
    
    /** 
     * Determine if the content node is able to be rendered to visitors.
     * @return false if this content is not meant to be rendered, and is instead a component of other content
     */
    boolean contentIsRenderable(WcmContent content) {
        // See if it is renderable directly - eg WcmWidget and WcmTemplate are not renderable on their own
        if (content.metaClass.hasProperty(proxyHandler.unwrapIfProxy(content).class, 'standaloneContent')) {
            return proxyHandler.unwrapIfProxy(content).class.standaloneContent
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
        def clashingFiles = new TreeSet()
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
                if (newFileContent) {
                    createdContent << newFileContent
                    existingFiles.add(newFileContent)
                    content = newFileContent.parent
                    while (content) {
                        if (!existingFiles.find { c -> c.absoluteURI == content.absoluteURI } ) {
                            existingFiles.add(content)
                        }
                        content = content.parent
                    }
                } else {
                    log.warn "Skipping server file ${relativePath}, cannot be created"
                }
            } else {
                log.debug "Skipping server file ${relativePath}, already has content node"
                if (proxyHandler.unwrapIfProxy(content).class in [WcmContentDirectory, WcmContentFile]) {
                    existingFiles.add(content)
                } else {
                    log.debug "Skipping server file ${relativePath}, a non-directory/file node already exists"
                    clashingFiles.add(content)
                }
            }
        }
        
        // @todo Remove this findAll which is a workaround for Grails 2 RC1 bug where all types are returned.
        def allFiles = WcmContentFile.findAllBySpace(space).findAll { n -> n instanceof WcmContentFile }
        
        def existingIds = existingFiles*.id
        def missedFiles = allFiles.findAll { f ->
            def adding = !existingIds.contains(f.id)
            if (adding) {
                log.info "Adding ${f.absoluteURI} (${proxyHandler.unwrapIfProxy(f).class}) to list of orphaned files who have no content node"
            }
            return adding
        }
        
        return ["created": createdContent, "missed": missedFiles, clashing: clashingFiles]
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

                    def ok = createNode(content, ancestor)
                    
                    if (!ok) {
                        log.warn "Failed to save content ${content} - errors: ${content.errors}"
                        return null
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

        // check CREATE permission on the new node
        requirePermissions(newContent, [WeceemSecurityPolicy.PERMISSION_CREATE])

        // Check for binding errors
        if (newContent.hasErrors()) {
            log.error("Submitted Content has errors: ")
            newContent.errors.allErrors.each { log.error(it) }
            return newContent
        } else {
            newContent.save()
            newContent.index()
            return newContent
        }
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
            log.debug "Finding children of ${parentOrSpace} for time period ${startDate} - ${endDate} with args $args"
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
                and {
                    ge('publishFrom', startDate)
                    le('publishFrom', endDate)
                }
            }
            
            order('publishFrom', 'desc')
            cache true
        }
        
        return onlyNodesWithPermissions(children, [WeceemSecurityPolicy.PERMISSION_VIEW])        
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
            cache false
        }
        def count = 0
        pendingContent?.each { content ->
            // Find the next status (in code order) that is public content, after the content's current status
            def status
            WcmStatus.withNewSession { status = WcmStatus.findByPublicContentAndCodeGreaterThan(true, content.status.code) }

            if (!status) {
                log.error "Tried to publish content ${content} with status [${content.status.dump()}] but found no public status with a code higher than it"
            } else if (log.debugEnabled) {
                log.debug "Publish: Transitioning content ${content} from status [${content.status.code}] to [${status.code}]"
            }
            content.status = status
            content.save() // This shouldn't be needed, but without it we get "collection not processed by flush"
            content.reindex()
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
            status {
                and {
                    eq('publicContent', true)
                    ne('code', archivedStatusCode)
                }
            }
            cache false
        }
        def count = 0
        def archivedCode
        WcmStatus.withNewSession { 
            archivedCode = getStatusByCode(archivedStatusCode) 
        }
        
        staleContent?.each { content ->
            if (log.debugEnabled) {
                log.debug "Archive: Transitioning content ${content} from status [${content.status.code}] to [${archivedCode.code}]"
            }
            content.status = archivedCode
            content.save() // This shouldn't be needed, but without it we get "collection not processed by flush"
            content.reindex()
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
        def types = []
        typeNames.tokenize(',').each { n -> 
            def cls = grailsApplication.getClassForName(n.trim()) 
            if (cls) {
                types << cls
            } else {
                log.warn "Attempt to filter content nodes by type ${n} but this class is not known"
            }
        }
        return listOfContent.findAll { c -> 
            types.any { t -> 
                t.isAssignableFrom(proxyHandler.unwrapIfProxy(c).class)
            }
        }
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
        if (!query) {
            return [total:0]
        }
        
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
                    def hasSCProp = it.metaClass.hasProperty(proxyHandler.unwrapIfProxy(it).class, 'standaloneContent')
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
            results.total = results.results.size()
        }
        
        return results
    }
    
    void resetAllCaches() {
        uriToIdCache.removeAll()
        gspClassCache.removeAll()
        wcmContentFingerprintService.reset()        
        wcmContentDependencyService.reload()        
    }
}

