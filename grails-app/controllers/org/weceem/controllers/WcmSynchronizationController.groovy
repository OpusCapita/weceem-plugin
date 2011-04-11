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
        render (view: "fileList", model: [missingFiles: missedFiles, createdContent: createdContent, 
            space: space, dirnum: dirnum, filenum: filenum])
    }
    
    /**
     * Delete content by set of ids
     *
     */
    def delete = {
        def pattern = ~/delete-\d+/
        def idpattern = ~/\d+/
        for (p in params){
            if (pattern.matcher(p.key).matches()){
                def id = idpattern.matcher(p.key)[0]
                wcmContentRepositoryService.deleteNode(WcmContentFile.get(id), true)
            }
        }
        redirect(controller: "wcmRepository")
     }

    def done = {
        redirect(controller: "wcmRepository")
    }
    
}
