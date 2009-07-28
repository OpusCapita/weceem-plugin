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
   
    def index = {}

    def list = {}

    /**
     * Renders list of files and Content objects which need to be synchonized.
     */
    def synchronizationList = {
        def result = [identifier: 'name', items: []]

        ContentFile.executeUpdate("update ContentFile cf set cf.syncStatus = 1")
        def orphanedFiles = []
        def contentIds = []

        def contentFilesDir = grailsApplication.parentContext.getResource(
                ContentFile.DEFAULT_UPLOAD_DIR).file
        contentFilesDir.eachDir {spaceDir ->
            def space = Space.findByName(spaceDir.name)
            if (space) {
                spaceDir.eachFileRecurse {file ->
                    def contentFile

                    def relativePath = file.absolutePath.substring(
                            spaceDir.absolutePath.size() + 1).replace('\\', '/')
                    def tokens = relativePath.split('/')
                    if (tokens.size() > 1) {
                        contentFile = Content.find("""from Content cn \
                                        where cn.parent.title=:parent and cn.title=:child \
                                        and cn.space=:space and (cn.parent is not null and cn.parent.class=:dirClass) \
                                        and (cn.class=:dirClass or cn.class=:fileClass)""",
                                        [parent: tokens[tokens.size() - 2], child: file.name,
                                        space: space, dirClass: ContentDirectory.class.name,
                                        fileClass: ContentFile.class.name])
                    } else {
                        contentFile = ContentFile.find("""from ContentFile cf \
                                where cf.id not in ( \
                                    select cn.id from Content cn \
                                    where cn.parent is not null and cn.parent.class = ?) \
                                and cf.space = ? and cf.title = ?""",
                                    [ContentDirectory.class.name, space, file.name])
                    }

                    if (contentFile) {
                        contentIds << contentFile.id
                    } else {
                        orphanedFiles << file
                        result.items << [namespace: 'files',
                                name: "${spaceDir.name}/${relativePath}",
                                title: file.name, path: "${spaceDir.name}/${relativePath}",
                                type: (file.directory ? 'Folder' : 'File')]
                    }
                }
            }
        }

        if (contentIds) {
            ContentFile.executeUpdate(
                    "update ContentFile cf set cf.syncStatus = 0 where cf.id in (${contentIds.join(',')})")
        }
        ContentFile.findAll("from ContentFile cf where cf.syncStatus = 1").each {
            result.items << [namespace: 'content', name: it.id, title: it.title,
                    type: message(code: "content.item.name.${it.class.name}"),
                    space: it.space.name]
        }

        render(result as JSON)
    }

    /**
     * Creates ContentFile/ContentDirectory from specified <code>path</code>
     * on the file system.
     *
     * @param path
     */
    def createContentFile = {
        println "in"
        List tokens = params.path.replace('\\', '/').split('/')
        println "tokens $tokens"
        if (tokens.size() > 1) {
            def space = Space.findByName(tokens[0])
            def parents = tokens[1..<(tokens.size() - 1)]
            def hierarchyParent = null
            def valid = true
            if (parents) {
                hierarchyParent = ContentDirectory.find("""from ContentDirectory cd \
                        where cd.title = ? and cd.space = ? \
                                and cd.id not in ( \
                                select cn.id from Content cn \
                                where cn.parent is not null and cn.parent.class = ?)""",
                        [parents[0], space, ContentDirectory.class.name])
                println "hier parent $hierarchyParent"
                if (hierarchyParent) {
                    if (parents.size() > 1) {
                        parents[1..<parents.size()].each {
                            println "finding by parent ${it}"
                            def content = Content.findWhere(parent: hierarchyParent, title: it)
                            if (!content) {
                                valid = false
                            }
                            hierarchyParent = content
                        }
                    }
                } else {
                    valid = false
                }
            }
            if (valid) {
                println "valid"
                def file = grailsApplication.parentContext.getResource(
                        "${ContentFile.DEFAULT_UPLOAD_DIR}/${params.path}").file
                def contentFile
                if (file.directory) {
                    contentFile = new ContentDirectory(title: file.name,
                            content: '', filesCount: 0, space: space,
                            mimeType: '', fileSize: 0, status: 0)
                } else {
                    def mimeType = servletContext.getMimeType(file.name)
                    contentFile = new ContentFile(title: file.name,
                            content: '', space: space,
                            mimeType: (mimeType ? mimeType : ''), fileSize: file.length(),
                            status: 0)
                }
                contentFile.createAliasURI(hierarchyParent)
                contentFile.parent = hierarchyParent

                if (hierarchyParent) {
                    println "valid"
                    if (!hierarchyParent.children) hierarchyParent.children = new TreeSet()
                    def newIndex = hierarchyParent?.children ?
                                       hierarchyParent.children.last().orderIndex + 1 : 0
                    contentFile.orderIndex = newIndex
                    hierarchyParent.children << contentFile
                    if (!(contentFile instanceof ContentDirectory)) {
                        hierarchyParent.filesCount += 1
                    }
                    if (hierarchyParent.save()) {
                        println "save ok"
                        
                        render([success: true] as JSON)
                    } else {
                        println "save fail"
                        render([success: false, error: message(code: 'error.synchronization.save')] as JSON)
                    }
                } else {
                    contentFile.orderIndex = 0
                    if (contentFile.save()) {
                        render([success: true] as JSON)
                    } else {
                        render([success: false, error: message(code: 'error.synchronization.save')] as JSON)
                    }

                }
            } else {
                render([success: false, error: message(code: 'error.synchronization.hierarchy')] as JSON)
            }
        } else {
            println "fail"
            render([success: false] as JSON)
        }
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
