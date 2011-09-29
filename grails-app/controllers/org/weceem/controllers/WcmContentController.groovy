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
import org.weceem.script.*

import org.weceem.security.AccessDeniedException

import org.weceem.util.MimeUtils

class WcmContentController {
    static String REQUEST_ATTRIBUTE_PREVIEWNODE = "weceem.preview.node"
    static String UI_MESSAGE = 'weceem.message'
    
    static SESSION_TIMESTAMP_KEY = "weceem.session.timestamp"
    
    def wcmContentRepositoryService
    def wcmContentFingerprintService
    def wcmSecurityService
    def wcmCacheService
    
    def wcmRenderEngine
 
    def preview = {
        if (!request[REQUEST_ATTRIBUTE_PREVIEWNODE]) {
            response.sendError(500, "No preview node set")
        } else {
            cache false // never allow browser to cache this
            wcmRenderEngine.showContent(delegate, request[REQUEST_ATTRIBUTE_PREVIEWNODE])
        }
    }
    
    def showUploadedFile = {
        // @todo See if content node exists for file, if so use that for meta info like mod date
        def f = new File(wcmContentRepositoryService.uploadDir, params.uri)
        renderFile(f, null)
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
                request[RenderEngine.REQUEST_ATTRIBUTE_SPACE] = space

                if (log.debugEnabled) {
                    log.debug "Loading content from for uri: ${uri}"
                }
                def contentInfo = wcmContentRepositoryService.findContentForPath(uri,space)

                // See if we hit non-renderable content, which might have a index.html under it i.e. Folder or Server Directory
                if (contentInfo) {
                    if (contentInfo.content && 
                        !uri.endsWith('/') &&
                        !wcmContentRepositoryService.contentIsRenderable(contentInfo.content)) {
                        contentInfo = wcmContentRepositoryService.findContentForPath(uri+'/',space)
                    }
                }

                if (contentInfo) {
                    if (log.debugEnabled) {
                        log.debug "Checking user is allowed to view content at $uri"
                    }
                    if (!wcmSecurityService.isUserAllowedToViewContent(contentInfo.content)) {
                        throw new AccessDeniedException("You cannot view this content")
                    }
                }
                
                request[RenderEngine.REQUEST_ATTRIBUTE_CONTENTINFO] = contentInfo
                
                // Resolve any virtual nodes
                def content = resolveActualContent(contentInfo?.content)
            
                if (log.debugEnabled) {
                    log.debug "Content after resolving virtual content for uri: ${uri} is: ${content?.dump()}"
                }
                
                def template
                if (content) {
                    template = wcmContentRepositoryService.getTemplateForContent(content)
                }
                
                def tagValue
                withCacheHeaders { 
                    etag {
                        if (content) {
                            tagValue = wcmContentFingerprintService.getFingerprintFor(content)
                        }
                        
                        // Create ETag including session timestamp if available && content is per-user, so
                        // that user-specific content refreshes but caches appropriately
                        // This means we don't add session timestamp to ETags for non-templated resources
                        if (template && template.userSpecificContent) {
                            if (log.debugEnabled) {
                                log.debug "Calculating user-specific ETag"
                            }
                            def sessionTimestamp = session[SESSION_TIMESTAMP_KEY] ?: ''
                            return tagValue + '/'+sessionTimestamp
                        } else {
                            return content ? tagValue : null
                        }
                    }
                    
                    lastModified {
                        content ? wcmContentRepositoryService.getLastModifiedDateFor(content) : null
                    }
                    
                    generate {
                        request[RenderEngine.REQUEST_ATTRIBUTE_USER] = wcmRenderEngine.makeUserInfo()
            
                        if (content) {
                            // @todo parameterize this default max age
                            def cacheMaxAge = 1
                            if (!template?.userSpecificContent) {
                                cacheMaxAge = (content.validFor != null ? content.validFor : template?.validFor) ?: 1
                            }
                            
                            // @todo parameterize the defaut publicly cacheable (shared) setting
                            def publiclyCacheable = template ? !template.userSpecificContent : true
                            
                            cache validFor: cacheMaxAge, shared:publiclyCacheable  // 1 second caching, just makes sure some OK headers are sent for proxies
                            wcmRenderEngine.showContent(delegate, content)
                        } else {
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
    private WcmContent resolveActualContent(WcmContent requestedContent) {
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
     * Render a file
     */
    def renderFile(File f, String mimeType) {
        def mt = mimeType ?: MimeUtils.getDefaultMimeType(f.name)
        response.setContentType(mt)    
        // @todo set caching headers just as for normal content
        // @todo is this fast enough?    
        response.outputStream << f.newInputStream()
        return null
    }
    
    def notFound = {
        def msg = "No content was found for path: ${params.path}"
        response.sendError(404, msg)
    }
}
