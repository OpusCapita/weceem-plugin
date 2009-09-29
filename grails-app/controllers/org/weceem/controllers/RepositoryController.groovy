/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.weceem.controllers

import grails.converters.JSON
import org.apache.commons.lang.StringUtils
import com.lowagie.text.Chapter
import com.lowagie.text.Document
import com.lowagie.text.Font
import com.lowagie.text.Paragraph
import com.lowagie.text.Section
import com.lowagie.text.html.HtmlParser
import com.lowagie.text.pdf.PdfWriter
import java.text.SimpleDateFormat
import org.compass.core.*

import org.weceem.content.*
// Design smell
import org.weceem.files.*
import org.weceem.html.*

/**
 * The ContentRepositoryController class works with UI representation, checks
 * parameters and call necessary service functions, show pages and prepare result
 * information for representation on GSP page.
 *
 * After each repository tree manupulation (except open(expand/collapse) and select) whole
 * tree has been reloaded (it means that tree send requests again for each node and its
 * children to the server), so in this case we always see updated information in the tree.
 *
 * State of the tree for collapsed/expanded nodes saves automatically by Dojo in browser
 * Cookie object. If we expand or collapse some node, then Cookies will be updated
 * automatically and when tree has been reloaded, it's nodes will be expanded to the state
 * which existed in browser cookies.
 *
 * In the repository tree each content node has unique path, it has the following structure:
 * SpaceId/contentType/rootContentId1/contentId2/.../contentIdn
 *
 * Content types are the names of all Content subclasses.
 *
 * Root contents are all content nodes that are not used as a childs in the content
 * hierarchy.
 */
class RepositoryController {
    
    /**
     * Defines instance of contentRepositoryService class.
     */
    
    static ACTIONS_NEEDING_SPACE = ['treeTable']
    
    def contentRepositoryService

    def grailsApplication

    def beforeInterceptor = {
        if (!(actionName in ACTIONS_NEEDING_SPACE)) return true

        if (!params.space) {
            if (session.currentAdminSpace) {
                params.space = Space.get(session.currentAdminSpace)
                // Be careful, the space might have been deleted
                if (params.space) return true
                // If it is null we fall through and resolve again
            } 

            // Find the default space(if there is any) and redirect so url has space in
            if (Space.count() == 0){
                flash.message = message(code: 'message.there.are.no.spaces')
                redirect(controller:'space')
                return false
            }
            def space = contentRepositoryService.findDefaultSpace()
            if (log.debugEnabled) {
                log.debug "Using default space: ${space.name}"
            }
            // Redirect to ourselves with the correct link for default dspace
            redirect(controller:controllerName, action:actionName, params:[space:space.name])
            return false
        } else {
            if (log.debugEnabled) {
                log.debug "Loading space from parameter: ${params.space}"
            }
            params.space = findSelectedSpace()
            if (!params.space) {
                // @todo in future we should default to another space if none found, with a message
                // "The space you selected can no longer be found"
                flash.message = message(code:'message.no.such.space', args:[params.space])
                params.space = contentRepositoryService.findDefaultSpace()
            }
        }
        if (params.space) {
            session.currentAdminSpace = params.space.id
        }
        return true
    }
    
    static defaultAction = 'treeTable'

    def findSelectedSpace() {
        def space
        if (log.debugEnabled) {
            log.debug "Resolving space from param: ${params.space}"
        }
        if (params.space) {
            space = Space.findByName(params.space)
        } else {
            space = contentRepositoryService.findDefaultSpace()
        }
        if (log.debugEnabled) {
            log.debug "Space resolved to: ${space?.dump()}"
        }
        return space        
    }

    def treeTable = {
        if (params.space && (Space.count() != 0)) {
            def nodes = contentRepositoryService.findAllRootContent( params.space, 
                [params: [sort:'aliasURI', fetch:[children:'eager']] ])
            def haveChildren = [:]
            for (domainClass in contentRepositoryService.listContentClasses()){
                def dcInst = domainClass.newInstance()
                haveChildren.put(domainClass.name, dcInst.canHaveChildren())
            }
            return [content:nodes, contentTypes:contentRepositoryService.listContentClassNames(), 
                'haveChildren':haveChildren, space: params.space, spaces: Space.listOrderByName() ]
        } else {
            flash.message = 'message.there.are.no.spaces'
            redirect(controller:'space')
        }
    }
    
