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

    public static final String DEFAULT_UPLOAD_DIR = 'WeceemFiles'

    String mimeType
    Long fileSize = 0
    Short syncStatus = 0

    MultipartFile uploadedFile

    static transients = ['uploadedFile']

    static constraints = {
        // @todo this is ugly, ContentDirectory should never have one, and all files SHOULD
        mimeType(nullable:true, blank:true)
    }

    static editors = {
        title( editor: 'ReadOnly')
        uploadedFile(editor:'ContentFileUpload')
        mimeType(group:'extra')
        fileSize(group:'extra', editor: 'ReadOnly')
        status()
        syncStatus(hidden: true)
    }

    public void createAliasURI(parent = null) {
        aliasURI = title.replaceAll(INVALID_ALIAS_URI_CHARS_PATTERN, '-')
    }

    Boolean canHaveChildren() { false }

    Boolean create(Content parentContent) {
        if (!title) {
            title = uploadedFile.originalFilename
        }
        assert title
        def path = ''
        if (parentContent && (parentContent instanceof ContentDirectory)) {
            //@todo surely this is redundant, we can just count children?
            parentContent.filesCount += 1
            assert parentContent.save()
            path = getPath(parentContent)
        }
        new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}")).mkdirs()
        try {
            def f = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}/${title}"))
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
            path = getPath(parent)
        }
        def oldFile = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}/${oldTitle}"))
        def newFile = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}/${title}"))
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
            dstPath = getPath(targetParent)
            if (this.parent instanceof ContentDirectory) {
                this.parent.filesCount -= 1
                assert this.parent.save()
            } 
            srcPath = getPath(this)
            if (srcPath || dstPath) {
                def file = new File(ServletContextHolder.servletContext.getRealPath(
                        "/${DEFAULT_UPLOAD_DIR}/${space.name}/${srcPath}"))
                def targetDir = new File(ServletContextHolder.servletContext.getRealPath(
                        "/${DEFAULT_UPLOAD_DIR}/${space.name}/${dstPath}"))
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
        def path = getPath(this.parent)

        def parentContent = this.parent ? Content.get(this.parent.id) : this.parent
        if (parentContent && (parentContent.class == ContentDirectory.class)) {
            parentContent.filesCount -= 1
            parentContent.children.remove(this)
            assert parentContent.save()
        }

        def file = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}/${title}"))
        if (!file.exists()) return true
        return FileUtils.deleteQuietly(file)
    }

    protected String getPath(Content sourceContent) {
        def dirs = []

        while (sourceContent && (sourceContent instanceof ContentFile)) {
            dirs << sourceContent.title
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
    
    public String toRelativePath(){
        return getPath(this)
    }
}
