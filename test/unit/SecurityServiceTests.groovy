import org.weceem.services.*
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.security.*

import groovy.mock.interceptor.*

class SecurityServiceTests extends grails.test.GrailsUnitTestCase {

    def service
    def mockSpace

    void setUp() {
        super.setUp()
        
        mockLogging(WcmSecurityService, true)
        mockDomain(WcmHTMLContent)
        mockDomain(WcmComment)

        mockSpace = new WcmSpace(name:'Test', aliasURI:'test')
    }

    void setupDefaultService() {
        service = new WcmSecurityService()
        
        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ],
                        archived: [ status: ''],
                        unmoderated: [ status: '']
                    ]
                ]
            ]
            afterPropertiesSet()
        }
    }

    void testPublicContent() {
        setupDefaultService()
        
        def publishedStatus = new WcmStatus(code:500, description:"published", publicContent:true)
        
        def mockContent = new WcmHTMLContent(status:publishedStatus, aliasURI:'index')
        mockContent.space = mockSpace
        
        service.securityDelegate = [
            getUserName : { -> "unknown" },
            getUserEmail : { -> "unknown@localhost" },
            getUserPrincipal : { -> [id:'unknown', name:'unknown', email:"unknown@localhost"] },
            getUserRoles: { -> ['ROLE_GUEST'] }
        ]
        
        assertTrue service.isUserAllowedToViewContent(mockContent)
    }

    void testTypeSpecificSpacePermission() {
        def hasPermsCalled = false
        
        def policyStub = [
            hasPermission: {
                    String spaceAlias, String uri, List roleList, List permissionList, Map args = null ->
                assertEquals 'test', spaceAlias
                assertEquals 'about', uri
                assertEquals([WeceemSecurityPolicy.ROLE_GUEST, 'USER_unknown'], roleList)
                assertEquals([WeceemSecurityPolicy.PERMISSION_CREATE], permissionList)
                assertEquals WcmComment, args.type
            
                hasPermsCalled = true
                
                false
            }
        ]
        
        service = new WcmSecurityService()

        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        archived: [ status: ''],
                        unmoderated: [ status: '']
                    ]
                ]
            ]
        }
        
        service.securityDelegate.getUserRoles = { -> [WeceemSecurityPolicy.ROLE_GUEST] }
        service.policy = policyStub
        
        assertFalse service.hasPermissions(mockSpace, 'about', 
            [WeceemSecurityPolicy.PERMISSION_CREATE], WcmComment)
            
        assertTrue hasPermsCalled
    }

    void testTypeSpecificContentPermission() {
        def hasPermsCalled = false
        
        def policyStub = [
            hasPermission: {
                    String spaceAlias, String uri, List roleList, List permissionList, Map args = null ->
                assertEquals 'test', spaceAlias
                assertEquals 'bla', uri
                assertEquals([WeceemSecurityPolicy.ROLE_GUEST, 'USER_unknown'], roleList)
                assertEquals([WeceemSecurityPolicy.PERMISSION_CREATE], permissionList)
                assertEquals WcmHTMLContent, args.type
            
                hasPermsCalled = true
                
                false
            }
        ]
        
        service = new WcmSecurityService()

        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        archived: [ status: ''],
                        unmoderated: [ status: '']
                    ]
                ]
            ]
        }
        
        service.securityDelegate.getUserRoles = { -> [WeceemSecurityPolicy.ROLE_GUEST] }
        service.policy = policyStub
        
        assertFalse service.hasPermissions(new WcmHTMLContent(space:mockSpace, aliasURI:'bla'), 
            [WeceemSecurityPolicy.PERMISSION_CREATE])
            
        assertTrue hasPermsCalled
    }

    void testDraftContent() {
        setupDefaultService()

        def draftStatus = new WcmStatus(code:100, description:"draft", publicContent:false)
        
        def mockContent = new WcmHTMLContent(status:draftStatus, aliasURI:'index')
        mockContent.space = mockSpace
        
        service.securityDelegate = [
            getUserName : { -> "unknown" },
            getUserEmail : { -> "unknown@localhost" },
            getUserPrincipal : { -> [id:'unknown', name:'unknown', email:"unknown@localhost"] },
            getUserRoles: { -> ['ROLE_GUEST'] }
        ]
        
        assertFalse service.isUserAllowedToViewContent(mockContent)

        service.securityDelegate = [
            getUserName : { -> "unknown" },
            getUserEmail : { -> "unknown@localhost" },
            getUserPrincipal : { -> [id:'unknown', name:'unknown', email:"unknown@localhost"] },
            getUserRoles: { -> ['ROLE_USER'] }
        ]
        
        assertTrue service.isUserAllowedToViewContent(mockContent)
    }
}