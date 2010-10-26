
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

/**
 * ImportExportTests.
 *
 * @author Sergei Shushkevich
 */
class ImportExportTests extends GroovyTestCase
        implements ApplicationContextAware {

    static transactional = true

    def wcmImportExportService
    def applicationContext

    protected void setUp() {
        ServletContextHolder.servletContext = new MockServletContext(
                'test/files/importExport', new FileSystemResourceLoader())
        ServletContextHolder.servletContext.setAttribute(
                GrailsApplicationAttributes.APPLICATION_CONTEXT,
                applicationContext)
        ApplicationHolder.application.mainContext.servletContext = ServletContextHolder.servletContext
        new WcmSpace(name: 'testSpace', aliasURI:'main').save(flush: true)
    }
/*
    void testDefaultImport() {
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_default_import.zip'))
        def spaceName = 'testSpaceImport'+System.currentTimeMillis()
        def space = new WcmSpace(name: spaceName, aliasURI:'test')
        space.save(flush: true)
        
        println "Importing into space: ${space.dump()}"
        wcmImportExportService.importSpace(space, 'defaultSpaceImporter', importFile)
    
        space = WcmSpace.findByName(spaceName)
        WcmContent.findAllBySpace(space).each() {
            println "Content node: ${it.dump()} - space is ${it.space.name}"
        }
        // check content
        assert WcmTemplate.findByAliasURIAndSpace('testTemplate', space)
        def htmlContent = WcmHTMLContent.findByAliasURIAndSpace('testHtmlContent', space)
        assertNotNull htmlContent
    
        assertNotNull WcmContentDirectory.findByAliasURIAndSpace('test_dir',space)
        assertNotNull WcmContentFile.findByAliasURIAndSpace('test_file', space)
        assertTrue WcmContentDirectory.findByAliasURIAndSpace('test_dir', space).children.size() != 0

        // check unpacked files
        assertTrue new File(servletContext.getRealPath(
                "/${WcmContentFile.uploadDir}/${space.makeUploadName()}/test_dir")).exists()
        assertTrue new File(servletContext.getRealPath(
                "/${WcmContentFile.uploadDir}/${space.makeUploadName()}/test_dir/test_file.txt")).exists()

        def ant = new AntBuilder()
        ant.delete(dir: servletContext.getRealPath("/${WcmContentFile.uploadDir}/${space.makeUploadName()}"))
    }
*/    
    void testSimpleImport() {
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_simple_import.zip'))
        def spaceName = 'testSpaceImport'+System.currentTimeMillis()
        def space = new WcmSpace(name: spaceName, aliasURI:'test')
        space.save(flush: true)
        
        wcmImportExportService.importSpace(space, 'simpleSpaceImporter', importFile)
    
        space = WcmSpace.findByName(spaceName)
        WcmContent.findAllBySpace(space).each() {
            println "Content node: ${it.dump()}"
        }
        
        // check content
        assert WcmTemplate.findByAliasURIAndSpace('testTemplate', space)
        def htmlContent = WcmHTMLContent.findByAliasURIAndSpace('testHtmlContent', space)
        assertNotNull htmlContent
    
        assertNotNull WcmContentDirectory.findByAliasURIAndSpace('test_dir', space)
        assertNotNull WcmContentFile.findByAliasURIAndSpace('test_file', space)
        assertTrue WcmContentDirectory.findByAliasURIAndSpace('test_dir', space).children.size() != 0

        // check unpacked files
        assertTrue new File(servletContext.getRealPath(
                "/${WcmContentFile.uploadDir}/${space.makeUploadName()}/test_dir")).exists()
        assertTrue new File(servletContext.getRealPath(
                "/${WcmContentFile.uploadDir}/${space.makeUploadName()}/test_dir/test_file.txt")).exists()

        def ant = new AntBuilder()
        ant.delete(dir: servletContext.getRealPath("/${WcmContentFile.uploadDir}/${space.makeUploadName()}"))
    }
    
    void testSimpleExport() {
        initDefaultData()
        def servletContext = ServletContextHolder.servletContext

        def file = wcmImportExportService.exportSpace(
                WcmSpace.findByName('testSpace'), 'simpleSpaceExporter')
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
        assertEquals 'org.weceem.content.WcmTemplate', result.children()[0].name().toString()
        assertEquals 'org.weceem.content.WcmVirtualContent', result.children()[4].name().toString()
        
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

        def space = WcmSpace.findByName('testSpace')

        wcmImportExportService.importSpace(space, 'confluenceSpaceImporter', importFile)

        // check content
        assert WcmWikiItem.findByAliasURIAndSpace('Home', space)
    }
    
    void testFixOrderImport() {
        // test inport file without orderIndexes
        def servletContext = ServletContextHolder.servletContext
        def importFile = new File(
                servletContext.getRealPath('/test_simple_import.zip'))
        def space = new WcmSpace(name: 'testSpaceImport', aliasURI:'test')
        space.save(flush: true)
        wcmImportExportService.importSpace(space, 'simpleSpaceImporter', importFile)
        //test for uniqueness orderIndexes on root level
        def roots = WcmContent.findAllByParentAndSpace(null, space)
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
        def defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush: true)
        new WcmTemplate(title: 'testTemplate', aliasURI: 'testTemplate',
                space: WcmSpace.findByName('testSpace'), status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                content: 'template content').save(flush: true)
        def test_cont = new WcmHTMLContent(title: 'testHtmlContent', aliasURI: 'testHtmlContent',
                content: "html content", status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                space: WcmSpace.findByName('testSpace'),
                keywords: 'keywords',
                template: WcmTemplate.findByAliasURI('testTemplate')).save(flush: true)
        def cont_dir = new WcmContentDirectory(title: 'test_dir', aliasURI: 'test_dir',
                content: '', filesCount: 1, status: defStatus,
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                space: WcmSpace.findByName('testSpace'),
                mimeType: '', fileSize: 0)
        assert cont_dir.save(flush: true)
        new WcmContentFile(title: 'test_file.txt', aliasURI: 'test_file',
                content: '', space: WcmSpace.findByName('testSpace'),
                createdBy: 'admin', createdOn: createdDate,
                changedBy: 'admin', changedOn: changedDate,
                mimeType: 'text/plain', fileSize: 17, 
                status: defStatus).save(flush: true)
        def test_dir = WcmContentDirectory.findByAliasURI('test_dir')
        test_dir.addToChildren(WcmContentFile.findByAliasURI('test_file'))
        test_dir.save(flush: true)
        def virt_cont = new WcmVirtualContent(title: 'Virtual WcmContent', aliasURI: 'virt_cont',
                content: '', space: WcmSpace.findByName('testSpace'),
                createdBy: 'admin', createdOn: createdDate, status: defStatus,
                changedBy: 'admin', changedOn: changedDate,
                mimeType: 'text/plain', fileSize: 17, 
                target: WcmContentFile.findByAliasURI("test_file")).save(flush: true)
        test_cont.addToChildren(virt_cont)
        test_cont.save(flush: true)
        
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }
}
