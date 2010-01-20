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
    static String REQUEST_PRERENDERED_CONTENT = "weceem.prerendered.content"
    
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

                    def contentClass = content.class

                    // See if it is renderable directly - eg Widget and Template are not renderable on their own
                    if (contentClass.metaClass.hasProperty(contentClass, 'standaloneContent')) {
                        def canRender = contentClass.standaloneContent
                        if (!canRender) {
                            log.warn "Request for [${params.uri}] resulted in content node that is not standalone and cannot be rendered directly"
                            response.sendError(406 /* Not acceptable */, "Content is not intended for rendering")
                            return null
                        }
                    }
                    
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
    
    static evaluateGSPContent(contentRepositoryService, Content content, model) {
        def groovyTemplate = contentRepositoryService.getGSPTemplate(content)
        return groovyTemplate?.make(model)
    }
    
    
    void renderGSPContent(Content content, model = null) {
        ContentController.renderGSPContent( contentRepositoryService, request, response, content, model)
    }
    
    /**
     * Render a content node with support for GSP tags and template
     *
     * Works in one of two ways:
     * 1. If content node is a template, evaluates the template and passes in model, presumed to contain "node" for for content
     * 2. If content node is not a template, evaluats the content as a GSP, then passes it as pre-rendered body content
     * to the template of "content" if there is one.
     */
    static renderGSPContent(contentRepositoryService, request, response, Content content, model = null) {
        if (model == null) {
            model = [:]
        }
        model.user = request[REQUEST_ATTRIBUTE_USER]
        model.page = request[REQUEST_ATTRIBUTE_PAGE]
        model.space = request[REQUEST_ATTRIBUTE_SPACE]
        // Prepare the existing output stream
        Writer out = GSPResponseWriter.getInstance(response, 65536)
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.setOut(out)

        boolean isTemplate = content instanceof Template
        
        if (isTemplate) {
            // Pass in the content so it can be rendered in the template by wcm:content
            request[REQUEST_ATTRIBUTE_NODE] = model.node
            // Set mime type to the one for this template if there is not one set by content already
            if (content.mimeType) {
                response.setContentType(content.mimeType)
            }
        } else {
            request[REQUEST_PRERENDERED_CONTENT] = evaluateGSPContent(contentRepositoryService, content, model)
            request[REQUEST_ATTRIBUTE_NODE] = content
            model.node = content
        }
        

        // See if there is a template for the content
        def template = isTemplate ? content : contentRepositoryService.getTemplateForContent(content)
        if (template) {
            def templatedContent = evaluateGSPContent(contentRepositoryService, template, model)
            templatedContent.writeTo(out)
        } else {
            out << request[REQUEST_PRERENDERED_CONTENT]
        }

        // flush the existing output stream
        out.flush()
        webRequest.renderView = false
    }

    /** 
     * Render the content using our convention based approach
     * If the content has a template, it is passed to the template for rendering as the "node" variable in the model
     * If the content has no template, if it has a content property it will be rendered verbatim to the client
     */     
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
                return null
            }
            return
        }
        
        // Render the template, this call will handle the content too by passing it in as model to template
        return renderGSPContent(template, [node: content])
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
