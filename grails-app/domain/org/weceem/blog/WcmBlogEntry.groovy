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

package org.weceem.blog

import org.weceem.content.*
import org.weceem.util.ContentUtils

/**
 * A blog entry
 *
 * @author Marc Palmer
 */
class WcmBlogEntry extends WcmContent {

    static searchable = {
        alias WcmBlogEntry.name.replaceAll("\\.", '_')
        
        only = ['content', 'keywords', 'summary', 'title']
    }
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "blog-entry-32.png"]

    String keywords
    String summary

    String content

    static mapping = {
        cache usage: 'nonstrict-read-write'
        columns {
            content type:'text'
        }
    }
    
    static constraints = {
        summary(nullable: true, blank: true, maxSize:500)
        content(maxSize:WcmContent.MAX_CONTENT_SIZE)
        status(nullable: false)
    }
    
    static editors = {
        summary(editor:'LongString')
        content(editor:'RichHTML')
        keywords(group:'meta')
    }

    static transients = WcmContent.transients

    String getMimeType() { "text/html" }

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
    
    Map getVersioningProperties() { 
       def r = super.getVersioningProperties() + [ 
           keywords:keywords,
           summary:summary
       ] 
       return r
    }
}