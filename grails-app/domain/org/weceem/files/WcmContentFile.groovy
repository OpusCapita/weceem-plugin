    package org.weceem.files

import org.springframework.web.multipart.MultipartFile
import org.apache.commons.io.FileUtils

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

import org.weceem.content.*

import org.weceem.util.MimeUtils

/**
 * WcmContentFile.
 *
 * @author Sergei Shushkevich
 */
class WcmContentFile extends WcmContent {

    static searchable = {
        alias WcmContentFile.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }

    String fileMimeType
    Long fileSize = 0
    Short syncStatus = 0

    MultipartFile uploadedFile

    static transients = WcmContent.transients + 'uploadedFile'

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "server-file-32.png"]

    static overrideRequired = [ 'title': false ]
    
    static constraints = {
        // @todo this is ugly, WcmContentDirectory should never have one, and all files SHOULD
        fileMimeType(nullable:true, blank:true)
    }

    static editors = {
        title()
        aliasURI( editor: 'ReadOnlyURI', group: 'extra')
        uploadedFile(editor:'ContentFileUpload')
        fileMimeType(group:'extra')
        fileSize(group:'extra', editor: 'ReadOnly')
        status()
        syncStatus(hidden: true)
    }

    public void createAliasURI(parent) {
        if (!aliasURI) {
            aliasURI = title
        }
    }

    // Get the servlet container to serve the file
    static handleRequest = { content ->
        renderFile(content.toFile(), content.fileMimeType)
    }
    
    String getMimeType() { 
        fileMimeType
    }
    
    boolean contentShouldAcceptChildren() { false }

    /**
     * Handle the create event to copy the file from the upload form into the filesystem
     * Files are *not* stored in the repository database
     */
    Boolean contentShouldBeCreated(WcmContent parentContent) {
        if (!title) {
            title = uploadedFile?.originalFilename
        }
        aliasURI = uploadedFile?.originalFilename
        assert title
        def path = ''
        if (parentContent && (parentContent instanceof WcmContentDirectory)) {
            assert parentContent.save()
            path = getPathTo(parentContent)
            //@todo surely this is redundant, we can just count children?
            parentContent.filesCount += 1
        }
        org.weceem.services.WcmContentRepositoryService.getUploadPath(space, path).mkdirs()
        
        if (uploadedFile) {
            def f = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "$path/${uploadedFile.originalFilename}")
            try {
                uploadedFile.transferTo(f)
                fileMimeType = uploadedFile.contentType ?: MimeUtils.getDefaultMimeType(uploadedFile.originalFilename)
                fileSize = f.length()
            } catch (Exception e) {
                return false
            }
        }
        
        return true
    }

    Boolean contentDidChangeTitle(String oldTitle) {
        def path = ''
        def parent = this.parent
        if (parent && (parent instanceof WcmContentDirectory)) {
            path = getPathTo(parent)
        }
        def oldFile = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "${path}/${oldTitle}")
        def newFile = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "${path}/${title}")
        oldFile.renameTo(newFile)
        createAliasURI(this.parent)
    }

    Boolean contentDidMove(WcmContent targetParent) {
        if (!targetParent || (targetParent instanceof WcmContentDirectory)) {

            def srcPath = ''
            def dstPath = ''

            // Increment file counter for new parent (really need this?)
            if (targetParent && !(this instanceof WcmContentDirectory)) {
                targetParent.filesCount += 1
                assert targetParent.save()
            }
            
            dstPath = getPathTo(targetParent)

            // Decrement file counter for previous parent (really need this?)
            if (this.parent instanceof WcmContentDirectory) {
                this.parent.filesCount -= 1
                assert this.parent.save()
            } 
            
            srcPath = getPathTo(this)
            
            if (srcPath || dstPath) {
                def file = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, srcPath)
                def targetDir = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, dstPath)
                log.info "Moving file ${file} to ${targetDir}"
                try {
                    FileUtils.moveToDirectory file, targetDir, true

                    return true
                } catch (Exception e) {
                    log.error "Couldn't move files", e
                    return false
                }
            }
        }

        return false // Move was not possible
    }

    Boolean contentWillBeDeleted() {
        def path = getPathTo(this.parent)

        def parentContent = this.parent ? WcmContent.get(this.parent.id) : this.parent
        if (parentContent && (parentContent.class == WcmContentDirectory.class)) {
            parentContent.filesCount -= 1
            parentContent.children.remove(this)
            assert parentContent.save()
        }

        def file = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "${path}/${title}")
        if (!file.exists()) return true
        return FileUtils.deleteQuietly(file)
    }

    /** 
     * Get the url path to the specified content
     * This is not a filesystem-safe path - it must be processed to be platform specific
     * @see toFile
     */
    protected String getPathTo(WcmContent sourceContent) {
        def dirs = []

        while (sourceContent && (sourceContent instanceof WcmContentFile)) {
            dirs << sourceContent.aliasURI // @todo this should be aliasURI shouldn't it?
            sourceContent = sourceContent.parent
        }
        def path = dirs.reverse().join('/')

        return path ? "/${path}" : path
    }
    
    public def findBaseDirectory(){
        def baseDir = this
        while (baseDir.parent && (baseDir.parent instanceof WcmContentFile)){
            baseDir = baseDir.parent
        }
        return baseDir
    }
    
    public String toRelativePath() {
        return getPathTo(this)
    }
    
    /** 
     * Get filesystem path to file, IF and only if it is in the web-app folder.
     */
    File toFile() {
        org.weceem.services.WcmContentRepositoryService.getUploadPath(space, toRelativePath())
    }
}
