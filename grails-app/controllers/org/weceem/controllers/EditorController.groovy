package org.weceem.controllers


import grails.converters.JSON
import org.weceem.content.*

class EditorController {
    def contentRepositoryService
    def editorService

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
            redirect(controller:'repository', params:[space:params.space?.name])
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

    def save = {
        assert params.type
        workaroundBlankAssociationBug()
        
        log.debug "Saving new content: ${params}"
        
        def content = contentRepositoryService.createNode(params.type, params)

        if (!content.hasErrors() && content.save()) {
            flash.message = message(code:'message.content.saved', args:[content.title, message(code:'content.item.name.'+content.class.name)])
            log.debug "Saved content: ${content}"
            redirect(controller:'repository', params:[space:content.space.name])
        } else {
            flash.message = message(code:'message.content.save.failed')
            log.error "Unable to save content: ${content.errors}"
            render(view: 'create', model: [content: content, editableProperties: editorService.getEditorInfo(content.class)])
        }
    }
    
    def cancel = {
        redirect(controller:'repository')
    }

    def update = {
        workaroundBlankAssociationBug()
        
        def result = contentRepositoryService.updateNode(params.id, params)
        if (result?.errors) {
            render(view: 'edit', model: [content: result.content, editableProperties: editorService.getEditorInfo(result.content.class)])
        } else if (result?.notFound) {
            response.sendError(404, "Cannot update node, no node found with id ${params.id}")
        } else {
            flash.message = message(code:'message.content.updated', args:[
                result.content.title, message(code:'content.item.name.'+result.content.class.name)])
            redirect(controller:'repository', action:'treeTable', params:[space:result.content.space.name])
        }        
    }

    def delete = {
        def content = contentRepositoryService.getContentClassForType(params.type).get(params.id)
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
}
