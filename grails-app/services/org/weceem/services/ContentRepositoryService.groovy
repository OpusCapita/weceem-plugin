package org.weceem.services

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

//@todo design smell!
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*

/**
 * ContentRepositoryService class provides methods for Content Repository tree
 * manipulations.
 * The service deals with Content and subclasses of Content classes.
 *
 * @author Sergei Shushkevich
 */
class ContentRepositoryService {

    public static final String DEFAULT_UPLOAD_DIR = 'WeceemFiles'
    static final CONTENT_CLASS = Content.class.name

    static transactional = true

    def grailsApplication
    def importExportService
    
    static DEFAULT_STATUSES = [
        [code:100, description:'draft', publicContent:false],
        [code:200, description:'reviewed', publicContent:false],
        [code:300, description:'approved', publicContent:false],
        [code:400, description:'published', publicContent:true]
    ]
    
    void createDefaultStatuses() {
        if (Status.count() == 0) {
            DEFAULT_STATUSES.each {
                assert new Status(it).save()
            }
        }
    }
    
    Space findDefaultSpace() {
        def space
        def spaces = Space.list()
        if (spaces) {
            space = spaces[0]
        }        
        return space
    }
    
    Space findSpaceByURI(String uri) {
        Space.findByAliasURI(uri)
    }
    
