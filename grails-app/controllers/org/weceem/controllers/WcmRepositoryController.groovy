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

import com.lowagie.text.html.HtmlParser
import com.lowagie.text.pdf.PdfWriter
import java.text.SimpleDateFormat

import org.weceem.content.*

// Design smell
import org.weceem.files.*
import org.weceem.html.*

import org.weceem.event.WeceemDomainEvents
import org.weceem.services.DeleteNotPossibleException
import org.weceem.services.ContentRepositoryException

/**
 */
class WcmRepositoryController {
    
    /**
     * Defines instance of contentRepositoryService class.
     */
    
    static ACTIONS_NEEDING_SPACE = ['treeTable']
    
    def wcmContentRepositoryService

    def grailsApplication

    def beforeInterceptor = {
        if (!(actionName in ACTIONS_NEEDING_SPACE)) return true

        if (!params.space) {
            if (session.currentAdminSpace) {
                params.space = WcmSpace.get(session.currentAdminSpace)
                // Be careful, the space might have been deleted
                if (params.space) return true
                // If it is null we fall through and resolve again
            } 

            // Find the default space(if there is any) and redirect so url has space in
            if (WcmSpace.count() == 0){
                flash.message = message(code: 'message.there.are.no.spaces')
                redirect(controller:'wcmSpace')
                return false
            }
            def space = wcmContentRepositoryService.findDefaultSpace()
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
                params.space = wcmContentRepositoryService.findDefaultSpace()
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
            space = WcmSpace.findByName(params.space)
        } else {
            space = wcmContentRepositoryService.findDefaultSpace()
        }
        if (log.debugEnabled) {
            log.debug "Space resolved to: ${space?.dump()}"
        }
        return space        
    }

    def treeTable = {
        if (params.space && (WcmSpace.count() != 0)) {
            def nodes = wcmContentRepositoryService.findAllRootContent( params.space, 
                [params: [sort:'aliasURI', fetch:[children:'eager']] ])
            def haveChildren = [:]
            for (domainClass in wcmContentRepositoryService.listContentClasses()){
                def dcInst = domainClass.newInstance()
                haveChildren.put(domainClass.name, wcmContentRepositoryService.triggerDomainEvent(dcInst, WeceemDomainEvents.contentShouldAcceptChildren))
            }
            return [content:nodes, contentTypes:wcmContentRepositoryService.listContentClassNames(), 
                'haveChildren':haveChildren, space: params.space, spaces: WcmSpace.listOrderByName() ]
        } else {
            flash.message = 'message.there.are.no.spaces'
            redirect(controller:'wcmSpace')
        }
    }
    
    def list = {
        return [availableContentTypes: getAvailableContentTypes() - 
        WcmVirtualContent.class.name]
    }

