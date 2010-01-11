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
package org.weceem.jobs

import org.apache.commons.lang.time.DateUtils

import org.weceem.content.*

/**
 * ExternalLinksJob class is a quartz job for checking external links.
 * This Job is executed every 6 hours: selects all external links and tries to
 * connect to them. According to connetn results (response codes?) the status
 * of the external content can be changed to 'unknownHost' or 'worked'.
 *
 * TODO: perhaps we need to create a special class for links statuses or use
 * some predefined constants.
 *
 * @author Stephan Albers
 * @author July Karpey
 *
 */
class ExternalLinksJob {
    // check external links every six hours
    // timeout value in milliseconds
    def timeout = DateUtils.MILLIS_PER_HOUR*6

    def execute(){

        RelatedContent.findAllWhere("isInternal": false).each() {
            // open connection to external link
            def url = new URL(it.toContent)
            def connection = url.openConnection()

            try {
                connection.connect()
                def code = connection.responseCode
                // ?? code value can be stored in status field
                it.status = "worked"
            } catch (UnknownHostException) {
                it.status = "unknownHost"
            }
            it.lastCheckedOn = new Date()
            it.save()
        }
    }
}