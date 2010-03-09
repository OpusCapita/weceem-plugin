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

/**
 * WcmRelatedContent class.
 * When creating new content, parse the content, delete the old links
 * and save the new links into WcmRelatedContent.
 *
 * @author Stephan Albers
 * @author July Karpey
 */
class WcmRelatedContent {
    WcmContent sourceContent

    // to Title (for internal links) or to URL (for external links)
    WcmContent targetContent

    Boolean isInternal

    //RelationType relationType
    String  relationType

    // extLinkLastChecked
    Date lastCheckedOn

    // status: links works..
    String status

    static mapping = {
        cache usage: 'nonstrict-read-write'
    }

    def WcmRelatedContent() {}

    def WcmRelatedContent(def contentNode, def contentLink, def isInternal,
                       def relationType) {
        this.sourceContent = contentNode
        this.targetContent = contentLink
        this.isInternal = isInternal
        this.relationType = relationType

        this.lastCheckedOn = new Date()

        // TODO: add special class for link statuses or constant values
        this.status = "worked"
    }

    String toString () {
        return "RelatedContent ${id}"
    }
}
