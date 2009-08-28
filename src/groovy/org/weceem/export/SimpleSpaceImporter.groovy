package org.weceem.export

import com.thoughtworks.xstream.XStream
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.weceem.content.*
import org.weceem.files.*
import org.weceem.blog.*
import org.weceem.css.*
import org.weceem.forum.*
import org.weceem.html.*
import org.weceem.wiki.*
import java.text.*

/**
 * SimpleSpaceImporter
 *
 * @author Viktor Fedorov
*/
class SimpleSpaceImporter implements SpaceImporter {
    
    Log log = LogFactory.getLog(DefaultSpaceImporter)
    
    def backrefMap = [:]
    def childrenMap = [:]
    def defStatus

    void execute(Space space, File file) {
        def tmpDir = File.createTempFile("unzip-import-", null)
        tmpDir.delete()
        tmpDir.mkdir()
        
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
        backrefMap.clear()
        def grailsApp = ApplicationHolder.application
        def xml = new XmlSlurper().parseText(contentXmlFile.text)
        def contents = [:]
        def cont_parent = [:]
        def cont_children = [:]
        //Obtaining default status
        defStatus = Status.findByPublicContent(true)
        //Parse each Content element
        xml.children().each{ch ->
            parse(ch, xml, space)
        }
        //Recursively save each element
        for (cnt in backrefMap.values()){
            saveContent(cnt)
        }
        //Update element's children
        for (entry in backrefMap.entrySet()){
            def cnt = entry.value
            if (cnt){
                def sid = entry.key
                def childrenList = childrenMap[(sid)]
                for (chid in childrenList){
                    cnt.addToChildren(backrefMap[(chid)])
                }
                if (!cnt.save()){
                    log.error("Can't save content: ${cnt.aliasURI}, error: ${cnt.errors}")
                }
            }
        }
        def filesDir = new File(ServletContextHolder.servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}"))
        ant.copy(todir: "${filesDir.absolutePath}/${space.name}", failonerror: false) {
            fileset(dir: "${tmpDir.absolutePath}/files")
        }
    }
    
    /*
    * Recursively parse content and it's references from XML to backrefMap
    */
    def parse(def element, def document, def space){
        def grailsApp = ApplicationHolder.application
        if (element.name() == "*") return
        def id = element.id.text().toLong()
        def props = grailsApp.getDomainClass(element.name()).getPersistantProperties()
        if (backrefMap[id] != null){
            return backrefMap[id]
        }
        def params = [:]
        params += ["space": (space)]
        //Getting element's properties
        element.children().each{child->
            if (child.name() != "id"){
                def currProp = props.find{prop -> prop.name == child.name()}
                //Check element's type: association or not
                if (currProp.isAssociation() && (currProp.name != "status")){
                    if (currProp.name == "children"){
                        def childrenList = []
                        child.children().each{chld ->
                            childrenList << chld.text().toLong()
                        }
                        childrenMap += [(id): (childrenList)]
                    }else{
                        def chldid = child.text().toLong()
                        def association
                        //If element was proccessed before, retrieve it from backrefMap
                        if (backrefMap[chldid] != null){
                            association = backrefMap[chldid]
                        }else{
                            def newElement = findByID(document, chldid)
                            association = parse(newElement, document, space)
                            backrefMap += [(chldid) : (association)]
                        }
                        params += [(child.name()) : association]
                    }    
                }else{
                    def conv = importConverters.find{ k, v -> 
                        k.isAssignableFrom(getClass(child.@class.text()))}.value
                    params += [(child.name()) : conv(child.text())]
                }
            }
        }
        def content = Content.findWhere(aliasURI: params.aliasURI, space: space)
        if (!content){
            content = getClass(element.name()).newInstance()    
        }
        params.remove "id"
        content.properties = params
        backrefMap += [(id): content]
        return content
    }
    
    /*
    * Recursively save content and it's references
    */
    def saveContent(def content){
        if (content == null) return
        def grailsApp = ApplicationHolder.application
        //if status isn't set then set default status
        if ((content instanceof Content) && (content.status == null)){
            content.status = defStatus
        } 
        //If id != null , then element has been already saved
        if (content.id == null){
            def props = grailsApp.
                getDomainClass(content.class.name).
                getPersistentProperties().findAll{p->
                    p.isAssociation()
                }
            //If property wasn't saved then save it
            for (prop in props){
                if ((content."${prop.name}" != null) && 
                    (prop.name != "children") &&
                    (content."${prop.name}".id != null)) 
                {
                    saveContent(content."${prop.name}")
                }
            }
            if (!content.save()){
                log.error("Can't save content: ${content.aliasURI}, error: ${content.errors}")
            }
        }
    }
    
    def findByID(def document, Long id){
        document.children().find{el ->
        el.id.text().toLong() == id}
    }
    
    Class getClass(def className){
        def classLoader = this.class.classLoader
        Class.forName(className, false, classLoader)
    }

    String getName() {
        'Weceem (ZIP)'
    }
    
    static importConverters = [
        (java.util.Date): {value->
            def dateConv = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy", Locale.UK);
            dateConv.parse(value)},
        (java.lang.Number): {value->
            value.toInteger()},
        (java.lang.String): {value -> value},
        (org.weceem.content.Status): {value-> Status.findByCode(value)}
    ]

}
