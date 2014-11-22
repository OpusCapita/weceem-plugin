package org.weceem.content

/**
 * Just a WcmFolder for organising other nodes
 */
class WcmFolder extends WcmContent {

    static transients = WcmContent.transients

    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "folder-32.png"]

    static standaloneContent = false
    
    static searchable = {
        only = ['title', 'status', 'space', 'aliasURI', 'parent']
    }
    
}