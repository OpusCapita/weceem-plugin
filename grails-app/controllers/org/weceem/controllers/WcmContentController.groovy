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

import org.weceem.content.*
import org.weceem.script.WcmScript

import org.weceem.security.AccessDeniedException
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.pages.GSPResponseWriter
import org.codehaus.groovy.grails.commons.ApplicationHolder

class WcmContentController {
    static String REQUEST_ATTRIBUTE_PAGE = "weceem.page"
    static String REQUEST_ATTRIBUTE_USER = "weceem.user"
    static String REQUEST_ATTRIBUTE_NODE = "weceem.node"
    static String REQUEST_ATTRIBUTE_SPACE = "weceem.space"
    static String REQUEST_ATTRIBUTE_CONTENTINFO = "weceem.content.info"
    static String REQUEST_ATTRIBUTE_PREPARED_MODEL = "weceem.prepared.model"
    static String REQUEST_ATTRIBUTE_PREVIEWNODE = "weceem.preview.node"
    static String REQUEST_PRERENDERED_CONTENT = "weceem.prerendered.content"
    static String UI_MESSAGE = 'weceem.message'
    
    static CACHE_NAME_TEMPLATE_CACHE = "gspCache"
    
    def wcmContentRepositoryService
    def wcmSecurityService
    def wcmCacheService
    
    
    /** 
     * Do the full default render pipeline on the supplied content instance.
     * NOTE: Only this exact instance will be rendered, so it must be pre-resolved if it is a WcmVirtualContent node
     * 
     * @todo Should we move this to ContentRepo service? Still requires a delegate that has all controller-style methods
     * but could be reusable in a non-request context e.g. jobs that produce PDFs or sending HTML emails from CMS
     */
    static renderPipeline = { content ->
        // Make this available to the rest of the request chain
        request[WcmContentController.REQUEST_ATTRIBUTE_NODE] = content 

        // This may have been supplied from content resolution if not we have to fake it
        def contentInfo = request[WcmContentController.REQUEST_ATTRIBUTE_CONTENTINFO]
        def req = request
        if (!contentInfo) {
            contentInfo = [:]
            contentInfo.with {
                parentURI = content.parent ? content.parent.absoluteURI : ''
                lineage = content.lineage
                content = req[WcmContentController.REQUEST_ATTRIBUTE_NODE]
            }
        }
        
		def pageInfo = WcmContentController.makePageInfo(content.absoluteURI, contentInfo, content)
        request[REQUEST_ATTRIBUTE_PAGE] = pageInfo

        def contentClass = content.class
        
        if (!wcmContentRepositoryService.contentIsRenderable(content)) {
            log.warn "Request for [${params.uri}] resulted in content node that is not standalone and cannot be rendered directly"
            response.sendError(406 /* Not acceptable */, "This content is not intended for rendering")
            return null
        }

        // Set mime type if there is one
        if (content.mimeType) {
            response.contentType = content.mimeType
        }

        // See if the content will handle rendering itself
        if (contentClass.metaClass.hasProperty(contentClass, 'handleRequest')) {
            if (log.debugEnabled) {
                log.debug "Content of type ${contentClass} at uri ${params.uri} is handling its own rendering"
                
                assert contentClass.handleRequest instanceof Closure
            }
            
            def handler = contentClass.handleRequest.clone()
            handler.delegate = delegate // The controller
            handler.resolveStrategy = Closure.DELEGATE_FIRST
            log.debug "Calling handler with delegate: ${handler.delegate}"
            try {
                return handler.call(content)
            } catch (Throwable t) {
                // Make sure error page is served as HTML 
                response.contentType = "text/html"
                throw t
            }
        } else {

            // Fall back to standard rendering
            return renderContent(content)
        }
    }
    
    static showContent(controllerDelegate, content) {
        // Clone the rendering closure and pass it the content
        Closure c = renderPipeline.clone()
        c.delegate = controllerDelegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c(content)
    }
    
    def preview = {
        if (!request[REQUEST_ATTRIBUTE_PREVIEWNODE]) {
            response.sendError(500, "No preview node set")
        } else {
            cache false // never allow browser to cache this
            WcmContentController.showContent(this, request[REQUEST_ATTRIBUTE_PREVIEWNODE])
        }
    }
    
