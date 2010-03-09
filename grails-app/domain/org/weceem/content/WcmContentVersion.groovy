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
 * Class for storing versioning information and versioned object data.
 *
 * @author Sergei Shushkevich
 */
class WcmContentVersion {

    Integer revision
    Long objectKey
    String objectClassName
    String objectContent
    String contentTitle
    String spaceName
    String createdBy
    Date createdOn

    def updateRevisions() {
        WcmContentVersion.executeUpdate(
                "update WcmContentVersion cv set cv.contentTitle = ?, cv.spaceName = ? where cv.objectKey = ?",
                [contentTitle, spaceName, objectKey])
    }

    static constraints = {
        revision(nullable: true)
        createdBy(nullable: true)
        createdOn(nullable: true)
    }

    static mapping = {
        objectContent type: 'text'
    }
}