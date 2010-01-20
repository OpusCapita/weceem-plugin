package org.weceem.files

import org.springframework.web.multipart.MultipartFile
import org.apache.commons.io.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

/**
 * ContentFile.
 *
 * @author Sergei Shushkevich
 */
class ContentFile extends Content {

    static final String EMPTY_ALIAS_URI = "_ROOT"
    
    static final String DEFAULT_UPLOAD_DIR = 'WeceemFiles'

    // @todo This needs to be configurable
    static final MIME_TYPES = [
        html: 'text/html',
        htm: 'text/html',
        xml: 'text/xml',
        txt: 'text/plain',
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        css: 'text/css',
        csv: 'text/csv',
        json: 'application/json',
        pdf: 'application/pdf',
        doc: 'application/msword',
        png: 'image/png',
        gif: 'image/gif',
        jpg: 'image/jpeg',
        jpeg: 'image/jpeg',
        swf: 'application/x-shockwave-flash',
        mov: 'video/quicktime',
        qt: 'video/quicktime',
        avi: 'video/x-msvideo',
        asf: 'video/x-ms-asf',
        asr: 'video/x-ms-asf',
        asx: 'video/x-ms-asf',
        mpa: 'video/mpeg',
        mpg: 'video/mpeg',
        mp2: 'video/mpeg',
        rtf: 'application/rtf',
        exe: 'application/octet-stream',
        xls: 'application/vnd.ms-excel',
        xlt: 'application/vnd.ms-excel',
        xlc: 'application/vnd.ms-excel',
        xlw: 'application/vnd.ms-excel',
        xla: 'application/vnd.ms-excel',
        xlm: 'application/vnd.ms-excel',
        ppt: 'application/vnd.ms-powerpoint',
        pps: 'application/vnd.ms-powerpoint',
        tgz: 'application/x-compressed',
        gz: 'application/x-gzip',
        zip: 'application/zip',
        mp3: 'audio/mpeg',
        mid: 'audio/mid',
        ico: 'image/x-icon'
    ]

    String mimeType
    Long fileSize = 0
    Short syncStatus = 0

    MultipartFile uploadedFile

    static transients = (Content.transients - 'mimeType') + 'uploadedFile'

    static constraints = {
        // @todo this is ugly, ContentDirectory should never have one, and all files SHOULD
        mimeType(nullable:true, blank:true)
    }

    static editors = {
        title()
        aliasURI( editor: 'ReadOnly', group: 'extra')
        uploadedFile(editor:'ContentFileUpload')
        mimeType(group:'extra')
        fileSize(group:'extra', editor: 'ReadOnly')
        status()
        syncStatus(hidden: true)
    }

    public void createAliasURI(parent = null) {
        if (aliasURI != title){
            aliasURI = title
        }
    }

    static getDefaultMimeType(String fileName) {
        def dotpos = fileName.indexOf('.')
        if (dotpos >= 0) {
            def ext = fileName[dotpos+1..-1]
            return MIME_TYPES[ext.toLowerCase()] ?: 'application/octet-stream'
        } else {
            return "text/plain"
        }
    }
    
    // Get the servlet container to serve the file
    static handleRequest = { content ->
        def mt = content.mimeType ?: getDefaultMimeType(content.aliasURI)
        response.setContentType(mt)
        renderAppResource(content.toResourcePath())
    }
    
    Boolean canHaveChildren() { false }

    Boolean create(Content parentContent) {
        if (!title) {
            title = uploadedFile.originalFilename
            createAliasURI()
        }
        assert title
        def path = ''
        if (parentContent && (parentContent instanceof ContentDirectory)) {
            //@todo surely this is redundant, we can just count children?
            parentContent.filesCount += 1
            assert parentContent.save()
            path = getPathTo(parentContent)
        }
        new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}")).mkdirs()
        try {
            def f = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}/${title}"))
            uploadedFile.transferTo(f)
            mimeType = uploadedFile.contentType
            fileSize = f.length()
        } catch (Exception e) {
            return false
        }

        return true
    }

    Boolean rename(String oldTitle) {
        def path = ''
        def parent = this.parent
        if (parent && (parent instanceof ContentDirectory)) {
            path = getPathTo(parent)
        }
        def oldFile = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}/${oldTitle}"))
        def newFile = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}/${title}"))
        oldFile.renameTo(newFile)
    }

    Boolean move(Content targetParent) {
        if (!targetParent || (targetParent instanceof ContentDirectory)) {

            def srcPath = ''
            def dstPath = ''

            if (targetParent && !(this instanceof ContentDirectory)) {
                targetParent.filesCount += 1
                assert targetParent.save()
            }
            dstPath = getPathTo(targetParent)
            if (this.parent instanceof ContentDirectory) {
                this.parent.filesCount -= 1
                assert this.parent.save()
            } 
            srcPath = getPathTo(this)
            if (srcPath || dstPath) {
                def file = new File(ServletContextHolder.servletContext.getRealPath(
                        "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}/${srcPath}"))
                def targetDir = new File(ServletContextHolder.servletContext.getRealPath(
                        "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}/${dstPath}"))
                try {
                    FileUtils.moveToDirectory file, targetDir, true
                } catch (Exception e) {
                    return false
                }
            }
        }

        return true
    }

    Boolean deleteContent() {
        def path = getPathTo(this.parent)

        def parentContent = this.parent ? Content.get(this.parent.id) : this.parent
        if (parentContent && (parentContent.class == ContentDirectory.class)) {
            parentContent.filesCount -= 1
            parentContent.children.remove(this)
            assert parentContent.save()
        }

        def file = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}/${title}"))
        if (!file.exists()) return true
        return FileUtils.deleteQuietly(file)
    }

    /** 
     * Get the url path to the specified content
     * This is not a filesystem-safe path - it must be processed to be platform specific
     * @see toFile
     */
    protected String getPathTo(Content sourceContent) {
        def dirs = []

        while (sourceContent && (sourceContent instanceof ContentFile)) {
            dirs << sourceContent.title // @todo this should be aliasURI shouldn't it?
            sourceContent = sourceContent.parent
        }
        def path = dirs.reverse().join('/')

        return path ? "/${path}" : path
    }
    
    public def findBaseDirectory(){
        def baseDir = this
        while (baseDir.parent && (baseDir.parent instanceof ContentFile)){
            baseDir = baseDir.parent
        }
        return baseDir
    }
    
    public String toRelativePath() {
        return getPathTo(this)
    }
    
    /**
     * Get the resource path IF and only if it is in the web-app folder.
     * @todo need to remove this later when files are not under webapp, we will have to just 
     * use File and copy the response to the user
     */
    public String toResourcePath() {
        def path = toRelativePath()
        return "/${DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}${path}"
    }
    
    /** 
     * Get filesystem path to file, IF and only if it is in the web-app folder.
     */
    File toFile() {
        new File(ServletContextHolder.servletContext.getRealPath(toResourcePath()))
    }
}
