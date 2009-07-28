package org.weceem.services

import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.ConfigurationHolder

import org.weceem.content.*

class EditorService {
    def editorInfo = [:]
    
    def grailsApplication
    def contentRepositoryService
    
    void cacheEditorInfo() {
        log.debug "Caching editor info for Content classes"
        contentRepositoryService.listContentClasses().each { cls ->
            log.debug "Found content class $cls"
            cacheEditorInfo(cls)
        }
    }
    
    /** 
     * Build up our cache of conventions info for the content class
     */
    void cacheEditorInfo(Class cls) {
        log.debug "Caching editor info for Content class $cls"
        assert Content.isAssignableFrom(cls)
        
        cachePropertyInfo(cls)

        cacheActionInfo(cls)
    }
    
    /**
     * Cache the action methods of the content class
     */    
    void cacheActionInfo(cls) {
        
    }

    void cachePropertyInfo(Class cls) {
        if (!cls.metaClass.hasProperty(cls, 'editors')) return
        
        assert Content.isAssignableFrom(cls)

        def ancestorChain = [cls]
        while (cls.superclass && (cls.superclass != Object)) {
            ancestorChain.push(cls.superclass)
            cls = cls.superclass
        }

        def data = []

        while (ancestorChain) {
            cls = ancestorChain.pop()
            if (cls.metaClass.hasProperty(cls, 'editors')) {
                def superClassInfo = editorInfo[cls.name]
                if (!superClassInfo) {
                    log.debug "Parsing editors property on $cls"
                    
                    def config = evaluateEditors(cls)
                    log.debug "Found CMS config info on $cls: $config"
                    config.each { clsPropInfo -> 
                        //if (!data.find( { clsPropInfo.property == it.property})) {
                        def existing = data.find( { clsPropInfo.property == it.property})
                        if (existing) {
                            data.remove(existing)
                        }
                        log.debug "Found CMS config for property ${clsPropInfo.property} on $cls that has no config already"
                        data << clsPropInfo
                    }
                }
            }
        }
        
        // Include only those that that aren't hidden
        data = data.grep {
            def include = (it.hidden == null) || (it.hidden == false)
            if (!include) log.debug "Hiding property ${it.property} from content editor for class ${cls}"
            return include
        }
        
        log.debug "Caching CMS config for $cls: $data"
        editorInfo[cls.name] = data
    }
    
    protected evaluateEditors(Class cls) {
        def eds = cls.editors.clone()
        if (!(eds instanceof Closure)) {
            log.warn "The [editors] property of Content classes must be a closure"
            return
        }

        def dc = grailsApplication.getDomainClass(cls.name)
        assert dc
        
        eds.delegate = new EditorsBuilder()
        eds.resolveStrategy = Closure.DELEGATE_FIRST
        eds()

        def data = eds.delegate.data

        def allProps = dc.persistentProperties
        
        def dataByPropName = [:]
        // Make sure every one has an editor set, imply if not explicit
        // and cache by prop name for later lookups
        data.each {
            // Get the actual type in case it is not defined in the editor
            if (!it.editor) {
                it.editor = GrailsNameUtils.getShortName(dc.getPropertyByName(it.property).type)
            }

            dataByPropName[it.property] = it
        }
        
        // Add all the ones not explicitly listed, in alpha order - should these go into an "extra" property
        // So we can group them in the UI?
        allProps.sort({a, b -> a.name.compareTo(b.name)}).each { p ->
            if (!dataByPropName.containsKey(p.name) && p.persistent && !p.inherited) {
                log.debug "Adding property ${p.name} to content editor for class ${cls}, not explicitly mapped"
                def info = [property:p.name]
                info.editor = GrailsNameUtils.getShortName(p.type)
                data << info
            }
        }

        return data
    }
    
    def getEditorInfo(Class cls) {
        def t = this
        editorInfo[cls.name] ?: Collections.EMPTY_LIST
    }
    
    def configureFCKEditor(){
        def settings = ConfigurationHolder.config
        def co = new ConfigObject()
        co.fckeditor.upload.basedir = "/${org.weceem.files.ContentFile.DEFAULT_UPLOAD_DIR}/"
        co.fckeditor.upload.overwrite = false
        co.fckeditor.upload.image.browser = true
        co.fckeditor.upload.image.upload = true
        co.fckeditor.upload.image.allowed = ['jpg', 'gif', 'jpeg', 'png']
        co.fckeditor.upload.image.denied = []
        co.fckeditor.upload.media.browser = true
        co.fckeditor.upload.media.upload = true
        co.fckeditor.upload.media.allowed = ['mpg','mpeg','avi','wmv','asf','mov']
        co.fckeditor.upload.media.denied = []
        settings.merge(co)
    }
}


class EditorsBuilder {
    def data = []
    
    def invokeMethod(String name, args) {
        def d = [:]
        if (args) {
            assert args[0] instanceof Map
            d.putAll(args[0])
        }
        d.property = name
        data << d
    }
}
