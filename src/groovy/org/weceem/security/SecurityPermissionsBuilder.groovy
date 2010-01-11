package org.weceem.security

class SecurityPermissionsBuilder {
    static PERM_CLAUSE_SPACE = "space"
    
    boolean topLevel
    def policy
    def role
    def uri
    def spaceAliases 
    
    // ctor variant for top level usage
    SecurityPermissionsBuilder(role, policy) {
        this.topLevel = true
        this.role = role
        this.policy = policy
    }
    
    // ctor variant for nested usage
    SecurityPermissionsBuilder(SecurityPermissionsBuilder parent, String uri) {
        this(parent.role, parent.policy)
        this.topLevel = false
        this.uri = uri
        this.spaceAliases = parent.spaceAliases
    }

    def methodMissing(String name, args) {
        switch (name) {
            case PERM_CLAUSE_SPACE:
                if (!topLevel) {
                    throw new IllegalArgumentException("Cannot set spaces on a nested permissions declaration")
                }
                assert args.size() >= 1
                def aliases
                if (args.size() > 1) {
                    aliases = args*.toString()
                } else {
                    aliases = (args[0] instanceof CharSequence) ? [args[0]] : args[0]
                }
                spaceAliases = aliases
                break;
            case WeceemSecurityPolicy.PERMISSION_ADMIN:
            case WeceemSecurityPolicy.PERMISSION_EDIT:
            case WeceemSecurityPolicy.PERMISSION_CREATE:
            case WeceemSecurityPolicy.PERMISSION_DELETE:
            case WeceemSecurityPolicy.PERMISSION_VIEW:
                assert args.size() == 1
                setPermission(name, args[0])
                break;
            default:
                assert args.size() == 1
                Closure c = args[0]
                def uri = name
                def nestedURIBuilder = new SecurityPermissionsBuilder(this, uri)
                c.delegate = nestedURIBuilder
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                break;
        }
    }

    void setPermission(perm, value) {
        spaceAliases.each { alias ->
            if (topLevel) {
                policy.setDefaultPermissionForSpaceAndRole(perm, value, alias, role)
            } else {
                policy.setURIPermissionForSpaceAndRole(uri, perm, value, alias, role)
            }
        }
    }

}