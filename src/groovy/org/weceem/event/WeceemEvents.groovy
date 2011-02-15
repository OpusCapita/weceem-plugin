package org.weceem.event

import org.weceem.content.WcmContent

class WeceemEvents {
    static events = {
        /* Called after a new node has been created */
        contentDidGetCreated { WcmContent node -> }

        /* Called before a node is deleted */
        contentWillBeDeleted { WcmContent node -> }
        /* Called after a node is deleted */
        contentDidGetDeleted { WcmContent node -> }
        
        /* Called after a new node is updated */
        contentDidGetUpdated { WcmContent node -> }
        
        /* Called after a new node is moved to a new parent */
        contentDidMove { WcmContent node -> }
    }
    
    static {
        EventManager.define(WeceemEvents)
    }
}
