package org.weceem.files

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

/**
 * WcmContentDirectory.
 *
 * @author Sergei Shushkevich
 */
class WcmContentDirectory extends WcmContentFile {

    static searchable = {
        alias WcmContentDirectory.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }

    Integer filesCount = 0

    Boolean canHaveChildren() { true }

    Boolean create(WcmContent parentContent) {
        def f
        if (parentContent && (parentContent instanceof WcmContentDirectory)) {
            def path = getPathTo(parentContent)
            def p = ServletContextHolder.servletContext.getRealPath(
                "/${WcmContentFile.DEFAULT_UPLOAD_DIR}/${(space.aliasURI == '') ? EMPTY_ALIAS_URI : space.aliasURI}${path}/${title}")
            log.debug "Creating directory path [$p]"
            f = new File(p)
            def r = f.mkdirs()
        } else {
            def p = ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${(space.aliasURI == '') ? EMPTY_ALIAS_URI : space.aliasURI}/${title}")
            log.debug "Creating directory path [$p]"
            f = new File(p)
            def r = f.mkdirs()
            assert r
        }
        return f.exists() && f.isDirectory()
    }

    static editors = {
        title()
        fileSize(group:'extra', editor: 'ReadOnly')
        filesCount(group:'extra', editor: 'ReadOnly')
        aliasURI( editor: 'ReadOnly', group:'extra')
        mimeType(hidden:true)
        uploadedFile(hidden:true)
    }

    // Deny access
    static handleRequest = { content ->
        request.accessDeniedMessage = "Directory browsing not allowed"
        response.sendError(403, "Directory browsing not allowed")
        return null
    }
     
     
    // we need to delete all content children here (recursively)
    Boolean deleteContent() {
        def path = getPathTo(this.parent)
        def file = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${(space.aliasURI == '') ? EMPTY_ALIAS_URI : space.aliasURI}${path}/${title}"))
        if (!file.exists()) return true
        if (FileUtils.deleteQuietly(file)) {
            def childrenList = new ArrayList(this.children)
            childrenList?.each() { child ->
                if (child) {
                    // delete all virtual copies
                    def copies = WcmVirtualContent.findAllWhere(target: child)
                    copies?.each() {
                        if (it.parent) {
                            chParent = WcmContent.get(it.parent.id)
                            chParent.children.remove(it)
                        }
                        it.delete()
                    }

                    // delete child from parent list of children
                    def chParent = child.parent
                    if (chParent) {
                        chParent.children.remove(child)
                        chParent.save()
                    }
                    child.delete()
                }
            }
            return true
        }
        return false
    }

}
