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
package org.weceem.tags

import java.text.SimpleDateFormat
import org.weceem.controllers.ContentController
import org.codehaus.groovy.grails.commons.ApplicationHolder as AH
import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH
import org.weceem.content.Content
import org.weceem.files.ContentFile
import org.weceem.services.ContentRepositoryService

import org.weceem.content.Template
import grails.util.GrailsUtil
import org.weceem.content.Space

class WeceemTagLib {
    
    static ATTR_ID = "id"
    static ATTR_NODE = "node"
    static ATTR_TYPE = "type"
    static ATTR_MAX = "max"
    static ATTR_SORT = "sort"
    static ATTR_ORDER = "order"
    static ATTR_OFFSET = "offset"
    static ATTR_PATH = "path"
    static ATTR_PARENT = "parent"
    static ATTR_SUCCESS = "success"
    static ATTR_STATUS = "status"
    static ATTR_SPACE = "space"
    static ATTR_VAR = "var"
    static ATTR_SIBLINGS = "siblings"
    static ATTR_LEVELS = "levels"
    static ATTR_CUSTOM = "custom"
    static ATTR_ACTIVE_CLASS = "activeClass"
    static ATTR_FIRST_CLASS = "firstClass"
    static ATTR_LAST_CLASS = "lastClass"
    static ATTR_LEVEL_CLASS_PREFIX = "levelClassPrefix"
    static ATTR_CHANGEDSINCE = "changedSince"
    static ATTR_CHANGEDBEFORE = "changedBefore"
    static ATTR_CREATEDSINCE = "changedSince"
    static ATTR_CREATEDBEFORE = "changedBefore"
    static ATTR_FILTER = "filter"
    static ATTR_FORMAT = "format"
    static ATTR_CODEC = "codec"
    static ATTR_TITLE = "title"

    static namespace = "wcm"
    
    def contentRepositoryService
    def weceemSecurityService
    def pluginManager
    
    private extractCodec(attrs) {
        attrs[ATTR_CODEC] == null ? 'HTML' : attrs[ATTR_CODEC]        
    }
    
    private renderNodeProperty(propname, attrs) {
        def codec = extractCodec(attrs)
        out << request[ContentController.REQUEST_ATTRIBUTE_NODE].propname."encodeAs$codec"()
    }
    
    def space = { attrs -> 
        def codec = extractCodec(attrs)
        out << request[ContentController.REQUEST_ATTRIBUTE_SPACE].name."encodeAs$codec"()
    }
    
    /**
     * Tag that reveals user info while hiding the implementation details of the authentication system
     */
    def userInfo = { attrs, body -> 
        def user = weceemSecurityService.getUserPrincipal()
        def var = attrs[ATTR_VAR] ?: null
        out << body(var ? [(var):user] : user)
    }
    
/*
    def title = { attrs -> 
        renderNodeProperty('title', attrs)
    }

    def createdBy = { attrs -> 
        renderNodeProperty('createdBy', attrs)
    }

    def createdOn = { attrs -> 
        def codec = extractCodec(attrs)
        def format = attrs.format ?: 'yyyy/mm/dd hh:MM:ss'
        out << new SimpleDateFormat(format).format(request['activeNode'].createdOn)."encodeAs$codec"()
    }

    def changedBy = { attrs -> 
        renderNodeProperty('changedBy', attrs)
    }

    def changedOn = { attrs -> 
        def codec = extractCodec(attrs)
        def format = attrs[ATTR_FORMAT] ?: 'yyyy/mm/dd hh:MM:ss'
        out << new SimpleDateFormat(format).format(request['activeNode'].changedOn)."encodeAs$codec"()
    }
*/    
    private makeFindParams(attrs) {
        def r = [:]
        r.max = attrs[ATTR_MAX]
        r.offset = attrs[ATTR_OFFSET]
        r.sort = attrs[ATTR_SORT] ?: 'orderIndex' // Default to orderIndex otherwise things are crazy
        r.order = attrs[ATTR_ORDER] ?: 'asc'
        r.changedSince = attrs[ATTR_CHANGEDSINCE]
        r.changedBefore = attrs[ATTR_CHANGEDBEFORE]
        r.createdSince = attrs[ATTR_CREATEDSINCE]
        r.createdBefore = attrs[ATTR_CREATEDBEFORE]
        return r
    }
    