    def list = {
        return [availableContentTypes: getAvailableContentTypes() - 
        VirtualContent.class.name]
    }

    def getList = {
        params.offset = params.offset ? new Integer(params.offset) : 0;
        params.max = params.max ? new Integer(params.max) : 20;
        def dateFormat = new SimpleDateFormat('dd-MM-yyyy')
        def criteria = Content.createCriteria()
        def contents = criteria.list {
            listRestrictions(criteria)
            firstResult(params.offset)
            maxResults(params.max)
            if (params.sort) {
              order(params.sort, params.order)
            }
        }
        def map = [:]
        def items = []
        contents.each {
            items << [path: "${it.space.id}/${it.class.name}/${it.id}",
                    title: it.title,
                    createdOn: dateFormat.format(it.createdOn),
                    createdBy: it.createdBy,
                    'class': message(code: "content.item.name.${it.class.name}")]
        }
        map.items = items
        render map as JSON
    }

    def getListRowCount = {
        def criteria = Content.createCriteria()
        def count = criteria.get {
            listRestrictions(criteria)
            projections {
                rowCount()
            }
        }
        render ([rowCount: count] as JSON)
    }

    def listRestrictions = {c ->
        def dateFormat = new SimpleDateFormat('dd-MM-yyyy')
        if (params.title) {
            c.ilike('title', "${params.title}%")
        }
        if (params.createdFrom) {
            c.ge('createdOn', dateFormat.parse(params.createdFrom))
        }
        if (params.createdTo) {
            c.lt('createdOn', dateFormat.parse(params.createdTo) + 1)
        }
        if (params.contentType) {
            c.eq('class', params.contentType)
        }
    }

    /**
     *
     * @param contentType
     * @param space
     * @param parentPath
     */
    def newContent = {
        def space
        if (params['space.id']) {
            space = Space.get(params['space.id']?.toLong())
        }
        def content = contentRepositoryService.newContentInstance(params.contentType, space)
        return [content: content, contentType: params.contentType, parentPath: params.parentPath,
                editor: getEditorName(params.contentType)]
    }

    def getEditorName(String contentType) {
        contentType[contentType.lastIndexOf('.')+1..-1]    
    }
    /**
     * Renders initial data for the tree as JSON.
     * This action is necessary for tree initialization and it is called only once for
     * tree.
     *
     * It is necessary, because in current implementation we load first two levels as
     * initial (static) data for the tree, it increases performance, because in this case
     * we do not need additional requests on server about spaces and content types
     * (open/expand operations for spaces and types).
     */
    def initTree = {
        def data = [identifier: 'path', label: 'label']
        def rootItem = [path: '$root$', label: 'spaces', type: '$root$',
                hasChildren: (Space.count() > 0)]
        def rootChildren = []
        Space.list(sort: 'name').each {space ->
            def contentTypes = []
            (getAvailableContentTypes() - ContentFile.class.name
                                        - ContentDirectory.class.name
                                        - VirtualContent.class.name).each { contentType ->
                def contentTypeChildren = getRootContents(space, contentType)
                contentTypes << [path: "${space.id}/${contentType}",
                        label: message(code: "content.type.name.${contentType}"),
                        type: 'contentType',
                        hasChildren: (contentTypeChildren.size() > 0),
                        children: contentTypeChildren]
            }
            def filesChildren = getRootFilesAndDirectories(space)
            contentTypes << [path: "${space.id}/Files",
                    label: message(code: "content.type.name.Files"),
                    type: 'contentType',
                    hasChildren: (filesChildren.size() > 0),
                    children: filesChildren]
            rootChildren << [path: space.id, label: space.name, type: Space.class.name,
                    hasChildren: true, children: contentTypes]
        }
        rootItem.children = rootChildren
        data.items = [rootItem]

        render data as JSON
    }

    /**
     * Loads details for selected node.
     *
     * params.contentPath
     */
    def loadNodeDetails = {
        def template = 'contentDetails'
        def content = getContent(params.contentPath, Space.get(params['space.id']?.toLong()))

        if (!content) {
            flash.message = """No content with path '${params.contentPath}' was found.
                               Probably it was deleted/edited by other user."""
        }

        def parentContent = getParentContent(params.contentPath)
        def model = [content: content, parentContent: parentContent]
        if (content && (content instanceof ContentFile)) {
            template = 'contentFileDetails'
            if (!(parentContent && (parentContent instanceof ContentFile))) {
                model.reference = true
            }
        }
        render(template: template, model: model)
    }

