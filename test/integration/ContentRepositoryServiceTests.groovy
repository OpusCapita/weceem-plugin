import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.files.*
import org.weceem.services.*


class ContentRepositoryServiceTests extends GroovyTestCase {
    
    static transactional = true
    
    def contentRepositoryService
    def nodeA
    def nodeB
    def nodeC
    def nodeWiki
    def spaceA
    def spaceB
    def template
    def defStatus
    
    def extraNode

    void setUp() {
        super.setUp()
        
        contentRepositoryService = new ContentRepositoryService()
        contentRepositoryService.cacheService = new CacheService()
        contentRepositoryService.cacheService.cacheManager = new net.sf.ehcache.CacheManager()
        contentRepositoryService.weceemSecurityService = new WeceemSecurityService()
        contentRepositoryService.weceemSecurityService.with {
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
        contentRepositoryService.grailsApplication = ApplicationHolder.application
        contentRepositoryService.afterPropertiesSet()
        
        defStatus = new Status(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        spaceA = new Space(name: 'jcatalog', aliasURI: 'jcatalog')
        assert spaceA.save(flush: true)
        spaceB = new Space(name: 'other', aliasURI: 'other')
        assert spaceB.save(flush: true)

        template = new Template(title: 'template', aliasURI: 'template',
                    space: spaceA, status: defStatus,
                    createdBy: 'admin', createdOn: new Date(),
                    content: 'template content', orderIndex: 0)
        assert template.save(flush: true)
        nodeA = new HTMLContent(title: 'contentA', aliasURI: 'contentA',
                content: 'sample A content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert nodeA.save(flush: true)
        nodeB = new HTMLContent(title: 'contentB', aliasURI: 'contentB',
                parent: nodeA,
                content: 'sample B content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeB.save(flush:true)
        nodeC = new HTMLContent(title: 'contentC', aliasURI: 'contentC',
                parent: nodeA,
                content: 'sample C content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 3)
        assert nodeC.save(flush:true)
        nodeWiki = new WikiItem(title: 'contentD', aliasURI: 'contentD',
                parent: nodeA,
                content: 'sample D content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 4)
        assert nodeWiki.save(flush:true)
        nodeA.children = new TreeSet()
        nodeA.children << nodeB
        nodeA.children << nodeC
        nodeA.children << nodeWiki
        nodeA.save(flush: true)

        def virtContent1 = new VirtualContent(title: 'virtContent1', aliasURI: 'virtContent1',
                           parent: nodeC, target: nodeB, status: defStatus,
                           content: 'VirtualContent B for nodeC',
                           space: spaceA, orderIndex: 5)
        assert virtContent1.save(flush:true)
        nodeC.children = new TreeSet()
        nodeC.children << virtContent1
        nodeC.save()

        def virtContent2 = new VirtualContent(title: 'virtContent2', aliasURI: 'virtContent2',
                           parent: nodeWiki, target: nodeB, status: defStatus,
                           content: 'VirtualContent B for nodeWiki',
                           space: spaceA, orderIndex: 6)
        assert virtContent2.save(flush:true)
        nodeWiki.children = new TreeSet()
        nodeWiki.children << virtContent2
        nodeWiki.save()    
                           
        // Tree structure:
        //
        //   a
        //   ----b (1)
        //   ----c
        //       ----b (2)
        //   ----d
        //       ----b (3)


        assert new HTMLContent(title: 'Other Index', aliasURI: 'contentA',
                content: 'Other Index page', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceB, 
                orderIndex: 0).save()
        
    }

    void tearDown() {
        Content.list().each {
            it.children = [] as SortedSet
            it.parent = null 
        }
        HTMLContent.list()*.template = null
        WikiItem.list()*.template = null
        VirtualContent.list()*.target = null
        //Content.list()*.delete(flush:true)
    }

    void testResolveSpaceAndURI() {
        def res = contentRepositoryService.resolveSpaceAndURI('/jcatalog/anything')
        assert res.space.id == spaceA.id
        assertEquals 'anything', res.uri
        
        res = contentRepositoryService.resolveSpaceAndURI('jcatalog/anything')
        assert res.space.id == spaceA.id
        assertEquals 'anything', res.uri

        res = contentRepositoryService.resolveSpaceAndURI('other/anything')
        assert res.space.id == spaceB.id
        assertEquals 'anything', res.uri
    }

    void testFindContentForPath() {
        // Without cache
        assertEquals nodeA.id, contentRepositoryService.findContentForPath('contentA', spaceA, false).content.id
        assertEquals nodeB.id, contentRepositoryService.findContentForPath('contentA/contentB', spaceA, false).content.id
        assertFalse nodeA.id == contentRepositoryService.findContentForPath('contentA', spaceB, false).content.id
        assertNull contentRepositoryService.findContentForPath('contentA/contentB', spaceB, false)

        // With cache, once to load, second to hit
        2.times {
            assertEquals nodeA.id, contentRepositoryService.findContentForPath('contentA', spaceA).content.id
            assertEquals nodeB.id, contentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id
            assertFalse nodeA.id == contentRepositoryService.findContentForPath('contentA', spaceB).content.id
            assertNull contentRepositoryService.findContentForPath('contentA/contentB', spaceB)
        }
    }
    
    void testDeleteNodeA() {
        // Result Tree:
        //   c
        //   ----b
        //   ----e
        //       ----b
        for (child in nodeA.children){
            child.parent = null
            assert child.save(flush:true)
        }
        nodeA.children = null
        assert nodeA.save(flush:true)
        contentRepositoryService.deleteNode(nodeA)
        nodeA = Content.findByTitle('contentA')
        nodeB = Content.findByTitle('contentB')

        // check that there are no parents for contentC
        def references = VirtualContent.findAllWhere(target: nodeC)?.unique()*.parent
        if (nodeC.parent) reference << nodeC.parent
         
        assertEquals 0, references.size()

        // check that contentA was deleted
        assertTrue "Countent should not exist!", Content.findByTitle('contentA') == null
    }

    void testDeleteNodeB() {
        // Result Tree:
        //   a
        //   ----c
        //   ----d
        
        contentRepositoryService.deleteNode(nodeB)
        nodeA.refresh()
        nodeC.refresh()
        nodeWiki.refresh()

        // check that there are two childen for contentA: contentC + contentD
        def references = nodeA.children

        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }
        
        // check that there are no children for contentC and contentD
        assertEquals 0, nodeC.children.size()
        assertEquals 0, nodeWiki.children.size()
    }

    void testDeleteNodeC() {
        // Result Tree:
        //   a
        //   ----b
        //   ----d
        //       ----b2
        contentRepositoryService.deleteNode(nodeC)
        nodeA.refresh()
        nodeWiki.refresh()
        
        // check that contentB has only two different parents: contentA + contentD
        def references = VirtualContent.findAllWhere(target: nodeB)*.parent
        if (nodeB.parent) references << nodeB.parent

        assertEquals 2, references.size()  // The node C and its virtual child should have been deleted
        assertEquals 2, references.unique().size()
        assertNotNull references.find { it == nodeA }
        assertNotNull references.find { it == nodeWiki }
    }

    void testDeleteVirtualContent() {
        // Result Tree:
        //   a
        //   ----c
        //   ----d

        contentRepositoryService.deleteNode(VirtualContent.findWhere(title: 'virtContent1'))

        nodeA = Content.findByTitle('contentA')
        nodeC = Content.findByTitle('contentC')
        nodeWiki = Content.findByTitle('contentD')

        // Make sure the target of the virtual content is not deleted
        nodeB = Content.findByTitle('contentB')
        assertNotNull nodeB

        // check that there are two childen for contentA: contentC + contentD
        def references = nodeA.children

        // There should still be 3 children (includiong B)
        assertEquals 3, references.size()
        assertNotNull references.find { it == nodeB }
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }

        // check that there are no children for contentC and contentD
        assertEquals 0, nodeC.children.size()
    }

    void testDeleteReferenceAC() {
        // Result Tree:
        //   a
        //   ----b
        //   c
        //   ----b
        //   ----e
        //       ----b
        
        contentRepositoryService.deleteLink(nodeC, nodeA)
        nodeA = Content.findByTitle('contentA')
        nodeC = Content.findByTitle('contentC')
        
        // check that nodeC do not have parent contentA
        def references = VirtualContent.findAllWhere(target: nodeC)*.parent
        if (nodeC.parent) references << nodeC.parent
        assertEquals 0, references.size()

        // check that nodeA do not have child contentC
        assertNull nodeA.children.find { it == nodeC }
    }

    void testDeleteReferenceAB() {
        // Result Tree:
        //   a
        //   ---- c
        //       ---- b (1)
        //   ----d
        //       ----b (2)
        //   b
        
        contentRepositoryService.deleteLink(nodeB, nodeA)
        nodeA = Content.findByTitle('contentA')
        nodeB = Content.findByTitle('contentB')

        // check that there is only two child for contentA: contentC, contentD
        def references = nodeA.children
        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }
    }

    void testDeleteReferenceCB() {
        // Result Tree:
        //   a
        //   ----b
        //   ----c
        //   ----b (virtual copy 1)
        //   ----d
        //       ----b (virtual copy 2)

        contentRepositoryService.deleteLink(VirtualContent.findWhere(title: 'virtContent1'), nodeC)
        nodeA = Content.findByTitle('contentA')
        nodeC = Content.findByTitle('contentC')
        nodeB = Content.findByTitle('contentB')
        
        // check that contentC does not have any children
        assertEquals 0, nodeC.children.size()

        assertEquals 4, nodeA.children.size()        
    }             

    void testCopyCtoE() {
        insertNewNode()
        // Result Tree:
        //   a
        //   ----b
        //   ----c
        //       ----b (virtual copy) 
        //   ----d
        //       ----b (virtual copy)
        //   e
        //   ----c (virtual copy) 

        assert null != contentRepositoryService.linkNode(nodeC, extraNode, 0)
        nodeC = Content.findByTitle('contentC')

        // check that contentC has 2 parents
        def references = VirtualContent.findAllWhere(target: nodeC)*.parent
        if (nodeC.parent) references << nodeC.parent
        assertEquals 2, references.size()
    }

    void testCopyBtoD() {
        insertNewNode()
        // Result Tree:
        //   a
        //   ----b
        //   ----c
        //       ----b
        //   e
        //   ----b
        //   ----d
        //       ----b
        assert null != contentRepositoryService.linkNode(nodeB, extraNode, 0)
        nodeB = Content.findByTitle('contentB')

        // check that contentB has 4 parents
        def references = VirtualContent.findAllWhere(target: nodeB)*.parent
        if (nodeB.parent) references << nodeB.parent
        assertEquals 4, references.size()

        assertEquals 1, extraNode.children.size()
        
    }

    void testMoveBtoRoot() {
        // Result Tree:
        //   a
        //   b
        //   ----c
        //       ----b (virtual copy)
        //   ----d
        //       ----b (virtual copy)
        contentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = Content.findByTitle('contentA')
        nodeB = Content.findByTitle('contentB')
                
        // check that copies of contentB has two parents: contentC and extraContent
        def references = VirtualContent.findAllWhere(target: nodeB)*.parent
        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }

        assertNull nodeB.parent
        assert nodeA.children.contains(nodeB) == false
    }

    void testMoveB1toRoot() {
        // Result Tree:
        //   b (virtual copy 1)
        //   a
        //   ----b
        //   ----c
        //   ----d
        //       ----b (virtual copy 2)
        contentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = Content.findByTitle('contentA')
        nodeB = Content.findByTitle('contentB')

        // check that copies of contentB has two parents: contentC and extraContent
        def references = VirtualContent.findAllWhere(target: nodeB)*.parent
        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }

        assertNull nodeB.parent
        assert nodeA.children.contains(nodeB) == false
     }

    void testMoveBtoE() {
        insertNewNode()
        // Result Tree:
        //   a
        //   ----c
        //       ----b (virtual copy 1)
        //   ----d
        //       ----b (virtual copy 2)
        //   e
        //   ----b (virtual copy 3)
        contentRepositoryService.moveNode(nodeB, extraNode, 0)
        extraNode = Content.findByTitle('newContent')
        nodeA = Content.findByTitle('contentA')
        nodeB = Content.findByTitle('contentB')

        // check that extraContent has child: contentB
        def references = extraNode.children
        assertEquals 1, references.size()
        assertNotNull references.find { it == nodeB }

        // check that contentA hasn't child contentB
        assert nodeA.children.contains(nodeB) == false
    }

    void testMoveEtoC() {
        insertNewNode()
        // Result Tree:
        //   a
        //   ----b
        //   ----c
        //       ----b
        //       ----e
        contentRepositoryService.moveNode(extraNode, nodeC, 0)
        extraNode = Content.findByTitle('newContent')
        nodeC = Content.findByTitle('contentC')

        // check that extraContent has parent: contentC
        assert extraNode.parent == nodeC
        def references = nodeC.children
        assertEquals 2, references.size()

        assertNotNull references.find { it == VirtualContent.findWhere(title: 'virtContent1')}
        assertNotNull references.find { it == extraNode}
    }

    void testContentDetails() {
        def content = Content.findByTitle('contentA')
        def details = contentRepositoryService.getContentDetails(content)
        assertEquals 'org.weceem.html.HTMLContent', details.className
        assertEquals 'contentA', details.title
        assertEquals 'admin', details.createdBy
        assertNotNull details.createdOn
        // @todo Fix this, for some reason it is being changed, but not sure why yet
        //assertNull details.changedBy // it has not been changed
        //assertNull details.changedOn
        assertEquals content.summary, details.summary
        assertEquals 'org.weceem.html.HTMLContent', details.contentType
    }

    void insertNewNode() {
        extraNode = new HTMLContent(title: 'newContent', aliasURI: 'newContent',
                content: 'sample newContent', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: Space.findWhere(name: 'jcatalog'), keywords: 'software',
                template: Template.findWhere(title: 'template')).save(flush: true)
    }

    void testFindChildrenNoType() {
        def children = contentRepositoryService.findChildren(nodeA)
        assertEquals 3, children.size()
        assertTrue children.contains(nodeB)
        assertTrue children.contains(nodeC)
        assertTrue children.contains(nodeWiki)
    }

    void testFindChildrenWithType() {
        def children = contentRepositoryService.findChildren(nodeA, [type:WikiItem])
        assertEquals 1, children.size()
        assertTrue children.contains(nodeWiki)

        children = contentRepositoryService.findChildren(nodeA, [type:HTMLContent])
        assertEquals 2, children.size()
        assertTrue children.contains(nodeB)
        assertTrue children.contains(nodeC)
    }

    void testFindParentsNoType() {
        def parents = contentRepositoryService.findParents(nodeB)
        assertEquals 3, parents.size()
        assertTrue parents.contains(nodeA)
        assertTrue parents.contains(nodeC)
        assertTrue parents.contains(nodeWiki)
    }

    void testFindParentsWithType() {
        def parents = contentRepositoryService.findParents(nodeB, [type:WikiItem])
        assertEquals 1, parents.size()
        assertTrue parents.contains(nodeWiki)

        parents = contentRepositoryService.findParents(nodeB, [type:HTMLContent])
        assertEquals 2, parents.size()
        assertTrue parents.contains(nodeA)
        assertTrue parents.contains(nodeC)
    }
        

    void testCreateNode() {
        def newnode = new HTMLContent(title: 'contentX', aliasURI: 'contentX',
                content: 'sample X content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)

        assertTrue contentRepositoryService.createNode(newnode, null)

        def newnode2 = new HTMLContent(title: 'contentY', aliasURI: 'contentY',
                content: 'sample Y content', status: defStatus, 
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assertTrue contentRepositoryService.createNode(newnode2, nodeB)
        assertEquals nodeB, newnode2.parent
        assertTrue nodeB.children.contains(newnode2)
    }

    void testFindByRootURI() {
        def node = contentRepositoryService.findRootContentByURI('contentA', spaceA)
        assertEquals nodeA, node
    }

    void testFindChildrenSorted() {
        def nodes = contentRepositoryService.findChildren(nodeA, [params:[sort:'title', order:'desc']])
        
        def expectedNodes = [nodeB, nodeC, nodeWiki]
        assertTrue nodes.every { expectedNodes.contains(it) }
        def expectedTitles = expectedNodes*.title.sort({ a, b -> a.compareTo(b) }).reverse()
        nodes.eachWithIndex { obj, idx -> 
            assertEquals obj.title, expectedTitles[idx]
        }
        
    }

    void testFindAllRootContent() {
        def nodes = contentRepositoryService.findAllRootContent(spaceA)
        println "nodes: $nodes"
        assertEquals 2, nodes.size()
        assertTrue nodes.contains(nodeA)
        assertTrue nodes.contains(template)
    }

    void testFindReferencesTo(){
        def contentC = Content.findByAliasURI("contentC")
        def contentA = Content.findByAliasURI("contentA")
        def virtContent1 = VirtualContent.findByAliasURI("virtContent1")
        def refs = contentRepositoryService.findReferencesTo(contentC)
        for (ref in refs){
            assert (ref.referencingContent.aliasURI == contentA.aliasURI) || 
            (ref.referencingContent.aliasURI == virtContent1.aliasURI)
            assert ref.targetContent.aliasURI == contentC.aliasURI
            def refCont = ref.referencingContent."${ref.referringProperty}"
            if (refCont instanceof Content){
                assert refCont.aliasURI == contentC.aliasURI
            }
            if (refCont instanceof Collection){
                assert contentC in refCont
            }
        }
    
    }

}
