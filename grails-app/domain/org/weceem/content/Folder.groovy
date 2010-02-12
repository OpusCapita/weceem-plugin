package org.weceem.content

/**
 * Just a Folder for organising other nodes
 */
class Folder extends Content {

    static transients = Content.transients

    static standaloneContent = false
    
    static searchable = {
        alias Folder.name.replaceAll("\\.", '_')
        only = ['title', 'status']
    }
    
}