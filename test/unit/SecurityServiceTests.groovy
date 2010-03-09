import org.weceem.services.*
import org.weceem.content.*
import org.weceem.html.*

class SecurityServiceTests extends grails.test.GrailsUnitTestCase {

    def service
    def mockSpace

    void setUp() {
        super.setUp()
        
        mockLogging(WcmSecurityService, true)

        service = new WcmSecurityService()
        
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
        
        mockSpace = new WcmSpace(name:'Test', aliasURI:'test')
    }

    void testPublicContent() {
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


    void testDraftContent() {
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