package org.weceem.controllers

import org.apache.commons.io.FileUtils
import grails.converters.JSON

import org.weceem.content.*
import org.weceem.files.*

/**
 * SynchronizationController.
 *
 * @author Sergei Shushkevich
 */
class SynchronizationController {

    def contentRepositoryService

    def index = {}

    def list = {
        def spaces = Space.list()
        render (view: "list", model: [spaces: spaces])
    }

    /**
     * Renders list of files and Content objects which need to be synchonized.
     */
    def synchronizationList = {
        def space = Space.get(params.id)
        def result = contentRepositoryService.synchronizeSpace(space)
        def createdContent = result.created
        def missedFiles = result.missed
        def dirnum = createdContent.findAll{c-> c instanceof ContentDirectory}.size()
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
                contentRepositoryService.deleteNode(ContentFile.get(id))
            }
        }
        redirect(controller: "repository")
     }

    def done = {
        redirect(controller: "repository")
    }
    
    /**
     * Deletes ContentFile/ContentDirectory with specified <code>id</code>.
     *
     * @param id
     */
    def deleteContent = {
        def content = ContentFile.get(params.id)
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

            def copies = VirtualContent.findAllWhere(target: content)
            copies?.each() {
               if (it.parent) {
                   parent = Content.get(it.parent.id)
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
     * with existing ContentFile/ContentDirectory.
     *
     * @param id
     * @param path
     */
    def linkContentFile = {
        def content = ContentFile.get(params.id)
        List tokens = params.path.replace('\\', '/').split('/')
        if (content && tokens.size() > 1) {
            if (content.space.name == tokens[0]) {
                def file = grailsApplication.parentContext.getResource(
                        "${ContentFile.DEFAULT_UPLOAD_DIR}/${params.path}").file
                if (((content instanceof ContentDirectory) && file.directory)
                        || (content.class == ContentFile && file.file)) {
                    def dstPath = ''
                    def parent = content.parent
                    if (parent && (parent instanceof ContentDirectory)) {
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
                            "${ContentFile.DEFAULT_UPLOAD_DIR}/${content.space.name}${dstPath}").file
                    if (srcDir != dstDir.absolutePath.replace('\\', '/')) {
                        FileUtils.moveToDirectory file, dstDir, true
                    }
                    def src = grailsApplication.parentContext.getResource(
                            "${ContentFile.DEFAULT_UPLOAD_DIR}/${content.space.name}${dstPath}/${file.name}").file
                    def dst = grailsApplication.parentContext.getResource(
                            "${ContentFile.DEFAULT_UPLOAD_DIR}/${content.space.name}${dstPath}/${content.title}").file
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
