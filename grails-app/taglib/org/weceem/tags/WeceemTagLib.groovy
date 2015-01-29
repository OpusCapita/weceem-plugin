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

import org.weceem.content.RenderEngine
import org.weceem.content.WcmContent
import org.weceem.content.WcmSpace
import org.weceem.controllers.WcmContentController
import org.weceem.css.WcmStyleSheet
import org.weceem.js.WcmJavaScript
import org.weceem.services.WcmContentRepositoryService
import org.weceem.util.ContentUtils

import java.text.DateFormatSymbols

class WeceemTagLib {
    
    static ATTR_ID = "id"
    static ATTR_NODE = "node"
    static ATTR_TYPE = "type"
    static ATTR_TYPES = "types"
    static ATTR_MAX = "max"
    static ATTR_SORT = "sort"
    static ATTR_ORDER = "order"
    static ATTR_OFFSET = "offset"
    static ATTR_SHUFFLE = "shuffle"
    static ATTR_PATH = "path"
    static ATTR_PARENT = "parent"
    static ATTR_SUCCESS = "success"
    static ATTR_STATUS = "status"
    static ATTR_SPACE = "space"
    static ATTR_COUNTER = "counter"
    static ATTR_VAR = "var"
    static ATTR_IN = "in"
    static ATTR_VALUE = "value"
    static ATTR_SIBLINGS = "siblings"
    static ATTR_LEVELS = "levels"
    static ATTR_LENGTH = "length"
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
    static ATTR_VERSION = "version"
    static ATTR_RESULTSPATH = "resultsPath"
    static ATTR_GPATH = "gpath"
    static ATTR_URL = "url"

    static namespace = "wcm"
    
    static returnObjectForTags = ['findNode', 'ancestorOfType', 'contentIconURL', 'getClassName']
    
    def wcmContentRepositoryService
    def wcmSecurityService
    def pluginManager
    def wcmCacheService
    def grailsApplication
    def proxyHandler
    
    private extractCodec(attrs) {
        attrs[ATTR_CODEC] == null ? 'HTML' : attrs[ATTR_CODEC]        
    }
    
    private renderNodeProperty(propname, attrs) {
        def codec = extractCodec(attrs)
        out << request[RenderEngine.REQUEST_ATTRIBUTE_NODE].propname."encodeAs$codec"()
    }
    
    def space = { attrs -> 
        def codec = extractCodec(attrs)
        out << request[RenderEngine.REQUEST_ATTRIBUTE_SPACE].name."encodeAs$codec"()
    }
    
    /**
     * Tag that reveals user info while hiding the implementation details of the authentication system
     */
    def userInfo = { attrs, body -> 
        def user = wcmSecurityService.getUserPrincipal()
        def var = attrs[ATTR_VAR] ?: null
        out << body(var ? [(var):user] : user)
    }

