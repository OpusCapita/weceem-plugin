package org.weceem.services

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.springframework.beans.factory.InitializingBean
import grails.util.Environment
// This is for a hack, remove later
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod


import org.weceem.content.*

//@todo design smell!
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*

import org.weceem.security.*

/**
 * ContentRepositoryService class provides methods for Content Repository tree
 * manipulations.
 * The service deals with Content and subclasses of Content classes.
 *
 * @author Sergei Shushkevich
 */
class ContentRepositoryService implements InitializingBean {

    static final CONTENT_CLASS = Content.class.name
    static final STATUS_ANY_PUBLISHED = 'published'
    
    static GSP_CONTENT_CLASSES = [ Template.class, Widget.class ]
    static CACHE_NAME_GSP_CACHE = "gspCache"
    static CACHE_NAME_URI_TO_CONTENT_ID = "uriToContentCache"
    
    static transactional = true

    def uriToIdCache
    def grailsApplication
    def importExportService
    def cacheService
    def groovyPagesTemplateEngine
    def weceemSecurityService
    
    static DEFAULT_STATUSES = [
        [code:100, description:'draft', publicContent:false],
        [code:200, description:'reviewed', publicContent:false],
        [code:300, description:'approved', publicContent:false],
        [code:400, description:'published', publicContent:true]
    ]
    
    void afterPropertiesSet() {
        uriToIdCache = cacheService.getCache(CACHE_NAME_URI_TO_CONTENT_ID)
        assert uriToIdCache
    }
    
    void createDefaultSpace() {
        if (Environment.current != Environment.TEST) {
            if (Space.count() == 0) {
                createSpace([name:'Default'])
            }
        }
    }
    
    void createDefaultStatuses() {
        if (Status.count() == 0) {
            DEFAULT_STATUSES.each {
                assert new Status(it).save()
            }
        }
    }
    
    List getAllPublicStatuses() {
        Status.findAllByPublicContent(true, [cache:true])
    }
    
    Space findDefaultSpace() {
        def space
        def spaces = Space.list([cache:true])
        if (spaces) {
            space = spaces[0]
        }        
        return space
    }
    
    Space findSpaceByURI(String uri) {
        Space.findByAliasURI(uri, [cache:true])
    }
    
