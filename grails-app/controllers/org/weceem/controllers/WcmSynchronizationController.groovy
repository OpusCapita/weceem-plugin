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
        render (view: "fileList", model: [result: missedFiles, createdContent: createdContent, 
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
                contentRepositoryService.deleteNode(WcmContentFile.get(id))
            }
        }
        redirect(controller: "wcmRepository")
     }

    def done = {
        redirect(controller: "wcmRepository")
    }
    
    /**
     * Deletes WcmContentFile/WcmContentDirectory with specified <code>id</code>.
     *
     * @param id
     */
    def deleteContent = {
        def content = WcmContentFile.get(params.id)
        if (content) {
            // we need to remove content from all associations

            if (content.children) {
                def children = new ArrayList(content.children)

                // define new parent for all children as null
                children?.each() {
                    content.children.remove(it)
                    it.orderIndex = 0
                    it.parent = null
                    it.save()
                }
            }

            def parent = content.parent
            if (parent) {
                parent.children.remove(content)
            }

            def copies = WcmVirtualContent.findAllWhere(target: content)
            copies?.each() {
               if (it.parent) {
                   parent = WcmContent.get(it.parent.id)
                   parent.children.remove(it)
               }
               it.delete()
            }
            
            content.delete()
            render([success: true] as JSON)
        } else {
            render([success: false] as JSON)
        }
    }

    /**
     * Links file from specified <code>path</code> on the file system
     * with existing WcmContentFile/WcmContentDirectory.
     *
     * @param id
     * @param path
     */
    def linkContentFile = {
        def content = WcmContentFile.get(params.id)
        List tokens = params.path.replace('\\', '/').split('/')
        if (content && tokens.size() > 1) {
            if (content.space.name == tokens[0]) {
                def file = grailsApplication.parentContext.getResource(
                        "${WcmContentFile.uploadDir}/${params.path}").file
                if (((content instanceof WcmContentDirectory) && file.directory)
                        || (content.class == WcmContentFile && file.file)) {
                    def dstPath = ''
                    def parent = content.parent
                    if (parent && (parent instanceof WcmContentDirectory)) {
                        dstPath = getPath(parent)
                    }
                    if (file.directory) {
                        content.filesCount = 0
                    } else {
                        content.fileSize = file.length()
                    }
                    def srcDir = file.absolutePath.replace('\\', '/')
                    srcDir = srcDir.substring(0, srcDir.size() - file.name.size() - 1)
                    def dstDir = grailsApplication.parentContext.getResource(
                            "${WcmContentFile.uploadDir}/${content.space.name}${dstPath}").file
                    if (srcDir != dstDir.absolutePath.replace('\\', '/')) {
                        FileUtils.moveToDirectory file, dstDir, true
                    }
                    def src = grailsApplication.parentContext.getResource(
                            "${WcmContentFile.uploadDir}/${content.space.name}${dstPath}/${file.name}").file
                    def dst = grailsApplication.parentContext.getResource(
                            "${WcmContentFile.uploadDir}/${content.space.name}${dstPath}/${content.title}").file
                    src.renameTo(dst)
                    content.save()
                    render([success: true] as JSON)
                } else {
                    render([success: false,
                            error: message(code: 'error.synchronization.incompatibleTypes')] as JSON)
                }
            } else {
                render([success: false,
                        error: message(code: 'error.synchronization.differentSpaces')] as JSON)
            }
        } else {
            render([success: false] as JSON)
        }
    }

    private String getPath(content) {
        def dirs = []
        def parent = content
        while (parent) {
            dirs << parent.title
            parent = parent.parent
        }
        def path = dirs.reverse().join('/')

        return path ? "/${path}" : path
    }
    
}
