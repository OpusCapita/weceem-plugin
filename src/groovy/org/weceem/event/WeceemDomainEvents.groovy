package org.weceem.event

import org.weceem.content.WcmContent

class WeceemDomainEvents {
    static events = {
        contentDidGetCreated()

        contentWillBeDeleted()
        contentDidGetDeleted()
        
        contentDidGetUpdated()
        
        contentDidMove()

        contentShouldBeCreated { WcmContent newNode -> }
        contentShouldBeDeleted()

        contentDidChangeTitle { String previousTitle -> }

        contentShouldMove { WcmContent newParent -> }

        contentShouldAcceptChildren()
        contentShouldAcceptChild { WcmContent possibleChild -> }
    }
    
    static {
        EventManager.define(WeceemDomainEvents)
    }
}
