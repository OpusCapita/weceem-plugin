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
        only = ['title', 'status', 'space', 'aliasURI', 'parent']
    }

    // This cannot be rendered
    static standaloneContent = false
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "server-folder-32.png"]

    Integer filesCount = 0

    boolean contentShouldAcceptChildren() { true }
    
    boolean contentShouldAcceptChild(WcmContent newChild) { 
        newChild.instanceOf(WcmContentFile)
    }

    boolean contentWillBeCreated(WcmContent parentContent) {
        def p
        if (parentContent && (parentContent instanceof WcmContentDirectory)) {
            def path = getPathTo(parentContent)
            p = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "/$path/$aliasURI")
            log.debug "Creating directory path [$p]"
            p.mkdirs()
        } else if (!parentContent) {
            p = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, title)
            log.debug "Creating directory path [$p]"
            p.mkdirs()
        } else {
            throw new IllegalArgumentException("Cannot create server directory nodes under content of type ${parentContent.class}")
        }
        def done = p.exists() && p.isDirectory()
        if (done && parentContent) {
            parentContent.filesCount += 1
        }
        
        return done
    }

    boolean contentShouldBeCreated(WcmContent targetParent) {
        return targetParent ? targetParent instanceof WcmContentDirectory : true // Can move to root
    }

    boolean contentShouldMove(WcmContent targetParent) {
        return targetParent ? targetParent instanceof WcmContentDirectory : true // Can move to root
    }

    
    static editors = {
        title()
        fileSize(group:'extra', editor: 'ReadOnly')
        filesCount(group:'extra', editor: 'ReadOnly')
        aliasURI( editor: 'ReadOnly', group:'extra')
        fileMimeType(hidden:true)
        uploadedFile(hidden:true)
    }

    // Deny access
    static handleRequest = { content ->
        request.accessDeniedMessage = "Directory browsing not allowed"
        response.sendError(403, "Directory browsing not allowed")
        return null
    }
     
     
    // we need to delete all content children here (recursively)
    boolean contentWillBeDeleted() {
        def path = getPathTo(this.parent)
        def file = org.weceem.services.WcmContentRepositoryService.getUploadPath(space, "$path/$title")
        if (!file.exists()) return true
        if (FileUtils.deleteQuietly(file)) {
            def childrenList = this.children ? new ArrayList(this.children) : null
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
