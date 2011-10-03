package org.weceem.export

import com.thoughtworks.xstream.XStream

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.weceem.content.*
import org.weceem.files.*

/**
 * DefaultSpaceImporter.
 *
 * @author Sergei Shushkevich
 */
class DefaultSpaceImporter implements SpaceImporter {
    
    Log log = LogFactory.getLog(DefaultSpaceImporter)
    
    def grailsApplication
    
    void execute(WcmSpace space, File file) {
        def tmpDir = File.createTempFile("unzip-import-", null)
        tmpDir.delete()
        tmpDir.mkdir()

        def grailsApp = grailsApplication

        def ant = new AntBuilder()

        try {
            ant.unzip(src: file.absolutePath, dest: tmpDir.absolutePath)
        } catch (Exception e) {
            log.error( "Unable to import Weceem ZIP file ${file}", e)
            throw new ImportException("Uploaded file can't be unpacked. Check it and try again.")
        }

        def contentXmlFile = new File("${tmpDir.absolutePath}/content.xml")
        if (!contentXmlFile.exists()) {
            throw new ImportException("Uploaded file doesn't contain 'content.xml' file.")
        }
        def defStatus = WcmStatus.findByPublicContent(true)
        def xml = new XmlSlurper().parseText(contentXmlFile.text)
        xml.children().each {child ->
            def builder = new groovy.xml.StreamingMarkupBuilder()
            def baos = new ByteArrayOutputStream(4096)
            baos << builder.bind({
                "${child.name()}" {
                    out << child.getBody()
                }
            })
            
            def xstream = new XStream()
            xstream.setClassLoader(getClass().getClassLoader())            
            xstream.registerConverter(new ImportExportConverter(child.name(), grailsApplication))
            def deserialized = xstream.fromXML(baos.toString())
            
            switch (deserialized.class) {
                case WcmSpace.class:
                    if (!WcmSpace.findByName(deserialized.name)) {
                        def spc = new WcmSpace(getRestoredProperties(deserialized))
                        if (!spc.save()) {
                            log.error( "Failed to import content node: ${spc.dump()} - ${spc.errors}")
                            throw new ImportException("Cannot save node ${spc}")
                        }
                    }
                    break

                case WcmTemplate.class:
                    def template = WcmTemplate.findWhere(aliasURI: deserialized.aliasURI,
                            space: deserialized.space)
                    if (!template) {
                        template = new WcmTemplate()
                    }
                    
                    // @todo remove this and revert to x.properties = y after Grails 1.2-RC1
                    grailsApp.mainContext.wcmContentRepositoryService.hackedBindData(template, getRestoredProperties(deserialized))
                    if (template.status == null){
                        template.status = defStatus
                    }
                    template.space = space
                    if (!template.save()) {
                        log.error( "Failed to import content node: ${template.dump()}")
                        throw new ImportException("Cannot save node ${template} - ${template.errors}")
                    }
                    break
                default:
                    def content = WcmContent.findWhere(aliasURI: deserialized.aliasURI,
                            space: space)
                    if (!content) {
                        content = deserialized.class.newInstance()
                    }

                    // @todo remove this and revert to x.properties = y after Grails 1.2-RC1
                    grailsApp.mainContext.wcmContentRepositoryService.hackedBindData(content, getRestoredProperties(deserialized))
                    content.status = defStatus
                    content.space = space
                    if (content instanceof WcmContentFile) content.syncStatus = 0
                    if (!content.orderIndex) content.orderIndex = 0
                    if (content instanceof WcmVirtualContent){
                        def target = WcmContent.findByAliasURI(content.target.aliasURI)
                        content.target = target
                    }
                    log.debug "Import parent test"
                    if (content.parent) {
                        log.debug "Import has parent: ${content.parent}"
                        
                        def parent = WcmContent.findByAliasURI(content.parent.aliasURI)
                        if (parent) {
                            def orderIndex = ((parent.children && !parent.children?.isEmpty())
                                              ? parent.children?.last()?.orderIndex + 1 : 0)
                            content.orderIndex = orderIndex
                            parent.addToChildren(content)
                            if (!parent.save()) {
                                log.error( "Failed to import content node: ${parent.dump()}")
                                throw new ImportException("Cannot save node ${parent} - ${parent.errors}")
                            }
                        } else {
                            log.debug "Import has parent: ${content.parent} but could not find the parent node in order to add child"
                        }
                    } else {
                        if (!content.save()) {
                            log.error( "Failed to import content node: ${content.dump()} - ${content.errors}")
                            throw new ImportException("Cannot save node ${content}")
                        }
                    }
            }
        }

        def filesDir = org.weceem.services.WcmContentRepositoryService.getUploadPath(space)
        ant.copy(todir: "${filesDir.absolutePath}", failonerror: false) {
            fileset(dir: "${tmpDir.absolutePath}/files")
        }
    }

    String getName() {
        'Weceem 0.1 (ZIP)'
    }

    private def getRestoredProperties(obj) {
        // @todo change this to use DefaultGrailsDomainClass.declaredProperties or equivalent
        obj.properties.findAll {key, value ->
            !['id', 'beforeInsert', 'beforeUpdate', 'beforeDelete', 'version', 'summary', 'space', 'versioningProperties'].contains(key)
        }
    }
}
