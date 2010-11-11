import org.weceem.services.*

class UploadConfigTests extends grails.test.GrailsUnitTestCase {

    def service

    void setUp() {
        super.setUp()
        
        mockLogging(WcmContentRepositoryService, true)

        service = new WcmContentRepositoryService()
    }
    
    void testUploadDirNotSet() {
        
        def basePath = new File('.')
        
        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        upload: [
                            dir: ''
                        ]
                    ]
                ],
                mainContext: [
                    getResource: { path -> [file:new File(basePath, path-'/')] }
                ]
            ]
            loadConfig()
        }
        
        assertTrue service.uploadInWebapp
        assertEquals '/WeceemFiles/', service.uploadUrl
        assertEquals new File(basePath, 'WeceemFiles'), service.uploadDir
    }

    void testWarRelativePathUploadDir() {
        
        def basePath = new File('.')
        
        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        upload: [
                            dir: 'uploaded-files'
                        ]
                    ]
                ],
                mainContext: [
                    getResource: { path -> [file:new File(basePath, path-'/')] }
                ]
            ]
            loadConfig()
        }
        
        assertTrue service.uploadInWebapp
        assertEquals '/uploaded-files/', service.uploadUrl
        assertEquals new File(basePath, 'uploaded-files'), service.uploadDir
    }

    void testAbsoluteUploadDir() {
        
        def basePath = new File('.', 'test-uploads-folder')

        service.with {
            grailsApplication = [
                config: [
                    weceem: [
                        upload: [
                            dir: 'file:'+basePath.absolutePath
                        ]
                    ]
                ],
                mainContext: [
                    getResource: { path -> fail("getResource should not be called!") }
                ]
            ]
            loadConfig()
        }
        
        assertFalse service.uploadInWebapp
        assertEquals '/uploads/', service.uploadUrl
        assertEquals basePath.absolutePath, service.uploadDir.absolutePath
    }
}