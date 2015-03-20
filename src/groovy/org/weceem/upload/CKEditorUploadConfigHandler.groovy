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
 
package org.weceem.upload

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 *
 * @author Stephan Albers
 * @author July Antonicheva
 */
class CKEditorUploadConfigHandler implements UploadConfigHandler {
    private static final Log log = LogFactory.getLog(CKEditorUploadConfigHandler.class)
    def grailsApplication

    def handleUploadConfig(File uploadDir, String uploadUrl) {
        def config = grailsApplication.config
        ConfigObject configObject = new ConfigObject()
        if (uploadDir) {
            configObject.ckeditor.upload.basedir = uploadDir.toString()
        }
        if (uploadUrl){
            configObject.ckeditor.upload.baseurl = uploadUrl.toString()
        }
        log.debug("Merging upload configuration to CKEditor config: basedir = ${uploadDir}, baseurl = ${uploadUrl}")
        config.merge(configObject)
    }

}