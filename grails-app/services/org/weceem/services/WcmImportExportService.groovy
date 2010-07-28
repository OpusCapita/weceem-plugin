package org.weceem.services

import org.weceem.content.*
import org.weceem.export.*

/**
 * SpaceImportExportService.
 *
 * @author Sergei Shushkevich
 */
class WcmImportExportService {

    def grailsApplication
    
    def searchableService
    
    def importSpace(WcmSpace space, String importerName, File file) throws ImportException {
        // @todo couldn't inject this service, circular dependency problem. Investigate
        searchableService.stopMirroring()
        
        try {
            WcmContent.withTransaction { txn ->
                try {
                    grailsApplication.mainContext.wcmContentRepositoryService.deleteSpaceContent(space)
                    grailsApplication.mainContext.wcmContentRepositoryService.invalidateCachingForSpace(space)
                    getImporters()."${importerName}"?.execute(space, file)
                } catch (Throwable t) {
                    txn.setRollbackOnly()
                    t.printStackTrace()
                    log.error(t)
                }
            }
        } finally {
            searchableService.startMirroring()
        }
        searchableService.reindex()
    }

    def exportSpace(WcmSpace space, String exporterName) {
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
