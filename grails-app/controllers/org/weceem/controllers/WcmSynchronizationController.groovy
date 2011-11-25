package org.weceem.controllers

import org.weceem.content.*
import org.weceem.files.*
import grails.converters.JSON

/**
 * WcmSynchronizationController.
 *
 * @author Sergei Shushkevich
 */
class WcmSynchronizationController {

    def wcmContentRepositoryService
    def wcmContentDependencyService

    def index = {}

    def list = {
        def spaces = WcmSpace.list()
        render (view: "list", model: [spaces: spaces])
    }

    /**
     * Renders list of files and WcmContent objects which need to be synchonized.
     */
    def synchronizationList = {
        def space = WcmSpace.get(params.id)
        def result = wcmContentRepositoryService.synchronizeSpace(space)
        def createdContent = result.created
        def missedFiles = result.missed
        def dirnum = createdContent.findAll{c-> c instanceof WcmContentDirectory}.size()
        def filenum = createdContent.size() - dirnum
        def hardRefs = [:]
        missedFiles.each { f ->
            hardRefs[f.ident()] = wcmContentDependencyService.getDependenciesOf(f).any { r -> r.isInstanceOf(WcmVirtualContent)  }
        }
        render (view: "fileList", model: [
            missingFiles: missedFiles, 
            missingFilesHardRefs: hardRefs,
            createdContent: createdContent, 
            space: space, 
            dirnum: dirnum, 
            filenum: filenum])
    }
    
    /**
     * Delete content by set of ids
     *
     */
    def delete = {
        def pattern = ~/delete-\d+/
        def idpattern = ~/\d+/
        // @todo these must happen in leaf --> parent order or we get errors
        // Alternatively we delete parents first and skip descendents.
        def nodes = []
        for (p in params){
            if (pattern.matcher(p.key).matches()){
                def id = idpattern.matcher(p.key)[0]
                nodes << WcmContentFile.get(id)
            }
        }
        if (nodes) {
            wcmContentRepositoryService.removeAllReferencesTo(nodes)
            wcmContentRepositoryService.deleteNodes(nodes, true)
        }
        redirect(controller: "wcmRepository")
     }

    def done = {
        redirect(controller: "wcmRepository")
    }
    
}
