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
    def wcmRenderEngine
    
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
            return [content: content, changeHistory: wcmContentRepositoryService.getChangeHistory(content), 
                editableProperties: wcmEditorService.getEditorInfo(content.class)]
        }
    }

    void workaroundBlankAssociationBug() {
        ['script', 'template', 'target', 'parent'].each { fld ->
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
                render(view: 'create', model: [content: content, 
                    changeHistory: wcmContentRepositoryService.getChangeHistory(content),
                    editableProperties: wcmEditorService.getEditorInfo(content.class)])
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
        println "Params: ${params}"
        def content
        if (params.type) {
            content = wcmContentRepositoryService.newContentInstance(params.type)
        } else {
            content = WcmContent.findById(params.id)
        }
        workaroundBlankAssociationBug()
        wcmContentRepositoryService.hackedBindData(content, params)
        println "Node: ${content.dump()}"
        wcmRenderEngine.showContent(this, content)
    }
    
    def update = {
        
        workaroundBlankAssociationBug()
        
        WcmContent.withTransaction { txn -> 
            def content
            def errors
            def notFound = false
            def result = wcmContentRepositoryService.updateNode(params.id, params)
            errors = result.errors
            content = result.content
            notFound = result.notFound
            if (errors) {
                txn.setRollbackOnly()
                if (errors) {
                    if (log.debugEnabled) {
                        log.debug "Couldn't update content, has errors: ${errors}"
                    }
                    flash.message =  message(code:'message.content.has.errors')
                    render(view: 'edit', model: [content: content, 
                        // Get history in a new session so we don't see unsaved revision
                        changeHistory: WcmContent.withNewSession { wcmContentRepositoryService.getChangeHistory(content) },
                        editableProperties: wcmEditorService.getEditorInfo(content.class)])
                    return
                }
            } else if (notFound) {
                response.sendError(404, "Cannot update node, no node found with id ${params.id}")
                return
            } else {
                flash.message = message(code:'message.content.updated', args:[
                    content.title, 
                    message(code:'content.item.name.'+content.class.name)] )
                if (!params['continue']) {
                    redirect(controller:'wcmRepository', action:'treeTable')
                    return
                } else {
                    redirect(action:'edit', params:[id:content.id])
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

    def showRevision = {
        def revId = params.id
        def rev = wcmContentRepositoryService.getChangeHistoryItem(revId)
        def props = rev?.extractProperties()
        def con = grailsApplication.classLoader.loadClass(rev.objectClassName).get(rev.objectKey)
        [historyItem:rev, content:props.content, contentProperties:props.properties, currentContent:con]
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
