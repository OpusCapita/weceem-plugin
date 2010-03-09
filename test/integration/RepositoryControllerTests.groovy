import org.weceem.controllers.*
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.FileSystemResourceLoader
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.context.ApplicationContext
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * wcmContentRepositoryService.
 *
 */
class RepositoryControllerTests extends GroovyTestCase {

    static transactional = true
    
    def grailsApplication
    def spaceA
    def nodeA
    def nodeB
    def nodeC
    def nodeD
    def wcmContentRepositoryService
    def applicationContext
    def servletContext
    def template
    def defStatus
    
    void setUp() {
        servletContext = new MockServletContext(
                'test/files/contentRepository', new FileSystemResourceLoader())
        servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                applicationContext)
        ServletContextHolder.servletContext = servletContext
        defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)
        spaceA = new WcmSpace(name: 'jcatalog', aliasURI: 'jcatalog').save(flush: true)
        template = new WcmTemplate(title: 'template', aliasURI: 'template',
                    space: spaceA, status: defStatus,
                    createdBy: 'admin', createdOn: new Date(),
                    changedBy: 'admin', changedOn: new Date(),
                    content: 'template content', orderIndex: 0).save(flush: true)
        nodeA = new WcmHTMLContent(title: 'contentA', aliasURI: 'contentA',
                content: 'sample A content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert nodeA.save(flush: true)
        nodeB = new WcmHTMLContent(title: 'contentB', aliasURI: 'contentB',
                parent: nodeA, status: defStatus,
                content: 'sample B content',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeB.save(flush:true)
        nodeC = new WcmHTMLContent(title: 'contentC', aliasURI: 'contentC',
                parent: nodeA, status: defStatus,
                content: 'sample C content',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 3)
        assert nodeC.save(flush:true)
        nodeD = new WcmWikiItem(title: 'contentD', aliasURI: 'contentD',
                parent: nodeA, status: defStatus,
                content: 'sample D content',
                createdBy: 'admin', createdOn: new Date(),
                changedBy: 'admin', changedOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 4)
        assert nodeD.save(flush:true)
        nodeA.addToChildren(nodeB)
        nodeA.addToChildren(nodeC)
        nodeA.addToChildren(nodeD)
        nodeA.save(flush: true)

        def virtContent1 = new WcmVirtualContent(title: 'virtContent1', aliasURI: 'virtContent1',
                                              parent: nodeC, target: nodeB, status: defStatus,
                                              content: 'WcmVirtualContent B for nodeC',
                                              space: spaceA, orderIndex: 5)
        assert virtContent1.save(flush:true)
        nodeC.addToChildren(virtContent1)
        assert nodeC.save(flush: true)

        def virtContent2 = new WcmVirtualContent(title: 'virtContent2', aliasURI: 'virtContent2',
                                              parent: nodeD, target: nodeB, status: defStatus,
                                              content: 'WcmVirtualContent B for nodeWiki',
                                              space: spaceA, orderIndex: 6)
        assert virtContent2.save(flush:true)
        nodeD.addToChildren(virtContent2)
        assert nodeD.save(flush: true)

        // Tree structure:
        //
        //   a
        //   ----b (1)
        //   ----c
        //       ----b (2)
        //   ----d
        //       ----b (3)
    }
/*    
    void testInsertNode() {
        def controller = new WcmRepositoryController()
        controller.grailsApplication = grailsApplication
        // @todo mock the service?
        controller.wcmContentRepositoryService = wcmContentRepositoryService
        controller.params.contentType = 'org.weceem.html.WcmHTMLContent'
        controller.params.parentPath = "${spaceA.ident()}/WcmHTMLContent/${nodeA.ident()}"
        controller.params.title = 'contentZ'
        controller.params['space.id'] = spaceA.ident()
        controller.params['template.id'] = template.id
        controller.params.content = 'some content'
        controller.params.keywords = 'some keywords'
        controller.params.status = defStatus
        controller.insertNode()
        
        // check that contentZ has been created 
        def node = WcmHTMLContent.findByTitle('contentZ')
        
        assertNotNull node

        // check parent/child relationship is correct
        def parent = WcmHTMLContent.findByTitle('contentA')
        println "parent: ${parent?.dump()}"
        println "node: ${node?.dump()}"
        assertEquals parent, node.parent
        assertNotNull parent.children?.find { it.id == node.id }
    }
*/

    void testCreateDirectory() {
        createDirectory([dirname: 'sample_dir', 'space.id': spaceA.ident(), statuscode: defStatus.code])

        // check created directory (also on the file system)
        assert WcmContentDirectory.findByTitleAndSpace('sample_dir', spaceA)
        def dir = new File(servletContext.getRealPath(
                "/${WcmContentFile.DEFAULT_UPLOAD_DIR}/jcatalog/sample_dir"))
        assert dir.exists()
        assert dir.directory

        new AntBuilder().delete(dir: dir.absolutePath)
    }

    void testCreateDirectoryWithParent() {
        def testDir = new WcmContentDirectory(title: 'test_dir', aliasURI: 'test_dir',
                content: '', filesCount: 0, space: spaceA,
                mimeType: '', fileSize: 0, status: defStatus)
        testDir.save(flush: true)
        createDirectory([dirname: 'sample_dir', 'space.id': spaceA.ident(),
                parentPath: "${spaceA.ident()}/Files/${testDir.ident()}", statuscode: defStatus.code])

        // check parent:child mappings
        def loadedDir = WcmContentDirectory.findByTitleAndSpace('sample_dir', spaceA)
        assertNotNull loadedDir
        testDir = WcmContentDirectory.findByTitle('test_dir')
        assertNotNull testDir
        assertEquals testDir, loadedDir.parent
        assertNotNull testDir.children?.find { it.id == loadedDir.id }
        
        // Now check filesystem dir exists
        def dir = new File(servletContext.getRealPath(
                "/${WcmContentFile.DEFAULT_UPLOAD_DIR}/jcatalog/test_dir/sample_dir"))
        assert dir.exists()
        assert dir.directory

        new AntBuilder().delete(dir: dir.absolutePath)
    }

    void testCreateDirectoryWithHtmlParent() {
        createDirectory([dirname: 'sample_dir', 'space.id': spaceA.ident(),
                parentPath: "${spaceA.ident()}/HTMLContent/${nodeA.ident()}", statuscode: defStatus.code])

        // check created directory (also on the file system)
        // check parent:child mappings
        def loadedDir = WcmContentDirectory.findByTitle('sample_dir')
        assertNotNull loadedDir
        def parent = WcmHTMLContent.findByTitle('contentA')
        assertNotNull parent
        assertEquals parent, loadedDir.parent
        assertNotNull parent.children?.find { it.id == loadedDir.id }
        
        // Check directory created correctly
        def dir = new File(servletContext.getRealPath(
                "/${WcmContentFile.DEFAULT_UPLOAD_DIR}/jcatalog/sample_dir"))
        assert dir.exists()
        assert dir.directory

        new AntBuilder().delete(dir: dir.absolutePath)
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    private void createDirectory(params) {
        def controller = new WcmRepositoryController()
        controller.wcmContentRepositoryService = wcmContentRepositoryService
        controller.grailsApplication = grailsApplication
        controller.params.putAll(params)
        controller.createDirectory()
    }

    private void uploadFile(path, originalFilename, mimeType, params) {
        def file = new File(servletContext.getRealPath(path))
        def multipartFile = new MockMultipartFile('file', originalFilename,
                mimeType, new FileInputStream(file))
        def multipartRequest = new MockMultipartHttpServletRequest()
        multipartRequest.addFile multipartFile

        def controller = new WcmRepositoryController()
        WcmRepositoryController.metaClass.getRequest = { -> multipartRequest }
        controller.wcmContentRepositoryService = wcmContentRepositoryService
        controller.grailsApplication = grailsApplication
        controller.params.putAll(params)
        controller.uploadFile()
    }
}
