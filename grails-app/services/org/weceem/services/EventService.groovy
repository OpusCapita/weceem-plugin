package org.weceem.services

import org.weceem.content.Content

/*
 * A service to send events to hooks used by plugins/internally
*/

class EventService {

    static final String CONTENT_ADDED = 'onWeceemContentAdded'

    boolean transactional = false

    def grailsApplication
    def listenerCache = [:]

    /*
     * Called when new content is added
    */
    void contentAdded(Content content, params) {
        findListeners(CONTENT_ADDED)*.onWeceemContentAdded(content, params)
    }

    /*
    * Clear the listener cache
    */
    void emptyCache() {
        listenerCache = [:]
    }

    /*
     * Find places where a hook is used and cache the results
    */
    private List findListeners(String event) {
        if (!listenerCache[event]) {
            listenerCache[event] = grailsApplication.allArtefacts.findAll {it.metaClass.respondsTo(it, event)}
        }
        return listenerCache[event]
    }

}



