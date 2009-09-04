package org.weceem.controllers

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import net.java.textilej.parser.builder.HtmlDocumentBuilder
import net.java.textilej.parser.MarkupParser
import com.jcatalog.wiki.WeceemDialect
import grails.converters.JSON
import org.weceem.content.*
import org.weceem.services.EventService

class EditorController {
    def contentRepositoryService
    def editorService
    EventService eventService

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [create:['GET'], delete: ['POST'], save: 'POST', update: 'POST']
    
    def create = {
        workaroundBlankAssociationBug()
        
        def content = contentRepositoryService.newContentInstance(params.type)
        content.properties = params

        return [content: content, editableProperties: editorService.getEditorInfo(content.class)]
    }

    def edit = {
        def content = Content.get(params.id)
        if (!content) {
            flash.message = "Content not found with id ${params.id}"
            redirect(controller:'repository')
        } else {
            return [content: content, editableProperties: editorService.getEditorInfo(content.class)]
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
        params.continue = 'y'
        save()
    }

    def save = {
        assert params.type
        workaroundBlankAssociationBug()
        
        log.debug "Saving new content: ${params}"
        
        def content = contentRepositoryService.createNode(params.type, params)

        if (!content.hasErrors() && content.save()) {
            eventService.afterContentAdded(content, params)
            flash.message = message(code:'message.content.saved', args:[content.title, message(code:'content.item.name.'+content.class.name)])
            log.debug "Saved content: ${content}"
            if (!params.continue) {
                redirect(controller:'repository')
            } else {
                redirect(action:edit, params:[id:content.id])
            }
        } else {
            flash.message = message(code:'message.content.save.failed')
            flash.error = renderErrors(bean: content)
            log.error "Unable to save content: ${content.errors}"
            render(view: 'create', model: [content: content, editableProperties: editorService.getEditorInfo(content.class)])
        }
    }
    
    def cancel = {
        redirect(controller:'repository')
    }

    def updateContinue = {
        params.continue = 'y'
        update()
    }

    def update = {
        workaroundBlankAssociationBug()
        
        def result = contentRepositoryService.updateNode(params.id, params)
        if (result?.errors) {
            render(view: 'edit', model: [content: result.content, editableProperties: editorService.getEditorInfo(result.content.class)])
        } else if (result?.notFound) {
            response.sendError(404, "Cannot update node, no node found with id ${params.id}")
        } else {
            eventService.afterContentUpdated(result.content, params)
            flash.message = message(code:'message.content.updated', args:[
                result.content.title, message(code:'content.item.name.'+result.content.class.name)])
            if (!params.continue) {
                redirect(controller:'repository', action:'treeTable')
            } else {
                redirect(action:'edit', params:[id:result.content.id])
            }
        }        
    }
    
    def delete = {
        def content = contentRepositoryService.getContentClassForType(params.type).get(params.id)
        if (content) {
            content.delete(flush: true)
            eventService.afterContentRemoved(content)
            flash.message = "Content ${params.id} deleted"
            redirect(action: list)
        } else {
            flash.message = "Content not found with id ${params.id}"
            redirect(action: list)
        }
    }
    
    def dochange = {
        def selectedSpace = Space.get(params.id)
        def templates = Template.findAllBySpace(selectedSpace)
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