    /**
     * Loads infromation about related content for selected node.
     *
     * params.contentPath
     */
    def loadRelatedContentDetails = {
        render(template: 'relatedContent',
                model: contentRepositoryService.getRelatedContent(getContent(params.contentPath, 
                    Space.get(params['space.id']?.toLong()) )))
    }

    /**
     * Loads infromation about recent changes of selected node.
     *
     * params.contentPath
     */
    def loadRecentChangesDetails = {
        render(template: 'recentChanges',
                model: contentRepositoryService.getRecentChanges(getContent(params.contentPath, 
                    Space.get(params['space.id']?.toLong()))))
    }

    /**
     * Loads children nodes for the expanded node
     *
     * params.contentPath
     */
    def loadNodeChildren = {
        def space = Space.get(params['space.id']?.toLong())
        def children = contentRepositoryService.findChildren(
                getContent(params.contentPath, space), space).collect {
            [path: "${params.contentPath}/${it.id}",
                label: it.title, type: it.class.name,
                hasChildren: it.children?.size() > 0 ? true: false,
                canHaveChildren: it.canHaveChildren(),
                canHaveMultipleParents: it.canHaveMultipleParents()]
        }
        render children as JSON
    }

    /**
     * Creates and save node.
     * Creates reference according to 'parentId' parameter.
     */
    def insertNode = {

        def space = Space.get(params['space.id']?.toLong())
        
        def insertedContent = contentRepositoryService.newContentInstance(params.contentType, space)
        insertedContent.properties = params
        if (!insertedContent.aliasURI && insertedContent.title) {
            insertedContent.createAliasURI()
        }

        def parent = params.parentPath ? getContent(params.parentPath, space) : null
        if (parent && (!parent.canHaveChildren() || (parent instanceof ContentDirectory))) {
            parent = null
        }
        contentRepositoryService.createNode(insertedContent, parent)

        if (insertedContent.hasErrors() || !insertedContent.save()) {
            log.debug("Unable to create new content: ${insertedContent.errors}")
            
            def editorToUse = getEditorName(params.contentType)
            
            render(view: 'newContent', model: [content: insertedContent,
                    contentType: params.contentType, parentPath: params.parentPath,
                    editor: editorToUse ])
        } else {
            redirect(action: treeTable)
        }
    }

    /**
     * @param dirname
     * @param space
     * @param parentPath
     */
    def createDirectory = {
        def space = Space.get(params['space.id']?.toLong())
        def parent = params.parentPath ? getContent(params.parentPath, space) : null

        if (!contentFileExists(params.dirname, parent, space)) {
            def contentDirectory = new ContentDirectory(title: params.dirname,
                    content: '', filesCount: 0, space: space,
                    mimeType: '', fileSize: 0, status: Status.findByCode(params.statuscode))
            contentDirectory.createAliasURI((parent && (parent instanceof ContentDirectory)) ? parent : null)
            if (contentDirectory.save()) {
                if (!contentRepositoryService.createNode(contentDirectory, parent)) {
                    flash.error = message(code: 'error.contentRepository.fileSystem')
                }
            } else {
                flash.contentNode = contentDirectory
            }
        } else {
            flash.error = message(code: 'error.contentRepository.fileExists')
        }

        redirect(action: treeTable)
    }

    /**
     * @param file
     * @param filename
     * @param space
     * @param parentPath
     */
    def uploadFile = {
        assert !"This obsolete?"
        
        def space = Space.get(params['space.id']?.toLong())
        def parent = params.parentPath ? getContent(params.parentPath, space) : null
        def file = request.getFile('file')
        def title = params.filename ? params.filename : file.originalFilename

        if (!contentFileExists(title, parent, space)) {
            // todo: fix when "file.contentType" returns null
            def contentFile = new ContentFile(title: title,
                    content: '', space: space,
                    mimeType: file.contentType, fileSize: file.size, status: Status.findByCode(params.statuscode))
            contentFile.createAliasURI((parent && (parent instanceof ContentDirectory)) ? parent : null)
            contentFile.uploadedFile = file
            if (contentFile.save()) {
                if (!contentRepositoryService.createNode(contentFile, parent)) {
                    flash.error = message(code: 'error.contentRepository.fileSystem')
                }
            } else {
                flash.contentNode = contentFile
            }
        } else {
            flash.error = message(code: 'error.contentRepository.fileExists')
        }

        redirect(action: treeTable)
    }

