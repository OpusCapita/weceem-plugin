package org.weceem.services

import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.content.*
import org.weceem.export.*

/**
 * SpaceImportExportService.
 *
 * @author Sergei Shushkevich
 */
class ImportExportService {

    def grailsApplication
    
    def importSpace(Space space, String importerName, File file) throws ImportException {
        // @todo couldn't inject this service, circular dependency problem. Investigate
        Content.withTransaction { txn ->
            try {
                grailsApplication.mainContext.contentRepositoryService.deleteSpaceContent(space)
                getImporters()."${importerName}"?.execute(space, file)
            } catch (Throwable t) {
                txn.setRollbackOnly()
                log.error(t)
            }
        }
    }

    def exportSpace(Space space, String exporterName) {
        getExporters()."${exporterName}"?.execute(space)
    }

    def getImporters() {
        grailsApplication.mainContext.getBeansOfType(SpaceImporter.class)
    }
    
    //for now only SimpleSpaceExporter(DefaultSpaceExporter is not supported)
    def getExporters() {
        grailsApplication.mainContext.getBeansOfType(SimpleSpaceExporter.class)
    }

    def getExportMimeType(String exporterName) {
        getExporters()."${exporterName}"?.mimeType
    }
}
