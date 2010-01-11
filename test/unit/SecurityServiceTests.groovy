import grails.test.*

import org.weceem.services.*
import org.weceem.content.*
import org.weceem.html.*

class SecurityServiceTests extends grails.test.GrailsUnitTestCase {

    def service
    def mockSpace

    void setUp() {
        super.setUp()
        
        mockLogging(WeceemSecurityService, true)

        service = new WeceemSecurityService()
        
        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        security: [
                            policy: [
                                path: ''
                            ]
                        ]
                    ]
                ]
            ]
            afterPropertiesSet()
        }
        
        mockSpace = new Space(name:'Test', aliasURI:'test')
    }

    void testPublicContent() {
        def publishedStatus = new Status(code:500, description:"published", publicContent:true)
        
        def mockContent = new HTMLContent(status:publishedStatus, aliasURI:'index')
        mockContent.space = mockSpace
        
        service.securityDelegate = [
            getUserName : { -> "unknown" },
            getUserEmail : { -> "unknown@localhost" },
            getUserPrincipal : { -> [id:'unknown', name:'unknown', email:"unknown@localhost"] },
            getUserRoles: { -> ['ROLE_GUEST'] }
        ]
        
        assertTrue service.isUserAllowedToViewContent(mockContent)
    }


    void testDraftContent() {
        def draftStatus = new Status(code:100, description:"draft", publicContent:false)
        
        def mockContent = new HTMLContent(status:draftStatus, aliasURI:'index')
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