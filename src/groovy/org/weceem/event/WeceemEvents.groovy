package org.weceem.event

import org.weceem.content.WcmContent

class WeceemEvents {
    static events = {
        contentDidGetCreated { WcmContent node -> }

        contentWillBeDeleted { WcmContent node -> }
        contentDidGetDeleted { WcmContent node -> }
        
        contentDidGetUpdated { WcmContent node -> }
        
        contentDidMove { WcmContent node -> }
    }
    
    static {
        EventManager.define(WeceemEvents)
    }
}
