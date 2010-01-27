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

/**
 * BlogContentNode class describes the content node of type 'blog'.
 * The teaser of the blog is an equivalent to summary of content node.
 *
 * @author Stephan Albers
 * @author July Karpey
 */
class BlogEntry extends Content {

    static searchable = {
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
        summary()
        content(editor:'RichHTML')
        keywords()
    }

    static transients = Content.transients + ['summary']

    String getMimeType() { "text/html" }

    String getVersioningContent() { content }

    Map getVersioningProperties() { 
       def r = super.getVersioningProperties() + [ 
           keywords:keywords,
           summary:summary
       ] 
       return r
    }
}