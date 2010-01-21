package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import org.weceem.content.Content
import org.weceem.content.Status
import org.weceem.content.Space
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
        def loc = grailsApplication.config.weceem.security.policy.path
        if (!(loc instanceof String)) {
            loc = null
        }
        def scriptLocation = loc ?: System.getProperty('weceem.security.policy.path')
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
    
    def getUserRoles() {
        def roles = securityDelegate.getUserRoles().clone()
        roles << ["USER_${userName}"]
        return roles
    }

    boolean hasPermissions(Space space, permList, Class<Content> type = null) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on space $space"
        }
        // Look at changing this so absoluteURI is not recalculated every time
        return policy.hasPermission(
            space.aliasURI, 
            null,  
            getUserRoles(), 
            permList)
    }

    boolean hasPermissions(Content content, permList, Class<Content> type = null) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on content at ${content.aliasURI}"
        }
        // Look at changing this so absoluteURI is not recalculated every time
        return policy.hasPermission(
            content.space.aliasURI, 
            content.absoluteURI, 
            getUserRoles(), 
            permList)
    }

    boolean hasPermissions(Space space, String uri, permList, Class<Content> type = null) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on content at ${uri}"
        }
        return policy.hasPermission(
            space.aliasURI, 
            uri, 
            getUserRoles(), 
            permList)
    }
    
    /**
     * Called to find out if the current user is allowed to transition content in to the specified status
     * Allows applications to control workflow
     */
    boolean isUserAllowedContentStatus(Status status) {
        // Temporary lame impl, need to add this to policy
        return true
    }

    /**
     * Called to find out if the current user is allowed to create the specified content type under the
     * specified content node.
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToCreateContent(Content parent, Class<Content> type) {
        hasPermissions(parent, [WeceemSecurityPolicy.PERMISSION_CREATE], type)
    }

    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToDeleteContent(Content content) {
        hasPermissions(content, [WeceemSecurityPolicy.PERMISSION_DELETE])
    }
    
    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToEditContent(Content content) {
        hasPermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])
    }
    
    /**
     * Called to find out if the current user is allowed to view the specified content
     * IF the status is not "public" they must also have the EDIT permission
     */
    boolean isUserAllowedToViewContent(Content content) {
        // Now work out if the user is allowed to see the content
        def allowedToViewContent = false
        def permsRequired = [WeceemSecurityPolicy.PERMISSION_VIEW]
        // If the status is not a "published" status then only those with edit permissions
        // on the url can see it
        if (!content.status.publicContent) {
            permsRequired << WeceemSecurityPolicy.PERMISSION_EDIT
        }
        if (log.debugEnabled) {
            log.debug "User requires permissions $permsRequired to view content ${content.absoluteURI}"
        }
        allowedToViewContent = hasPermissions(content, permsRequired)
        if (!allowedToViewContent && log.debugEnabled) {
            log.debug "User is not denied viewing of content at ${content.absoluteURI}"
        }
        return allowedToViewContent
    }
    
    /**
     * Called to find out if the current user is allowed perform administrative actions eg manipulate spaces
     */
    boolean isUserAdministrator() {
        hasPermissions(content, [WeceemSecurityPolicy.PERMISSION_ADMIN])
    }
    
    def getUserPrincipal() {
        securityDelegate.getUserPrincipal()
    }
    
    
}