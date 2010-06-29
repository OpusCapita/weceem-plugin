package org.weceem.export

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.weceem.content.*
import org.weceem.files.*

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

    void execute(WcmSpace space, File file) {
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
        defStatus = WcmStatus.findByPublicContent(true)
        //Parse each WcmContent element
        xml.children().each{ch ->
            parse(ch, xml, space)
        }
        // if orderIndexes are duplicated than fix it
        fixBrokenIndexes()
        //Recursively save each element
        for (cntInfo in backrefMap.values()){
            def savedContent = saveContent(cntInfo.content)
            
            // Reinstate tags
            if (cntInfo.tags) {
                savedContent.parseTags(cntInfo.tags)
            }
        }
        //Update element's children
        for (entry in backrefMap.entrySet()){
            def cnt = entry.value.content
            if (cnt){
                def sid = entry.key
                def childrenList = childrenMap[(sid)]
                for (chid in childrenList){
                    cnt.addToChildren(backrefMap[(chid)].content)
                }
                if (!cnt.save()){
                    log.error("Can't save content: ${cnt.aliasURI}, error: ${cnt.errors}")
                }
            }
        }
        def filesDir = new File(ApplicationHolder.application.mainContext.servletContext.getRealPath(
                "/${WcmContentFile.DEFAULT_UPLOAD_DIR}"))
        ant.copy(todir: "${filesDir.absolutePath}/${space.makeUploadName()}", failonerror: false) {
            fileset(dir: "${tmpDir.absolutePath}/files")
        }
    }
    
    /**
    * Fixing duplicated orderIndexes
    **/
    def fixBrokenIndexes(){
        //update orderIndex
        def rootNodes = backrefMap.findAll{it ->
            for (chPair in childrenMap){
                if (it.key in chPair.value){
                    return false
                }
            }
            return true
        }.collect{it -> it.value.content }
        // update orderIndex for root nodes 
        def prevIndex = 0
        if (rootNodes*.orderIndex.unique().size() != rootNodes.size()){
            rootNodes.sort().eachWithIndex(){it, i->
                if (it.orderIndex == prevIndex){
                    for (j in i..(rootNodes.size()-1)){
                        rootNodes[j].orderIndex++
                    }
                }
                prevIndex = it.orderIndex
            }
        }
        // update orderIndex for all children
        childrenMap.each{parent, children->
            prevIndex = 0
            def chdr = children.collect{ it -> 
                backrefMap[it]?.content
            }.findAll{it-> it != null}
            if (chdr*.orderIndex.unique().size() != chdr.size()){
                chdr.sort().eachWithIndex(){it, i->
                    if (it.orderIndex == prevIndex){
                        for (j in i..(chdr.size()-1)){
                            chdr[j].orderIndex++
                        }
                    }
                    prevIndex = it.orderIndex
                }
            }
        }
    }
    
    /*
    * Recursively parse content and it's references from XML to backrefMap
    */
    def parse(def element, def document, def space){
        if (element.name() == "*") return
        def id = element.id.text().toLong()
        def props = getDomainClassArtefact(element.name()).getPersistantProperties()
        if (backrefMap[id] != null){
            return backrefMap[id]
        }
        def params = [:]
        def tags
                
        //Getting element's properties
        element.children().each{child->
            if (!["id", 'tags'].contains(child.name()) ){
                def currProp = props.find{prop -> prop.name == child.name()}
                //Check element's type: association or not
                if (currProp?.isAssociation() && (currProp.name != "status")){
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
                            association = backrefMap[chldid].content
                        }else{
                            def newElement = findByID(document, chldid)
                            association = parse(newElement, document, space).content
                            //backrefMap += [(chldid) : [content:association]
                        }
                        params += [(child.name()) : association]
                    }    
                }else{
                    def conv = importConverters.find{ k, v -> 
                        k.isAssignableFrom(getClass(child.@class.text()))}.value
                    params += [(child.name()) : conv(child.text())]
                }
            } else if ('tags' == child.name()) {
                tags = child.text()
            }
        }
        def content = WcmContent.findWhere(aliasURI: params.aliasURI, space: space)
        if (!content){
            content = getClass(element.name()).newInstance()
        }
        params.remove "id"
        params.remove "space"
        params.remove "space.id"
        
        // @todo remove this and revert to x.properties = y after Grails 1.2-RC1
        def grailsApp = ApplicationHolder.application
        grailsApp.mainContext.wcmContentRepositoryService.hackedBindData(content, params)
        
        content.space = space
        
        if (content.orderIndex == null) content.orderIndex = 0
        backrefMap += [(id): [content:content, tags:tags] ]
        return content
    }
    
    /*
    * Recursively save content and it's references
    */
    def saveContent(def content){
        if (content == null) return
        def grailsApp = ApplicationHolder.application
        //if status isn't set then set default status
        if ((content instanceof WcmContent) && (content.status == null)){
            content.status = defStatus
        } 
        //If id != null , then element has been already saved
        if (content.id == null){
            def props = grailsApp.getDomainClass(content.class.name).persistentProperties.findAll { p ->
                p.isAssociation()
            }
            //If property wasn't saved then save it
            for (prop in props) {
                if ((content."${prop.name}" != null) && 
                    (prop.name != "children") &&
                    (content."${prop.name}".id != null)) 
                {
                    saveContent(content."${prop.name}")
                }
            }
            
            def result = content.save()
            if (!result){
                log.error("Can't save content: ${content.aliasURI}, error: ${content.errors}")
            }
            return result
        } else return content
    }
    
    def findByID(def document, Long id){
        document.children().find{el ->
        el.id.text().toLong() == id}
    }

    String convertLegacyClassNames(def className) {
        if (!className.startsWith('org.weceem')) {
            return className
        }
        // It might be an old weceem <= 0.8 export so lets try adding Wcm to the class name
        def classParts = className.toString().tokenize('.')
        def convertedLegacyClassName = classParts[0..classParts.size()-2].join('.')
        convertedLegacyClassName += '.Wcm' + classParts[-1]
        return convertedLegacyClassName
    }

    Class getClass(def className){
        def classLoader = this.class.classLoader
        def c
        try {
            c = Class.forName(className, false, classLoader)
        } catch (ClassNotFoundException cnfe) {
            c = Class.forName(convertLegacyClassNames(className), false, classLoader)
        }
        return c
    }

    def getDomainClassArtefact(def className){
        def grailsApp = ApplicationHolder.application
        def c = grailsApp.getDomainClass(className)
        if (!c) {
            def newName = convertLegacyClassNames(className)
            println "Trying to get artefact for legacy class: ${className} using modified name ${newName}"
            c = grailsApp.getDomainClass(newName)
        }
        return c
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
        (java.lang.Boolean): {value -> value.toBoolean()},
        (org.weceem.content.WcmStatus): {value-> WcmStatus.findByCode(value)}
    ]

}
