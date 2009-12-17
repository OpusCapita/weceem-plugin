package org.weceem.security

class SecurityPermissionsBuilder {
    static PERM_CLAUSE_SPACE = "space"
    
    boolean topLevel
    def policy
    def spaceAliases 
    
    // ctor variant for top level usage
    SecurityPermissionsBuilder(role, policy) {
        this.topLevel = true
        this.role = role
        this.policy = policy
    }
    
    // ctor variant for nested usage
    SecurityPermissionsBuilder(SecurityPermissionsBuilder parent) {
        this(parent.role, parent.policy)
        this.topLevel = false
        this.uri = parent.uri
        this.spaceAliases = parent.spaceAliases
    }

    def methodMissing(String name, Object[] args) {
        assert args.size() == 1
        switch (name) {
            case PERM_CLAUSE_SPACE:
                if (!topLevel) {
                    throw new IllegalArgumentException("Cannot set spaces on a nested permissions declaration")
                }
                spaceAliases = (args[1] instanceof CharSequence) ? [args[1]] : args[1]
                break;
            case WeceemSecurityPolicy.PERMISSION_ADMINISTER:
            case WeceemSecurityPolicy.PERMISSION_EDIT:
            case WeceemSecurityPolicy.PERMISSION_CREATE:
            case WeceemSecurityPolicy.PERMISSION_DELETE:
            case WeceemSecurityPolicy.PERMISSION_VIEW:
                setPermission(name, args[1])
                break;
            default:
                Closure c = args[1]
                def uri = name
                def nestedURIBuilder = new SecurityPermissionsBuilder(this)
                c.delegate = nestedURIBuilder
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                break;
        }
    }

    void setPermission(perm, value) {
        spaceAliases.each { alias ->
            if (topLevel) {
                policy.setDefaultPermissionForSpaceAndRole(perm, value.toBoolean(), alias, role)
            } else {
                policy.setURIPermissionForSpaceAndRole(uri, perm, value.toBoolean(), alias, role)
            }
        }
    }

}