    Map resolveSpaceAndURI(String uri) {
        def spaceName
        def space
        
        def n = uri?.indexOf('/')
        if (n >= 0) {
            spaceName = uri[0..n-1]
            uri = uri[n+1..-1]
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
    
    Space createSpace(params) {
        def s = new Space(params)
        if (s.save()) {
            importSpaceTemplate('default', s)
        }
        return s // If this fails we still return the original space so we can see errors
    }
    
    /**
     * Import a named space template (import zip) into the specified space
     */
    void importSpaceTemplate(String templateName, Space space) {
        // For now we only load files, in future we may get them as blobs from DB
        def f = File.createTempFile("default-space-import", null)
        def res = ServletContextHolder.servletContext.getResourceAsStream('/WEB-INF/$templateName-space-template.zip')
        if (!res) {
            log.error "Unable to import space template [${templateName}] into space [${space.name}], space template not found"
            return
        }
        f.withOutputStream { os ->
            os << res
        }
        try {
            importExportService.importSpace(space, params.importer, f)
        } catch (Throwable t) {
            log.error "Unable to import space template [${templateName}] into space [${space.name}]", t
            throw t // rethrow, this is sort of fatal
        }
    }
    
    void deleteSpace(Space space) {
        def contents = Content.findAllWhere(space: space)
        def templateList = []
        def copiesList = []
        contents.each() {
            // @todo This is bad, we should not rely on specific types of relationships
            // Needs smarter code like the new import/export
            if (it instanceof Template) {
                templateList << it
            } else if (it instanceof VirtualContent) {
                copiesList << it
            }
        }
        // delete all copies for contents in space
        copiesList*.delete()
        // delete other contents
        (contents - copiesList - templateList)*.delete()
        // delete all templates from space
        templateList*.delete()
        // delete space
        space.delete(flush: true)
    }

    // @todo cache the list of known type ans mappings that are assignable to a Content variable
    // so that we can skip the isAssignableFrom which will affect performance a lot, as this function may be
    // called a lot
    Class getContentClassForType(String type) {
        def cls = grailsApplication.getClassForName(type)
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
    Map getContentDetails(content) {
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
    Map getRelatedContent(content) {
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
    Map getRecentChanges(content) {
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
        content.properties = params
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
        log.debug "Creating node: ${content.dump()}"
        if (!content.validate()){
            return false
        }
        if (parentContent == null) parentContent = content.parent

        // Update date orderIndex to last order index + 1 in the parent's child list
        if (parentContent) {
            def orderIndex = parentContent.children ?
                             parentContent.children?.last()?.orderIndex + 1 : 0
            content.orderIndex = orderIndex
            parentContent.addToChildren(content)
        } else content.orderIndex = 0

        def result 
        if (content.metaClass.respondsTo(content, 'create', Content)) {
            // Call the event so that nodes can perform post-creation tasks
            result = content.create(parentContent)
        } else {
            result = true
        }

        if (result) {
            // We complete the AliasURI, AFTER handling the create() event which may need to affect title/aliasURI
            if (!content.aliasURI) {
                content.createAliasURI()
            }
            
            result = content.validate()
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
    VirtualContent linkNode(sourceContent, targetContent, orderIndex) {
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
        if (Content.findAll("from Content c where c.orderIndex=? and c.parent=?", [orderIndex, targetContent]) == null){
            vcont.orderIndex = orderIndex
        }else{
            vcont.orderIndex = orderIndex + 1
        }
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
        if (!sourceContent) return false
        
        if (sourceContent.metaClass.respondsTo(sourceContent, "move", Content)){
            sourceContent.move(targetContent)
        }
        def parent = sourceContent.parent
        if (parent) {
            parent.children.remove(sourceContent)
            sourceContent.parent = null
            assert parent.save()
        }
        if (Content.findAll("from Content c where c.orderIndex=? and c.parent=? ", [orderIndex, targetContent]) == null){
            sourceContent.orderIndex = orderIndex
        }else{
            Content.executeUpdate("update Content c set c.orderIndex=c.orderIndex+1 where c.orderIndex>? and c.parent=?", [orderIndex, targetContent])
            sourceContent.orderIndex = orderIndex + 1
        }
        if (targetContent) {
            if (!targetContent.children) targetContent.children = new TreeSet()
            targetContent.addToChildren(sourceContent)
            assert targetContent.save()
        }
        return sourceContent.save(flush: true)
     }

    /**
     * Use introspection to find all references to the specified content. Requires finding all
     * associations/relationships to other Content and querying them all individually. Hideous but
     * less ugly than forcing all references to be ContentRef(s) we decided.
     */
    ContentReference[] findReferencesTo(Content content) {
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
        
        // Create a versioning entry
        sourceContent.saveRevision(sourceContent.title, sourceContent.space.name)
        
        if (sourceContent.metaClass.respondsTo(sourceContent, 'deleteContent')) {
            if (sourceContent.deleteContent()) {
                sourceContent.delete(flush: true)
                return true
            } else return false
        } else {
            def parent = sourceContent.parent

/* @todo Check but I'm pretty sure we don't want to keep children - users can move them first 
            if (sourceContent.children) {
                def children = new ArrayList(sourceContent.children)
                // define for new parent for all children
                children?.each() {
                    sourceContent.children.remove(it)
                    // update orderIndex for new association
                    def newIndex = parent?.children?.last()?.orderIndex ?
                                       parent?.children?.last()?.orderIndex + 1 : 0
                    it.orderIndex = newIndex
                    it.parent = parent
                    it.save()
                }
            }
*/
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
            sourceContent.delete(flush: true)

            return true
        }
    }

    /**
     * Deletes content reference 
     *
     * @param child
     * @param parent
     */
    void deleteLink(child, parent) {
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

    /**
     * Update a node with the new properties supplied, binding them in using Grails binding
     * @return a map containing an optional "errors" list property and optional notFound boolean property
     */
    def updateNode(String id, def params) {
        def content = Content.get(id)
        if (content) {
            updateNode(content, params)
        } else {
            return [notFound:true]
        }        
    }
    
    def updateNode(Content content, def params) {
        // firstly we save revision: to prevent errors that we have 2 objects
        // in session with the same identifiers
        if (log.debugEnabled) {
            log.debug("Updating node with id ${content.id}, with parameters: $params")
        }
        content.saveRevision(params.title ?: content.title, params.space ? Space.get(params.'space.id')?.name : content.space.name)
        def oldTitle = content.title
        // map in new values
        content.properties = params
        if (content instanceof ContentFile){
            content.rename(oldTitle)
        }
        if (log.debugEnabled) {
            log.debug("Updated node with id ${content.id}, properties are now: ${content.dump()}")
        }
        if (!content.aliasURI && content.title) {
            content.createAliasURI()
        }
        def ok = content.validate()
        if (content.save()) {
            if (log.debugEnabled) {
                log.debug("Update node with id ${content.id} saved OK")
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
     * Find all the children of the specified node, within the content hierarchy, optionally filtering by a content type class
     * @todo we can probably improve performance by applying the typeRestriction using some HQL
     */ 
    def findChildren(sourceNode, typeRestriction = null, params = null) {
        if (!sourceNode) return Content.findAll("from Content c where c.parent is null")

        // for VirtualContent - the children list is a list of target children
        if (sourceNode instanceof VirtualContent) {
            sourceNode = sourceNode.target
        }
        // @todo replace this with smarter queries on children instead of requiring loading of all child objects
        if (typeRestriction && (typeRestriction instanceof Class)) {
            typeRestriction = typeRestriction.name
        }
        def clz = typeRestriction ? ApplicationHolder.application.getClassForName(typeRestriction) : Content
        def children = clz.findAllByParent(sourceNode, params)
        
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
     * Find all the parents of the specified node, within the content hierarchy, optionally filtering by a content type class
     * @todo we can probably improve performance by applying the typeRestriction using some HQL
     */ 
    def findParents(sourceNode, typeRestriction = null, params = null) {
        // @todo change to criteria/select
        def references = VirtualContent.findAllWhere(target: sourceNode)*.parent
        if (sourceNode.parent) references << sourceNode.parent

        if (typeRestriction && (typeRestriction instanceof Class)) {
            typeRestriction = typeRestriction.name
        }
        def parents = []
        references?.unique()?.each { 
            if (typeRestriction ? it.class.name == typeRestriction : true) {
                parents << it
            }
        }
        return sortNodes(parents, params?.sort, params?.order)
    }

    /**
     * Locate a root node by uri, type and space
     */ 
    def findRootContentByURI(String aliasURI, Space space, String type = null) {
        if (log.debugEnabled) {
            log.debug "findRootContentByURI: aliasURI $aliasURI, space ${space?.name}, type ${type}"
        }
        if (!type) type = CONTENT_CLASS
        getContentClassForType(type)?.find("""from ${type} c \
            where c.aliasURI = ? and c.space = ? and c.parent is null""",
            [aliasURI, space])        
    }
    
    /**
     * find all root nodes by type and space
     */ 
    def findAllRootContent(Space space, String type = null, Map params = null) {
        if (!type) type = CONTENT_CLASS
        ApplicationHolder.application
            .getClassForName(type)?.findAllBySpaceAndParent(space, null, params)        
    }
    
    /**
     *
     * Find the content node that is identified by the specified uri path. This always finds a single Content node
     * or none at all. Each node can have multiple URI paths, so this code returns the node AND the uri to its parent
     * so that you can tell where it is in the hierarchy
     *
     * @param uriPath
     * @param type
     * @param space
     *
     * @return a map of 'content' (the node), 'lineage' (list of parent Content nodes to reach the node) 
     * and 'parentURI' (the uri to the parent of this instance of the node)
     */
    def findContentForPath(String uriPath, Space space, String type = null) {
        log.debug "findContentForPath uri: ${uriPath} type: ${type} space: ${space}"
        def tokens = uriPath.split('/')

        // todo: optimize query
        def content = findRootContentByURI(tokens[0], space, type)
        log.debug "findContentForPath $uriPath - root content node is $content"
        def lineage = []
        if (content && (tokens.size() > 1)) {
            for (n in 1..tokens.size()-1) {
                def child = Content.find("""from Content c \
                        where c.parent = ? and c.aliasURI = ?""",
                        [content, tokens[n]])
                log.debug "findContentForPath $uriPath - found child $child for path token ${tokens[n]}"
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

        return [content:content, parentURI:parentURIParts.join('/'), lineage:lineage]
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
    
}

