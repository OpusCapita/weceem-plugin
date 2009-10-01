package org.weceem.services

import org.weceem.content.Content
import org.weceem.content.Status

/**
 * A service that hides the actual security implementation we are using
 */
class WeceemSecurityService {
    static transactional = false
    
    def policyInfo
    def grailsApplication
    
    /** 
     * Policy is just a DSL like this:
     
     'role:Admin' {
         spaces '*'
         administer true
     }
     
     'group:MillerLtd_User' {
          spaces 'miller', 'miller_intranet'
          administer false
          '/miller/home' {
              createContent true
              editContent true
          }
          '/miller_intranet/customers' {
              editContent true
          }
      }
     
     * so it takes the form of:
     * 
     * ROLESTRING {
     *     spaces '*' or list of aliasURIs stringd
     *     permissionName true/false
     *     URIPATH {
     *          permissionName true/false    
     *     }
     *     URIPATH {
     *          permissionName true/false    
     *     }
     * }
     *
     * However we need to resolve permissions by perm name + uripath + space, and support inheriting perms into 
     * subnodes of the uri space, so we need to store this differently internally
     *
     * We need to access permissions so:
     *     userHasPermission(permission, space, uri)
     * which means seeing if any of their roles has the permission on space+uri
     *
     * space ---> uri ---> permissions ---> map of roles permitted
     *
     void loadPolicy() {
         def scriptLocation = grailsApplication.config.weceem.security.policy.path ?: System.getProperty('weceem.security.policy.path')
         def g = new GroovyClassLoader().parseClass(scriptLocation)
     }
     */
     
    
    /** 
     * Applications set this to an object implementing these methods
     */
    def securityDelegate = [
        getUserName : { -> "unknown" },
        getUserEmail : { -> "unknown@localhost" },
        getUserPrincipal : { -> [name:'unknown', email:"unknown@localhost"] },
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