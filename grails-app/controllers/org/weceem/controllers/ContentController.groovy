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
package org.weceem.controllers


import org.codehaus.groovy.grails.web.pages.GSPResponseWriter
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder

import grails.util.GrailsUtil

import org.weceem.content.*
import org.weceem.security.WeceemSecurityPolicy
import org.weceem.security.AccessDeniedException

class ContentController {
    static String REQUEST_ATTRIBUTE_PAGE = "weceem.page"
    static String REQUEST_ATTRIBUTE_USER = "weceem.user"
    static String REQUEST_ATTRIBUTE_NODE = "weceem.node"
    static String REQUEST_ATTRIBUTE_SPACE = "weceem.space"
    static CACHE_NAME_TEMPLATE_CACHE = "gspCache"
    
    def contentRepositoryService
    def weceemSecurityService
    def cacheService
    
    def show = {
        try {
            if (log.debugEnabled) {
                log.debug "Content request for uri: ${params.uri}"
            }

            def info = contentRepositoryService.resolveSpaceAndURI(params.uri)
            def space = info.space
            def uri = info.uri

            if (log.debugEnabled) {
                log.debug "Loading content from space: ${space?.name}"
            }

            if (space) {
                if (log.debugEnabled) {
                    log.debug "Loading content from for uri: ${uri}"
                }
                def contentInfo = contentRepositoryService.findContentForPath(uri,space)
                def content = contentInfo?.content
            
                // Resolve virtual content refs
                if (log.debugEnabled) {
                    log.debug "Content for uri: ${uri} is: ${content?.dump()}"
                    if (content?.metaClass?.hasProperty(content, 'target')) {
                        log.debug "Content for uri: ${uri} has a target value of [${content.target}]"
                    }
                }
                if (content) {
                    if (content instanceof VirtualContent) {
                        content = content.target
                    }
                }

                if (log.debugEnabled) {
                    log.debug "Content after resolving virtual content for uri: ${uri} is: ${content?.dump()}"
                }
            
                def activeUser = weceemSecurityService.userName
            
                if (content) {
        			def pageInfo = [ URI:uri, 
        			    parentURI:contentInfo.parentURI, 
        			    lineage: contentInfo.lineage, 
        			    title: content.title,
        			    titleForHTML: content.titleForHTML,
        			    titleForMenu: content.titleForMenu
        			]

                    // Make this available to the rest of the request chain
                    request[REQUEST_ATTRIBUTE_NODE] = content
                    request[REQUEST_ATTRIBUTE_USER] = activeUser
                    request[REQUEST_ATTRIBUTE_PAGE] = pageInfo
                    request[REQUEST_ATTRIBUTE_SPACE] = space

                    // Set mime type if there is one
                    if (content.mimeType) {
                        response.setContentType(content.mimeType)
                    }

                    // See if the content will handle rendering itself
                    def contentClass = content.class
                    if (contentClass.metaClass.hasProperty(contentClass, 'handleRequest')) {
                        if (log.debugEnabled) {
                            log.debug "Content of type ${contentClass} at uri ${params.uri} is handling its own rendering"
                            
                            assert contentClass.handleRequest instanceof Closure
                        }
                        
                        def handler = contentClass.handleRequest.clone()
                        handler.delegate = this
                        handler.resolveStrategy = Closure.DELEGATE_FIRST
                        return handler.call(content)
                    }

                    // Fall back to standard rendering
                    return renderContent(content)
                
                } else {
                    response.sendError 404, "No content found for this URI"
                    return null
                }
            } else {
                response.sendError 404, "No space specified"
                return null
            }        
        } catch (AccessDeniedException ade) {
            request.accessDeniedMessage = ade.message
            response.sendError 403, ade.message
            return null
        }
    }
    
    def renderContent(Content content) {
        
        def pageInfo = request[REQUEST_ATTRIBUTE_PAGE]
        def contentText
        if (content.metaClass.hasProperty(content, 'content')) {
            contentText = content.content
            pageInfo.text = contentText
    
            log.debug "Content is: $contentText"
        }
    
        def template = contentRepositoryService.getTemplateForContent(content)
        log.debug "Content's template is: $template"

        if (!template) {
            if (contentText != null) {
                // todo: what need to be rendered?
                log.debug "Rendering content of type [${content.mimeType}] without template: $contentText"
                // @todo This needs to handle ContentFile/ContentDirectory requests and pipe them through request dispatcher
                render(text:contentText, contentType:content.mimeType)
            } else {
                response.sendError(500, "Unable to render content at ${uri}, no content property and no template defined")
            }
            return
        }

        Writer out = GSPResponseWriter.getInstance(response, 65536)
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.setOut(out)
        def groovyTemplate = contentRepositoryService.getGSPTemplate(template.absoluteURI, template.content)

        // Pass in the content so it can be rendered in the template
        def preparedContent = groovyTemplate?.make([
            user: request[REQUEST_ATTRIBUTE_USER], 
            node: content, 
            page: pageInfo, 
            space: request[REQUEST_ATTRIBUTE_SPACE]])
        if (preparedContent)  {
           preparedContent.writeTo(out)
        }

        out.flush()
        webRequest.renderView = false
        return null        
    }
    
    def renderFile(File f) {
        throw new RuntimeException("Not implemented yet")
    }
    
    /**
     * Use the servlet container to return the file - more optimal
     */
    def renderAppResource(String path) {
        request.getRequestDispatcher(path).forward(request, response)
        return null
    }
    
    def notFound = {
        def msg = "No content was found for path: ${params.path}"
        response.sendError(404, msg)
    }
    
    
}
