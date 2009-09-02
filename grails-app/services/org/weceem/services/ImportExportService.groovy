package org.weceem.services

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

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
        // Let's brute-force this
        // @todo remove/rework this for 0.2
        Content.executeUpdate("update org.weceem.html.HTMLContent con set con.template = null where con.space = ?", [space])
        Content.executeUpdate("update org.weceem.content.VirtualContent con set con.target = null where con.space = ?", [space])
        Content.executeUpdate("update org.weceem.wiki.WikiItem con set con.template = null where con.space = ?", [space])
        Content.executeUpdate("update org.weceem.content.Content con set con.parent = null where con.space = ?", [space])
        Content.executeUpdate("delete from org.weceem.content.Content con where con.parent = null and con.space = ?", [space])
        println "Content counts in space $space: ${Content.countBySpace(space)}"
        getImporters()."${importerName}"?.execute(space, file)
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
