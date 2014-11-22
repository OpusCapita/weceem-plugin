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

package org.weceem.content

import org.weceem.util.ContentUtils

/**
 * WcmWidget class presents a piece of information on the screen with special
 * settings. WcmWidget also has a template node with the path to the widget
 * template (GSP page).
 *
 * TODO: add properties fields for settings, like:
 *   Setting setting (a widget can define it's variables);
 *   Milliseconds cacheFor (all widgets having should be cached with their parameters);
 *   configurationParameter (show 5 latest news);
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class WcmWidget extends WcmContent {

    static standaloneContent = false

    static searchable = {
        only = ['content', 'title', 'status', 'space', 'aliasURI', 'parent']
    }

    Integer width
    Integer height
    Integer color

    // 64Kb Unicode text with HTML/Wiki Markup
    String content
    
    String contentDependencies

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { content }

    /**
     * Should be overriden by content types that can represent their content as HTML.
     * Used for wcm:content tag (content rendering)
     */
    public String getContentAsHTML() { content }
        
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "widget-32.png"]

    static mapping = {
        cache usage: 'nonstrict-read-write' 
    }
    
    static editors = {
        content(editor:'HtmlCode')
        width()
        height()
        color()
    }

    static transients = WcmContent.transients
    
    static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE)
        width(nullable: true)
        height(nullable: true)
        color(nullable: true)
    }
}
