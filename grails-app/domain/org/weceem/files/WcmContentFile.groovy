    package org.weceem.files

import org.springframework.web.multipart.MultipartFile
import org.apache.commons.io.FileUtils

import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

import org.weceem.util.MimeUtils

/**
 * WcmContentFile.
 *
 * @author Sergei Shushkevich
 */
class WcmContentFile extends WcmContent {

    static searchable = {
        only = ['title', 'status', 'space', 'aliasURI', 'parent']
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

    void setUploadedFile(MultipartFile file) {
        this.@uploadedFile = file
        if (!title) {
            title = file.originalFilename
        }
        aliasURI = file.originalFilename
    }
    
    void createAliasURI(parent) {
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

    boolean contentShouldBeCreated(WcmContent parentContent) {
        if (parentContent == null) {
            return false // cannot be at root
        } else {
            return true
        }
    }

    /**
     * Handle the create event to copy the file from the upload form into the filesystem
     * Files are *not* stored in the repository database
     */
    boolean contentWillBeCreated(WcmContent parentContent) {
        
        def path = ''
        if (parentContent instanceof WcmContentDirectory) {
            assert parentContent.save()
            path = getPathTo(parentContent)
            //@todo surely this is redundant, we can just count children?
            parentContent.filesCount += 1
        }

        def wcmContentRepositoryService =  this.domainClass.grailsApplication
                .mainContext.wcmContentRepositoryService

        wcmContentRepositoryService.getUploadPath(space, path).mkdirs()
        
        if (uploadedFile) {
            def f = wcmContentRepositoryService.getUploadPath(space, "$path/${uploadedFile.originalFilename}")
            uploadedFile.transferTo(f)
            fileMimeType = uploadedFile.contentType ?: MimeUtils.getDefaultMimeType(uploadedFile.originalFilename)
            fileSize = f.length()
        }
    }

    boolean contentDidChangeTitle(String oldTitle) {
        def path = ''
        def parent = this.parent
        if (parent && (parent instanceof WcmContentDirectory)) {
            path = getPathTo(parent)
        }
        def wcmContentRepositoryService =  this.domainClass.grailsApplication
                .mainContext.wcmContentRepositoryService
        def oldFile = wcmContentRepositoryService.getUploadPath(space, "${path}/${oldTitle}")
        def newFile = wcmContentRepositoryService.getUploadPath(space, "${path}/${title}")
        oldFile.renameTo(newFile)
        // Update our URI to match new file
        aliasURI = this.title 
    }

    boolean contentShouldMove(WcmContent targetParent) {
        return targetParent ? targetParent instanceof WcmContentDirectory : false
    }
    
    void contentDidMove(String originalURI, WcmContent originalParent) {
        if (originalParent?.ident() == parent?.ident()) {
            if (log.debugEnabled) {
                log.debug "Not moving server file ${originalURI} because parent has not changed"
            }
            return
        }
        
        def srcPath = originalURI
        def dstPath = ''

        // Increment file counter for new parent (really need this?)
        if (parent && !(this instanceof WcmContentDirectory)) {
            parent.filesCount += 1
            assert parent.save()
        }
        
        // Decrement file counter for previous parent (really need this?)
        if (originalParent) {
            originalParent.filesCount -= 1
            assert originalParent.save()
        }
        def wcmContentRepositoryService =  this.domainClass.grailsApplication
                .mainContext.wcmContentRepositoryService
        def file = wcmContentRepositoryService.getUploadPath(space, srcPath)
        def targetDir = parent ? parent.toFile() : toFile()
        log.info "Moving file ${file} to ${targetDir}"
        try {
            FileUtils.moveToDirectory file, targetDir, true

        } catch (Exception e) {
            log.error "Couldn't move files", e
        }
    }

    boolean contentWillBeDeleted() {
        def path = getPathTo(this.parent)

        def parentContent = this.parent ? WcmContent.get(this.parent.id) : this.parent
        if (parentContent && (proxyHandler.unwrapIfProxy(parentContent).class == WcmContentDirectory.class)) {
            parentContent.filesCount -= 1
            parentContent.children.remove(this)
            assert parentContent.save()
        }
        def wcmContentRepositoryService =  this.domainClass.grailsApplication
                .mainContext.wcmContentRepositoryService
        def file = wcmContentRepositoryService.getUploadPath(space, "${path}/${title}")
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
        def wcmContentRepositoryService =  this.domainClass.grailsApplication
                .mainContext.wcmContentRepositoryService
        wcmContentRepositoryService.getUploadPath(space, toRelativePath())
    }

    @Override 
    Date getLastModified() {
        new Date(toFile().lastModified())
    }
}