    def show = { 
        try {
            if (log.debugEnabled) {
                log.debug "Content request for uri: ${params.uri}"
            }

            def info = wcmContentRepositoryService.resolveSpaceAndURI(params.uri)
            def space = info.space
            def uri = info.uri

            if (log.debugEnabled) {
                log.debug "Loading content from space: ${space?.name}"
            }

            if (space) {
                request[REQUEST_ATTRIBUTE_SPACE] = space

                if (log.debugEnabled) {
                    log.debug "Loading content from for uri: ${uri}"
                }
                def contentInfo = wcmContentRepositoryService.findContentForPath(uri,space)
                if (contentInfo) {
                    if (log.debugEnabled) {
                        log.debug "Checking user is allowed to view content at $uri"
                    }
                    if (!wcmSecurityService.isUserAllowedToViewContent(contentInfo.content)) {
                        throw new AccessDeniedException("You cannot view this content")
                    }
                }
                
                request[REQUEST_ATTRIBUTE_CONTENTINFO] = contentInfo
                
                // Resolve any virtual nodes
                def content = resolveActualContent(contentInfo?.content)
            
                if (log.debugEnabled) {
                    log.debug "Content after resolving virtual content for uri: ${uri} is: ${content?.dump()}"
                }
            
                withCacheHeaders { 
                    etag {
                        def et = content.version.encodeAsSHA1();
                        println "Etag is: "+et
                        return et
                    }
                    
                    lastModified {
                        println "In ETag lastmod closure"
                        (content.changedOn ?: content.createdOn)
                    }
                    
                    generate {
                        println "In ETag generate closure"
                        def activeUser = wcmSecurityService.userName
                        request[REQUEST_ATTRIBUTE_USER] = activeUser
            
                        if (content) {
                            response.setDateHeader('Last-Modified', (content.changedOn ?: content.createdOn).time )
                    
                            println "In ETag closure, with content"
                            cache validFor: 1, shared:true  // 1 second caching, just makes sure some OK headers are sent for proxies
                            WcmContentController.showContent(this, content)
                        } else {
                            println "In ETag closure, no content"
                            response.sendError 404, "No content found for this URI"
                            return null
                        }
                    }
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
    
    /**
     * Take the requested content and resolve it to the actual target content, which may differ
     * if the requested content is a WcmVirtualContent node
     */
    WcmContent resolveActualContent(WcmContent requestedContent) {
        // Resolve virtual content refs to act as if they are served directly by this URI
        if (log.debugEnabled) {
            log.debug "Content is: ${requestedContent?.dump()}"
            if (requestedContent?.metaClass?.hasProperty(requestedContent, 'target')) {
                log.debug "Content has a target value of [${requestedContent.target}]"
            }
        }
        if (requestedContent) {
            if (requestedContent instanceof WcmVirtualContent) {
                return requestedContent.target
            }
        }
        return requestedContent
    }

    /**
     * Construct the page info object for custom-rendering situations where you have not yet resolved the node
     */
    static PageInfo makePageInfo(uri, contentInfo, WcmContent actualContent) {
        [
            URI:uri, 
		    parentURI: contentInfo.parentURI, 
		    lineage: contentInfo.lineage, 
		    title: actualContent.title,
		    titleForHTML: actualContent.titleForHTML,
		    titleForMenu: actualContent.titleForMenu
		] as PageInfo
    }

    /**
     * Construct the page info object where the lineage and related info has not yet been resolved
     */
    static PageInfo makePageInfo(uri, WcmContent actualContent) {
        [
            URI:uri, 
		    parentURI: actualContent?.absoluteURI, 
		    lineage: actualContent?.lineage, 
		    title: actualContent?.title,
		    titleForHTML: actualContent?.titleForHTML,
		    titleForMenu: actualContent?.titleForMenu
		] as PageInfo
    }
    
    /**
     * Evaluate some content with the specified model, the result can be converted to a string or written to output
     */
    static evaluateGSPContent(wcmContentRepositoryService, WcmContent content, model) {
        def groovyTemplate = wcmContentRepositoryService.getGSPTemplate(content)
        return groovyTemplate?.make(model)
    }
    
    
    /**
     * Render a content node with support for GSP tags and template
     * @see static method impl
     */
    void renderGSPContent(WcmContent content, model = null) {
        WcmContentController.renderGSPContent( wcmContentRepositoryService, request, response, content, model)
    }
    
    /**
     * Render a content node with support for GSP tags and template
     *
     * Works in one of two ways:
     * 1. If content node is a template, evaluates the template and passes in model, presumed to contain "node" for for content
     * 2. If content node is not a template, evaluats the content as a GSP, then passes it as pre-rendered body content
     * to the template of "content" if there is one.
     */
    static renderGSPContent(wcmContentRepositoryService, request, response, WcmContent content, model = null) {
        if (model == null) {
            model = [:]
        }

        // Copy in any data supplied by an outside bit of code, eg the content submission controller
        def previousModel = request[REQUEST_ATTRIBUTE_PREPARED_MODEL]
        if (previousModel) {
            model.putAll(previousModel)
        }
        
        // Patch up request attributes if don't exist already

        // User info might have been resolved already, if not get it - remember we are static
        if (!request[REQUEST_ATTRIBUTE_USER]) {
            request[REQUEST_ATTRIBUTE_USER] = ApplicationHolder.application.mainContext.wcmSecurityService.userName
        }
        
        // Render pipeline might have already supplied page info, if not generate it
        if (!request[REQUEST_ATTRIBUTE_PAGE]) {
            request[REQUEST_ATTRIBUTE_PAGE] = WcmContentController.makePageInfo(content.absoluteURI, content)
        }

        // Render pipeline might have already supplied space
        if (!request[REQUEST_ATTRIBUTE_SPACE]) {
            request[REQUEST_ATTRIBUTE_SPACE] = content.space
        }
        
        model.user = request[REQUEST_ATTRIBUTE_USER]
        model.page = request[REQUEST_ATTRIBUTE_PAGE] 
        model.space = request[REQUEST_ATTRIBUTE_SPACE]

        // Prepare the existing output stream
        Writer out = GSPResponseWriter.getInstance(response, 65536)
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        webRequest.setOut(out)

        boolean isTemplate = content instanceof WcmTemplate
        
        if (isTemplate) {
            // Pass in the content so it can be rendered in the template by wcm:content
            request[REQUEST_ATTRIBUTE_NODE] = model.node
            // Set mime type to the one for this template if there is not one set by content already
            if (content.mimeType) {
                response.setContentType(content.mimeType)
            }
        } else {
            StringWriter evaluatedContent = new StringWriter()
            request[REQUEST_ATTRIBUTE_NODE] = content
            model.node = content
            evaluatedContent << evaluateGSPContent(wcmContentRepositoryService, content, model)
            request[REQUEST_PRERENDERED_CONTENT] = evaluatedContent.toString()
        }
        
        // See if there is a template for the content
        def template = isTemplate ? content : wcmContentRepositoryService.getTemplateForContent(content)
        if (template) {
            def templatedContent = evaluateGSPContent(wcmContentRepositoryService, template, model)
            templatedContent.writeTo(out)
        } else {
            out << request[REQUEST_PRERENDERED_CONTENT]
        }

        // flush the existing output stream
        out.flush()
        webRequest.renderView = false
    }
    
    /** 
     * Get a new instance of a script content's Groovy code
     */
    def getWcmScriptInstance(WcmScript s) {
        wcmContentRepositoryService.getWcmScriptInstance(s)
    }

    /** 
     * Render the content using our convention based approach
     * If the content has a template, it is passed to the template for rendering as the "node" variable in the model
     * If the content has no template, if it has a content property it will be rendered verbatim to the client
     */     
    def renderContent(WcmContent content) {
        
        def pageInfo = request[REQUEST_ATTRIBUTE_PAGE]
        def contentText
        if (content.metaClass.hasProperty(content, 'content')) {
            contentText = content.content
            pageInfo.text = contentText
    
            log.debug "Content is: $contentText"
        }
    
        def template = wcmContentRepositoryService.getTemplateForContent(content)
        log.debug "Content's template is: $template"

        if (!template) {
            if (contentText != null) {
                // todo: what need to be rendered?
                log.debug "Rendering content of type [${content.mimeType}] without template: $contentText"
                // @todo This needs to handle WcmContentFile/WcmContentDirectory requests and pipe them through request dispatcher
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

/** 
 * The object passed to the model representing info about the current page (not same as content!)
 */
class PageInfo {
    String URI
    String parentURI
    List lineage
    String text
    String title
    String titleForHTML
    String titleForMenu
}