    def getList = {
        params.offset = params.offset ? new Integer(params.offset) : 0;
        params.max = params.max ? new Integer(params.max) : 20;
        def dateFormat = new SimpleDateFormat('dd-MM-yyyy')
        def criteria = WcmContent.createCriteria()
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
        def criteria = WcmContent.createCriteria()
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
            space = WcmSpace.get(params['space.id']?.toLong())
        }
        def content = wcmContentRepositoryService.newContentInstance(params.contentType, space)
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
                hasChildren: (WcmSpace.count() > 0)]
        def rootChildren = []
        WcmSpace.list(sort: 'name').each {space ->
            def contentTypes = []
            (getAvailableContentTypes() - WcmContentFile.class.name
                                        - WcmContentDirectory.class.name
                                        - WcmVirtualContent.class.name).each { contentType ->
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
            rootChildren << [path: space.id, label: space.name, type: WcmSpace.class.name,
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
        def content = getContent(params.contentPath, WcmSpace.get(params['space.id']?.toLong()))

        if (!content) {
            flash.message = """No content with path '${params.contentPath}' was found.
                               Probably it was deleted/edited by other user."""
        }

        def parentContent = getParentContent(params.contentPath)
        def model = [content: content, parentContent: parentContent]
        if (content && (content instanceof WcmContentFile)) {
            template = 'contentFileDetails'
            if (!(parentContent && (parentContent instanceof WcmContentFile))) {
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
                model: wcmContentRepositoryService.getRelatedContent(getContent(params.contentPath, 
                    WcmSpace.get(params['space.id']?.toLong()) )))
    }

    /**
     * Loads infromation about recent changes of selected node.
     *
     * params.contentPath
     */
    def loadRecentChangesDetails = {
        render(template: 'recentChanges',
                model: wcmContentRepositoryService.getRecentChanges(getContent(params.contentPath, 
                    WcmSpace.get(params['space.id']?.toLong()))))
    }

    /**
     * Loads children nodes for the expanded node
     *
     * params.contentPath
     */
    def loadNodeChildren = {
        def space = WcmSpace.get(params['space.id']?.toLong())
        def children = wcmContentRepositoryService.findChildren(
                getContent(params.contentPath, space), space).collect {
            [path: "${params.contentPath}/${it.id}",
                label: it.title, type: it.class.name,
                hasChildren: it.children ? true : false,
                canHaveChildren: wcmContentRepositoryService.triggerDomainEvent(it, WeceemDomainEvents.contentShouldAcceptChildren)]
        }
        render children as JSON
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
        def sourceContent = WcmContent.get(params.sourceId)
        def targetContent = WcmContent.get(params.targetId)
        def vcont = wcmContentRepositoryService.linkNode(sourceContent,
            targetContent, params.index.toInteger())
        if (vcont == null){
            render([result: 'failure', error: message(code: 'error.contentRepository.linkNode')] as JSON)
        }else{
            def indexes = [:]
            wcmContentRepositoryService.findChildren(targetContent)?.collect{indexes.put(it.id, it.orderIndex)}
            render ([result: 'success', id: vcont.id, indexes: indexes, ctype: vcont.class.name] as JSON)
        }
    }

    /**
     * Moves selected node to other location in repository tree.
     *
     * params.sourcePath
     * params.targetPath
     */
    def moveNode = {
        log.debug "Move node action: ${params}"
        def sourceContent = WcmContent.get(params.sourceId)
        def targetContent = WcmContent.get(params.targetId)
        try {
            wcmContentRepositoryService.moveNode(sourceContent, targetContent, params.index.toInteger())
            def indexes = [:]
            if (targetContent) {
                wcmContentRepositoryService.findChildren(targetContent)?.collect{indexes.put(it.id, it.orderIndex)}
            }
            render([result: 'success', indexes: indexes] as JSON)
        } catch (ContentRepositoryException cre) {
            render([result: 'failure', error: message(code: 'error.contentRepository.moveNode', args:[cre.message])] as JSON)
        }
    }

    /**
     * Deletes node for the selected content.
     *
     * params.contentPath
     */
    def deleteNode = {
        log.debug "deleteNode called with id: ${params.id}"
        
        def node = WcmContent.get(params.id)
        if (!node) {
            render ([result:'error', error: message(code: 'error.content.repository.node.not.found')]) as JSON
        } else {
            def resp
            try {
                wcmContentRepositoryService.deleteNode(node)
                resp = [result: 'success']
            } catch (DeleteNotPossibleException de) {
                log.error de
                resp = [result: 'error', message: de.message]
            }
            render resp as JSON
        }
    }

    /**
     *
     * params.contentPath
     */
    def deleteNodeInfo = {
        def content = getContent(params.contentPath, WcmSpace.get(params['space.id']?.toLong()))
        def templateName = ''
        def model = [:]

        if (content && (content instanceof WcmContentFile)) {
            templateName = 'deleteFileInfo'
        } else {
            templateName = 'deleteNodeInfo'
        }

        if (content) {
            model.childContentCount = content.children.size()
            model.parentContentCount = WcmVirtualContent.countByTarget(content)
            model.relatedContentCount = WcmRelatedContent.countByTargetContent(content)
        }

        render(template: templateName, model: model)
    }

    /**
     * Deletes node reference for the selected content.
     *
     * params.contentPath
     */
    def deleteReference = {
        wcmContentRepositoryService.deleteLink(getContent(params.contentPath, WcmSpace.get(params['space.id']?.toLong())),
                getParentContent(params.contentPath))
        render ([result: 'success'] as JSON)
    }

    /**
     * Renders WcmContent as PDF document.
     *
     * params.path
     * params.isHierarchy
     */
    def pdfView = {
        def content = getContent(params.path, WcmSpace.get(params['space.id']?.toLong() ))
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
        response.setHeader("WcmContent-disposition", "attachment; filename=content.pdf")
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
        def rootNodes = wcmContentRepositoryService.findAllRootContent(space, [type:contentType])
        def result = rootNodes.collect { content ->
            [path: generateContentName(space.id, contentType, content.id),
                label: content.title, type: content.class.name,
                hasChildren: content.children ? true : false,
                canHaveChildren: wcmContentRepositoryService.triggerDomainEvent(content, WeceemDomainEvents.contentShouldAcceptChildren)
            ]
        }
        return result
    }

    private List getRootFilesAndDirectories(space) {
        def items = wcmContentRepositoryService.findAllRootContent(space, [type:WcmContentFile])
        def result = items.collect {
             [path: generateContentName(space.id, 'Files', it.id),
                    label: it.title, type: it.class.name,
                    hasChildren: it.children ? true : false,
                    canHaveChildren: wcmContentRepositoryService.triggerDomainEvent(it, WeceemDomainEvents.contentShouldAcceptChildren)
             ]
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
     * Returns WcmContent object from full content path eg <spaceid>/<type>/<id1>/..<idN>
     *
     * @param contentPath
     */
     private def getContent(contentPath, space) {
         if (!contentPath) return null
         
         def tokens = contentPath.split('/')
         if (tokens.size() >= 3) {
             return WcmContent.get(contentPath.substring(contentPath.lastIndexOf('/') + 1))
         } else {
             return null
         }
     }
     
    /**
     * Returns parent WcmContent object from content path.
     * @todo This is broken, cannot assume this about the URL tokens
     * @param contentPath
     */
    private def getParentContent(contentPath) {
        if (!contentPath) return null
        
        def tokens = contentPath.split('/')
        if (tokens.size() >= 4) {
            def parentId = tokens[tokens.size() - 2]
            return WcmContent.get(parentId)
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
     * Returns all names of WcmContent subclasses, they are used as content types.
     *
     * @param contentPath
     */
    private def getAvailableContentTypes() {
        grailsApplication.domainClasses.findAll( { 
            (it.clazz != WcmContent) && WcmContent.isAssignableFrom(it.clazz)
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
     * Returns 'true' if WcmContent has HTML markup.
     *
     * @param content
     */
    private Boolean isHtmlContent(content) {
        return (content && (content instanceof WcmHTMLContent))
    }

    /**
     * Checks if the WcmContentFile/WcmContentDirectory with <code>title</code>
     * already contained in <code>content</code>.
     *
     * @param title
     * @param content
     */
    protected Boolean contentFileExists(String title, WcmContent content, WcmSpace space) {
        if (!content) return Boolean.FALSE

        if ((content instanceof WcmContentDirectory)
                && content.children.find { it.title == title }) {
            return Boolean.TRUE
        } else if (!(content instanceof WcmContentDirectory)
                && WcmContentFile.find("""from WcmContentFile cf \
                        where cf.parent.class = ? \
                        and cf.space = ? and cf.title = ?""",
                        [WcmContentDirectory.class.name, space, title])) {
            return Boolean.TRUE
        }

        return Boolean.FALSE
    }
    
    def preview = {
        // todo: 'id' param for WeceemController simply hardcoded
        def content = WcmContent.get(params.id)
        redirect(controller: 'wcmContent', action: 'show', params:[uri:content.space.aliasURI+'/'+content.absoluteURI])
    }
    
    def searchRequest = {
        if (log.debugEnabled) {
            log.debug "Searching repository: ${params}"
        }
        // define search parameters
        def searchStr = params.data
        def space = null
        if (params.space) space = WcmSpace.get(params.long('space'))
        
        def filterClass = WcmContent
        if (params.classFilter != "none") 
            filterClass = Class.forName("${params.classFilter}", true, this.class.classLoader)
//        def searchResults = filterClass.search("*$searchStr* +name:$space".toString(), [reload: true])
        def searchResults = wcmContentRepositoryService.searchForContent("*$searchStr*", space, null, [type:filterClass])
        def searchResult = []
        searchResult.addAll(searchResults.results)
        
        if (log.debugEnabled) {
            log.debug "Searching repository resulted in: ${searchResults}"
        }

        def fromDateFilter = null
        def toDateFilter = null
        if (params.fromDateFilter != "") fromDateFilter = new Date(params.fromDateFilter)
        if (params.toDateFilter != "") toDateFilter = new Date(params.toDateFilter)
        def sortField = params.sortField
        def ascOrder = Boolean.valueOf(params.isAsc)
        def statusFilter = Integer.valueOf(params.statusFilter)
        //performing search
        searchResult = searchResult.sort({a,b ->
            def fieldPath = sortField.tokenize('.')
            def valA = a
            for (field in fieldPath){
                valA = valA?."$field"
            }
            def valB = b
            for (field in fieldPath){
                valB = valB?."$field"
            }
            if (ascOrder){ 
                if (valA == null) return -1
                if (valB == null) return 1
                return valA.compareTo(valB)
            }
            else{
                if (valA == null) return 1
                if (valB == null) return -1
                return valB.compareTo(valA)
            }
        })
        searchResult = searchResult.findAll {
            def flag = true
            if (fromDateFilter){
                flag = flag && (it."${params.fieldFilter}" > fromDateFilter)
            }
            if (toDateFilter){
                flag = flag && (it."${params.fieldFilter}" < toDateFilter)
            }
            flag && ((it.status.code == statusFilter) || (statusFilter == 0))
        }
        def result = searchResult.collect { 
            ["id": it.id, "title": it.title, 
            "aliasURI": it.aliasURI, "status": it.status?.description, 
            "createdBy": it.createdBy.toString(), 
            "changedOn": wcm.humanDate(date: it.changedOn).toString(), 
            "iconHref": wcm.contentIconURL(type:it.class),
            "href": createLink(controller: "wcmEditor", action: "edit", id: it.id),
            "parentURI": (it.parent == null ? "": "/${it.parent.absoluteURI}"), 
            "type": message(code: "content.item.name.${it.class.name}")]
        } 

        render ([result: result] as JSON)
    }
}
