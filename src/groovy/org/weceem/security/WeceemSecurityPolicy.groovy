package org.weceem.security

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

class WeceemSecurityPolicy {
    
    Log log = LogFactory.getLog(WeceemSecurityPolicy)
    
    private entriesBySpace = [:]
    
    static DEFAULT_POLICY_URI = "*/"
    static ANY_SPACE_ALIAS = "*"

    static ROLE_ADMIN = "ROLE_ADMIN"
    static ROLE_USER = "ROLE_USER"
    static ROLE_GUEST = "ROLE_GUEST"

    static PERMISSION_ADMIN = "admin"
    static PERMISSION_EDIT = "edit"
    static PERMISSION_CREATE = "create"
    static PERMISSION_DELETE = "delete"
    static PERMISSION_VIEW = "view"
        
    void load(location) {
        def g = new GroovyClassLoader().parseClass(new File(location))
        def script = g.newInstance()
        assert script instanceof Script
        script.binding = new Binding()
        script.run()

        // Get the closure
        Closure policy = script.binding.policy 
        if (!policy) {
            log.error "No policy closure found in script [$location], giving up"
            throw new RuntimeException("Path to a security policy was specified, but policy did not provide any usable info.")
        }

        // Get a graph of space>uri>permissions defined by this closure
        def policyBuilder = new SecurityPolicyBuilder(this)

        // Now call the policy closure, delegating to the builder
        policy.delegate = policyBuilder
        policy.resolveStrategy = Closure.DELEGATE_FIRST
        policy.call()
    }

    void initDefault() {
        log.info "Initializing default security policy"
        
        setDefaultPermissionForSpaceAndRole(PERMISSION_ADMIN, true, ANY_SPACE_ALIAS, ROLE_ADMIN)
        setDefaultPermissionForSpaceAndRole(PERMISSION_EDIT, true, ANY_SPACE_ALIAS, ROLE_ADMIN)
        setDefaultPermissionForSpaceAndRole(PERMISSION_VIEW, true, ANY_SPACE_ALIAS, ROLE_ADMIN)
        setDefaultPermissionForSpaceAndRole(PERMISSION_CREATE, true, ANY_SPACE_ALIAS, ROLE_ADMIN)
        setDefaultPermissionForSpaceAndRole(PERMISSION_DELETE, true, ANY_SPACE_ALIAS, ROLE_ADMIN)
        
        setDefaultPermissionForSpaceAndRole(PERMISSION_EDIT, true, ANY_SPACE_ALIAS, ROLE_USER)
        setDefaultPermissionForSpaceAndRole(PERMISSION_VIEW, true, ANY_SPACE_ALIAS, ROLE_USER)
        setDefaultPermissionForSpaceAndRole(PERMISSION_CREATE, true, ANY_SPACE_ALIAS, ROLE_USER)
        setDefaultPermissionForSpaceAndRole(PERMISSION_DELETE, true, ANY_SPACE_ALIAS, ROLE_USER)
        
        setDefaultPermissionForSpaceAndRole(PERMISSION_EDIT, false, ANY_SPACE_ALIAS, ROLE_GUEST)
        setDefaultPermissionForSpaceAndRole(PERMISSION_VIEW, true, ANY_SPACE_ALIAS, ROLE_GUEST)
        setDefaultPermissionForSpaceAndRole(PERMISSION_CREATE, false, ANY_SPACE_ALIAS, ROLE_GUEST)
    }

    void setDefaultPermissionForSpaceAndRole(String perm, boolean permGranted, String alias, String role) {
        setPermissionForSpaceAndRole(DEFAULT_POLICY_URI, perm, permGranted, alias, role)
    }
    
    void setPermissionForSpaceAndRole(String key, String perm, boolean permGranted, String alias, String role) {
        if (log.debugEnabled) {
            log.debug "Adding permission to policy for space: ${alias}, uri: ${key}, permission: $perm = $permGranted for role $role"
        }
        def spaceEntries = entriesBySpace.get(alias, new TreeMap({ a, b -> b.compareTo(a) } as Comparator))
        def uriPerms = spaceEntries.get(key, [:])
        def permsForRole = uriPerms.get(role, [:])
        
        permsForRole[perm] = permGranted
    }
    
    void setURIPermissionForSpaceAndRole(String uri, String perm, boolean permGranted, String alias, String role) {
        // Force trailing slash so a simple startsWith search works correctly
        if (!uri.endsWith('/')) {
            uri += '/'
        }
        setPermissionForSpaceAndRole(uri, perm, permGranted, alias, role)
    }
    
    /**
     * Find out if the permission name supplied is granted for the role, spaceAlias and uri
     */
    boolean hasPermission(String spaceAlias, String uri, List roleList, List permissionList ) {
        def spaceEntries = entriesBySpace[spaceAlias]
        if (log.debugEnabled) {
            log.debug "Found policy entries for space [$spaceAlias]: $spaceEntries"
        }
        if (!spaceEntries) {
            spaceEntries = entriesBySpace[ANY_SPACE_ALIAS]
            if (log.debugEnabled) {
                log.debug "Using policy entries for 'any' space as [$spaceAlias] has none defined: $spaceEntries"
            }
            if (!spaceEntries) {
                log.warn "No permissions set for space with alias [$spaceAlias], and no default space permissions set"
                return false // No permissions set for the "any" space
            }
        }
        
        if (!uri.endsWith('/')) {
            uri += '/'
        }
        
        // Assume these are sorted in descending order so that we find longest matches first
        def uriPermCandidates = (spaceEntries?.findAll { k, v ->
            (k == DEFAULT_POLICY_URI) || uri.startsWith(k)
        })

        if (log.debugEnabled) {
            log.debug "Found policy permissions that could apply for uri [$uri]: $uriPermCandidates"
        }

        def explicitMatch
        uriPermCandidates*.value.find { uriPerms ->
            roleList.find { role ->
                def permsForRole = uriPerms?.get(role)
                
                def explicitGrant
                permissionList.each { permission ->
                    def grant = permsForRole?.get(permission)
                    if (grant != null) {
                        if ( explicitGrant != null) { 
                            explicitGrant &= grant
                        } else {
                            explicitGrant = grant
                        }
                    }
                }
                
                if (explicitGrant != null) {
                    explicitMatch = explicitGrant
                    return true
                } else {
                    return false
                }
            }
            // We stop the find as soon as we have something that is an actual true/false vs a null
            return explicitMatch != null 
        }
        
        // See if we need to fallback to space defaults
        if ((explicitMatch == null) && (uri != DEFAULT_POLICY_URI)) {
            explicitMatch = hasPermission(spaceAlias, DEFAULT_POLICY_URI, roleList, permissionList)
        }
        return explicitMatch
    }
}