package org.weceem.export

import groovy.xml.MarkupBuilder
import org.weceem.content.WcmContent
import org.weceem.content.WcmSpace
import org.weceem.content.WcmStatus
import org.weceem.services.WcmContentRepositoryService

import java.text.SimpleDateFormat

/**
 * SimpleSpaceExporter.
 *
 * @author Viktor Fedorov
 */
class SimpleSpaceExporter implements SpaceExporter {
    
    def grailsApplication
    def proxyHandler
    
    File execute(WcmSpace spc) {
        def ts = new SimpleDateFormat('yyMMddHHmmssSSS').format(new Date())
        def filesDir = new File( System.getProperty('java.io.tmpdir'), "weceem-temp"+System.nanoTime() )
        filesDir.mkdirs()

        def baseDir = new File(filesDir, "export-${ts}")
        baseDir.mkdirs()

        def file = new File(baseDir, "content.xml")
                
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        
        def contentList = WcmContent.findAllBySpace(spc)
        //Building XML structure
        xml.content(){
            for (cnt in contentList){
                def node = proxyHandler.unwrapIfProxy(cnt)
                
                "${node.class.name}"(){
                    id("class": cnt.id.class.name, "${cnt.id}")
                    //Getting Grails Domain object properties
                    getDeclaredProperties(cnt).each{prop->
                        def cntProp = cnt."${prop.name}"
                        if (cntProp != null){
                            def cntPropClass = cntProp.class
                            def cntPropClassName = ""
                            cntPropClassName = cntProp.class.name
                            //Check property's type: association or not
                            if (prop.isAssociation() && !WcmStatus.isAssignableFrom(cntPropClass)){
                                if (Collection.isAssignableFrom(cntPropClass)){
                                    "${prop.name}"("class": cntPropClassName){
                                        for (child in cntProp){
                                            def childNode = proxyHandler.unwrapIfProxy(child)
                                            "${childNode.class.name}"("${child.id}")
                                        }
                                    }
                                }else{
                                    "${prop.name}"("class": cntPropClassName,
                                        cntProp.id)
                                }
                            }else{
                                def conv = SimpleImportExportConverters.exportConverters.find{k, v ->
                                    k.isAssignableFrom(cntPropClass)}?.value
                                "${prop.name}"("class": cntPropClassName, 
                                    conv ? conv(cntProp) : cntProp)
                            }
                        }
                    }
                    // Write out the tags
                    tags(cnt.tags.join(','))
                }
            }
        }
        file.write(writer.toString(), 'UTF-8')
        
        def ant = new AntBuilder()

        ant.copy(todir: "${baseDir.absolutePath}/files", failonerror: false) {
            fileset(dir: WcmContentRepositoryService.getUploadPath(spc))
        }

        def tmp = File.createTempFile("export", ".zip")
        tmp.delete()
        ant.zip(destfile: tmp.absolutePath, basedir: baseDir.absolutePath)
        ant.delete(dir: baseDir.absolutePath)

        return tmp
    }

    String getMimeType() {
        'application/zip'
    }

    String getName() {
        'Weceem (ZIP)'
    }
    
    def getDeclaredProperties(def obj){
        def grailsApp = grailsApplication
        def node = proxyHandler.unwrapIfProxy(obj)
        def props = grailsApp.getDomainClass(node.class.name).
            getPersistentProperties().findAll{prop -> 
                !(prop.name in ["space"])}
        return props
    }
}
