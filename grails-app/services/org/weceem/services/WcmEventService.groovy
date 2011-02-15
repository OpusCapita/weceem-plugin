package org.weceem.services

import org.weceem.content.WcmContent
import org.weceem.event.WeceemEvents
import org.weceem.event.EventMethod

/*
 * A service to send events to hooks used by plugins/internally
 * @author Scott Burch
*/

class WcmEventService {

    boolean transactional = false

    private listeners = []

    /*
     * Register a listener with a callback method
     */
    void addListener(listener) {
        assert listener.conformsTo(WeceemEvents)
        listeners << listener
    }

    /*
     * Unregister a listener
     */
    void removeListener(listener) {
        listeners.remove(listener)
    }

    /**
     * Call the event handler on any listeners
     */
    void event(EventMethod event, WcmContent contentNode) {
        assert event.definedIn(WeceemEvents)
        
        if (log.debugEnabled) {
            log.debug "Triggering notification event: ${event} for ${contentNode.absoluteURI}"
        }
        
        def listenerList
        synchronized (listeners) {
            listenerList = listeners.toArray()
        }
        
        def eventName = event
        listenerList.each { l ->
            if (l.conformsTo(event)) {
                event.invokeOn(l, [contentNode])
            }
        }
    }
}
