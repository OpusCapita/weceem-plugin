package org.weceem.controllers

import org.apache.commons.io.FilenameUtils

import org.weceem.content.*
import org.weceem.security.WeceemSecurityPolicy

import org.weceem.export.*

class WcmSpaceController {

    def wcmImportExportService
    def wcmContentRepositoryService
    def wcmSecurityService
    
    static allowedMethods = [delete: ['GET', 'POST'], save: 'POST', update: 'POST']

    static defaultAction = 'list'
    
    def list = {
        if (!params.max) params.max = 10
        [spaceList: WcmSpace.list(params)]
    }

    def create = {
        def space = new WcmSpace()
        // Using bindData to work around Grails 1.2m2 bugs, change to .properties when 1.2-RC1 is live
        bindData(space, params)
        return ['space': space]
    }

    def edit = {
        def space = WcmSpace.get(params.id)

        if (!space) {
            flash.message = "Space not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [space: space]
        }
    }

    def save = {
        def space = wcmContentRepositoryService.createSpace(params)
        if (!space.hasErrors()) {
            flash.message = "Space '${space.name}' created"
            redirect(action: list, id: space.id)
        } else {
            render(view: 'create', model: [space: space])
        }
    }

    def update = {
        def result = wcmContentRepositoryService.updateSpace(params.id, params)
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
        def space = WcmSpace.get(params.id)
        if (space) {
            wcmContentRepositoryService.deleteSpace(space)

            flash.message = "Space '${space.name}' deleted"
            redirect(action: list)
        } else {
            flash.message = "No space found with id ${params.id}"
            redirect(action: list)
        }
    }

    def importSpace = {
        return [importers: wcmImportExportService.importers]
    }

    /**
     *
     * @param space
     * @param importer
     * @param file
     */
    def startImport = {
        def space = WcmSpace.get(params.space)

        assert wcmSecurityService.hasPermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])
        
        def file = request.getFile('file')

        if (!file.empty) {
            def tmp = File.createTempFile('import',
                    ".${FilenameUtils.getExtension(file.originalFilename)}")
            file.transferTo(tmp)
            try {
                wcmImportExportService.importSpace(space, params.importer, tmp)
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
        return [exporters: wcmImportExportService.exporters]
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
        def space = WcmSpace.get(params.space)
        assert wcmSecurityService.hasPermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])
        log.debug "Export found space [${space}]"
        try {
            def file = wcmImportExportService.exportSpace(space, params.exporter)
            log.debug "Exported space to temp file [${file}]"
            response.contentType = wcmImportExportService.getExportMimeType(params.exporter)
            response.addHeader('WcmContent-Length', file.length().toString())
            def contDisp = "attachment;filename=${space.name}.${FilenameUtils.getExtension(file.name)}"
            log.debug "Returning exported space to client with content disposition: [${contDisp}]"
            response.addHeader('Content-disposition', contDisp)
            response.outputStream << file.readBytes()
        } catch (Exception e) {
            log.error "Could not export space ${params.space}", e
            // This is unlikely to work!
            flash.message = e.message
            redirect(action: exportSpace)
        }
    }
}
