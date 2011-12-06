package org.weceem.content

import org.weceem.content.WcmTemplate
import org.weceem.script.WcmScript
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.pages.GSPResponseWriter
import org.weceem.script.WcmScript
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import org.weceem.util.MimeUtils


class RenderEngine {
    Log log = LogFactory.getLog(RenderEngine)
    
    static String REQUEST_ATTRIBUTE_PAGE = "weceem.page"
    static String REQUEST_ATTRIBUTE_USER = "weceem.user"
    static String REQUEST_ATTRIBUTE_NODE = "weceem.node"
    static String REQUEST_ATTRIBUTE_SPACE = "weceem.space"
    static String REQUEST_ATTRIBUTE_CONTENTINFO = "weceem.content.info"
    static String REQUEST_ATTRIBUTE_PREPARED_MODEL = "weceem.prepared.model"
    static String REQUEST_PRERENDERED_CONTENT = "weceem.prerendered.content"

    def wcmSecurityService
    def wcmContentRepositoryService
    def proxyHandler
    
    /**
     * Construct the page info object for custom-rendering situations where you have not yet resolved the node
     */
    PageInfo makePageInfo(uri, contentInfo, WcmContent actualContent) {
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
    PageInfo makePageInfo(uri, WcmContent actualContent) {
        [
            URI:uri, 
		    parentURI: actualContent?.absoluteURI, 
		    lineage: actualContent?.lineage, 
		    title: actualContent?.title,
		    titleForHTML: actualContent?.titleForHTML,
		    titleForMenu: actualContent?.titleForMenu
		] as PageInfo
    }
    
	UserInfo makeUserInfo()
	{
		def activeUser = wcmSecurityService.userName
		def u = wcmSecurityService.userPrincipal
		boolean isAuth = !(u instanceof String)
		[
		    username: activeUser,
		    email: isAuth,
		    email: isAuth ? u.email : null,
		    firstName: isAuth ? u.firstName : null,
		    lastName: isAuth ? u.lastName : null
		] as UserInfo
	}
    
    void showContent(controllerDelegate, content) {
        // Clone the rendering closure and pass it the content
        Closure c = renderPipeline.clone()
        c.delegate = controllerDelegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c(content)
    }
    
    
    /**
     * Render a content node with support for GSP tags and template
     *
     * Works in one of two ways:
     * 1. If content node is a template, evaluates the template and passes in model, presumed to contain "node" for for content
     * 2. If content node is not a template, evaluats the content as a GSP, then passes it as pre-rendered body content
     * to the template of "content" if there is one.
     */
    void renderGSPContent(request, response, WcmContent content, model = null) {
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
            request[REQUEST_ATTRIBUTE_USER] = makeUserInfo()
        }
        
        // Render pipeline might have already supplied page info, if not generate it
        if (!request[REQUEST_ATTRIBUTE_PAGE]) {
            request[REQUEST_ATTRIBUTE_PAGE] = makePageInfo(content.absoluteURI, content)
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
     * Render the content using our convention based approach
     * If the content has a template, it is passed to the template for rendering as the "node" variable in the model
     * If the content has no template, if it has a content property it will be rendered verbatim to the client
     */     
    def renderContent(controllerDelegate, WcmContent content) {
        
        def pageInfo = controllerDelegate.request[REQUEST_ATTRIBUTE_PAGE]
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
                controllerDelegate.response.contentType = content.mimeType
                controllerDelegate.render(text:contentText)
            } else {
                controllerDelegate.response.sendError(500, "Unable to render content at ${content.absoluteURI}, no content property and no template defined")
                return null
            }
            return
        }
        
        // Render the template, this call will handle the content too by passing it in as model to template
        return renderGSPContent(controllerDelegate, template, [node: content])
    }
    
    /**
     * Evaluate some content with the specified model, the result can be converted to a string or written to output
     */
    def evaluateGSPContent(wcmContentRepositoryService, WcmContent content, model) {
        def groovyTemplate = wcmContentRepositoryService.getGSPTemplate(content)
        return groovyTemplate?.make(model)
    }
    