    def eachChild = { attrs, body -> 
        def params = makeFindParams(attrs)
        if (attrs[ATTR_NODE] && attrs[ATTR_PATH]) {
          throwTagError("can not specify ${ATTR_NODE} and ${ATTR_PATH} attributes")
        }
        def baseNode = attrs[ATTR_NODE] ?: request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        if (attrs[ATTR_PATH]) {
            baseNode = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], 
                request[ContentController.REQUEST_ATTRIBUTE_SPACE])?.content
        }
        def children = contentRepositoryService.findChildren(baseNode, [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) children = children?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        children?.each { child ->
            out << body(var ? [(var):child] : child)
        }
    }
    
    def countChildren = { attrs ->
       if (attrs[ATTR_NODE] && attrs[ATTR_PATH]) {
          throwTagError("You cannot specify both ${ATTR_NODE} and ${ATTR_PATH} attributes")
       }

        def baseNode = attrs[ATTR_NODE]
        if (!baseNode) {
            if (attrs[ATTR_PATH]) {
                baseNode = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], 
                    request[ContentController.REQUEST_ATTRIBUTE_SPACE])?.content 
            } else {
                baseNode = request[ContentController.REQUEST_ATTRIBUTE_NODE]
            }
        }
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        out << contentRepositoryService.countChildren(baseNode, [type:attrs[ATTR_TYPE], status:status])
    }
    
    def eachParent = { attrs, body -> 
        def params = makeFindParams(attrs)
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        def parents = contentRepositoryService.findParents(request[ContentController.REQUEST_ATTRIBUTE_NODE], 
            [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) parents = parents?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        parents?.each { parent ->
            out << body(var ? [(var):parent] : parent)
        }
    }
   
    def eachSibling = { attrs, body -> 
        def params = makeFindParams(attrs)
        def lineage = request[ContentController.REQUEST_ATTRIBUTE_PAGE].lineage
        def parentHierarchyNode = lineage.size() > 0 ? lineage[-1] : null
        def status = attrs[ATTR_STATUS] ?: ContentRepositoryService.STATUS_ANY_PUBLISHED
        def siblings 
        if (!parentHierarchyNode) {
            siblings = contentRepositoryService.findAllRootContent( 
                request[ContentController.REQUEST_ATTRIBUTE_SPACE],attrs[ATTR_TYPE])
        } else {
            siblings = contentRepositoryService.findChildren( parentHierarchyNode.parent, 
                [type:attrs[ATTR_TYPE], params:params, status:status])
        }
        if (attrs[ATTR_FILTER]) siblings = siblings?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        siblings?.each { sibling ->
            out << body(var ? [(var):sibling] : sibling)
        }
    }
    
    def eachDescendent = { attrs, body -> 
        throwTagError("eachDescendent not implemented yet")
    }
    
    def eachContent = { attrs, body -> 
        throwTagError("link not implemented yet")
        /*
        def params = makeFindParams(attrs)
        def content = contentRepositoryService.findContent(attrs[ATTR_TYPE], params)
        if (attrs[ATTR_FILTER]) content = content?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        content?.each { node ->
            out << body(var ? [(var):node] : node)
        }
        */
    }
    
    /**
     * Renders a breadcrumb trail. Renders up to but not including the current page, from the root of
     * the space. 
     * Attributes:
     *  divider - an optional string that is the HTML used between breadcrumb elements. HTML encode the value in your GSP
     *  custom - an optional value, which if evaluates to boolean "true" will use the supplied tag body to render each
     *     item in the trail. Variables "first" and "node" are passed to the body
     */
    def breadcrumb = { attrs, body -> 
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def lineage = request[ContentController.REQUEST_ATTRIBUTE_PAGE].lineage
        def div = attrs.divider?.decodeHTML() ?: ' &gt; '
        def first = true
        if (!attrs.custom?.toString()?.toBoolean()) {
            body = { args -> 
                def o = new StringBuilder()
                if (!args.first) {
                    o << div
                }

                o << link(node:args.node) {
                    out << args.node.titleForMenu.encodeAsHTML() 
                } 
                return o.toString()
            }
        }
        
        lineage.each { parent ->
            if (parent != node) {
                out << body(first:first, node:parent)
            }
            first = false
        }
    }
    
    /**
     * Render a menu based on the content hierarchy
     * Attributes:
     *  levels - the number of levels to render, defaults to 2
     *  custom - if set to boolean "true" will use the body to render each item, passing in the variables:
     *      first, last, active, link and node variables. The link is the href to the content, node is the content node of that menu item
     */
    def menu = { attrs, body ->
        def node = attrs.node ?: request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def lineage = request[ContentController.REQUEST_ATTRIBUTE_PAGE].lineage

        def currentLevel = request['_weceem_menu_level'] == null ? 0 : request['_weceem_menu_level']
        def siblings = currentLevel > 0 || ((attrs[ATTR_SIBLINGS] ?: 'y').toString().toBoolean())
        def activeClass = attrs[ATTR_ACTIVE_CLASS] ?: 'weceem-menu-active'
        def firstClass = attrs[ATTR_FIRST_CLASS] ?: 'weceem-menu-first'
        def lastClass = attrs[ATTR_LAST_CLASS] ?: 'weceem-menu-last'
        def levelClassPrefix = attrs[ATTR_LEVEL_CLASS_PREFIX] ?: 'weceem-menu-level'

        def custom = attrs[ATTR_CUSTOM]?.toString()?.toBoolean()
        def levels = attrs[ATTR_LEVELS]?.toString()?.toInteger() ?: 2
        def bodyToUse = body
        if (!custom) {
            bodyToUse = { args -> 
                def o = new StringBuilder()

                o << "<li class=\"$levelClassPrefix${args.level} ${args.active ? activeClass : ''} ${args.first ? firstClass : ''} ${args.last ? lastClass : ''}\">"
                o << link(node:args.node) {
                    out << args.node.titleForMenu.encodeAsHTML() 
                } 
                o << "</li>"
                return o.toString()
            }
        }
                
        def activeNode = lineage ? lineage[currentLevel] : null

        def levelnodes
        def args = [
            status:ContentRepositoryService.STATUS_ANY_PUBLISHED, 
            type: org.weceem.html.HTMLContent,
            params:[sort:'orderIndex']
        ]
            
        if (siblings) {
            if (currentLevel == 0) {
                levelnodes = contentRepositoryService.findAllRootContent(node.space, args)
            } else {
                levelnodes = contentRepositoryService.findChildren(node, args)
            }
        } else {
            levelnodes = [activeNode]
        }
            
        def first = true
        def last = false
        def lastIndex = levelnodes.size()-1
        levelnodes?.eachWithIndex { n, i -> 
            if (!custom && first) {
                out << "<ul>"
            }
            last = i == lastIndex
            out << bodyToUse(first:first, active:n == activeNode, level:currentLevel, last: last, 
                link:custom ? createLink(node:n, { out << n.titleForMenu.encodeAsHTML()}) : '', node:n)
            if (currentLevel+1 < levels) {
                request['_weceem_menu_level'] = currentLevel+1
                out << menu([custom:true, node:n], bodyToUse)
                request['_weceem_menu_level'] = currentLevel // back to where we were
            }
            first = false
            if (!custom && last) {
                out << "</ul>"
            }
        }
    }

    /**
    * output a recursive treemenu based either on a node or the root
     * attributes:
     * node - the node to base the menu on
     * levels - the number of levels
     * id - id of the menu
     */
    def treeMenu = { attrs ->
        def node = attrs.node
        int levels = attrs.levels ? attrs.levels.toInteger() : 2
        def id = attrs.id

        def args = [
            status:ContentRepositoryService.STATUS_ANY_PUBLISHED,
            type: org.weceem.html.HTMLContent,
            params:[sort:'orderIndex']
        ]

        def tmenu = { aNode, level=1 ->
            out << "<ul ${id ? 'id=${id}' : ''} class='menu menu-level-${level}'>"
            out << "<li class='menu-item'>"
            out << link(node:aNode, {aNode.titleForMenu.encodeAsHTML()})
            if (level < levels) {
                contentRepositoryService.findChildren(aNode, args).each {child ->
                    owner.call(child, ++level)
                }
            }
            out << "</li></ul>"
        }

        if(node) {
            tmenu(node)
        } else {
            contentRepositoryService.findAllRootContent(request[ContentController.REQUEST_ATTRIBUTE_SPACE], args).each {
                tmenu(it)
            }
        }
    }


    def link = { attrs, body -> 
        def o = out
        o << "<a href=\"${createLink(attrs)}\""
        attrs.remove('path')
        o << attrs.collect {k, v -> " $k=\"$v\"" }.join('')
        o << '>'
        o << body()
        o << "</a>"
    }
    
    def createLink = { attrs, body -> 
        def space = request[ContentController.REQUEST_ATTRIBUTE_SPACE]
        if (attrs[ATTR_SPACE]) {
            space = Space.findByAliasURI(attrs[ATTR_SPACE])
            if (!space) {
                throwTagError "Tag invoked with space attribute value [${attrs[ATTR_SPACE]}] but no space could be found with that aliasURI"
            }
        }
        def content = attrs[ATTR_NODE]
        if (content && !(content instanceof Content)) {
            throwTagError "Tag invoked with [$ATTR_NODE] attribute but the value is not a Content instance"
        }
        if (!content) {
            def contentInfo = contentRepositoryService.findContentForPath(attrs[ATTR_PATH], space)
            if (!contentInfo?.content) {
                log.error ("Tag [wcm:createLink] cannot create a link to the content at path ${attrs[ATTR_PATH]} as "+
                    "there is no content node at that URI")
                out << g.createLink(controller:'content', action:'notFound', params:[path:attrs[ATTR_PATH]])
                return
            }
            content = contentInfo.content
        }
        
        // @todo This is quite crappy, we should be getting these urls from a cache
        StringBuffer path = new StringBuffer()
        if (space.aliasURI) {
            path << space.aliasURI
            path << '/'
        }
        if(content.absoluteURI) {
            path << content.absoluteURI
            path <<  '/'
        }
        attrs.params = [uri:path.toString()]
        attrs.controller = 'content'
        attrs.action = 'show'
        out << g.createLink(attrs)
    }
    
    def date = { attrs, body -> 
        out << formatDate(format:format ?: 'dd MMM yyyy', date: new Date())
    }
    
    def find = { attrs, body -> 
        def params = makeFindParams(attrs)
        def id = attrs[ATTR_ID]
        def title = attrs[ATTR_TITLE]
        def path = attrs[ATTR_PATH]
        def c
        if (id) {
            c = Content.get(id)
        } else if (title) {
            c = Content.findByTitle(title, params)
        } else if (path) {
            c = contentRepositoryService.findContentForPath(path, 
                request[ContentController.REQUEST_ATTRIBUTE_SPACE])?.content
        } else throwTagError("One of [id], [title] or [path] must be specified")
        def var = attrs[ATTR_VAR] ?: null

        out << body(var ? [(var):c] : c)
    }
    
    def content = { attrs, body ->
        def codec = attrs.codec
        // See if there is pre-rendered content, if so use that
        def text = request[ContentController.REQUEST_PRERENDERED_CONTENT] ?: 
            request[ContentController.REQUEST_ATTRIBUTE_NODE]?.content
        def content = codec ? text."encodeAs$codec"() : text 
        out << content
    }
    
    def createLinkToFile = { attrs ->
        def space = attrs.space ? Space.findByAliasURI(attrs.space) : request[ContentController.REQUEST_ATTRIBUTE_SPACE]
        if (!space) {throwTagError("Space ${attrs.space} not found")}
        if (!attrs[ATTR_PATH]) {
            throwTagError("Attribute [${ATTR_PATH}] must be specified, eg the path to the file: images/icon.png")
        }
        def aliasURI = space.aliasURI ?: ContentFile.EMPTY_ALIAS_URI
        
        out << g.resource(dir:"${ContentFile.DEFAULT_UPLOAD_DIR}/${aliasURI}", file:attrs[ATTR_PATH])
    }

    def humanDate = { attrs ->
        def now = new Date()
        if (attrs.date) {
            use(org.codehaus.groovy.runtime.TimeCategory) {
                def millisDelta = now - attrs.date
                def daysElapsed = millisDelta.days
                if (daysElapsed > 0) {
                    out << message(code:'human.date.days.ago', args:[daysElapsed])
                } else {
                    def hoursElapsed = millisDelta.hours
                    if (hoursElapsed > 0) {
                        out << message(code:'human.date.hours.ago', args:[hoursElapsed])
                    } else {
                        def minutesElapsed = millisDelta.minutes
                        if (minutesElapsed > 0) {
                            out << message(code:'human.date.minutes.ago', args:[minutesElapsed])
                        } else {
                            def secondsElapsed = millisDelta.seconds
                            if (secondsElapsed >= 0) {
                                out << message(code:'human.date.seconds.ago', args:[secondsElapsed])
                            }
                        }
                    }
                }
            }
        } else {
            out << message(code:'human.date.null')
        }
    }
    
    def loggedInUserName = { attrs ->
        out << weceemSecurityService.userName?.encodeAsHTML()
    }
    
    def ifUserCanEdit = { attrs, body ->
        def node = attrs[ATTR_NODE]
        if (!node) node = request[ContentController.REQUEST_ATTRIBUTE_NODE]

        if (weceemSecurityService.isUserAllowedToEditContent(node)) {
            out << body()
        }
    }
    
    def ifContentIs = { attrs, body ->
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        if (!targetType)
            throwTagError("Attribute [${ATTR_TYPE}] is required on tag ifContentIs. It must be a fully qualified class name")

        def targetClass = grailsApplication.getClassForName(targetType)
        if (!targetClass)
            throwTagError("Attribute [${ATTR_TYPE}] specified class [${targetType}] but it could not be located")
        
        if (targetClass.isAssignableFrom(node.class)) {
            out << body()
        }
    }

    def ifContentIsNot = { attrs ->
        def node = request[ContentController.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        
        if (!grailsApplication.getClassForName(targetType).isAssignableFrom(node.class)) {
            out << body()
        }
    }
    
    def renderContentItemIcon = { attrs ->
        def type = attrs[ATTR_TYPE]
        def id = attrs[ATTR_ID]
        def iconconf = type.icon
        def plugin = iconconf.plugin
        out << "<div id='${id}' class='ui-content-icon'><img src='${g.resource(plugin:plugin, dir: iconconf.dir, file: iconconf.file)}'/></div>"
    }    

    /**
     * Utility function to get a content node from an attribute value that can be any of:
     * 
     * a number type - the id of a content node to get
     * a Content node - will be returned as the value
     * anything that can be coerced to a String - if results in a number, will 
     *    call get on it else calls findContentForPath in the current space
     */
    Content attributeToContent(attribValue) {
        if (attribValue instanceof Number) {
            Content.get(attribValue.toLong())
        } else if (attribValue instanceof Content) {
            attribValue
        } else {
            def s = attribValue.toString()
            if (s.isLong()) {
                Content.get(s.toLong())
            } else {
                contentRepositoryService.findContentForPath(s, request[ContentController.REQUEST_ATTRIBUTE_SPACE])
            }
        }
    }

    def submitContent = { attrs ->
        def parent = attributeToContent(attrs[ATTR_PARENT])
        def type = attrs[ATTR_TYPE]
        def success = attributeToContent(attrs[ATTR_SUCCESS])
        def currentContentPath = request[ContentController.REQUEST_ATTRIBUTE_PAGE].URI
        def space = request[ContentController.REQUEST_ATTRIBUTE_SPACE]
        
        out << g.createLink(controller:'contentSubmission', action:'submit', params:[
            spaceId:space.id,
            parentId:parent.id,
            type:type,
            successPath:success.absoluteURI,
            formPath:currentContentPath
        ])
    }
}
