
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.commons.ApplicationHolder


import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*
import org.weceem.export.*
import org.weceem.services.*

/**
 * ImportExportTests.
 *
 * @author Sergei Shushkevich
 */
class ImportExportTests extends GroovyTestCase
        implements ApplicationContextAware {

    static transactional = true

    def importExportService
    def applicationContext

    protected void setUp() {
        ServletContextHolder.servletContext = new MockServletContext(
                'test/files/importExport', new FileSystemResourceLoader())
        ServletContextHolder.servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                applicationContext)
        ApplicationHolder.application.mainContext.servletContext = ServletContextHolder.servletContext
        new Space(name: 'testSpace', aliasURI:'main').save(flush: true)
    }

    void testDefaultImport() {
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_default_import.zip'))
        def space = new Space(name: 'testSpaceImport', aliasURI:'test')
        space.save(flush: true)
        
        importExportService.importSpace(space, 'defaultSpaceImporter', importFile)
    
        Content.findAllBySpace(Space.findByName('testSpace')).each() {
            println "Content node: ${it.dump()}"
        }
        // check content
        assert Template.findByAliasURIAndSpace('testTemplate', Space.findByName('testSpace'))
        def htmlContent = HTMLContent.findByAliasURIAndSpace('testHtmlContent', Space.findByName('testSpace'))
        assertNotNull htmlContent
    
        assertNotNull ContentDirectory.findByAliasURIAndSpace('test_dir', Space.findByName('testSpace'))
        assertNotNull ContentFile.findByAliasURIAndSpace('test_file', Space.findByName('testSpace'))
        assertTrue ContentDirectory.findByAliasURIAndSpace('test_dir', Space.findByName('testSpace')).children.size() != 0

        // check unpacked files
        assertTrue new File(servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}/test/test_dir")).exists()
        assertTrue new File(servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}/test/test_dir/test_file.txt")).exists()

        def ant = new AntBuilder()
        ant.delete(dir: servletContext.getRealPath("/${ContentFile.DEFAULT_UPLOAD_DIR}/testSpaceImport"))
    }
    
    void testSimpleImport() {
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_simple_import.zip'))
        def space = new Space(name: 'testSpaceImport', aliasURI:'test')
        space.save(flush: true)
        
        importExportService.importSpace(space, 'simpleSpaceImporter', importFile)
    
        Content.findAllBySpace(Space.findByName('testSpaceImport')).each() {
            println "Content node: ${it.dump()}"
        }
        // check content
        assert Template.findByAliasURIAndSpace('testTemplate', Space.findByName('testSpaceImport'))
        def htmlContent = HTMLContent.findByAliasURIAndSpace('testHtmlContent', Space.findByName('testSpaceImport'))
        assertNotNull htmlContent
    
        assertNotNull ContentDirectory.findByAliasURIAndSpace('test_dir', Space.findByName('testSpaceImport'))
        assertNotNull ContentFile.findByAliasURIAndSpace('test_file', Space.findByName('testSpaceImport'))
        assertTrue ContentDirectory.findByAliasURIAndSpace('test_dir', Space.findByName('testSpaceImport')).children.size() != 0

        // check unpacked files
        assertTrue new File(servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}/test_dir")).exists()
        assertTrue new File(servletContext.getRealPath(
                "/${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}/test_dir/test_file.txt")).exists()

        def ant = new AntBuilder()
        ant.delete(dir: servletContext.getRealPath("/${ContentFile.DEFAULT_UPLOAD_DIR}/${space.makeUploadName()}"))
    }
    
    void testSimpleExport() {
        initDefaultData()
        def servletContext = ServletContextHolder.servletContext

        def file = importExportService.exportSpace(
                Space.findByName('testSpace'), 'simpleSpaceExporter')
        assert file
        assert file.exists()

        def ant = new AntBuilder()

        def tmpDir = new File(servletContext.getRealPath('/unzip'))
        tmpDir.mkdir()
        ant.unzip(src: file.absolutePath, dest: tmpDir.absolutePath)

        assert new File(servletContext.getRealPath('/unzip/content.xml')).exists()
        assert new File(servletContext.getRealPath('/unzip/files/test_dir')).exists()
        assert new File(servletContext.getRealPath('/unzip/files/test_dir/test_file.txt')).exists()

        def xmlFile = new File(servletContext.getRealPath('/unzip/content.xml'))
        def result = new XmlSlurper().parseText(xmlFile.text)
        assert result.children().size() == 5
        assertEquals 'org.weceem.content.Template', result.children()[0].name().toString()
        assertEquals 'org.weceem.content.VirtualContent', result.children()[4].name().toString() 
        
        assertEquals 'testTemplate', result.children()[0].aliasURI.toString()
        assertEquals 'test_file', result.children()[3].aliasURI.toString()

        assertEquals 'virt_cont', result.children()[4].aliasURI.toString()
        assert result.children()[4].parent.toString().length() != 0 
        assert result.children()[4].target.toString().length() != 0

        ant.delete(dir: tmpDir.absolutePath)
    }

    void testConfluenceImport() {
        def importFile = new File(
                ServletContextHolder.servletContext.getRealPath('/test_confluence_import.xml'))

        def space = Space.findByName('testSpace')

        importExportService.importSpace(space, 'confluenceSpaceImporter', importFile)

        // check content
        assert WikiItem.findByAliasURIAndSpace('Home', space)
    }
    
    void testFixOrderImport() {
        // test inport file without orderIndexes
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_simple_import.zip'))
        def space = new Space(name: 'testSpaceImport', aliasURI:'test')
        space.save(flush: true)
        importExportService.importSpace(space, 'simpleSpaceImporter', importFile)
        //test for uniqueness orderIndexes on root level
        def roots = Content.findAllByParentAndSpace(null, space)
        assert roots.size() == roots*.orderIndex.unique().size()
        //test for uniqueness orderIndexes on child levels
        for (root in roots){
            if (root.children)
                assert root.children.size() == root.children*.orderIndex.unique().size()
        }
    }

    private void initDefaultData() {
        def createdDate = new Date()
        def changedDate = createdDate + 1
        def defStatus = new Status(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush: true)
        new Template(title: 'testTemplate', aliasURI: 'testTemplate',
                space: Space.findByName('testSpace'), status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                content: 'template content').save(flush: true)
        def test_cont = new HTMLContent(title: 'testHtmlContent', aliasURI: 'testHtmlContent',
                content: "html content", status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                space: Space.findByName('testSpace'),
                keywords: 'keywords',
                template: Template.findByAliasURI('testTemplate')).save(flush: true)
        def cont_dir = new ContentDirectory(title: 'test_dir', aliasURI: 'test_dir',
                content: '', filesCount: 1, status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                space: Space.findByName('testSpace'),
                mimeType: '', fileSize: 0)
        assert cont_dir.save(flush: true)
        new ContentFile(title: 'test_file.txt', aliasURI: 'test_file',
                content: '', space: Space.findByName('testSpace'),
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                mimeType: 'text/plain', fileSize: 17, 
                status: defStatus).save(flush: true)
        def test_dir = ContentDirectory.findByAliasURI('test_dir')
        test_dir.addToChildren(ContentFile.findByAliasURI('test_file'))
        test_dir.save(flush: true)
        def virt_cont = new VirtualContent(title: 'Virtual Content', aliasURI: 'virt_cont',
                content: '', space: Space.findByName('testSpace'),
                createdBy: 'admin', createdOn: createdDate, status: defStatus,
                changedBy: 'admin', changedOn: changedDate,
                mimeType: 'text/plain', fileSize: 17, 
                target: ContentFile.findByAliasURI("test_file")).save(flush: true)
        test_cont.addToChildren(virt_cont)
        test_cont.save(flush: true)
        
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }
}
