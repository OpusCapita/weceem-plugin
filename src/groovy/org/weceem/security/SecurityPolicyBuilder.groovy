package org.weceem.security

class SecurityPolicyBuilder {
    WeceemSecurityPolicy policy = new WeceemSecurityPolicy()
    
    def methodMissing(String name, Object[] args) {
        def securityRole = name

        // Start handling URI-specific block
        assert args.size() == 1
        
        Closure perms = args[1]
        assert perms instanceof Closure
        def permBuilder = new SecurityPermissionsBuilder(securityRole, policy)
        perms.delegate = permBuilder
        perms.resolveStrategy = Closure.DELEGATE_FIRST
        perms.call()
    }

}