    /**
     * @param contentPath
     * @param title
     */
    def renameNode = {
        assert !"This obsolete?"
        def content = getContent(params.contentPath, Space.get(params['space.id']?.toLong()))
        def parent = content.parent
        if (!contentFileExists(content.title, parent, content.space)) {
            def oldTitle = content.title
            content.title = params.title
            if (content.save()) {
                if (!contentRepositoryService.renameNode(content, oldTitle)) {
                    flash.error = message(code: 'error.contentRepository.fileSystem')
                }
            } else {
                flash.contentNode = content
            }
        } else {
            flash.error = message(code: 'error.contentRepository.fileExists')
        }

        redirect(action: treeTable)
    }

    /**
     * This actions 'copyNode' and 'moveNode' are called when the user drops selected node.
     *
     * There are 3 different dialogs that can be shown on UI after drop:
     * 1. If the user wants to create the selected node as root node: then the confirm dialog
     * with the message "Delete Node Reference? [Yes][No]" is dislayed. In this situation we
     * cannot perform 'Copy Node' operation, we can delete 'parent' node reference - in this case
     * if the selected node hasn't any other parents it become root node.
     *
     * 2. If the user drops root node to any other node: then the confirm dialog with the message
     * "Create Node Reference? [Yes][No]" is dislayed. In this situation we cannot perform
     * copying operation, because if the node has parents - it cannot be a root node.
     *
     * 3. If the user drops any child node to any other node: then the confirm dialog
     * with the 3 buttons is dislayed. These buttons are: 'Copy Node', 'Move Node', 'Cancel'.
     *
     * The errors during copying or moving operations are handled by Tree: in this case
     * the selected node cannot be dropped.
     *
     */

    /**
     * Copies selected node to other node in repository tree.
     *
     * params.sourcePath
     * params.targetPath
     */
    def copyNode = {
        def sourceContent = Content.get(params.sourceId)
        def targetContent = Content.get(params.targetId)
        def vcont = contentRepositoryService.linkNode(sourceContent,
            targetContent, params.index.toInteger())
        if (vcont == null){
            render([result: 'failure', error: message(code: 'error.contentRepository.linkNode')] as JSON)
        }else{
            def indexes = [:]
            contentRepositoryService.findChildren(targetContent)?.collect{indexes.put(it.id, it.orderIndex)}
            render ([result: 'success', id: vcont.id, indexes: indexes, ctype: vcont.toName()] as JSON)
        }
    }

    /**
     * Moves selected node to other location in repository tree.
     *
     * params.sourcePath
     * params.targetPath
     */
    def moveNode = {
        def sourceContent = Content.get(params.sourceId)
        def targetContent = Content.get(params.targetId)
        if (contentRepositoryService.moveNode(sourceContent, targetContent, params.index.toInteger())) {
            def indexes = [:]
            contentRepositoryService.findChildren(targetContent)?.collect{indexes.put(it.id, it.orderIndex)}
            render([result: 'success', indexes: indexes] as JSON)
        } else {
            render([result: 'failure', error: message(code: 'error.contentRepository.moveNode')] as JSON)
        }
    }

    /**
     * Deletes node for the selected content.
     *
     * params.contentPath
     */
    def deleteNode = {
        log.debug "deleteNode called with id: ${params.id}"
        
        def node = Content.get(params.id)
        if (!node) {
            render ([result:'error', error: message(code: 'error.content.repository.node.not.found')]) as JSON
        } else {
            def success = contentRepositoryService.deleteNode(node)
            def resp = [result: (success ? 'success' : 'error')]
            if (!success) {
                resp.error = message(code: 'error.content.repository.node.not.found')
            }
            render resp as JSON
        }
    }

    /**
     *
     * params.contentPath
     */
    def deleteNodeInfo = {
        def content = getContent(params.contentPath, Space.get(params['space.id']?.toLong()))
        def templateName = ''
        def model = [:]

        if (content && (content instanceof ContentFile)) {
            templateName = 'deleteFileInfo'
        } else {
            templateName = 'deleteNodeInfo'
        }

        if (content) {
            model.childContentCount = content.children.size()
            model.parentContentCount = VirtualContent.countByTarget(content)
            model.relatedContentCount = RelatedContent.countByTargetContent(content)
        }

        render(template: templateName, model: model)
    }

