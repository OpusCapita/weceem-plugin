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
package org.weceem.tags

import org.weceem.content.*
import grails.util.GrailsUtil
import org.weceem.security.AccessDeniedException

import org.weceem.controllers.WcmContentController
import org.weceem.content.RenderEngine

/**
 * Tags for apps that embed local weceem content
 */
class EmbedTagLib {

    static namespace = "wcm"
    
    def wcmRenderEngine
    def wcmContentRepositoryService
    
    def render = { attrs ->
        def path = attrs.path
        if (!path) {
            throwTagError "No [path] attribute supplied to wcm:render tag. Specify the Weceem content URI that you wish to render"
        }
        def uriInfo = wcmContentRepositoryService.resolveSpaceAndURI(path)
        // @todo enhance the logic to work same as WcmContentController
        def node = wcmContentRepositoryService.findContentForPath(uriInfo.uri, uriInfo.space)
        if (node) {
            // @todo Should verify it is embeddable content type here i.e. images/downloads can't embed!
            out << g.include(controller:'wcmContent', action:'show', params:[uri:path])
        } else {
            out << "Content not found at [${path}]"
        }
    }
}


