package org.weceem.filter

import javax.servlet.*
import org.springframework.web.context.support.WebApplicationContextUtils
import org.weceem.util.MimeUtils
import org.weceem.security.WeceemSecurityPolicy
import org.weceem.content.WcmContent

/**
 * Filter for files uploaded with CK Editor, to apply our security policy / status / caching rules
 */
class UploadedFileFilter implements Filter {
    
    def wcmContentFingerprintService
    def wcmContentRepositoryService
    def wcmSecurityService
    def cacheHeadersService
    
    void init(FilterConfig config) throws ServletException {
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        wcmContentRepositoryService = applicationContext.wcmContentRepositoryService
        wcmSecurityService = applicationContext.wcmSecurityService
        cacheHeadersService = applicationContext.cacheHeadersService
        wcmContentFingerprintService = applicationContext.wcmContentFingerprintService
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        // Force a continuous hib session across service calls to avoid detached objects causing lazy init problems
        WcmContent.withTransaction {
            def info = wcmContentRepositoryService.resolveSpaceAndURIOfUploadedFile(request.requestURI.decodeURL() - request.contextPath)
            def space = info.space
            def uri = info.uri
 
            def canView = wcmSecurityService.hasPermissions(space, uri, [WeceemSecurityPolicy.PERMISSION_VIEW])
            if (!canView) {
                canView = wcmSecurityService.hasPermissions(space, uri, [WeceemSecurityPolicy.PERMISSION_EDIT])
            }
        
            // @todo also implement Draft status checks here so ppl cannot view drafts even if they have perms (e.g. guests)
            if (!canView) {
                response.sendError(503, "You do not have permission to view this resource")
                chain.doFilter(request, response)
                return
            }

            def contentInfo = wcmContentRepositoryService.findContentForPath(uri, space)
            def content
            if (contentInfo?.content) {
                content = contentInfo.content
            }
        
            def f = wcmContentRepositoryService.getUploadPath(space, uri)
            if (!f.exists()) {
                response.sendError(404)
                return
            }
        
            def ctx = [
                cacheHeadersService:cacheHeadersService, 
                wcmContentFingerprintService:wcmContentFingerprintService, 
                wcmContentRepositoryService:wcmContentRepositoryService, 
                request:request, 
                response:response,
            ]
            cacheHeadersService.withCacheHeaders(ctx) { 
                if (content) {
                    etag {
                        tagValue = wcmContentFingerprintService.getFingerprintFor(content)
                    }
                }
            
                lastModified {
                    content ? wcmContentRepositoryService.getLastModifiedDateFor(content) : new Date(f.lastModified())
                }
            
                generate {
                    def cacheMaxAge = content?.validFor ?: 1
                    
                    def publiclyCacheable = true
                
                    cacheHeadersService.cache(response, [validFor: cacheMaxAge, shared:publiclyCacheable])

                    def mt = content ? content.mimeType : MimeUtils.getDefaultMimeType(f.name)

                    response.setContentType(mt)    
                    try {
                        // @todo is this fast enough?    
                        response.outputStream << f.newInputStream()
                    } catch (IOException ioe) {
                        // Munch. We should never do this but the client has gone away so...
                    }
                }
            }
        }
 
        chain.doFilter(request, response)
    }
}