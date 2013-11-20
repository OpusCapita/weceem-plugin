import org.weceem.services.*

class UploadConfigTests extends grails.test.GrailsUnitTestCase {

    def service

    void setUp() {
        super.setUp()
        
        mockLogging(WcmContentRepositoryService, true)

        service = new WcmContentRepositoryService()
    }
    
    void testUploadDirNotSet() {
        
        def basePath = new File(System.getProperty('user.home')+File.separator+'weceem-uploads')
        
        def dummyConfig = new ConfigObject()
        dummyConfig.weceem.upload.dir = ''
        dummyConfig.grails.mime.file.extensions = false

        service.with {
            grailsApplication = [
                config: dummyConfig,
                mainContext: [
                    getResource: { path -> [file:new File(basePath, path-'/')] }
                ]
            ]
            loadConfig()
        }
        
        assertEquals '/WeceemFiles/', service.uploadUrl
        assertEquals new File(basePath, 'WeceemFiles'), service.uploadDir
    }

    void testWarRelativePathUploadDir() {
        
        def basePath = new File(System.getProperty('user.home')+File.separator+'weceem-uploads')
        
        def dummyConfig = new ConfigObject()
        dummyConfig.weceem.upload.dir = '/uploaded-files/'
        dummyConfig.grails.mime.file.extensions = false

        service.with {
            grailsApplication = [
                config: dummyConfig,
                mainContext: [
                    getResource: { path -> [file:new File(basePath, path-'/')] }
                ]
            ]
            loadConfig()
        }
        
        assertEquals '/uploaded-files/', service.uploadUrl
        assertEquals new File(basePath, 'uploaded-files'), service.uploadDir
    }

    void testAbsoluteUploadDir() {
        
        def basePath = new File('.', 'test-uploads-folder')

        def dummyConfig = new ConfigObject()

        if (basePath.absolutePath.replace('\\', '/').startsWith('/')) {
            dummyConfig.weceem.upload.dir = 'file:'+basePath.absolutePath
        } else {
            dummyConfig.weceem.upload.dir = 'file:/'+basePath.absolutePath.replace('\\', '/')
        }
        dummyConfig.grails.mime.file.extensions = false

        service.with {
            grailsApplication = [
                config: dummyConfig,
                mainContext: [
                    getResource: { path -> fail("getResource should not be called!") }
                ]
            ]
            loadConfig()
        }
        
        assertEquals '/uploads/', service.uploadUrl
        assertEquals basePath.absolutePath, service.uploadDir.absolutePath
    }

    void testGStringUploadDir() {
        
        def basePath = new File('.', 'test-uploads-folder')

        def dummyConfig = new ConfigObject()
        if (basePath.absolutePath.replace('\\', '/').startsWith('/')) {
            dummyConfig.weceem.upload.dir = 'file:'+basePath.absolutePath
        } else {
            dummyConfig.weceem.upload.dir = 'file:/'+basePath.absolutePath.replace('\\', '/')
        }
        dummyConfig.grails.mime.file.extensions = false
        service.with {
            grailsApplication = [
                config: dummyConfig,
                mainContext: [
                    getResource: { path -> fail("getResource should not be called!") }
                ]
            ]
            loadConfig()
        }
        
        assertEquals '/uploads/', service.uploadUrl
        assertEquals basePath.absolutePath, service.uploadDir.absolutePath
    }
}