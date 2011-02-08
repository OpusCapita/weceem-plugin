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
        log.info "Loading security policy from script [$location]"
        def f = new File(location)
        def g = new GroovyClassLoader().parseClass(f)
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

    void setDefaultPermissionForSpaceAndRole(String perm, def permGranted, String alias, String role) {
        setPermissionForSpaceAndRole(DEFAULT_POLICY_URI, perm, permGranted, alias, role)
    }
    
    void dumpPermissions(Closure outputLine = { println it }) {
        entriesBySpace.each { spaceAlias, spaceURIEntries ->
            outputLine("Space: ${spaceAlias}")
            spaceURIEntries.each { uri, perms ->
                outputLine("       |--- URI: ${uri}")
                perms.each { role, permissions ->
                    outputLine("                     |-- ROLE: ${role}")
                    permissions.each { permission, grant ->
                        outputLine("                               |-- Permission: ${permission} - granted?: "+grant.settings)
                    }
                }
            }
        }
    }
    
    void setPermissionForSpaceAndRole(String key, String perm, def permGranted, String spaceAlias, String role) {
        if (log.debugEnabled) {
            log.debug "Adding permission to policy for space: ${spaceAlias}, uri: ${key}, permission: $perm = $permGranted for role $role"
        }
        // The data is stored as a graph of maps:
        //
        //   spaceAlias ====> uri1       ====> ROLE_USER  ====> create ====> [types:[WcmComment, WcmBlog]]
        //                                                      edit   ====> true
        //                                                      admin  ====> false
        //                    uri2/xxx   ====> ROLE_GUEST ====> create ====> [types:[WcmComment]]
        //                    uri3/xxx   ====> ROLE_ADMIN ====> admin  ====> true
        //
        def spaceEntries = entriesBySpace.get(spaceAlias, new TreeMap({ a, b -> b.compareTo(a) } as Comparator))
        def uriPerms = spaceEntries.get(key, [:])
        def permsForRole = uriPerms.get(role, [:])
        
        def permEvaluatingClosure
        switch (permGranted) {
            case Boolean: 
                permEvaluatingClosure = { args -> permGranted }; 
                break; 
            case Map: 
                if (permGranted.types) {
                    // Convert to a map of Type:Constraints map
                    def typesInfo = [:]
                    if (permGranted.types instanceof Map) {
                        typesInfo += permGranted.types
                    } else if (permGranted.types instanceof List) {
                        permGranted.types.each { t ->
                            typesInfo[t] = Collections.EMPTY_MAP
                        }
                    } else {
                        throw new IllegalArgumentException("The types argument for a permission must be a list of types or a map of type to map of property restrictions")
                    }
                    permEvaluatingClosure = { args -> 
                        println "Checking perm granted args ${args} - restriction info: ${typesInfo}"
                        if (args?.type) {
                            def ti = typesInfo[args.type]  
                            println "Checking perm ti: $ti"
                            if (ti != null) {
                                return (ti.size() == 0) || (args.content && ti.every { k, v -> 
                                    println "Checking $k/$v on ${args.content.dump()}"
                                    args.content[k] == v 
                                })
                            } else {
                                return false // not in permitted types list
                            }
                        }
                    } 
                }
                break
            default:
                throw new IllegalArgumentException("I don't understand the permission granted [$permGranted] - I only understand Maps and Booleans")
        }
        permsForRole[perm] = [granted:permEvaluatingClosure, settings:permGranted]
    }
    
    void setURIPermissionForSpaceAndRole(String uri, String perm, def permGranted, String spaceAlias, String role) {
        // Force trailing slash so a simple startsWith search works correctly
        if (!uri.endsWith('/')) {
            uri += '/'
        }
        setPermissionForSpaceAndRole(uri, perm, permGranted, spaceAlias, role)
    }

    /**
     * Find out if the permission name supplied is granted for the role, spaceAlias and uri
     * @param spaceAlias The url alias of the space you are checking
     * permissions for. If no permissions are found for this space, the "any"
     * permissions will be used
     * @param uri The uri the user needs permission to access. Will look up
     * the url path to find nearest ancestor with permissions, or default to
     * space's default permissions if no uri-specific permissions set for
     * ancestors of this uri
     * @param roleList The list of roles the user has
     * @param permissionList The list of permissions required to perform the action
     * @param args Optional map of arguments used for more detailed permissions access etc restriction by type
     */
    boolean hasPermission(String spaceAlias, String uri, List roleList, List permissionList, Map args = null ) {
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
        
        // If the uri is null this means we are checking for permissions to access the SPACE rather than a URI in the space
        // eg we use the default permissions for the space` 
        if (uri == null) {
            uri = DEFAULT_POLICY_URI
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
                // Get the permissions associated with this role for this URI candidate
                // These may be booleans or maps of args eg types list
                def permsForRole = uriPerms?.get(role)
                
                def explicitGrant
                permissionList.each { permission ->
                    def grant = permsForRole?.get(permission)
                    if (grant != null) {
                        if ( explicitGrant != null) { 
                            explicitGrant &= grant.granted(args)
                        } else {
                            explicitGrant = grant.granted(args)
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
        
        // See if we need to fallback to space defaults, there's nothing URI specific
        if ((explicitMatch == null) && (uri != DEFAULT_POLICY_URI)) {
            explicitMatch = hasPermission(spaceAlias, DEFAULT_POLICY_URI, roleList, permissionList)
        }

        // See if we need to fallback to global space defaults
        if ((explicitMatch == null) && (spaceAlias != ANY_SPACE_ALIAS)) {
            explicitMatch = hasPermission(ANY_SPACE_ALIAS, DEFAULT_POLICY_URI, roleList, permissionList)
        }
        return explicitMatch
    }
}