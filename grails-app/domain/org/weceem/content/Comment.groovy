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

import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * Comment class encapsulates comments on any content node, where the submitting person 
 * may not be a user of the system - eg comments need to be spam checked and IP address tracked
 *
 * @author Marc Palmer
 */
class Comment extends Content {
    String author
    String email
    String ipAddress
    String websiteUrl
    String content
    
    static standaloneContent = false
    
    static constraints = {
        author(maxSize:80, blank: false, nullable: false)
        email(email:true, maxSize:80, blank: false, nullable: false)
        content(maxSize:4000, blank: false, nullable: false)
        status(nullable: false)
        websiteUrl(url:true, maxSize:100, nullable: true, blank: false)
        ipAddress(maxSize:50, nullable: false, blank: false)
    }
    
    static transients = Content.transients
    
    @Override
    public void createAliasURI(parent) {
        Content.withNewSession {
            def kidList = ApplicationHolder.application.mainContext.contentRepositoryService.findChildren(parent, [
                 type:'org.weceem.content.Comment', 
                 params:[max:1, sort:'orderIndex', order:'desc']
            ])
            def lastIdx = kidList ? kidList[0].orderIndex : 0
            aliasURI = "comment-"+(lastIdx+1)
        }    
    }
    
    String getVersioningContent() { content }
}