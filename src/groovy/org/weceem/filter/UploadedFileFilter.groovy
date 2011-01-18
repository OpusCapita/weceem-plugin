package org.weceem.filter

import javax.servlet.*
import org.springframework.web.context.support.WebApplicationContextUtils
import org.weceem.util.MimeUtils
import org.weceem.security.WeceemSecurityPolicy

class UploadedFileFilter implements Filter {
    
    def wcmContentRepositoryService
    def wcmSecurityService
    def cacheHeadersService
    
    void init(FilterConfig config) throws ServletException {
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        wcmContentRepositoryService = applicationContext.wcmContentRepositoryService
        wcmSecurityService = applicationContext.wcmSecurityService
        cacheHeadersService = applicationContext.cacheHeadersService
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        def info = wcmContentRepositoryService.resolveSpaceAndURIOfUploadedFile(request.requestURI - request.contextPath)
        def space = info.space
        def uri = info.uri
 
        def canView = wcmSecurityService.hasPermissions(space, uri, [WeceemSecurityPolicy.PERMISSION_VIEW])
        if (!canView) {
            canView = wcmSecurityService.hasPermissions(space, uri, [WeceemSecurityPolicy.PERMISSION_EDIT])
        }
        
        // @todo also implement Draft status checks here so ppl cannot view drafts even if they have perms (e.g. guests)
        if (!canView) {
            response.sendError(503, "You do not have permission to view this resource")
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
        
        cacheHeadersService.withCacheHeaders([cacheHeadersService:cacheHeadersService, request:request, response:response]) { 
            etag {
                // @todo implement ETag somehow for files with no ContentFile, store hash in domain object?
                content ? "${content.ident()}:${content.version}".encodeAsSHA1() : null
            }
            
            lastModified {
                content ? (content.changedOn ?: content.createdOn) : new Date(f.lastModified())
            }
            
            generate {
                cacheHeadersService.cache(response, [validFor: 1, shared:true])  // 1 second caching, just makes sure some OK headers are sent for proxies

                def mt = MimeUtils.getDefaultMimeType(f.name)
                response.setContentType(mt)    
                // @todo is this fast enough?    
                response.outputStream << f.newInputStream()
            }
        }
    }
}