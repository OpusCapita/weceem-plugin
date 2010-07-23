package org.weceem.content

/**
 * Just a WcmFolder for organising other nodes
 */
class WcmFolder extends WcmContent {

    static transients = WcmContent.transients

    static standaloneContent = false
    
    static searchable = {
        alias WcmFolder.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }
    
}