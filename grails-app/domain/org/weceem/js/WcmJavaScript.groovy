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

package org.weceem.js

import org.weceem.content.*

/**
 * WcmJavaScript representins JS source code in the content repository.
 *
 * @author Marc Palmer
 */
class WcmJavaScript extends WcmContent {

    String content

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "js-file-32.png"]

    static searchable = {
        only = ['content', 'title', 'status', 'space', 'aliasURI', 'parent']
    }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { content }
    
    String getMimeType() { "text/javascript" }

    static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE) // 500k
        status(nullable: false) // Workaround for Grails 1.1.1 constraint inheritance bug
    }

    static mapping = {
        columns {
            content type:'text'
        }
    }

    static editors = {
        content(editor:'JSCode')
    }

    static transients = WcmContent.transients

}