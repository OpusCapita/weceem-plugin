package org.weceem.controllers

import org.apache.commons.io.FilenameUtils

import org.weceem.content.*
import org.weceem.security.WeceemSecurityPolicy
import org.weceem.services.*
import org.weceem.export.*
import org.weceem.files.ContentFile

class WcmSpaceController {

    def importExportService
    def contentRepositoryService
    def weceemSecurityService
    
    static allowedMethods = [delete: ['GET', 'POST'], save: 'POST', update: 'POST']

    static defaultAction = 'list'
    
    def list = {
        if (!params.max) params.max = 10
        [spaceList: Space.list(params)]
    }

    def create = {
        def space = new Space()
        // Using bindData to work around Grails 1.2m2 bugs, change to .properties when 1.2-RC1 is live
        bindData(space, params)
        return ['space': space]
    }

    def edit = {
        def space = Space.get(params.id)

        if (!space) {
            flash.message = "Space not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [space: space]
        }
    }

    def save = {
        def space = contentRepositoryService.createSpace(params)
        if (!space.hasErrors()) {
            flash.message = "Space '${space.name}' created"
            redirect(action: list, id: space.id)
        } else {
            render(view: 'create', model: [space: space])
        }
    }

    def update = {
        def result = contentRepositoryService.updateSpace(params.id, params)
        if (!result.notFound) {
            if (!result.errors) {
                flash.message = "Space '${result.space.name}' updated"
                redirect(action: list, id: result.space.id)
            } else {
                render(view: 'edit', model: [space: result.space])
            }
        } else {
            flash.message = "Space not found with id ${params.id}"
            redirect(action: edit, id: params.id)
        }
    }

    def delete = {
        def space = Space.get(params.id)
        if (space) {
            contentRepositoryService.deleteSpace(space)

            flash.message = "Space '${space.name}' deleted"
            redirect(action: list)
        } else {
            flash.message = "No space found with id ${params.id}"
            redirect(action: list)
        }
    }

    def importSpace = {
        return [importers: importExportService.importers]
    }

    /**
     *
     * @param space
     * @param importer
     * @param file
     */
    def startImport = {
        def space = Space.get(params.space)

        assert weceemSecurityService.hasPermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])
        
        def file = request.getFile('file')

        if (!file.empty) {
            def tmp = File.createTempFile('import',
                    ".${FilenameUtils.getExtension(file.originalFilename)}")
            file.transferTo(tmp)
            try {
                importExportService.importSpace(space, params.importer, tmp)
                flash.message = message(code: 'message.import.finished')
            } catch (Throwable e) {
                log.error("Unable to import space", e)
                flash.message = e instanceof ImportException ? e.message : e.toString()
            } finally {
                redirect(controller: 'wcmRepository', action: 'treeTable', params: ["space": space.name])
            }
        } else {
            flash.message = message(code: 'error.import.emptyFile')
            redirect(action: importSpace)
        }
    }

    def exportSpace = {
        return [exporters: importExportService.exporters]
    }

    def startExport = {
    }

    /**
     *
     * @param space
     * @param exporter
     */
    def performExport = {
        log.debug "Starting export of space [${params.space}] - all params: ${params}"
        def space = Space.get(params.space)
        assert weceemSecurityService.hasPermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])
        log.debug "Export found space [${space}]"
        try {
            def file = importExportService.exportSpace(space, params.exporter)
            log.debug "Exported space to temp file [${file}]"
            response.contentType = importExportService.getExportMimeType(params.exporter)
            response.addHeader('Content-Length', file.length().toString())
            def contDisp = "attachment;filename=${space.name}.${FilenameUtils.getExtension(file.name)}"
            log.debug "Returning exported space to client with content disposition: [${contDisp}]"
            response.addHeader('Content-disposition', contDisp)
            response.outputStream << file.readBytes()
        } catch (Exception e) {
            flash.message = e.message
            redirect(action: exportSpace)
        }
    }
}
