package org.weceem.controllers

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import net.java.textilej.parser.builder.HtmlDocumentBuilder
import net.java.textilej.parser.MarkupParser
import com.jcatalog.wiki.WeceemDialect
import grails.converters.JSON
import org.weceem.content.*

class WcmEditorController {
    def wcmContentRepositoryService
    def wcmEditorService

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [create:['GET'], delete: ['POST'], save: 'POST', update: 'POST']
    
    def create = {
        workaroundBlankAssociationBug()
        
        def content = wcmContentRepositoryService.newContentInstance(params.type)
        // Using bindData to work around Grails 1.2m2 bugs, change to .properties when 1.2-RC1 is live
        bindData(content, params)

        return [content: content, editableProperties: wcmEditorService.getEditorInfo(content.class)]
    }

    def edit = {
        def content = WcmContent.get(params.id)
        if (!content) {
            flash.message = "Content not found with id ${params.id}"
            redirect(controller:'wcmRepository')
        } else {
            return [content: content, editableProperties: wcmEditorService.getEditorInfo(content.class)]
        }
    }

    void workaroundBlankAssociationBug() {
        ['template', 'target', 'parent'].each { fld ->
            if (params[fld]) params.remove(['fld'])
            if (params[fld+'.id'] == '') {
                params.remove(fld+'.id')
                params[fld] = null
            }
        }
    }

    def saveContinue = {
        params['continue'] = 'y'
        save()
    }

    def save = {
        assert params.type
        workaroundBlankAssociationBug()
        
        log.debug "Saving new content: ${params}"
        
        WcmContent.withTransaction { txn -> 
            def content = wcmContentRepositoryService.createNode(params.type, params)

            if (!content.hasErrors()) {
                flash.message = message(code:'message.content.saved', args:[content.title, message(code:'content.item.name.'+content.class.name)])
                if (log.debugEnabled) {
                    log.debug "Saved content: ${content}"
                }
                if (!params['continue']) {
                    redirect(controller:'wcmRepository')
                } else {
                    redirect(action:edit, params:[id:content.id])
                }
            } else {
                flash.message =  message(code:'message.content.has.errors')
                flash.error = renderErrors(bean: content)
                txn.setRollbackOnly()
                log.error "Unable to save content: ${content.errors}"
                render(view: 'create', model: [content: content, editableProperties: wcmEditorService.getEditorInfo(content.class)])
            }
        }
    }
    
    def cancel = {
        flash.message = "Editing of content cancelled"
        redirect(controller:'wcmRepository')
    }

    def updateContinue = {
        params['continue'] = 'y'
        update()
    }

    def preview = {
        println "IN PREVIEW!"
        params._preview = true
        update()
    }
    
    def update = {
        println "In update"
        
        workaroundBlankAssociationBug()
        
        WcmContent.withTransaction { txn -> 
            def result = wcmContentRepositoryService.updateNode(params.id, params)
            if (result?.errors || params._preview) {
                txn.setRollbackOnly()
                if (result?.errors) {
                    if (log.debugEnabled) {
                        log.debug "Couldn't update content, has errors: ${result}"
                    }
                    flash.message =  message(code:'message.content.has.errors')
                    if (params._preview) {
                        render "Cannot preview, content has errors. Please return to editor"
                        return
                    } else {
                        render(view: 'edit', model: [content: result.content, editableProperties: wcmEditorService.getEditorInfo(result.content.class)])
                        return
                    }
                } else if (params._preview) {
                    if (log.debugEnabled) {
                        log.debug "Not updating content, preview mode for: ${result}"
                    }
                    WcmContentController.renderGSPContent(wcmContentRepositoryService, request, response, result.content)
                    return null
                }
            } else if (result?.notFound) {
                response.sendError(404, "Cannot update node, no node found with id ${params.id}")
                return
            } else {
                flash.message = message(code:'message.content.updated', args:[
                    result.content.title, 
                    message(code:'content.item.name.'+result.content.class.name)] )
                if (!params['continue']) {
                    redirect(controller:'wcmRepository', action:'treeTable')
                    return
                } else {
                    redirect(action:'edit', params:[id:result.content.id])
                    return
                }
            }        
        }
    }
    
    def delete = {
        def content = wcmContentRepositoryService.getContentClassForType(params.type).get(params.id)
        if (content) {
            content.delete(flush: true)
            flash.message = "Content ${params.id} deleted"
            redirect(action: list)
        } else {
            flash.message = "Content not found with id ${params.id}"
            redirect(action: list)
        }
    }
    
    def dochange = {
        def selectedSpace = WcmSpace.get(params.id)
        def templates = WcmTemplate.findAllBySpace(selectedSpace)
        def data = []
        templates.each {
        def template = [:]
            template.id = it.id
            template.title = it.title
            template.space = selectedSpace.name
            data << template
        }
        render data as JSON
    }
    
    def convertToWiki = {
        render ([result: toWiki(params.text)] as JSON)
    }
    
    def convertToHtml = {
        render ([result: toHtml(params.text)] as JSON)
    }
    
    // @todo rework this, it looks hideous. Loading up a new XSL to transform the content?!
    private String toWiki(String text) {
        if (!text) {
            return ''
        }
        text = text.replaceAll('&nbsp;', '&#160;')
        def tFactory = TransformerFactory.newInstance()
        def stream = grailsApplication.parentContext.getResource(
                'WEB-INF/xslt/xhtml2confluence.xsl').inputStream
        def transformer = tFactory.newTransformer(new StreamSource(stream))
        def out = new StringWriter()
        if (!text.startsWith('<body>') && !text.endsWith('</body>')) {
            text = "<body>${text}</body>"
        }
        transformer.transform(
                new StreamSource(new StringReader(text)),
                new StreamResult(out))
        return out.toString()
    }
    
    private String toHtml(String text) {
        if (!text) {
            return ''
        }
        def out = new StringWriter(text.size() * 2)
        def builder = new HtmlDocumentBuilder(out)
        builder.emitAsDocument = false
        def parser = new MarkupParser(new WeceemDialect(), builder)
        parser.parse(text)
        return out.toString()
    }
}
