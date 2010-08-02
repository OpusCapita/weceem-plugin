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
 * BlogContentNode class describes the content node of type 'blog'.
 * The teaser of the blog is an equivalent to summary of content node.
 *
 * @author Stephan Albers
 * @author July Karpey
 */
class WcmBlogEntry extends WcmContent {

    static searchable = {
        alias WcmBlogEntry.name.replaceAll("\\.", '_')
        
        only = ['content', 'keywords', 'summary', 'title']
    }
    
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
        summary(maxSize:500)
        content(maxSize:100000)
        status(nullable: false)
    }
    
    static editors = {
        summary(editor:'LongString')
        content(editor:'RichHTML')
        keywords(group:'meta')
    }

    static transients = WcmContent.transients + ['summary']

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