    /**
     * Render a content node with support for GSP tags and template
     * @see static method impl
     */
    void renderGSPContent(controllerDelegate, WcmContent content, model = null) {
        renderGSPContent( 
            controllerDelegate.request, 
            controllerDelegate.response, 
            content, 
            model)
    }
    
    /** 
     * Get a new instance of a script content's Groovy code
     */
    def getWcmScriptInstance(WcmScript s) {
        wcmContentRepositoryService.getWcmScriptInstance(s)
    }

    def executeScript(controllerDelegate, WcmScript script) {
        Closure code = getWcmScriptInstance(script)
        code.delegate = controllerDelegate
        code.resolveStrategy = Closure.DELEGATE_FIRST
        return code()
    }
    
    /**
     * Render a file
     */
    def renderFile(controllerDelegate, File f, String mimeType) {
        def mt = mimeType ?: MimeUtils.getDefaultMimeType(f.name)
        controllerDelegate.response.setContentType(mt)    
        // @todo set caching headers just as for normal content
        // @todo is this fast enough?    
        controllerDelegate.response.outputStream << f.newInputStream()
        return null
    }
    
    
   /** 
    * Do the full default render pipeline on the supplied content instance.
    * NOTE: Only this exact instance will be rendered, `so it must be pre-resolved if it is a WcmVirtualContent node
    * 
    * @todo Should we move this to ContentRepo service? Still requires a delegate that has all controller-style methods
    * but could be reusable in a non-request context e.g. jobs that produce PDFs or sending HTML emails from CMS
    */
   private renderPipeline = { content ->
       // Make this available to the rest of the request chain
       request[REQUEST_ATTRIBUTE_NODE] = content 

       // This may have been supplied from content resolution if not we have to fake it
       def contentInfo = request[REQUEST_ATTRIBUTE_CONTENTINFO]
       def req = request
       if (!contentInfo) {
           contentInfo = [:]
           contentInfo.with {
               parentURI = content.parent ? content.parent.absoluteURI : ''
               lineage = content.lineage
               content = req[RenderEngine.REQUEST_ATTRIBUTE_NODE]
           }
       }
       
	   def pageInfo = makePageInfo(content.absoluteURI, contentInfo, content)
       request[REQUEST_ATTRIBUTE_PAGE] = pageInfo

       def contentClass = proxyHandler.unwrapIfProxy(content).class
       
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
                log.debug "Content of type ${contentClass} at uri ${content.absoluteURI} is handling its own rendering"

                assert contentClass.handleRequest instanceof Closure
            }

            def handler = contentClass.handleRequest.clone()
            handler.delegate = new HandleRequestDelegator(controllerDelegate:delegate, renderEngine:this)
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
            return renderContent(delegate, content)
        }
    }
}

/**
 * This class is the API we provide for handleRequest actions, delegating to the controller anything
 * we don't handle
 */
class HandleRequestDelegator {
    def controllerDelegate
    def renderEngine 
    
    def methodMissing(String name, args) {
        def argTypes = args.collect { a -> a != null ? a.getClass() : null }
        if (this.metaClass.respondsTo(name, argTypes )) {
            return renderEngine."$name"(*args)
        } else {
            return controllerDelegate."$name"(*args)
        }
    }

    def propertyMissing(String name) {
        if (controllerDelegate.metaClass.hasProperty(name)) {
            return controllerDelegate["$name"]
        } else {
            return renderEngine[name]
        }
    }

    def renderGSPContent(content, model = null) {
        renderEngine.renderGSPContent(controllerDelegate, content, model)
    }
    
    def renderContent(content) {
        renderEngine.renderContent(controllerDelegate, content)
    }
    
    def renderFile(File file, String mimetype) {
        renderEngine.renderFile(controllerDelegate, file, mimetype)
    }
    def executeScript(WcmScript script) {
        renderEngine.executeScript(controllerDelegate, script)
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

/**
 * The object passed to the model representing the user
 */
class UserInfo
{
	String username
	String email
	String firstName
	String lastName
}