    Map resolveSpaceAndURI(String uri) {
        def spaceName
        def space
        
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
                log.debug "Content request for no space, looking for space with blank aliasURI"
            }
            space = findSpaceByURI('')
            if (!space) {
                if (log.debugEnabled) {
                    log.debug "Content request for no space, looking for any space, none with blank aliasURI"
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
                    log.debug "Content request has no space found in database, looking for space with blank aliasURI to see if doc is there"
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

        if (!uri) {
            uri = 'index'
        }

        [space:space, uri:uri]
    }
    
    Space createSpace(params, templateName = 'default') {
        def s
        Content.withTransaction { txn ->
            s = new Space(params)
            if (s.save()) {
                if (templateName) {
                    importSpaceTemplate('default', s)
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
    void importSpaceTemplate(String templateName, Space space) {
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
            importExportService.importSpace(space, 'simpleSpaceImporter', f)
        } catch (Throwable t) {
            log.error "Unable to import space template [${templateName}] into space [${space.name}]", t
            throw t // rethrow, this is sort of fatal
        }
        log.info "Successfully imported space template [${templateName}] into space [${space.name}]"
    }
    
    void requirePermissions(Space space, permissionList) throws AccessDeniedException {
        if (!weceemSecurityService.hasPermissions(space, permissionList)) {
            throw new AccessDeniedException("User [${weceemSecurityService.userName}] with roles [${weceemSecurityService.userRoles}] does not have the permissions [$permissionList] to access space [${space.name}]")
        }
    }       
    
    void requirePermissions(Content content, permissionList) throws AccessDeniedException {
        if (!weceemSecurityService.hasPermissions(content, permissionList)) {
            throw new AccessDeniedException("User [${weceemSecurityService.userName}] with roles [${weceemSecurityService.userRoles}] does not have the permissions [$permissionList] to access content at [${content.absoluteURI}] in space [${content.space.name}]")
        }
    }       

    void deleteSpaceContent(Space space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        log.info "Deleting content from space [$space]"
        // Let's brute-force this
        // @todo remove/rework this for 0.2
        def contentList = Content.findAllBySpace(space)
        for (content in contentList){
            content.parent = null
            content.save()
        }
        def wasDelete = true
        while (wasDelete){
            contentList = Content.findAllBySpace(space)
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
    
    void deleteSpace(Space space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        // Delete space content
        deleteSpaceContent(space)
        // Delete space
        space.delete(flush: true)
    }

    def getGSPTemplate(pageName, content) {
        cacheService.getOrPutObject(CACHE_NAME_GSP_CACHE, pageName) {
            if (log.debugEnabled) {
                log.debug "Creating GSP template class for $pageName"
            }
            groovyPagesTemplateEngine.createTemplate(content, pageName)
        }
    }

    /**
     * Take a string or Class or null and turn it into a content Class
     */
    // @todo cache the list of known type ans mappings that are assignable to a Content variable
    // so that we can skip the isAssignableFrom which will affect performance a lot, as this function may be
    // called a lot
    Class getContentClassForType(def type) {
        if (type == null) {
            return Content.class
        }        
        
        def cls = (type instanceof Class) ? type : grailsApplication.getClassForName(type)
        if (cls) {
            if (!Content.isAssignableFrom(cls)) {
                throw new IllegalArgumentException("The class $clazz does not extend Content")
            } else {
                return cls
            }
        } else {
            throw new IllegalArgumentException("There is no content class with name $type")
        }
    }

    List listContentClassNames() {
        def results = []
        grailsApplication.domainClasses.each { dc ->
            def cls = dc.clazz
            if (Content.isAssignableFrom(cls) && (cls != Content)) {
                results << cls.name
            }
        }
        return results
    }
    
    List listContentClasses() {
        def results = []
        grailsApplication.domainClasses.each { dc ->
            def cls = dc.clazz
            if (Content.isAssignableFrom(cls) && (cls != Content)) {
                results << cls
            }
        }
        return results
    }
    
    Content newContentInstance(String typename, Space space = null) {
        def cls = getContentClassForType(typename)
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
    Map getContentDetails(Content content) {
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
    Map getRelatedContent(Content content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        def result = [:]
        // @todo change to criteria/select
        result.parents = VirtualContent.findAllByTarget(content)*.parent
        if (content.parent) result.parents << content.parent
        result.children = content.children
        
        def relatedContents = []
        // @todo replace with more efficient select/criteria
        relatedContents.addAll(RelatedContent.findAllWhere(targetContent: content).collect {
            it.sourceContent
        })
        // @todo replace with more efficient select/criteria
        relatedContents.addAll(RelatedContent.findAllWhere(sourceContent: content).collect {
            it.targetContent
        })
        result.related = relatedContents.unique()

        return result
    }

    /**
     * Returns map of recent changes for specified Content.
     *
     * @param content
     */
    Map getRecentChanges(Content content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        def changes = ContentVersion.findAllByObjectKey(content.ident(),
                [sort: 'revision', order: 'desc'])
        return [changes: changes]
    }

    /**
     * Creates new Content node and it's relation from request parameters
     *
     * @param content
     * @param parentContent
     */
    def createNode(String type, def params) {
        def content = newContentInstance(params.type)
        hackedBindData(content, params)
        createNode(content)
        return content
    }

    /**
     * Creates new Content node and it's relation
     *
     * @param content
     * @param parentContent
     */
    Boolean createNode(Content content, Content parentContent = null) {
        requirePermissions(parentContent ?: content.space, [WeceemSecurityPolicy.PERMISSION_CREATE])        

        log.debug "Creating node: ${content.dump()}"
        if (parentContent == null) parentContent = content.parent

        def result 
        if (content.metaClass.respondsTo(content, 'create', Content)) {
            // Call the event so that nodes can perform post-creation tasks
            result = content.create(parentContent)
        } else {
            result = true
        }
        def uniqueURI = true
        if (result) {
            // We complete the AliasURI, AFTER handling the create() event which may need to affect title/aliasURI
            if (!content.aliasURI) {
                content.createAliasURI()
            }
            
            result = content.validate()

            // Check aliasURI uniqueness within content items
          // The withNewSession is a patch for the ADT project that causes an exception when saving a Bar with categories
          // TODO: (Scott) - take out the withNewSession and test after the 1.2 release
          Content.withNewSession {
            uniqueURI = Content.findByParentAndAliasURI(parentContent, content.aliasURI) ? false : true
          }            
        }
        
        if (uniqueURI){
            // Update date orderIndex to last order index + 1 in the parent's child list
            if (parentContent) {
                def orderIndex = parentContent.children ?
                                 parentContent.children?.last()?.orderIndex + 1 : 0
                content.orderIndex = orderIndex
                parentContent.addToChildren(content)
            } else {
                def criteria = Content.createCriteria()
                def nodes = criteria {
                    isNull("parent")
                    maxResults(1)
                    order("orderIndex", "desc")
                }
                content.orderIndex = nodes[0].orderIndex + 1
            }
        }else{
            if (!parentContent)
                content.errors.rejectValue("aliasURI", "org.weceem.content.Content.aliasURI.unique")
        }
        return result
    }

    /**
     * Changes node's title.
     *
     * @param content
     * @param oldTitle
     */
    Boolean renameNode(Content content, oldTitle) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        if (content.metaClass.respondsTo(content, 'rename', String)) {
            return content.rename(oldTitle)
        } else {
            return true
        }
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
    VirtualContent linkNode(Content sourceContent, Content targetContent, orderIndex) {
        // Check they can create under the target
        requirePermissions(targetContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        if (sourceContent == null){
            return null
        }
        if (sourceContent && (sourceContent instanceof VirtualContent)) {
            sourceContent = sourceContent.target
        }
        if (Content.findWhere(parent: targetContent, aliasURI: sourceContent.aliasURI + "-copy") != null){
            return null
        }
        VirtualContent vcont = new VirtualContent(title: sourceContent.title,
                                          aliasURI: sourceContent.aliasURI + "-copy",
                                          target: sourceContent, status: sourceContent.status, 
                                          space: sourceContent.space)
        Content inPoint = Content.findByOrderIndexAndParent(orderIndex, targetContent)
        if (inPoint != null){
            shiftNodeChildrenOrderIndex(targetContent, orderIndex)
        }
        vcont.orderIndex = orderIndex
        if (targetContent) {
            if (VirtualContent.findWhere(parent: targetContent, target: sourceContent)){
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
    Boolean moveNode(Content sourceContent, Content targetContent, orderIndex) {
        if (targetContent) {
            requirePermissions(targetContent, [WeceemSecurityPolicy.PERMISSION_CREATE])        
        }
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_EDIT,WeceemSecurityPolicy.PERMISSION_VIEW])        

        if (!sourceContent) return false
        if (!targetContent){
            def criteria = Content.createCriteria()
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
        if (sourceContent.metaClass.respondsTo(sourceContent, "move", Content)){
            sourceContent.move(targetContent)
        }
        def parent = sourceContent.parent
        if (parent) {
            parent.children.remove(sourceContent)
            sourceContent.parent = null
            assert parent.save()
        }
        Content inPoint = Content.findByOrderIndexAndParent(orderIndex, targetContent)
        if (inPoint != null){
            shiftNodeChildrenOrderIndex(targetContent, orderIndex)
        }
        sourceContent.orderIndex = orderIndex
        if (targetContent) {
            if (!targetContent.children) targetContent.children = new TreeSet()
            targetContent.addToChildren(sourceContent)
            assert targetContent.save()
        }
        return sourceContent.save(flush: true)
     }
     
    def shiftNodeChildrenOrderIndex(parent = null, shiftedOrderIndex){
        // Can't do this until space is supplied
        //requirePermissions(parent, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        def criteria = Content.createCriteria()
        def nodes = criteria {
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
     * associations/relationships to other Content and querying them all individually. Hideous but
     * less ugly than forcing all references to be ContentRef(s) we decided.
     */
    ContentReference[] findReferencesTo(Content content) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        def results = [] 
        // @todo this will perform rather poorly. We should find all assocation properties FIRST
        // and then run a query for each association, which - with caching - should run a lot faster than
        // checking every property on every node
        for (cont in Content.list()){
            def perProps = grailsApplication.getDomainClass(cont.class.name).persistentProperties.findAll { p -> 
                p.isAssociation() && Content.isAssignableFrom(p.referencedPropertyType)
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
    
    /**
     * Deletes content node and all it's references.
     * All children of sourceContent will be assigned to all its parents.
     *
     * @param sourceContent
     */
    Boolean deleteNode(Content sourceContent) {
        if (!sourceContent) return Boolean.FALSE
        
        requirePermissions(sourceContent, [WeceemSecurityPolicy.PERMISSION_DELETE])        
        
        // Create a versioning entry
        sourceContent.saveRevision(sourceContent.title, sourceContent.space.name)
        
        if (sourceContent.metaClass.respondsTo(sourceContent, 'deleteContent')) {
            if (!sourceContent.deleteContent()) return false
        }

        def parent = sourceContent.parent

        // if there is a parent  - we delete node from its association
        if (parent) {
            parent.children = parent.children.findAll{it-> it.id != sourceContent.id}
            assert parent.save()
        }

        // we need to delete all virtual contents that reference sourceContent
        def copies = VirtualContent.findAllWhere(target: sourceContent)
        copies?.each() {
           if (it.parent) {
               parent = Content.get(it.parent.id)
               parent.children.remove(it)
           }
           it.delete()
        }

        // delete node
        
        // @todo replace this with code that looks at all the properties for relationships
        if (sourceContent.metaClass.hasProperty(sourceContent, 'template')?.type == Template) {
            sourceContent.template = null
        }
        if (sourceContent.metaClass.hasProperty(sourceContent, 'target')?.type == Content) {
            sourceContent.target = null
        }
        sourceContent.delete(flush: true)

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
    void deleteLink(Content child, Content parent) {
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
        def space = Space.get(id)
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        if (space){
            def oldAliasURI = space.makeUploadName()
            hackedBindData(space, params)
            if (!space.hasErrors() && space.save()) {
                def oldFile = new File(SCH.servletContext.getRealPath(
                        "/${ContentFile.DEFAULT_UPLOAD_DIR}/${oldAliasURI}"))
                def newFile = new File(SCH.servletContext.getRealPath(
                        "/${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}"))
                oldFile.renameTo(newFile)
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
        Content content = Content.get(id)
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        if (content) {
            updateNode(content, params)
        } else {
            return [notFound:true]
        }        
    }
    
    // @todo This is a hack so we can bind without x.properties = y which is broken in production on Grails 1.2-M2
    public hackedBindData(obj, params) {
        new BindDynamicMethod().invoke(this, 'bindData', obj, params)
    }
    
    def updateNode(Content content, def params) {
        requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])        

        // firstly we save revision: to prevent errors that we have 2 objects
        // in session with the same identifiers
        if (log.debugEnabled) {
            log.debug("Updating node with id ${content.id}, with parameters: $params")
        }
        def oldAbsURI = content.absoluteURI
        content.saveRevision(params.title ?: content.title, params.space ? Space.get(params.'space.id')?.name : content.space.name)
        def oldTitle = content.title
        // map in new values
        hackedBindData(content, params)
        if (content instanceof ContentFile){
            content.rename(oldTitle)
        }
        if (log.debugEnabled) {
            log.debug("Updated node with id ${content.id}, properties are now: ${content.dump()}")
        }
        if (content instanceof ContentFile){
            content.createAliasURI()
        }else
        if (!content.aliasURI && content.title) {
            content.createAliasURI()
        }
        def ok = content.validate()
        if (content.save()) {
            if (log.debugEnabled) {
                log.debug("Update node with id ${content.id} saved OK")
            }
            if (GSP_CONTENT_CLASSES.contains(content.class)) {
                cacheService.removeValue("gspCache", oldAbsURI)
            }
            return [content:content]
        } else {
            if (log.debugEnabled) {
                log.debug("Update node with id ${content.id} failed with errors: ${content.errors}")
            }
            return [errors:content.errors, content:content]
        }
    }
    
    /**
     * Count child nodes of a given node, where nodes match the type and status (if any) supplied in args
     * Very useful for rendering the number of published comments on an item, for example in blogs.
     */
    def countChildren(Content sourceNode, Map args = null) {
        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        // for VirtualContent - the children list is a list of target children
        if (sourceNode instanceof VirtualContent) {
            sourceNode = sourceNode.target
        }
        
        def clz = args.type ? getContentClassForType(args.type) : Content
        return (doCriteria(clz, args.status, args.params) {
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
     * - a Status instance eg Status.get(1)
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
                if (status == ContentRepositoryService.STATUS_ANY_PUBLISHED) {
                    inList('status', allPublicStatuses)
                } else if (status instanceof Collection) {
                    // NOTE: This assumes collection is a collection of codes, not Status objects
                    inList('status', Status.findAllByCodeInList(status))
                } else if (status instanceof Status) {
                    eq('status', status)
                } else if (status instanceof Integer) {
                    eq('status', Status.findByCode(status) )
                } else if (status instanceof IntRange) {
                    between('status', status.fromInt, status.toInt)
                } else {
                    def s = status.toString()
                    if (s.isInteger()) {
                        eq('status', Status.findByCode(s.toInteger()) )
                    } else throw new IllegalArgumentException(
                        "The [status] argument must be null (for 'any'), or '${ContentRepositoryService.STATUS_ANY_PUBLISHED}',  an integer (or integer string), a collection of codes (numbers), a Status instance or an IntRange. You supplied [$status]")
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
    def findChildren(Content sourceNode, Map args = Collections.EMPTY_MAP) {
        // @todo we also need to filter the result list by VIEW permission too!
        assert sourceNode != null
        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        
        // for VirtualContent - the children list is a list of target children
        if (sourceNode instanceof VirtualContent) {
            sourceNode = sourceNode.target
        }
        
        // @todo replace this with smarter queries on children instead of requiring loading of all child objects
        def typeRestriction = getContentClassForType(args.type)
        def children = doCriteria(typeRestriction, args.status, args.params) {
            if (sourceNode == null) {
                isNull('parent')
            } else {
                eq('parent', sourceNode)
            }
            cache true
        }
        
        return children //  sortNodes(children, params?.sort, params?.order)
    }


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
    boolean contentMatchesStatus(status, Content node) {
        if (status == null) {
            return true
        } else if (status == ContentRepositoryService.STATUS_ANY_PUBLISHED) {
            return Status.findAllByPublicContent(true).find { it == node.status }
        } else if (status instanceof Collection) {
            // NOTE: This assumes collection is a collection of codes, not Status objects
            return status.find { it == node.status.code }  
        } else if (status instanceof Status) {
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
                "The [status] argument must be null (for 'any'), or '${ContentRepositoryService.STATUS_ANY_PUBLISHED}', an integer (or integer string), a collection of codes (numbers), a Status instance or an IntRange. You supplied [$status]")
        }        
    }
    
    /**
     * Find all the parents of the specified node, within the content hierarchy, optionally filtering by status and a content type class
     * @todo we can probably improve performance by applying the typeRestriction using some HQL
     */ 
    def findParents(Content sourceNode, Map args = Collections.EMPTY_MAP) {
        requirePermissions(sourceNode, [WeceemSecurityPolicy.PERMISSION_VIEW])        

        // @todo change to criteria/select
        def references = (doCriteria(VirtualContent, args.status, Collections.EMPTY_MAP) {
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
    def findRootContentByURI(String aliasURI, Space space, Map args = Collections.EMPTY_MAP) {
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
        Content node = r ? r[0] : null
        if (node) {
            requirePermissions(node, [WeceemSecurityPolicy.PERMISSION_VIEW])        
        }
        return node
    }
    
    /**
     * find all root nodes by type and space
     */ 
    def findAllRootContent(Space space, Map args = Collections.EMPTY_MAP) {
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
     *
     * Find the content node that is identified by the specified uri path. This always finds a single Content node
     * or none at all. Each node can have multiple URI paths, so this code returns the node AND the uri to its parent
     * so that you can tell where it is in the hierarchy
     *
     * This call does NOT filter by path.
     *
     * @param uriPath
     * @param space
     *
     * @return a map of 'content' (the node), 'lineage' (list of parent Content nodes to reach the node) 
     * and 'parentURI' (the uri to the parent of this instance of the node)
     */
    def findContentForPath(String uriPath, Space space, boolean useCache = true) {
        if (log.debugEnabled) {
            log.debug "findContentForPath uri: ${uriPath} space: ${space}"
        }

        if (useCache) {
            // This looks up the uriPath in the cache to see if we can get a Map of the content id and parentURI
            // If we call getValue on the cache hit, we lose 50% of our performance. Just retrieving
            // the cache hit is not expensive.
            def cachedElement = uriToIdCache.get(space.aliasURI+':'+uriPath)
            def cachedContentInfo = cachedElement?.getValue()
            if (cachedContentInfo) {
                if (log.debugEnabled) {
                    log.debug "Found content info into cache for uri $uriPath: ${cachedContentInfo}"
                }
                // @todo will this break with different table mapping strategy eg multiple ids of "1" with separate tables?
                Content c = Content.get(cachedContentInfo.id)
                // @todo re-load the lineage objects here, currently they are ids!
                def reloadedLineage = cachedContentInfo.lineage?.collect { l_id ->
                    Content.get(l_id)
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
        Content content = findRootContentByURI(tokens[0], space)
        if (!content) content = findFileRootContentByURI(tokens[0], space)
        if (log.debugEnabled) {
            log.debug "findContentForPath $uriPath - root content node is $content"
        }
        
        def lineage = [content]
        if (content && (tokens.size() > 1)) {
            for (n in 1..tokens.size()-1) {
                def child = Content.find("""from Content c \
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
            cacheService.putToCache(uriToIdCache, space.aliasURI+':'+uriPath, cacheValue)
        }
        
        if (content) {
            requirePermissions(content, [WeceemSecurityPolicy.PERMISSION_VIEW])        

            [content:content, parentURI:parentURI, lineage:lineage]
        } else {
            return null
        }
    }
    
    def findFileRootContentByURI(String aliasURI, Space space, Map args = Collections.EMPTY_MAP) {
        if (log.debugEnabled) {
            log.debug "findFileRootContentByURI: aliasURI $aliasURI, space ${space?.name}, args ${args}"
        }
        def r = doCriteria(ContentFile, args.status, args.params) {
            eq('aliasURI', aliasURI)
            eq('space', space)
        }
        def res = r?.findAll(){it-> (it.parent == null) || !(it.parent instanceof ContentFile)}
        Content result = res ? res[0] : null
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
     * Synchronize given space with file system
     * 
     * @param space - space to synchronize
    **/
    def synchronizeSpace(space) {
        requirePermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])        

        def existingFiles = new TreeSet()
        def createdContent = []
        def spaceDir = grailsApplication.parentContext.getResource(
                "${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}").file
        if (!spaceDir.exists()) spaceDir.mkdirs()
        spaceDir.eachFileRecurse {file ->
            def relativePath = file.absolutePath.substring(
                    spaceDir.absolutePath.size() + 1)
            def content = findContentForPath(relativePath, space, false)?.content
            //if content wasn't found then create new
            if (!content){
                createdContent += createContentFile("${spaceDir.name}/${relativePath}")
                content = findContentForPath(relativePath, space, false)?.content
                while (content){
                    existingFiles << content
                    content = content.parent
                }
            }else{
                existingFiles << content
            }
        }
        def allFiles = ContentFile.findAllBySpace(space);
        def missedFiles = allFiles.findAll(){f->
            !(f.id in existingFiles*.id)
        }
        
        return ["created": createdContent, "missed": missedFiles]
    }
    
    /**
     * Creates ContentFile/ContentDirectory from specified <code>path</code>
     * on the file system.
     *
     * @param path
     */
    def createContentFile(path) {
        if (log.debugEnabled) {
            log.debug "Creating content node for server file at [$path]"
        }
        
        List tokens = path.replace('\\', '/').split('/')
        if (tokens.size() > 1) {
            def space = Space.findByAliasURI((tokens[0] == ContentFile.EMPTY_ALIAS_URI) ? '' : tokens[0])
            def parents = tokens[1..(tokens.size() - 1)]
            def ancestor = null
            def content = null
            def createdContent = []
            parents.eachWithIndex(){ obj, i ->
                def parentPath = "${parents[0..i].join('/')}"
                def file = grailsApplication.parentContext.getResource(
                        "${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}/${parentPath}").file
                content = findContentForPath(parentPath, space)?.content
                if (!content){
                    if (file.isDirectory()){
                        content = new ContentDirectory(title: file.name,
                            content: '', filesCount: 0, space: space, orderIndex: 0,
                            mimeType: '', fileSize: 0, status: Status.findByPublicContent(true))
                    }else{
                        def mimeType = SCH.servletContext.getMimeType(file.name)
                        content = new ContentFile(title: file.name,
                            content: '', space: space, orderIndex: 0, 
                            mimeType: (mimeType ? mimeType : ''), fileSize: file.length(),
                            status: Status.findByPublicContent(true))
                    }
                    content.createAliasURI()

                    requirePermissions(content.parent ?: space, [WeceemSecurityPolicy.PERMISSION_CREATE])        

                    if (!content.save()) {
                        log.error "Failed to save content ${content} - errors: ${content.errors}"
                        assert false
                    } else {
                        createdContent << content
                    }
                }
                if (ancestor){
                    if (ancestor.children == null) ancestor.children = new TreeSet()
                    ancestor.children << content
                    if (ancestor instanceof ContentDirectory)
                        ancestor.filesCount += 1
                    if (log.debugEnabled) {
                        log.debug "Updated parent node of new file node [${ancestor.dump()}]"
                    }
                    assert ancestor.save(flush: true)
                    content.parent = ancestor
                    if (log.debugEnabled) {
                        log.debug "Saving content node of new file node [${content.dump()}]"
                    }
                    if (log.debugEnabled && !content.validate()) {
                        log.debug "Saving content node of new file node is about to fail. Node: [${content.dump()}], Errors: [${content.errors}]"
                    }
                    assert content.save(flush: true)
                }
                ancestor = content
            }
            return createdContent
        }
        return null
    }
    
}

