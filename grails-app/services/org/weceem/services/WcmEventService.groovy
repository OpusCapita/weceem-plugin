package org.weceem.services

import org.weceem.content.WcmContent
import org.weceem.event.Events

/*
 * A service to send events to hooks used by plugins/internally
 * @author Scott Burch
*/

class WcmEventService {

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
    void afterContentAdded(WcmContent content) {
      getListeners(Events.AFTER_CONTENT_ADDED)*.afterWeceemContentAdded(content)
    }

    /*
     * Called after conent is updated
     */
    void afterContentUpdated(WcmContent content) {
      getListeners(Events.AFTER_CONTENT_UPDATED)*.afterWeceemContentUpdated(content)
    }

    void afterContentRemoved(WcmContent content) {
      getListeners(Events.AFTER_CONTENT_REMOVED)*.afterWeceemContentRemoved(content)
    }
}