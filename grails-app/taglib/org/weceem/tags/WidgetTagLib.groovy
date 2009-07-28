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

/**
 * Widget tag library describes the widget tag.
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
    
    def widget = {attrs, body ->

        def widget = Widget.findBySpaceAndTitle(pageScope.space, attrs.id)
        if (!widget) {
            throwTagError("There is no Widget with title [${attrs.id}] in the space [${pageScope.space.name}]")
        }
        if (session.mode == 'edit') {
            out << "<div id=\"${attrs.id}\" onclick=\"window.open('${createLink(controller: 'widget', action: 'edit', id: widget.id, params: ['externalCall': true])}', 'Edit Widget', 'resizable=yes, scrollbars=yes, status=no'\">"
        } else {
            out << "<div id=\"${attrs.id}\">"
        }
        out << body()

        def engine = grailsAttributes.getPagesTemplateEngine()
        def groovyTemplate = engine.createTemplate(widget.content, widget.title)
        if (attrs.model instanceof Map) {
            groovyTemplate.make(attrs.model).writeTo(out)
        } else {
            groovyTemplate.make().writeTo(out)
        }

        out << "</div>"
    }
    
}
