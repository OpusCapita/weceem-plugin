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

    def list = {}

    /**
     * Renders list of files and Content objects which need to be synchonized.
     */
    def synchronizationList = {
        def existingFiles = new TreeSet()
        def contentFilesDir = grailsApplication.parentContext.getResource(
                ContentFile.DEFAULT_UPLOAD_DIR).file
        contentFilesDir.eachDir {spaceDir ->
            def space = Space.findByName(spaceDir.name)
            if (space) {
                spaceDir.eachFileRecurse {file ->
                    def relativePath = file.absolutePath.substring(
                            spaceDir.absolutePath.size() + 1)
                    def content = contentRepositoryService.findContentForPath(relativePath, space).content
                    //if content wasn't found then create new
                    if (!content){
                        createContentFile("${spaceDir.name}/${relativePath}")
                        content = contentRepositoryService.findContentForPath(relativePath, space).content
                        while (content){
                            existingFiles << content
                            content = content.parent
                        }
                    }else{
                        existingFiles << content
                    }
                }
                
            }
        }
        def allFiles = ContentFile.list()
        def missedFiles = allFiles - existingFiles
        render (view: "list", model: [result: missedFiles])
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

    /**
     * Creates ContentFile/ContentDirectory from specified <code>path</code>
     * on the file system.
     *
     * @param path
     */
    def createContentFile(path) {
        List tokens = path.replace('\\', '/').split('/')
        if (tokens.size() > 1) {
            def space = Space.findByName(tokens[0])
            def parents = tokens[1..(tokens.size() - 1)]
            def ancestor = null
            def content = null
            parents.eachWithIndex(){ obj, i ->
                def parentPath = "${parents[0..i].join('/')}"
                def file = grailsApplication.parentContext.getResource(
                        "${ContentFile.DEFAULT_UPLOAD_DIR}/${space.name}/${parentPath}").file
                content = contentRepositoryService.findContentForPath(parentPath, space).content
                if (!content){
                    if (file.isDirectory()){
                        content = new ContentDirectory(title: file.name,
                            content: '', filesCount: 0, space: space, orderIndex: 0,
                            mimeType: '', fileSize: 0, status: Status.findByPublicContent(true))
                    }else{
                        def mimeType = servletContext.getMimeType(file.name)
                        content = new ContentFile(title: file.name,
                            content: '', space: space, orderIndex: 0, 
                            mimeType: (mimeType ? mimeType : ''), fileSize: file.length(),
                            status: Status.findByPublicContent(true))
                    }
                    content.createAliasURI()
                    if (!content.save()){
                        println contentParent.errors
                        assert false
                    }
                }
                if (ancestor){
                    if (ancestor.children == null) ancestor.children = new TreeSet()
                    ancestor.children << content
                    ancestor.filesCount += 1
                    assert ancestor.save(flush: true)
                    content.parent = ancestor
                    assert content.save(flush: true)
                }
                ancestor = content
            }
            return content
        }
        return null
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
