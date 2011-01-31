package org.weceem.event

import org.weceem.content.WcmContent

class WeceemDomainEvents {
    final static protocol = {
        contentDidGetCreated()

        contentWillBeDeleted()
        contentDidGetDeleted()
        
        contentDidGetUpdated()
        
        contentDidMove()

        contentShouldBeCreated(WcmContent /* newNode */)
        contentShouldBeDeleted()

        contentDidChangeTitle(String /* previousTitle */)

        contentShouldMove(WcmContent)

        contentShouldAcceptChildren()
        contentShouldAcceptChild(WcmContent /* possibleChild */)
    }
    
    static {
        EventManager.define(WeceemDomainEvents)
    }
}
