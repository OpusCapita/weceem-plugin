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
 * WcmTemplate class describes the content node of type 'template'.
 * WcmTemplate node unlike other types of nodes has a page (template) that is stored as file,
 * 'templatePath' field contains a path to this file.
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class WcmTemplate extends WcmContent {
    
    static standaloneContent = false

    String getMimeType() { "text/html" }
    
    // 64Kb Unicode text with HTML/GSP Markup
    String content

    static searchable = {
        alias WcmTemplate.name.replaceAll("\\.", '_')

        only = ['content', 'title', 'status']
    }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { ContentUtils.htmlToText(content) }

    /**
     * Should be overriden by content types that can represent their content as HTML.
     * Used for wcm:content tag (content rendering)
     */
    public String getContentAsHTML() { content }
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "template-32.png"]

    static constraints = {
        content(nullable: false, maxSize: 65536)
    }
    
    static editors = {
        content(editor:'HtmlCode')
    }

    static transients = (WcmContent.transients - 'mimeType')

    static mapping = {
        cache usage: 'nonstrict-read-write'
        columns {
            content type:'text'
        }
    }
}
