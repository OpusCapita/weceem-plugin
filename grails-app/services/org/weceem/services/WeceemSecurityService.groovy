package org.weceem.services

import org.weceem.content.Content
import org.weceem.content.Status
import org.weceem.security.*

/**
 * A service that hides the actual security implementation we are using
 */
class WeceemSecurityService {
    static transactional = false
    
    WeceemSecurityPolicy policy
    def grailsApplication

    void loadPolicy() {
        def scriptLocation = grailsApplication.config.weceem.security.policy.path ?: System.getProperty('weceem.security.policy.path')
        if (scriptLocation) {
            policy.load(scriptLocation)
        } else poliy.initDefault()
    }
     
    
    /** 
     * Applications set this to an object implementing these methods
     */
    def securityDelegate = [
        getUserName : { -> "unknown" },
        getUserEmail : { -> "unknown@localhost" },
        getUserPrincipal : { -> [id:'unknown', name:'unknown', email:"unknown@localhost"] },
        getUserRoles: { -> ['ROLE_ADMIN'] }
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
        // Temporary lame impl
        return securityDelegate.userRoles?.contains('ROLE_ADMIN')
    }
    
    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    def isUserAllowedToEditContent(Content content) {
        // Temporary lame impl
        return securityDelegate.getUserRoles()?.contains('ROLE_ADMIN')
    }
    
    /**
     * Called to find out if the current user is allowed perform administrative actions eg manipulate spaces
     */
    def isUserAdministrator() {
        // Temporary lame impl
        return securityDelegate.userRoles?.contains('ROLE_ADMIN')
    }
    
    def getUserPrincipal() {
        securityDelegate.getUserPrincipal()
    }
    
    
}