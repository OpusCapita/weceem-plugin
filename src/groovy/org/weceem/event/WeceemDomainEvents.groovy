package org.weceem.event

import org.weceem.content.WcmContent

class WeceemDomainEvents {
    static events = {
        /* Called before a new node is created. Return true to allow the creation */
        contentShouldBeCreated { WcmContent parentConent -> }
        /* Called just before saving the node */
        contentWillBeCreated { WcmContent parentConent -> }
        /* Called after a node has been created, so that it can intialize anything that depends on it */
        contentDidGetCreated()


        /* Called to see if a node can be deleted. Return true to permit the deletion */
        contentShouldBeDeleted()
        /* Called before a node is deleted. */
        contentWillBeDeleted()
        /* Called after a node is deleted. */
        contentDidGetDeleted()
        
        /* Called after a node's title has changed (edited). */
        contentDidChangeTitle { String previousTitle -> }
        /* Called after a node is updated (edited). */
        contentDidGetUpdated()
        
        /* Called before a node is moved to a new parent. Return true to permit the move. Parent may be null */
        contentShouldMove { WcmContent newParent -> }
        /* Called after a node is moved to a new parent. */
        contentDidMove { String previousURL, WcmContent previousParent -> }

        /* Called to establish whether this node can have children. Return true to permit addition of children */
        contentShouldAcceptChildren()
        /* Called before a node is moved to become a child of this node. Return true to permit addition of the new child */
        contentShouldAcceptChild { WcmContent possibleChild -> }
    }
    
    static {
        EventManager.define(WeceemDomainEvents)
    }
}
