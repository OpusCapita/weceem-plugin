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
 * WcmComment class encapsulates comments on any content node, where the submitting person
 * may not be a user of the system - eg comments need to be spam checked and IP address tracked
 *
 * @author Marc Palmer
 */
class WcmComment extends WcmContent {
    String author
    String email
    String ipAddress
    String websiteUrl
    String content
    
    static searchable = {
        alias WcmComment.name.replaceAll("\\.", '_')

        only = ['content', 'email', 'author', 'title', 'status']
    }
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "comment-32.png"]
    
    static standaloneContent = false
    
    static publicSubmitProperties = [
        'title',
        'author',
        'email',
        'websiteUrl',
        'content'
    ]
    
    static constraints = {
        author(maxSize:80, blank: false, nullable: false)
        email(email:true, maxSize:80, blank: false, nullable: false)
        content(maxSize:WcmContent.MAX_CONTENT_SIZE, blank: false, nullable: false)
        status(nullable: false)
        websiteUrl(url:true, maxSize:100, nullable: true, blank: true)
        ipAddress(maxSize:50, nullable: false, blank: false)
    }
    
    // Injected
    def wcmContentRepositoryService
    
    static transients = WcmContent.transients + ['wcmContentRepositoryService']

    static editors = {
        content editor: 'RichHTML'
        author()
        email()
        websiteUrl()
        ipAddress editor: 'ReadOnly'
        aliasURI hidden:true
    }
    
    @Override
    public void createAliasURI(parent) {
        // Create an aliasURI that is sequential and unique under the parent, using the highest orderIndex
        WcmContent.withNewSession {
            def kidList = wcmContentRepositoryService.findChildren(parent, [
                 type:'org.weceem.content.WcmComment',
                 params:[sort:'aliasURI']
            ])
            def highestCommentNumber = kidList.inject(0) { number, comment -> 
                Math.max(number, (comment.aliasURI - "comment-").toInteger())
            }
            aliasURI = "comment-"+(highestCommentNumber+1)
        }    
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
}