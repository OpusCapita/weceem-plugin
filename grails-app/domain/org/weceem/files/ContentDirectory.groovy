package org.weceem.files

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

/**
 * ContentDirectory.
 *
 * @author Sergei Shushkevich
 */
class ContentDirectory extends ContentFile {

    Integer filesCount = 0

    Boolean canHaveChildren() { true }

    Boolean create(Content parentContent) {
        def f
        if (parentContent && (parentContent instanceof ContentDirectory)) {
            def path = getPath(parentContent)
            def p = ServletContextHolder.servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}/${space.name}${path}/${title}")
            log.debug "Creating directory path [$p]"
            f = new File(p)
            def r = f.mkdirs()
        } else {
            def p = ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}/${title}")
            log.debug "Creating directory path [$p]"
            f = new File(p)
            def r = f.mkdirs()
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

    // we need to delete all content children here (recursively)
    Boolean deleteContent() {
        def path = getPath(this.parent)
        def file = new File(ServletContextHolder.servletContext.getRealPath(
                "/${DEFAULT_UPLOAD_DIR}/${space.name}${path}/${title}"))

        if (FileUtils.deleteQuietly(file)) {
            def childrenList = new ArrayList(this.children)
            childrenList?.each() { child ->
                if (child) {
                    // delete all virtual copies
                    def copies = VirtualContent.findAllWhere(target: child)
                    copies?.each() {
                        if (it.parent) {
                            chParent = Content.get(it.parent.id)
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
