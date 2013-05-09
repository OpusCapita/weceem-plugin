package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import org.weceem.content.WcmContent
import org.weceem.content.WcmStatus
import org.weceem.content.WcmSpace
import org.weceem.security.*

/**
 * A service that hides the actual security implementation we are using
 */
class WcmSecurityService implements InitializingBean {
    static transactional = false
    
    def policy = new WeceemSecurityPolicy()
    
    def grailsApplication
    def proxyHandler

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
        def roles = []
        roles.addAll(securityDelegate.getUserRoles())
        roles << "USER_${userName}"
        return roles
    }

    boolean hasPermissions(WcmSpace space, permList, Class<WcmContent> type = null) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on space $space"
        }
        // Look at changing this so absoluteURI is not recalculated every time
        return policy.hasPermission(
            space.aliasURI, 
            null,  
            getUserRoles(), 
            permList,
            [type:type])
    }

    boolean hasPermissions(WcmContent content, permList) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on content at ${content.aliasURI}"
        }
        // Look at changing this so absoluteURI is not recalculated every time
        return policy.hasPermission(
            content.space.aliasURI, 
            content.absoluteURI, 
            getUserRoles(), 
            permList,
            [type:proxyHandler.unwrapIfProxy(content).class, content:content])
    }

    boolean hasPermissions(WcmSpace space, String uri, permList, Class<WcmContent> type = null) {
        if (log.debugEnabled) {
            log.debug "Checking if user $userName with roles $userRoles has permissions $permList on content at ${uri}"
        }
        return policy.hasPermission(
            space.aliasURI, 
            uri, 
            getUserRoles(), 
            permList,
            [type:type])
    }
    
    /**
     * Called to find out if the current user is allowed to transition content in to the specified status
     * Allows applications to control workflow
     */
    boolean isUserAllowedContentStatus(WcmStatus status) {
        // Temporary lame impl, need to add this to policy
        return true
    }

    /**
     * Called to find out if the current user is allowed to create the specified content type under the
     * specified content node.
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToCreateContent(WcmSpace space, WcmContent parent, WcmContent proposedContent) {
        def uri = parent ? parent.absoluteURI : ''
        return hasPermissions(space, uri ? uri+'/'+proposedContent.aliasURI : proposedContent.aliasURI, [WeceemSecurityPolicy.PERMISSION_CREATE], proposedContent.class)
    }

    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToDeleteContent(WcmContent content) {
        hasPermissions(content, [WeceemSecurityPolicy.PERMISSION_DELETE])
    }
    
    /**
     * Called to find out if the current user is allowed to edit the specified content
     * Allows applications to implement ACLs
     */
    boolean isUserAllowedToEditContent(WcmContent content) {
        hasPermissions(content, [WeceemSecurityPolicy.PERMISSION_EDIT])
    }
    
    /**
     * Called to find out if the current user is allowed to view the specified content
     * IF the status is not "public" they must also have the EDIT permission
     */
    boolean isUserAllowedToViewContent(WcmContent content) {
        // Now work out if the user is allowed to see the content
        def allowedToViewContent = false
        def permsRequired = [WeceemSecurityPolicy.PERMISSION_VIEW]
        // If the status is not a "published" status then only those with edit permissions
        // on the url can see it
        if (!content.status.publicContent) {
            permsRequired = [WeceemSecurityPolicy.PERMISSION_EDIT]
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
    boolean isUserAdministrator(WcmSpace space) {
        hasPermissions(space, [WeceemSecurityPolicy.PERMISSION_ADMIN])
    }
    
    def getUserPrincipal() {
        securityDelegate.getUserPrincipal()
    }
    
    
}