    /**
     * Deletes node reference for the selected content.
     *
     * params.contentPath
     */
    def deleteReference = {
        contentRepositoryService.deleteLink(getContent(params.contentPath, Space.get(params['space.id']?.toLong())),
                getParentContent(params.contentPath))
        render ([result: 'success'] as JSON)
    }

    /**
     * Renders Content as PDF document.
     *
     * params.path
     * params.isHierarchy
     */
    def pdfView = {
        def content = getContent(params.path, Space.get(params['space.id']?.toLong() ))
        Document document = new Document()
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        try {
            PdfWriter.getInstance(document, baos)
            document.open()
            Chapter chapter = new Chapter(
                    new Paragraph(content.title,
                            new Font(Font.HELVETICA, 20, Font.BOLD)), 1)
            document.add(chapter)
            chapter.flushContent()
            document.add(new Paragraph(' '))
            if (isHtmlContent(content)) {
                def preparedContent = StringUtils.replace(content.content, "&nbsp", "&#160")
                HtmlParser.parse(document, new StringReader(preparedContent))
            } else {
                document.add(new Paragraph(content.content))
            }
            /* @todo do we still need this, if so what should it do?
            if (Boolean.valueOf(params.isHierarchy)) {
                addSections(chapter, content.children, document)
            }
            */
            document.close()
        } catch(Exception e) {
            log.error e
        }

        response.setContentType("application/pdf")
        response.setHeader("Content-disposition", "attachment; filename=content.pdf")
        response.setContentLength(baos.size())
        response.getOutputStream().write(baos.toByteArray())
    }

    /**
     * Returns list of root contents for the determined space and contentType.
     *
     * @param space
     * @param contentType
     */
    private List getRootContents(space, contentType) {
        def rootNodes = contentRepositoryService.findAllRootContent(space, [type:contentType])
        def result = rootNodes.collect { content ->
            [path: generateContentName(space.id, contentType, content.id),
                label: content.title, type: content.class.name,
                hasChildren: (content.children && content.children.size()) ? true : false,
                canHaveChildren: content.canHaveChildren(),
                canHaveMultipleParents: content.canHaveMultipleParents()]
        }
        return result
    }

    private List getRootFilesAndDirectories(space) {
        def items = contentRepositoryService.findAllRootContent(space, [type:ContentFile])
        def result = items.collect {
             [path: generateContentName(space.id, 'Files', it.id),
                    label: it.title, type: it.class.name,
                    hasChildren: (it.children && it.children.size() > 0) ? true : false,
                    canHaveChildren: it.canHaveChildren(),
                    canHaveMultipleParents: it.canHaveMultipleParents()]
        }
        return result
    }

    /**
     * Generates unique content name for root content nodes.
     *
     * @param spaceId
     * @param contentType
     * @param contentId
     */
    private String generateContentName(spaceId, contentType, contentId) {
        def buf = new StringBuffer()
        buf << spaceId << '/' << contentType << '/' << contentId
        return buf.toString()
    }

    /**
     * Returns Content object from full content path eg <spaceid>/<type>/<id1>/..<idN>
     *
     * @param contentPath
     */
     private def getContent(contentPath, space) {
         if (!contentPath) return null
         
         def tokens = contentPath.split('/')
         if (tokens.size() >= 3) {
             return Content.get(contentPath.substring(contentPath.lastIndexOf('/') + 1))
         } else {
             return null
         }
     }
     
    /**
     * Returns parent Content object from content path.
     * @todo This is broken, cannot assume this about the URL tokens
     * @param contentPath
     */
    private def getParentContent(contentPath) {
        if (!contentPath) return null
        
        def tokens = contentPath.split('/')
        if (tokens.size() >= 4) {
            def parentId = tokens[tokens.size() - 2]
            return Content.get(parentId)
        } else {
            return null
        }
    }

    /**
     * Returns content type from content path.
     *
     * @param contentPath
     */
    private def getContentType(contentPath) {
        if (!contentPath) return null

        def tokens = contentPath.split('/')
        if (tokens.size() >= 2) {
            return tokens[1]
        } else {
            return null
        }
    }

