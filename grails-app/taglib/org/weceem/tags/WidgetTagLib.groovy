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

import org.weceem.content.*
import grails.util.GrailsUtil
import org.weceem.security.AccessDeniedException

import org.weceem.controllers.WcmContentController
import org.weceem.content.RenderEngine

/**
 * WcmWidget tag library describes the widget tag.
 * Tag renders a DIV widget with special settings and behaviour.
 * If the session mode is editable (session.mode == 'edit') its possible to
 * edit widgets by clicking on it.
 *
 * TODO: it seems to be unconvenientif because the user can press a link in the widget
 * (e.g. he wants to see an article or blog page) - the widget editor will be opened too.  
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class WidgetTagLib {

    static namespace = "wcm"
    
    def wcmContentRepositoryService
    
    def widget = {attrs, body ->
        def widget
        def path = attrs.path
        def space = attrs.space ? WcmSpace.findByAliasURI(attrs.space) : request[RenderEngine.REQUEST_ATTRIBUTE_SPACE]
        if(!space) {throwTagError("No space by name ${attrs.space} or in page scope")}

        if (path) {
            widget = wcmContentRepositoryService.findContentForPath(path, space)?.content
            if (!widget) {
                throwTagError("There is no Widget at aliasURI [${path}] in the space [${space.name}]")
            }
        } else if (attrs.id) {
            log.warn("Use of [id] attribute on widget tag is deprecated")
            widget = WcmWidget.findBySpaceAndTitle(space, attrs.id)
            if (!widget) {
                throwTagError("There is no Widget with title [${attrs.id}] in the space [${space.name}]. Tip: use path attribute!")
            }
        }
        if (log.debugEnabled) {
            log.debug "Widget tag resolved to widget [${widget?.dump()}"
        }

        def id = attrs.id ?: widget.id
        /*
        if (session.mode == 'edit') {
            out << "<div id=\"${id}\" onclick=\"window.open('${createLink(controller: 'widget', action: 'edit', id: widget.id, params: ['externalCall': true])}', 'Edit WcmWidget', 'resizable=yes, scrollbars=yes, status=no'\">"
        } else {
            out << "<div id=\"${id}\">"
        }*/

        out << body()

        try {
            def groovyTemplate = wcmContentRepositoryService.getGSPTemplate(widget)
            if (attrs.model instanceof Map) {
                def model = [:] 
                model.putAll( pageScope.variables)
                model.putAll( attrs.model )
                if (log.debugEnabled) {
                    log.debug "Widget executing with model: ${model}"
                }
                groovyTemplate.make(model).writeTo(out)
            } else {
                if (log.debugEnabled) {
                    log.debug "WcmWidget executing with pageScope variables"
                }
                groovyTemplate.make(pageScope.variables).writeTo(out)
            }
        } catch (AccessDeniedException ade) {
            log.error "Security errors prevented widget from rendering", GrailsUtil.deepSanitize(ade)
            out << "Security errors prevented widget ${widget.absoluteURI} from rendering"
        } catch (Throwable t) {
            log.error "Error executing widget page", GrailsUtil.deepSanitize(t)
            out << "An error occurred in widget ${widget.absoluteURI}: ${t}"
        }

        //out << "</div>"
    }
    
}
