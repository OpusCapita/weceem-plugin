package org.weceem.export

import java.text.SimpleDateFormat
import org.codehaus.groovy.grails.commons.ApplicationHolder
import com.thoughtworks.xstream.XStream
import groovy.xml.MarkupBuilder

import org.weceem.content.*
import org.weceem.css.*
import org.weceem.files.*
import org.weceem.blog.*
import org.weceem.html.*
import org.weceem.wiki.*
import java.text.SimpleDateFormat

/**
 * SimpleSpaceExporter.
 *
 * @author Viktor Fedorov
 */
class SimpleSpaceExporter implements SpaceExporter {
    
    File execute(Space spc) {
        def ts = new SimpleDateFormat('yyMMddHHmmssSSS').format(new Date())
        def filesDir = new File(ApplicationHolder.application.mainContext.servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}"))

        def baseDir = new File("${filesDir.absolutePath}/export-${ts}")
        baseDir.mkdir()

        def file = new File("${baseDir.absolutePath}/content.xml")
        
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        
        def contentList = Content.findAll(
                "from Content c where c.space=? ", spc)
        //Building XML structure
        xml.content(){
            for (cnt in contentList){
                "${cnt.class.name}"(){
                    id("class": cnt.id.class.name, "${cnt.id}")
                    //Getting Grails Domain object properties
                    getDeclaredProperties(cnt).each{prop->
                        def cntProp = cnt."${prop.name}"
                        if (cntProp != null){
                            def cntPropClass = cntProp.class
                            def cntPropClassName = ""
                            cntPropClassName = cntProp.class.name
                            //Check property's type: association or not
                            if (prop.isAssociation() && !Status.isAssignableFrom(cntPropClass)){
                                if (Collection.isAssignableFrom(cntPropClass)){
                                    "${prop.name}"("class": cntPropClassName){
                                        for (child in cntProp){
                                            "${child.class.name}"("${child.id}")
                                        }
                                    }
                                }else{
                                    "${prop.name}"("class": cntPropClassName,
                                        cntProp.id)
                                }
                            }else{
                                def conv = exportConverters.find{k, v -> 
                                    k.isAssignableFrom(cntPropClass)}?.value
                                "${prop.name}"("class": cntPropClassName, 
                                    conv ? conv(cntProp) : cntProp)
                            }
                        }
                    }
                }
            }
        }
        file << writer.toString()
        
        def ant = new AntBuilder()

        ant.copy(todir: "${baseDir.absolutePath}/files", failonerror: false) {
            fileset(dir: "${filesDir.absolutePath}/${spc.makeUploadName()}")
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
        def grailsApp = ApplicationHolder.application
        def props = grailsApp.getDomainClass(obj.class.name).
            getPersistentProperties().findAll{prop -> 
                !(prop.name in ["language", "space"])}
        return props
    }
    
    static def exportConverters = [
        (java.util.Date): {value ->
             def dateConv = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy", Locale.UK);
             dateConv.format(value)
        },
        (java.lang.Long): {value -> value},
        (java.lang.Integer): {value-> value},
        (java.lang.Short): {value-> value},
        (java.lang.String): {value -> value},
        (org.weceem.content.Status): {value -> value.code}
    ]

}
