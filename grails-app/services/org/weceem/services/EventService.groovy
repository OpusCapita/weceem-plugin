package org.weceem.services

import org.weceem.content.Content
import org.weceem.event.Events

/*
 * A service to send events to hooks used by plugins/internally
*/

class EventService {

    boolean transactional = false

    private listeners = [:]

    /* get listeners for an event */
    private getListeners(Events event) {
      listeners[event] ?: []
    }

    /*
    * Register a listener with a callback method
     */
    void registerListener(Events event, listener) {
      listeners[event] = getListeners(event) << listener
    }

    /*
    * Unregister a listener
     */
    void unregisterListener(Events event, listener) {
      getListeners(event).remove(listener)
    }


  /************************************
   **** Place events here
   **********************************/

    /*
     * Called after new content is added
    */
    void afterContentAdded(Content content, params) {
      getListeners(Events.AFTER_CONTENT_ADDED)*.afterWeceemContentAdded(content, params)
    }

    /*
     * Called after conent is updated
     */
    void afterContentUpdated(Content content, params) {
      getListeners(Events.AFTER_CONTENT_UPDATED)*.afterWeceemContentUpdated(content, params)
    }
}
