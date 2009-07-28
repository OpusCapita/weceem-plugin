package org.weceem.controllers

import org.apache.commons.io.FilenameUtils

import org.weceem.content.*
import org.weceem.services.*
import org.weceem.export.*

class SpaceController {

    def importExportService

    static allowedMethods = [delete: ['GET', 'POST'], save: 'POST', update: 'POST']

    static defaultAction = 'list'
    
    def list = {
        if (!params.max) params.max = 10
        [spaceList: Space.list(params)]
    }

    def create = {
        def space = new Space()
        space.properties = params
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
        def space = new Space(params)
        if (!space.hasErrors() && space.save()) {
            flash.message = "Space ${space.id} created"
            redirect(action: list, id: space.id)
        } else {
            render(view: 'create', model: [space: space])
        }
    }

    def update = {
        def space = Space.get(params.id)
        if (space) {
            space.properties = params
            if (!space.hasErrors() && space.save()) {
                flash.message = "Space ${params.id} updated"
                redirect(action: list, id: space.id)
            } else {
                render(view: 'edit', model: [space: space])
            }
        } else {
            flash.message = "Space not found with id ${params.id}"
            redirect(action: edit, id: params.id)
        }
    }

    def delete = {
        def space = Space.get(params.id)
        if (space) {
            def contents = Content.findAllWhere(space: space)
            def templateList = []
            def copiesList = []
            contents.each() {
                if (it instanceof Template) {
                    templateList << it
                } else if (it instanceof VirtualContent) {
                    copiesList << it
                }
            }
            // delete all copies for contents in space
            copiesList*.delete()
            // delete all templates from space
            templateList*.delete()
            // delete other contents
            (contents - copiesList - templateList)*.delete()
            // delete space
            space.delete(flush: true)
            
            flash.message = "Space ${params.id} deleted"
            redirect(action: list)
        } else {
            flash.message = "Space not found with id ${params.id}"
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
                redirect(action: importSpace)
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
        def space = Space.get(params.space)
        try {
            def file = importExportService.exportSpace(space, params.exporter)
            response.contentType = importExportService.getExportMimeType(params.exporter)
            response.addHeader('Content-Length', file.length().toString())
            response.addHeader('Content-disposition',
                    "attachment;filename=${space.name}.${FilenameUtils.getExtension(file.name)}")
            response.outputStream << file.readBytes()
        } catch (Exception e) {
            flash.message = e.message
            redirect(action: exportSpace)
        }
    }
}