    private makeFindParams(attrs) {
        def r = [:]
        r.max = attrs[ATTR_MAX]?.toInteger()
        r.offset = attrs[ATTR_OFFSET]?.toInteger()
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
        def baseNode = resolveNode(attrs)
        def status = attrs[ATTR_STATUS] ?: WcmContentRepositoryService.STATUS_ANY_PUBLISHED

        def shuffle = attrs[ATTR_SHUFFLE] ? Boolean.parseBoolean(attrs[ATTR_SHUFFLE].toString()) : false
        if (shuffle && attrs[ATTR_MAX] && attrs[ATTR_OFFSET] == null) {
            def countParams = [ ATTR_CHANGEDSINCE:params.changedSince, ATTR_CHANGEDBEFORE:params.changedBefore, ATTR_CREATEDSINCE:params.createdSince, ATTR_CREATEDBEFORE:params.createdBefore]
            def count = wcmContentRepositoryService.countChildren(baseNode, [type:attrs[ATTR_TYPE], status:status, params:countParams])
            params.offset = (int) Math.round(Math.random() * (count - params.max))
        }

        def children = wcmContentRepositoryService.findChildren(baseNode, [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) children = children?.findAll(attrs[ATTR_FILTER])
        if (shuffle) Collections.shuffle(children)

        def var = attrs[ATTR_VAR] ?: null
        def counter = attrs[ATTR_COUNTER] ?: null
        children?.eachWithIndex { child, idx ->
            def args = child
            if (counter || var) {
                args = [:]
                args[var ?: 'it'] = child
                if (counter) {
                    args[counter] = idx
                }
            }
            out << body(args)
        }
    }
    
    def countChildren = { attrs ->
        def baseNode = resolveNode(attrs)
        def status = attrs[ATTR_STATUS] ?: WcmContentRepositoryService.STATUS_ANY_PUBLISHED
        out << wcmContentRepositoryService.countChildren(baseNode, [type:attrs[ATTR_TYPE], status:status])
    }
    
    def eachParent = { attrs, body -> 
        def params = makeFindParams(attrs)
        def status = attrs[ATTR_STATUS] ?: WcmContentRepositoryService.STATUS_ANY_PUBLISHED
        def parents = wcmContentRepositoryService.findParents(request[RenderEngine.REQUEST_ATTRIBUTE_NODE],
            [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) parents = parents?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        def counter = attrs[ATTR_COUNTER] ?: null
        parents?.eachWithIndex { parent, idx ->
            def args = parent
            if (counter || var) {
                args = [:]
                args[var ?: 'it'] = parent
                if (counter) {
                    args[counter] = idx
                }
            }
            out << body(args)
        }
    }
   
    def eachSibling = { attrs, body ->
        def space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        if (attrs[ATTR_SPACE] != null) {
            space = WcmSpace.findByAliasURI(attrs[ATTR_SPACE])
            if (!space) {
                throwTagError "Tag invoked with space attribute value [${attrs[ATTR_SPACE]}] but no space could be found with that aliasURI"
            }
        }

        def params = makeFindParams(attrs)
        def lineage = request[RenderEngine.REQUEST_ATTRIBUTE_PAGE].lineage
        def parentHierarchyNode = lineage.size() > 0 ? lineage[-1] : null
        if (attrs[ATTR_NODE] != null || attrs[ATTR_PATH] != null) {
            def attr_node = attrs[ATTR_NODE]
            def attr_path = attrs[ATTR_PATH]
            def node = resolveNode(attrs)
            if (!node) {
                throwTagError "Tag invoked with node, nodePath attribute values [${attr_node}], [${attr_path}]  but no node could be found with these values"
            }
            parentHierarchyNode = node?.parent ? node : null
        }

        def status = attrs[ATTR_STATUS] ?: WcmContentRepositoryService.STATUS_ANY_PUBLISHED
        def siblings 
        if (!parentHierarchyNode) {
            siblings = wcmContentRepositoryService.findAllRootContent(space, [type: attrs[ATTR_TYPE]])
        } else {
            siblings = wcmContentRepositoryService.findChildren( parentHierarchyNode.parent, 
                [type:attrs[ATTR_TYPE], params:params, status:status])
        }

        if (attrs[ATTR_FILTER]) siblings = siblings?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        def counter = attrs[ATTR_COUNTER] ?: null
        siblings?.eachWithIndex { sibling, idx ->
            def args = sibling
            if (counter || var) {
                args = [:]
                args[var ?: 'it'] = sibling
                if (counter) {
                    args[counter] = idx
                }
            }
            out << body(args)
        }
    }
    
    def eachDescendent = { attrs, body -> 
        throwTagError("eachDescendent not implemented yet")
    }
    
    /**
     * Executes the body for each content in the given/current space. 
     */
    def eachContent = { attrs, body -> 
        def space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        if (attrs[ATTR_SPACE] != null) {
            space = WcmSpace.findByAliasURI(attrs[ATTR_SPACE])
            if (!space) {
                throwTagError "Tag invoked with space attribute value [${attrs[ATTR_SPACE]}] but no space could be found with that aliasURI"
            }
        }
        def params = makeFindParams(attrs)
        def status = attrs[ATTR_STATUS] ?: WcmContentRepositoryService.STATUS_ANY_PUBLISHED
        
        def contentList = wcmContentRepositoryService.findAllContent(space, [type:attrs[ATTR_TYPE], status:status, params:params])
        if (attrs[ATTR_FILTER]) contentList = contentList?.findAll(attrs[ATTR_FILTER])
        def var = attrs[ATTR_VAR] ?: null
        contentList?.each { content ->
            out << body(var ? [(var):content] : content)
        }
    }
         
    /**
     * Renders a breadcrumb trail. Renders entries including the current page, from the root of
     * the space, but the final element is not a link
     * Attributes:
     *  divider - an optional string that is the HTML used between breadcrumb elements. HTML encode the value in your GSP
     *  custom - an optional value, which if evaluates to boolean "true" will use the supplied tag body to render each
     *     item in the trail. Variables "first" and "node" are passed to the body
     */
    def breadcrumb = { attrs, body -> 
        def node = request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        def lineage = request[RenderEngine.REQUEST_ATTRIBUTE_PAGE].lineage
        def div = (attrs.divider != null) ? attrs.divider.decodeHTML() : ' &gt; '
        def first = true
        if (!attrs.custom?.toString()?.toBoolean()) {
            body = { args -> 
                def o = new StringBuilder()
                if (!args.first) {
                    o << div
                }

                def title = args.breadcrumbNode.titleForMenu.encodeAsHTML() 
                if (!args.last) {
                    o << wcm.link(node:args.breadcrumbNode) {
                        out << title
                    } 
                } else {
                    o << "<span class=\"last\">${title}</span>"
                }
                return o.toString()
            }
        }
        
        def nodes = lineage
        nodes.each { current ->
            out << body(first:first, last: current.ident() == node.ident(), breadcrumbNode:current)
            first = false
        }
    }

    static DEFAULT_MENU_BODY = { args -> 
        def o = args.out

        o << "<li class=\"${args.levelClassPrefix}${args.level} ${args.active ? args.activeClass : ''} ${args.first ? args.firstClass : ''} ${args.last ? args.lastClass : ''}\">"
        def n = args.menuNode
        o << wcm.link(node:n) { ->
            n.titleForMenu.encodeAsHTML() 
        } 
        o << args.nested
        o << "</li>"
        return null
    }

    WcmContent resolveNode(Map attrs, defaultToCurrentNode = true) {
        def nodePath = attrs.remove(ATTR_PATH)
        def node = attrs.remove(ATTR_NODE)
        if (node && nodePath) {
            throwTagError("You cannot specify both ${ATTR_NODE} and ${ATTR_PATH} attributes")
        }
        def rootNodeSpecified = nodePath || node
        if (!node && nodePath) {
            if (nodePath instanceof WcmContent) {
                node = nodePath
            } else {
                def space = attrs.remove(ATTR_SPACE)
                if (space != null) {
                    if (!(space instanceof WcmSpace)) {
                        space = WcmSpace.findByAliasURI(space)
                        if (!space) {
                            throwTagError "Tag invoked with space attribute value [${space}] but no space could be found with that aliasURI"
                        }
                    }
                } else {
                    space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
                    if (!space) {
                        throwTagError "Tag invoked from outside a Weceem request requires the [${ATTR_SPACE}] attribute to be set to the aliasURI of the space or a space instance"
                    }
                }

                def contentInfo = wcmContentRepositoryService.findContentForPath(nodePath, space)
                if (!contentInfo?.content) {
                    return null
                }
                node = contentInfo.content
            }
        }
        
        if (node && !(node instanceof WcmContent)) {
            throwTagError "Tag invoked with [$ATTR_NODE] or [$ATTR_PATH] attribute but the value it results in is not a WcmContent instance"
        }

        if (!node && defaultToCurrentNode) {
            node = request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        }
        node
    }
    
    /**
     * Render a menu based on the content hierarchy
     * Attributes:
     *  levels - the number of levels to render, defaults to 2
     *  custom - if set to boolean "true" will use the body to render each item, passing in the variables:
     *      first, last, active, link and node variables. The link is the href to the content, node is the content node of that menu item
     */
    def menu = { attrs, body ->
        // We need to know whether or not one was specified
        def node = resolveNode(attrs, false)        
        def rootNodeSpecified = node ? true : false
        if (node == null) {
            node = request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        }
        def lineage = request[RenderEngine.REQUEST_ATTRIBUTE_PAGE].lineage

        def currentLevel = request['_weceem_menu_level'] == null ? 0 : request['_weceem_menu_level']
        def siblings = currentLevel > 0 || ((attrs[ATTR_SIBLINGS] ?: 'y').toString().toBoolean())
        def activeClass = attrs[ATTR_ACTIVE_CLASS] ?: 'weceem-menu-active'
        def firstClass = attrs[ATTR_FIRST_CLASS] ?: 'weceem-menu-first'
        def lastClass = attrs[ATTR_LAST_CLASS] ?: 'weceem-menu-last'
        def levelClassPrefix = attrs[ATTR_LEVEL_CLASS_PREFIX] ?: 'weceem-menu-level'

        def types = attrs[ATTR_TYPES] ?: [org.weceem.html.WcmHTMLContent]
        def custom = attrs[ATTR_CUSTOM]?.toString()?.toBoolean()
        def levels = attrs[ATTR_LEVELS] == null ? 2 : attrs[ATTR_LEVELS]
        if (!(levels instanceof Number)) {
            levels = levels.toString().toInteger()
        }
        
        def bodyToUse = body
        if (!custom) {
            bodyToUse = DEFAULT_MENU_BODY.clone()
            bodyToUse.delegate = delegate
        }
                
        // Which node is considered "active"
        def activeNode = attrs.activeNode ?: request[RenderEngine.REQUEST_ATTRIBUTE_NODE]

        def levelnodes
        def args = [
            status:WcmContentRepositoryService.STATUS_ANY_PUBLISHED,
            type: types.size() == 1 ? types[0] : org.weceem.content.WcmContent,
            params:[sort:'orderIndex']
        ]
            
        if (siblings) {
            if ((currentLevel == 0) && !rootNodeSpecified) {
                if (log.debugEnabled) {
                    log.debug "Locating root nodes: ${args}"
                }
                levelnodes = wcmContentRepositoryService.findAllRootContent(node.space, args)
            } else {
                if (log.debugEnabled) {
                    log.debug "Locating child nodes of ${node}: ${args}"
                }
                levelnodes = wcmContentRepositoryService.findChildren(node, args)
            }
        } else {
            if (log.debugEnabled) {
                log.debug "Locating menu nodes using active node: ${activeNode}"
            }

            levelnodes = [activeNode]
        }
            
        // Filter the nodes if we did a promiscuous query due to multiple types
        if (types.size() > 1) {
            levelnodes = levelnodes.findAll { n -> 
                types.find { cls -> 
                    n.instanceOf(cls) 
                }
            }
        }
        
        def first = true
        def last = false
        def lastIndex = levelnodes.size()-1

        if (log.debugEnabled) {
            log.debug "Rendering menu for level [$currentLevel] nodes: ${levelnodes}"
        }
        
        def o = out
        levelnodes?.eachWithIndex { n, i -> 
            if (!custom && first) {
                o << "<ul>"
            }
            last = i == lastIndex
            
            def bodyargs = [
                first:first,
                active: (n.ident() == activeNode.ident()) || (lineage.find { it.ident() == n.ident() }),
                custom:custom,
				types: types,
                level:currentLevel,
                last: last, 
                firstClass: firstClass,
                lastClass: lastClass,
                activeClass: activeClass,
                levelClassPrefix: levelClassPrefix,
                levels: levels-1,
                link: createLink(node:n),
                menuNode:n
            ]

            def nestedOutput = ''
            if (currentLevel+1 <= levels) {
                request['_weceem_menu_level'] = currentLevel+1
                def attribs = bodyargs.clone()
                attribs.node = n // node attrib needed
                nestedOutput = menu(attribs, bodyToUse).toString()
                request['_weceem_menu_level'] = currentLevel // back to where we were
            }
            bodyargs.nested = nestedOutput
            
            // Render item
            if (custom) {
                o << bodyToUse(bodyargs)
            } else {
                bodyargs.out = out
                bodyToUse(bodyargs)
            }

            first = false
            if (!custom && last) {
                o << "</ul>"
            }
        }
        return null // we don't want any accidental output
    }

    /**
     * @todo This code is not officially announced yet. I don't think it is necessary, it should be possible to do
     * this with the menu tag - if not we need to find out why. Scott, what was the rationale behind this?
     *
     * output a recursive treemenu based either on a node or the root
     * attributes:
     * node - the node to base the menu on
     * levels - the number of levels
     * id - id of the menu
     */
/*    def treeMenu = {attrs ->
        def node = attrs.node
        int levels = attrs.levels ? attrs.levels.toInteger() : 2
        def id = attrs.id

        def args = [
                status: WcmContentRepositoryService.STATUS_ANY_PUBLISHED,
                type: org.weceem.html.WcmHTMLContent,
                params: [sort: 'orderIndex']
        ]

        def tmenu = {items, level = 1 ->
            if (items) {
                out << "<ul ${id && level == 1 ? 'id=${id}' : ''} class='menu menu-level-${level}'>"
                items.each {item ->
                    def children = wcmContentRepositoryService.findChildren(item, args)
                    out << "<li class='menu-item ${children ? 'has-children' : ''}'>"
                    out << link(node: item, {item.titleForMenu.encodeAsHTML()})
                    if (level < levels) {
                        owner.call(children, level + 1)
                    }
                    out << "</li>"
                }
                out << "</ul>"
            }
        }

        if (node) {
            tmenu([node])
        } else {
            tmenu(wcmContentRepositoryService.findAllRootContent(request[RenderEngine.REQUEST_ATTRIBUTE_SPACE], args))
        }
    }
*/

    def link = { attrs, body -> 
        def o = out
        o << "<a href=\"${wcm.createLink(attrs)}\""
        o << attrs.collect {k, v -> " $k=\"$v\"" }.join('')
        o << '>'
        o << body()
        o << "</a>"
    }
    
    def createLink = { attrs, body -> 
        def space = attrs[ATTR_SPACE]
        def path = attrs[ATTR_PATH]
        
        if (space != null) {
            space = WcmSpace.findByAliasURI(space)
            if (!space) {
                throwTagError "Tag invoked with space attribute value [${attrs[ATTR_SPACE]}] but no space could be found with that aliasURI"
            }
        } else {
            space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
            if (!space) {
                throwTagError "Tag [createLink] invoked from outside a Weceem request requires the [${ATTR_SPACE}] attribute to be set to the aliasURI of the space"
            }
        }

        def content = resolveNode(attrs)
        if (!content) {
            log.error ("Tag [wcm:createLink] cannot create a link to the content as "+
                "there is no content node at [${path}]")
            out << g.createLink(controller:'wcmContent', action:'notFound', params:[path:path])
        } else {

            def linkAttrs = attrs.clone()
            linkAttrs.remove(ATTR_PATH)
        
            linkAttrs.params = [uri:WeceemTagLib.makeFullContentURI(content)]
            linkAttrs.controller = 'wcmContent'
            linkAttrs.action = 'show'
            out << g.createLink(linkAttrs)
        }
    }
    
    /**
     * Make a full URI to content including the space URI
     */
    static makeFullContentURI(WcmContent content) {
        // @todo This is quite crappy, we should be getting these urls from a cache
        StringBuffer path = new StringBuffer()
        if (content.space.aliasURI) {
            path << content.space.aliasURI
            path << '/'
        }
        path << content.absoluteURI
        path.toString()
    }
    
    def date = { attrs, body -> 
        out << formatDate(format:attrs.format ?: 'dd MMM yyyy', date: new Date())
    }
    
    def find = { attrs, body -> 
        def var = attrs.remove(ATTR_VAR)
        def c = findNode(attrs)
        if (c) {
            out << body(var ? [(var):c] : c)
        }
    }
    
    def findNode = { attrs -> 
        def params = makeFindParams(attrs)
        def id = attrs[ATTR_ID]
        def title = attrs[ATTR_TITLE]
        def path = attrs[ATTR_PATH]
        def c
        if (id) {
            c = WcmContent.get(id)
        } else if (title) {
            c = WcmContent.findByTitle(title, params)
        } else if (path) {
            c = wcmContentRepositoryService.findContentForPath(path, 
                request[RenderEngine.REQUEST_ATTRIBUTE_SPACE])?.content
        } else throwTagError("One of [id], [title] or [path] must be specified")
        
        return c
    }
    
    /**
     * Get the current or specified node's content AS HTML
     */
    def content = { attrs ->
        def codec = attrs.codec
        def node = attrs.node ?: request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        if (!node) {
            throwTagError "The wcm:content tag requires a node. There is no node associated with this request, and no node attribute specified"
        }
        // See if there is pre-rendered content (eg evaluated GSP content), if so use that
        def text = request[RenderEngine.REQUEST_PRERENDERED_CONTENT]
        if (text == null) {
            text = node.getContentAsHTML()
        }
        out << (codec ? text."encodeAs$codec"() : text)
    }
    
    def createLinkToFile = { attrs ->
        def space = attrs.space ? WcmSpace.findByAliasURI(attrs.space) : request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        if (!space) { throwTagError("Space [${attrs.space}] not found") }
        if (!attrs[ATTR_PATH]) {
            throwTagError("Attribute [${ATTR_PATH}] must be specified, eg the path to the file: images/icon.png")
        }
        def aliasURI = space.aliasURI ?: WcmContentRepositoryService.EMPTY_ALIAS_URI
        
        // Don't specify plugin:'weceem' here!
        out << g.resource(dir:"${wcmContentRepositoryService.uploadUrl}${aliasURI}", file:attrs[ATTR_PATH])
    }

    def humanDate = { attrs ->
        def now = new Date()
        if (attrs.date) {
            use(groovy.time.TimeCategory) {
                def millisDelta = now - attrs.date
                def daysElapsed = millisDelta.days
                if (daysElapsed > 30) {
                    out << message(code:'human.date.on', args:[g.formatDate(date:attrs.date, format:'yyyy/MM/dd'),
                        g.formatDate(date:attrs.date, format:'hh:mm:ss')])
                } else if (daysElapsed > 7) {
                    out << message(code:'human.date.weeks.ago', args:[Math.round(daysElapsed/7)])
                } else if (daysElapsed > 0) {
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
        def u = wcmSecurityService.userName
        if (u) {
            out << u.encodeAsHTML()
        }
    }
    
    def loggedInUserEmail = { attrs ->
        def u = wcmSecurityService.userEmail
        if (u) {
            out << u.encodeAsHTML()
        }
    }
    
    def ifUserCanEdit = { attrs, body ->
        def node = resolveNode(attrs)

        if (wcmSecurityService.isUserAllowedToEditContent(node)) {
            out << body()
        }
    }
    
    def ifUserCanView = { attrs, body ->
        def node = resolveNode(attrs)
        if (node) {
            if (wcmSecurityService.isUserAllowedToViewContent(node)) {
                out << body()
            }
        }
    }
    
    def ifContentIs = { attrs, body ->
        def node = request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        if (!targetType)
            throwTagError("Attribute [${ATTR_TYPE}] is required on tag ifContentIs. It must be a fully qualified class name")

        def targetClass = grailsApplication.getClassForName(targetType)
        if (!targetClass)
            throwTagError("Attribute [${ATTR_TYPE}] specified class [${targetType}] but it could not be located")
        
        if (node.instanceOf(targetClass)) {
            out << body()
        }
    }

    def ifContentIsNot = { attrs ->
        def node = request[RenderEngine.REQUEST_ATTRIBUTE_NODE]
        def targetType = attrs[ATTR_TYPE]
        
        if (!node.instanceOf(grailsApplication.getClassForName(targetType))) {
            out << body()
        }
    }
    
    def renderContentItemIcon = { attrs ->
        def node = resolveNode(attrs)
        def id = attrs[ATTR_ID]
        def iconconf = proxyHandler.unwrapIfProxy(node)?.class.icon
        out << "<div id='${id}' class='ui-content-icon'><img src='${g.resource(plugin:iconconf.plugin, dir: iconconf.dir, file: iconconf.file)}'/></div>"
    }    

    def contentIconURL = { attrs ->
        def type = attrs[ATTR_TYPE]
        def iconconf = type.icon
        def plugin = iconconf.plugin
        return g.resource(plugin:plugin, dir: iconconf.dir, file: iconconf.file)
    }    

    /**
     * Utility function to get a content node from an attribute value that can be any of:
     * 
     * a number type - the id of a content node to get
     * a WcmContent node - will be returned as the value
     * anything that can be coerced to a String - if results in a number, will 
     *    call get on it else calls findContentForPath in the current space
     */
    WcmContent attributeToContent(attribValue) {
        if (attribValue instanceof Number) {
            WcmContent.get(attribValue.toLong())
        } else if (attribValue instanceof WcmContent) {
            attribValue
        } else {
            def s = attribValue.toString()
            if (s.isLong()) {
                return WcmContent.get(s.toLong())
            } else {
                return wcmContentRepositoryService.findContentForPath(s, request[RenderEngine.REQUEST_ATTRIBUTE_SPACE])?.content
            }
        }
    }

    def submitContentLink = { attrs ->
        def parent = attributeToContent(attrs[ATTR_PARENT])
        def type = attrs[ATTR_TYPE]
        def success = attributeToContent(attrs[ATTR_SUCCESS])
        def currentContentPath = request[RenderEngine.REQUEST_ATTRIBUTE_PAGE].URI
        def space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        
        out << g.createLink(mapping:'contentSubmission', action:'submit', params:[
            spaceId:space.id,
            parentId:parent.id,
            type:type,
            successPath:success.absoluteURI,
            formPath:currentContentPath
        ])
    }

    def submitContentForm = { attrs, body ->
        def parent = attributeToContent(attrs[ATTR_PARENT])
        def type = attrs[ATTR_TYPE]
        def success = attributeToContent(attrs[ATTR_SUCCESS])
        def currentContentPath = request[RenderEngine.REQUEST_ATTRIBUTE_PAGE].URI
        def space = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        
        def link = g.createLink(mapping:'contentSubmission', action:'submit')
        def o = out
        o << "<form action=\"${link}\" method=\"POST\">"
        [spaceId:space.id,
            parentId:parent.id,
            type:type,
            successPath:success.absoluteURI,
            formPath:currentContentPath].each { k, v ->
                o << "<input type=\"hidden\" name=\"$k\" value=\"$v\"/>"
        }
        o << body()
        o << "</form>" 
    }
    
    def eachComment = { attrs, body ->
        attrs[ATTR_TYPE] = org.weceem.content.WcmComment
        attrs[ATTR_VAR] = "comment"
        out << wcm.eachChild(attrs, body)
    }
    
    /**
     * Invokes the body for every month/year combination that has content under the parent, of the specified type
     * Results are in reverse year and month order
     */
    def archiveList = { attrs, body ->
        def type = attrs[ATTR_TYPE] ?: org.weceem.blog.WcmBlogEntry
        def node = resolveNode(attrs)
        if (!node) {
            throwTagError( "archiveList tag requires a node")
        }
        def monthsWithContent = wcmContentRepositoryService.findMonthsWithContent(node, type)
        monthsWithContent.each { entry ->
            out << body(month:entry.month, year:entry.year, 
                link:g.createLink(mapping:'archive', params:[uri:WeceemTagLib.makeFullContentURI(node)+"/${entry.year}/${entry.month}"]) )
        }
    }
    
    // @todo Make this client locale aware!
    def monthName = { attrs ->
        def v = attrs[ATTR_VALUE]
        if (!v) {
            throwTagError( "archiveList tag requires [$ATTR_VALUE] attribute")
        }
        out << new DateFormatSymbols().months[v-1]
    }
    
    def createFeedLink = { attrs ->
        def node = resolveNode(attrs)
        def type = attrs[ATTR_TYPE] ?: 'rss'
        out << g.createLink(mapping:'feeds', action:type, params:[uri:WeceemTagLib.makeFullContentURI(node)])
    }

    def feedLink = { attrs ->
        def node = resolveNode(attrs)
        def type = attrs[ATTR_TYPE] ?: 'rss'
        out << feed.meta( kind:type, version:attrs[ATTR_VERSION] ?: '',
            mapping:'feed', 
            action:type, 
            params:[uri:WeceemTagLib.makeFullContentURI(node)] )
    }

    def dataFeed = { attrs, body -> 
        def type = attrs.remove(ATTR_TYPE) ?: 'rss'
        def max = attrs[ATTR_MAX] ? attrs[ATTR_MAX].toInteger() : 5
        
        def custom = attrs[ATTR_CUSTOM] != null ? attrs[ATTR_CUSTOM].toString().toBoolean() : false
        def bodyClosure = custom ? body : null
        if (!custom) { 
            bodyClosure = { args ->
                out << "<li><a href=\"${args.item.link.encodeAsHTML()}\">${args.item.title.encodeAsHTML()}</a></li>"
            }
        }
        
        def gpath
        switch (type) {
            case 'rss': 
                gpath = 'channel.item'
                break
            case 'atom': 
                gpath = 'entry'
                break
            default:
                gpath = attrs.remove(ATTR_GPATH)
                break
        }
        
        def url = attrs[ATTR_URL]
        def _log = log
        def feedData = wcmCacheService.getOrPutValue('dataFeeds', url, {
            if (_log.infoEnabled) {
                _log.info "Retrieving feed ${url}"
            }
            try {
                return new URL(url).getText('utf-8')
            } catch (IOException ioe) {
                log.warn "Unable to retrieve data feed at ${url}: ${ioe}"
                return null
            }
        })
        
        if (feedData == null) {
            out << "Unable to retrieve data feed ${url}"
            return
        }
        
        // @todo cache the actual DOM not the text of the feed
        def feedDOM = new XmlSlurper().parseText(feedData)
        def nodeSet = feedDOM
        gpath.tokenize('.').each { t ->
            nodeSet = nodeSet."$t"
        }
        
        if (!custom) {
            out << '<ul>'
        }
        int limit = Math.min(nodeSet.size(), max)
        for (int i = 0; i < limit; i++) {
            custom ? out << bodyClosure(item:nodeSet[i]) : bodyClosure(item:nodeSet[i])
        }
        if (!custom) {
            out << '</ul>'
        }
    }
    
    def join = { attrs, body ->
        def items = attrs[ATTR_IN]?.collect { item ->
            def vars = attrs[ATTR_VAR] ? [(attrs[ATTR_VAR]):item] : item
            return body(vars).trim()
        } 
        if (items) {
            out << g.join(in:items, delimiter:attrs.delimiter)
        }
    }
    
    def search = { attrs ->
        def spaceAlias = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE].aliasURI
        def resPath = attrs.remove(ATTR_RESULTSPATH)
        def p = resPath ? [resultsPath:spaceAlias+'/'+resPath] : [:]
        // Search the current space only
        p.uri = spaceAlias+'/'
        def base = attrs.remove('baseURI')
        if (base) {
            p.uri += base
        }
        // Copy the rest of attribs over
        p.putAll(attrs)
        
        out << g.form(controller:'wcmSearch', action:'search', params:p) {
            out << wcm.searchField()
            out << g.submitButton(name:'submit', value:'Search')
        }
    }
    
/*
<div class="blog-entry-date"><g:formatDate date="${node.publishFrom}" format="dd MMM yyyy 'at' hh:mm"/></div>
<div class="blog-entry-content">
${node.content}
</div>
<div class="blog-entry-post-info">
	<span class="quiet"><wcm:countChildren node="${node}" type="org.weceem.content.WcmComment"/> Comments</span>
</div>
<div class="blog-entry-post-info">
	<span class="quiet">Tags:
	<wcm:join in="${node.tags}" delimiter=", " var="tag">
		<a href="${wcm.searchLink(mode:'tag', query:tag)}">${tag.encodeAsHTML()}</a>
	</wcm:join>
	</span>
</div>
*/
    def createSearchLink = { attrs ->
        def spaceAlias = request[RenderEngine.REQUEST_ATTRIBUTE_SPACE].aliasURI
        def resPath = attrs.remove(ATTR_RESULTSPATH)
        def p = resPath ? [resultsPath:spaceAlias+'/'+resPath] : [:]
        // Search the current space only
        p.uri = spaceAlias+'/'
        def base = attrs.remove('baseURI')
        if (base) {
            p.uri += base
        }
        // Copy the rest of attribs over
        p.putAll(attrs)

        log.debug "Params are: $p"
        out << g.createLink(mapping:'search', params:p) 
    }

    def searchLink = { attrs, body ->
        out << g.link(url:wcm.createSearchLink(attrs), body)
    }
    
    def searchField = { attrs ->
        out << g.textField(name:'query', 'class':'searchField')
    }
    
    def paginateSearch = { attrs ->
        def t = pageScope.searchResults?.total
        out << g.paginate(controller:'wcmSearch', action:'search', total:t ?: 0)
    }
    
    def summarize = { attrs, body ->
        int maxLen = (attrs[ATTR_LENGTH] ?: 100).toInteger()
        def codec = attrs.encodeAs
        def ellipsis = attrs.ellipsis ?: '...'
        def s = ContentUtils.summarize(body().toString(), maxLen, ellipsis)
        out << (codec ? s."encodeAs$codec"() : s    )
    }
    
    /**
     * Remove markup from HTML but leave escaped entities, so result can
     * be output with encodeAsHTML() or not as the case may be
     */
    def htmlToText = { attrs, body ->
        out << ContentUtils.htmlToText(body())
    }
    
    /**
     * Return configuration values (from Config.groovy / external properties file)
     */
    def config = { attrs ->
        def codec = attrs.encodeAs
        def s = grailsApplication.config.flatten()[attrs.property]
        out << (codec ? s."encodeAs$codec"() : s)
    }
    
    def uiMessage = { attrs, body ->
        def s = pageScope.variables[WcmContentController.UI_MESSAGE] ?: flash[WcmContentController.UI_MESSAGE]
        if (s) {
            out << body(message:s)
        }
    }
    
    def ancestorOfType = { attrs ->
        def node = resolveNode(attrs)
        def type = attrs[ATTR_TYPE]
        if (!type) {
            throwTagError("The [$ATTR_TYPE] attribute is required")
        }
        def typeClass = wcmContentRepositoryService.getContentClassForType(type)
        
        while (node && !node.instanceOf(typeClass)) {
            node = node.parent
        }
        
        return node
    }
    
    def resource = { attrs -> 
        def node = resolveNode(attrs)
        if (!attrs[ATTR_TYPE]) { 
            switch (proxyHandler.unwrapIfProxy(node).class) {
                case WcmStyleSheet: 
                    attrs[ATTR_TYPE] = "css"
                    break;
                case WcmJavaScript: 
                    attrs[ATTR_TYPE] = 'js'
                    break;
            }
        }
        
        if (!attrs[ATTR_TYPE]) {
            throwTagError("The [$ATTR_TYPE] attribute is required to indicate what kind of resource you are linking to")
        }

        attrs.url = wcm.createLink(path:node)
        out << jqui.resourceLink(attrs)
    }
    
    def getClassName = { attrs ->
        return proxyHandler.unwrapIfProxy(attrs.node)?.class.name
    }
}
