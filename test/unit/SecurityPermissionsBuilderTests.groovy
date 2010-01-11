import grails.test.*

import org.weceem.security.*

class SecurityPermissionsBuilderTests extends grails.test.GrailsUnitTestCase {
    void testSpaceDefaultPermissionsSet() {
        def policyMockControl = mockFor(WeceemSecurityPolicy)
        
        def expectedPermArgs = [
            [perm:'admin', granted:false, alias:'main', role:'ROLE_TEST'],
            [perm:'admin', granted:false, alias:'extranet', role:'ROLE_TEST'],
            [perm:'view', granted:true, alias:'main', role:'ROLE_TEST'],
            [perm:'view', granted:true, alias:'extranet', role:'ROLE_TEST']
        ] 
        def invocation = 0
        policyMockControl.demand.setDefaultPermissionForSpaceAndRole(4..4) { perm, granted, alias, role ->
            println "Args for invocation $invocation $perm, $granted, $alias, $role"

            assertEquals expectedPermArgs[invocation].perm, perm
            assertEquals expectedPermArgs[invocation].granted, granted
            assertEquals expectedPermArgs[invocation].alias, alias
            assertEquals expectedPermArgs[invocation].role, role
            invocation++
        }

        def policy = policyMockControl.createMock()
        
        def builder = new SecurityPermissionsBuilder("ROLE_TEST", policy)
        builder.with {
            space 'main', 'extranet'
            admin false
            view true
        }
        
        policyMockControl.verify()
    }

    void testURIPermissions() {
        def policyMockControl = mockFor(WeceemSecurityPolicy)
        
        def expectedPermArgs = [
            [uri:'/public', perm:'admin', granted:false, alias:'main', role:'ROLE_TEST'],
            [uri:'/public', perm:'admin', granted:false, alias:'extranet', role:'ROLE_TEST'],
            [uri:'/public', perm:'view', granted:true, alias:'main', role:'ROLE_TEST'],
            [uri:'/public', perm:'view', granted:true, alias:'extranet', role:'ROLE_TEST'],
            [uri:'/private', perm:'edit', granted:true, alias:'main', role:'ROLE_TEST'],
            [uri:'/private', perm:'edit', granted:true, alias:'extranet', role:'ROLE_TEST'],
            [uri:'/private', perm:'view', granted:false, alias:'main', role:'ROLE_TEST'],
            [uri:'/private', perm:'view', granted:false, alias:'extranet', role:'ROLE_TEST']
        ] 
        
        def invocation = 0
        
        policyMockControl.demand.setURIPermissionForSpaceAndRole(8..8) { uri, perm, granted, alias, role ->
            println "Args for invocation $invocation - $uri, $perm, $granted, $alias, $role"

            assertEquals expectedPermArgs[invocation].perm, perm
            assertEquals expectedPermArgs[invocation].granted, granted
            assertEquals expectedPermArgs[invocation].alias, alias
            assertEquals expectedPermArgs[invocation].role, role
            invocation++
        }

        def policy = policyMockControl.createMock()
        
        def builder = new SecurityPermissionsBuilder("ROLE_TEST", policy)
        builder.with {
            space 'main', 'extranet'
            "/public" {
                admin false
                view true
            }
            "/private" {
                edit true
                view false
            }
        }
        
        policyMockControl.verify()
    }
}