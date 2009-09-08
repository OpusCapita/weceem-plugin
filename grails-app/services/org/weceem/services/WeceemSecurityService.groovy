package org.weceem.services

import org.weceem.content.Content
import org.weceem.content.Status

/**
 * A service that hides the actual security implementation we are using
 */
class WeceemSecurityService {
    static transactional = false
    
    /** 
     * Applications set this to an object implementing these methods
     */
    def securityDelegate = [
        getUserName : { -> "unknown" },
        getUserEmail : { -> "unknown@localhost" },
        getUserPrincipal : { -> [name:'unknown', email:"unknown@localhost"] },
        isUserAllowedContentStatus : { s -> true },
        isUserAllowedToEditContent : { c -> true },
        isUserAdministrator : { -> true }
    ]
    
    def getUserName() {
        securityDelegate.getUserName()
    }
    
    def getUserEmail() {
        securityDelegate.getUserEmail()
    }
    
    /**
     * Called to find out if the current user is allowed to transition content in to the specified status
     * Allows applications to control workflow
     */
    def isUserAllowedContentStatus(Status status) {
        securityDelegate.isUserAllowedContentStatus(status)
    }
    
    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    def isUserAllowedToEditContent(Content content) {
        securityDelegate.isUserAllowedToEditContent(content)
    }
    
    /**
     * Called to find out if the current user is allowed perform administrative actions eg manipulate spaces
     */
    def isUserAdministrator() {
        securityDelegate.isUserAdministrator()
    }
    
    def getUserPrincipal() {
        securityDelegate.getUserPrincipal()
    }
    
    
}