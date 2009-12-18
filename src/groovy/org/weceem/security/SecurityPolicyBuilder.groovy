package org.weceem.security

/**
 * A builder that handles any method calls as role names, and takes the closure
 * and passes it to a permissions builder to update the security policy
 */
class SecurityPolicyBuilder {
    WeceemSecurityPolicy policy 
    
    SecurityPolicyBuilder(policy) {
        this.policy = policy
    }
    
    def methodMissing(String name, args) {
        def securityRole = name

        // Start handling URI-specific block
        assert args.size() == 1
        
        Closure perms = args[0]
        assert perms instanceof Closure
        def permBuilder = new SecurityPermissionsBuilder(securityRole, policy)
        perms.delegate = permBuilder
        perms.resolveStrategy = Closure.DELEGATE_FIRST
        perms.call()
    }

}