    /**
     * Returns all names of Content subclasses, they are used as content types.
     *
     * @param contentPath
     */
    private def getAvailableContentTypes() {
        grailsApplication.domainClasses.findAll( { 
            (it.clazz != Content) && Content.isAssignableFrom(it.clazz) 
        }).collect({ it.clazz.name}).sort()
    }

    /**
     * Recursively adds Contents from list of hierarchies to the resulting document.
     *
     * @param parentSection
     * @param hierarchies
     * @param document
     */
/* @todo work out what this was doing and if we still need it
    private void addSections(Section parentSection, List virtualNodes,
            Document document) {
        hierarchies.each {hierarchy ->
            def section = parentSection.addSection(
                    new Paragraph(hierarchy.child.title,
                            new Font(Font.HELVETICA, 18, Font.BOLD)))
            document.add(section)
            section.flushContent()
            document.add(new Paragraph(' '))
            if (isHtmlContent(hierarchy.child)) {
                HtmlParser.parse(document, new StringReader(hierarchy.child.content))
            } else {
                document.add(new Paragraph(hierarchy.child.content))
            }
            addSections(section, ContentHierarchy.findAllByParent(hierarchy.child), document)
        }
    }
*/
    /**
     * Returns 'true' if Content has HTML markup.
     *
     * @param content
     */
    private Boolean isHtmlContent(content) {
        return (content && (content instanceof HTMLContent))
    }

    /**
     * Checks if the ContentFile/ContentDirectory with <code>title</code>
     * already contained in <code>content</code>.
     *
     * @param title
     * @param content
     */
    protected Boolean contentFileExists(String title, Content content, Space space) {
        if (!content) return Boolean.FALSE

        if ((content instanceof ContentDirectory)
                && content.children.find { it.title == title }) {
            return Boolean.TRUE
        } else if (!(content instanceof ContentDirectory)
                && ContentFile.find("""from ContentFile cf \
                        where cf.parent.class = ? \
                        and cf.space = ? and cf.title = ?""",
                        [ContentDirectory.class.name, space, title])) {
            return Boolean.TRUE
        }

        return Boolean.FALSE
    }
    
    def preview = {
        // todo: 'id' param for WeceemController simply hardcoded
        def content = Content.get(params.id)
        redirect(controller: 'content', action: 'show', params:[uri:content.space.aliasURI+'/'+content.absoluteURI])
    }
    
    def searchRequest = {
        // define search parameters
        def searchStr = params.data
        def space = null
        if (params.space) space = params.space
        def filterClass = Content
        if (params.classFilter != "none") 
            filterClass = Class.forName("${params.classFilter}", true, this.class.classLoader)
        def searchResult = filterClass.searchEvery("+title:*$searchStr* +name:$space".toString(), [reload: true])
        def fromDateFilter = null
        def toDateFilter = null
        if (params.fromDateFilter != "") fromDateFilter = new Date(params.fromDateFilter)
        if (params.toDateFilter != "") toDateFilter = new Date(params.toDateFilter)
        def sortField = params.sortField
        def ascOrder = Boolean.valueOf(params.isAsc)
        def statusFilter = Integer.valueOf(params.statusFilter)
        //performing search
        searchResult.sort({a,b -> 
            if (ascOrder) 
                return a?."$sortField"?.compareTo(b?."$sortField")
            else
                return -a?."$sortField"?.compareTo(b?."$sortField")
        })
        searchResult = searchResult.findAll{
            def flag = true
            if (fromDateFilter){
                flag = flag && (it."${params.fieldFilter}" > fromDateFilter)
            }
            if (toDateFilter){
                flag = flag && (it."${params.fieldFilter}" < toDateFilter)
            }
            flag && ((it.status.code == statusFilter) || (statusFilter == 0))
        }
        def result = searchResult.collect{["id": it.id, "title": it.title, 
        "aliasURI": it.aliasURI, "status": it.status?.description, 
        "createdBy": it.createdBy.toString(), 
        "changedOn": wcm.humanDate(date: it.changedOn), 
        "href": createLink(controller: "editor", action: "edit", id: it.id),
        "parentURI": (it.parent == null ? "": "/${it.parent.absoluteURI}"), 
        "type": message(code: "content.item.name.${it.toName()}")]} 
        render ([result: result] as JSON)
    }
}
