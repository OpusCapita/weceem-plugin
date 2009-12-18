package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import org.weceem.content.Content
import org.weceem.content.Status
import org.weceem.security.*

/**
 * A service that hides the actual security implementation we are using
 */
class WeceemSecurityService implements InitializingBean {
    static transactional = false
    
    WeceemSecurityPolicy policy = new WeceemSecurityPolicy()
    
    def grailsApplication

    void afterPropertiesSet() {
        loadPolicy()
    }
    
    void loadPolicy() {
        def scriptLocation = grailsApplication.config.weceem.security.policy.path ?: System.getProperty('weceem.security.policy.path')
        if (scriptLocation) {
            policy.load(scriptLocation)
        } else policy.initDefault()
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

    protected hasPermission(Content content, perm) {
        // Look at changing this so absoluteURI is not recalculated every time
        return policy.hasPermission(
            content.space.aliasURI, 
            content.absoluteURI, 
            securityDelegate.getUserRoles(), 
            perm)
    }

    /**
     * Called to find out if the current user is allowed to transition content in to the specified status
     * Allows applications to control workflow
     */
    def isUserAllowedContentStatus(Status status) {
        // Temporary lame impl, need to add this to policy
        return true
    }
    
    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    def isUserAllowedToEditContent(Content content) {
        hasPermission(content, WeceemSecurityPolicy.PERMISSION_EDIT)
    }
    
    /**
     * Called to find out if the current user is allowed perform administrative actions eg manipulate spaces
     */
    def isUserAdministrator() {
        hasPermission(content, WeceemSecurityPolicy.PERMISSION_ADMIN)
    }
    
    def getUserPrincipal() {
        securityDelegate.getUserPrincipal()
    }
    
    
}