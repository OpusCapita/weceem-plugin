package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)

class ContentRepositoryServiceTests extends AbstractWeceemIntegrationTest {
    
    static transactional = true
    
    def nodeA
    def nodeB
    def nodeC
    def nodeWiki
    def spaceA
    def spaceB
    def spaceC
    def template
    def defStatus
    def virtContent1
    def virtContent2
    def virtContent3
    def defaultDocRootIndex
    def defaultDocSubIndex
    def defaultDocSubIndexHtml
    def defaultDocSubParent
    def defaultDocSubParent2
    def defaultDocTestFolder
    def defaultDocTestFolderIndex

    def spaceBnodeA
    def spaceBdefaultDoc
    
    def extraNode

    void setUp() {
        super.setUp()
        
        defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        spaceA = new WcmSpace(name: 'jcatalog', aliasURI: 'jcatalog')
        assert spaceA.save(flush: true)
        spaceB = new WcmSpace(name: 'other', aliasURI: 'other')
        assert spaceB.save(flush: true)
        spaceC = new WcmSpace(name: 'root', aliasURI: '')
        assert spaceC.save(flush: true)

        template = new WcmTemplate(title: 'template', aliasURI: 'template',
                    space: spaceA, status: defStatus,
                    createdBy: 'admin', createdOn: new Date(),
                    content: 'template content', orderIndex: 0)
        assert template.save(flush: true)
        nodeA = new WcmHTMLContent(title: 'contentA', aliasURI: 'contentA',
                content: 'sample A content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert nodeA.save(flush: true)
        nodeB = new WcmHTMLContent(title: 'contentB', aliasURI: 'contentB',
                parent: nodeA,
                content: 'sample B content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 2)
        assert nodeB.save(flush:true)
        nodeC = new WcmHTMLContent(title: 'contentC', aliasURI: 'contentC',
                parent: nodeA,
                content: 'sample C content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 3)
        assert nodeC.save(flush:true)
        nodeWiki = new WcmWikiItem(title: 'contentD', aliasURI: 'contentD',
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

        virtContent1 = new WcmVirtualContent(title: 'virtContent1', aliasURI: 'virtContent1',
                           parent: nodeC, target: nodeB, status: defStatus,
                           content: 'WcmVirtualContent B for nodeC',
                           space: spaceA, orderIndex: 5)
        assert virtContent1.save(flush:true)
        nodeC.children = new TreeSet()
        nodeC.children << virtContent1
        nodeC.save()

        virtContent2 = new WcmVirtualContent(title: 'virtContent2', aliasURI: 'virtContent2',
                           parent: nodeWiki, target: nodeB, status: defStatus,
                           content: 'WcmVirtualContent B for nodeWiki',
                           space: spaceA, orderIndex: 6)
        assert virtContent2.save(flush:true)
        virtContent3 = new WcmVirtualContent(title: 'virtContent3', aliasURI: 'virtContent3',
                           parent: nodeWiki, target: nodeA, status: defStatus,
                           content: 'WcmVirtualContent cyclic node A for nodeWiki',
                           space: spaceA, orderIndex: 7)
        assert virtContent3.save(flush:true)

        nodeWiki.children = new TreeSet()
        nodeWiki.children << virtContent2
        nodeWiki.children << virtContent3
        nodeWiki.save()    
          
        defaultDocRootIndex = new WcmHTMLContent(title: 'root index', aliasURI: 'index',
            content: 'root index content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocRootIndex.save(flush:true)
                           
        defaultDocSubParent = new WcmHTMLContent(title: 'default test sub', aliasURI: 'defaultTest',
            content: 'default doc sub parent 1 content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocSubParent.save(flush:true)
                           
        defaultDocSubParent2 = new WcmHTMLContent(title: 'default test 2 sub', aliasURI: 'defaultTest2',
            content: 'default doc sub parent 2 content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocSubParent2.save(flush:true)
                           
        defaultDocSubIndex = new WcmHTMLContent(title: 'sub index', aliasURI: 'index',
            parent: defaultDocSubParent,
            content: 'sub index content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocSubIndex.save(flush:true)
                           
        defaultDocSubIndexHtml = new WcmHTMLContent(title: 'sub index html', aliasURI: 'index.html',
            parent: defaultDocSubParent2,    
            content: 'sub index html content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocSubIndexHtml.save(flush:true)
                         
        defaultDocSubParent.children = new TreeSet()  
        defaultDocSubParent.children << defaultDocSubIndex
        assert defaultDocSubParent.save()

        defaultDocSubParent2.children = new TreeSet()  
        defaultDocSubParent2.children << defaultDocSubIndexHtml
        assert defaultDocSubParent2.save()
        
        defaultDocTestFolder = new WcmFolder(title: 'default test folder', aliasURI: 'defaultTestFolder',
            status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocTestFolder.save(flush:true)
                           
        defaultDocTestFolderIndex = new WcmHTMLContent(title: 'folder index', aliasURI: 'index',
            parent: defaultDocTestFolder,
            content: 'folder index content', status: defStatus,
            createdBy: 'admin', createdOn: new Date(),
            space: spaceA)
        assert defaultDocTestFolder.save(flush:true)

        defaultDocTestFolder.children = new TreeSet()  
        defaultDocTestFolder.children << defaultDocTestFolderIndex
        assert defaultDocTestFolder.save()

        // Tree structure:
        //
        //   a
        //   ----b (real)
        //   ----c
        //       ----b (virtual 1)
        //   ----d
        //       ----b (virtual 2)


        spaceBnodeA = new WcmHTMLContent(title: 'Other Index', aliasURI: 'contentA',
                content: 'Other Index page', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceB, 
                orderIndex: 0).save()
        assert spaceBnodeA
        
        spaceBdefaultDoc = new WcmHTMLContent(title: 'spaceb default', aliasURI: 'index',
                content: 'spaceb default page', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceB, 
                orderIndex: 1).save()
        assert spaceBdefaultDoc
        
        wcmContentRepositoryService.wcmContentDependencyService.reset()
        wcmContentRepositoryService.invalidateCachingForSpace(spaceA)
        wcmContentRepositoryService.invalidateCachingForSpace(spaceB)
    }

    void tearDown() {
        WcmContent.list().each {
            it.children = [] as SortedSet
            it.parent = null 
        }
        WcmHTMLContent.list()*.template = null
        WcmWikiItem.list()*.template = null
        WcmVirtualContent.list()*.target = null
        //WcmContent.list()*.delete(flush:true)
    }

    void testResolveSpaceAndURI() {
        def res = wcmContentRepositoryService.resolveSpaceAndURI('/jcatalog/anything')
        assert res.space.id == spaceA.id
        assert 'anything'.equals(res.uri)
        
        res = wcmContentRepositoryService.resolveSpaceAndURI('jcatalog/')
        assert res.space.id == spaceA.id
        assert ''.equals(res.uri)

        res = wcmContentRepositoryService.resolveSpaceAndURI('jcatalog/anything')
        assert res.space.id == spaceA.id
        assert 'anything'.equals(res.uri)

        res = wcmContentRepositoryService.resolveSpaceAndURI('other/anything')
        assert res.space.id == spaceB.id
        assert 'anything'.equals(res.uri)

        res = wcmContentRepositoryService.resolveSpaceAndURI('libs/jquery/jquery-1.4.2.min.js')
        assert res.space.id == spaceC.id
        assert 'libs/jquery/jquery-1.4.2.min.js'.equals(res.uri)

        res = wcmContentRepositoryService.resolveSpaceAndURI('other/')
        assert res.space.id == spaceB.id
        assert ''.equals(res.uri)
    }

    void testFindContentForPath() {
        // Without cache
        assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('contentA', spaceA, false).content.id)
        assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA, false).content.id)
        assert nodeA.id != wcmContentRepositoryService.findContentForPath('contentA', spaceB, false).content.id
        assert wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceB, false) == null

        // With cache, once to load, second to hit
        2.times {
            assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('contentA', spaceA).content.id)
            assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id)
            assert nodeA.id != wcmContentRepositoryService.findContentForPath('contentA', spaceB).content.id
            assert wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceB) == null
        }
    }
    
    void testFindContentForPathWithDefaultDocumentURIs() {
        Closure doTests = { withCache ->
            assert defaultDocRootIndex.ident()
                .equals(wcmContentRepositoryService.findContentForPath('', spaceA, withCache).content?.ident())

            assert defaultDocRootIndex.ident()
                .equals(wcmContentRepositoryService.findContentForPath('index', spaceA, withCache).content?.ident())

            assert spaceBdefaultDoc.ident()
                .equals(wcmContentRepositoryService.findContentForPath('index', spaceB, withCache).content?.ident())


            assert defaultDocSubParent.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTest', spaceA, withCache).content?.ident())
            assert defaultDocSubIndex.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTest/', spaceA, withCache).content?.ident())
            assert defaultDocSubParent2.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTest2', spaceA, withCache).content?.ident())
            assert defaultDocSubIndexHtml.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTest2/', spaceA, withCache).content?.ident())

            // Test for non-standaloneContent types - this MUST NOT resolve to default docs
            assert defaultDocTestFolder.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTestFolder', spaceA, withCache).content?.ident())

            // Test for non-standaloneContent types - this MUST resolve to default doc
            assert defaultDocTestFolderIndex.ident()
                .equals(wcmContentRepositoryService.findContentForPath('defaultTestFolder/', spaceA, withCache).content?.ident())
        }
        
        // Do first with no caching
        doTests(false)
        
        // Do with cache twice, so first caches, second gets from cache
        doTests(true)
        doTests(true)
    }

    void testUpdatingNodesMaintainsURICacheIntegrity() {
        assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('contentA', spaceA).content.id)
        assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id)

        def result = wcmContentRepositoryService.updateNode(nodeA.id.toString(), [aliasURI:'changed-alias'])
        assert (!result.notFound)
        assert (!result.errors)
        
        println "Trying to get content info"
        assert wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA') == null
        assert wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA/contentB') == null
        assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('changed-alias', spaceA).content.id)
        assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('changed-alias/contentB', spaceA).content.id)
    }    
    

    void testUpdatingNodeInvalidatesPreviouslyNotFoundCacheEntry() {
        assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('contentA', spaceA).content.id)
        assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id)

        // First, foul the cache
        wcmContentRepositoryService.findContentForPath('changed-alias', spaceA)

        // Now update to the URL that was sought
        def result = wcmContentRepositoryService.updateNode(nodeA.id.toString(), [aliasURI:'changed-alias'])
        assert (!result.notFound)
        assert (!result.errors)
        
        
        assert wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA') == null
        assert wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA/contentB') == null
        assert nodeA.id.equals(wcmContentRepositoryService.findContentForPath('changed-alias', spaceA).content.id)
        assert nodeB.id.equals(wcmContentRepositoryService.findContentForPath('changed-alias/contentB', spaceA).content.id)
    }    

    /*
    void testDeletePreventedIfHaschildren() {
        shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeA)
        }
    }
    */

    void testDeletePreventedIfVirtualContentReferencesIt() {
        def kids = nodeA.children.collect { it }
        for (child in kids){
            nodeA.children.remove(child)
            child.parent = null
        }
        nodeA.children.clear()
        assert nodeA.save(flush:true)
        /*
        shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeA)
        } */
    }

    void testDeleteNodeA() {
        // Result Tree:
        //   c
        //   ----b
        //   ----e
        //       ----b
        def kids = nodeA.children.collect { it }
        for (child in kids){
            nodeA.children.remove(child)
            child.parent = null
        }
        nodeA.children.clear()
        assert nodeA.save(flush:true)
        
        // Remove virtual ref
        wcmContentRepositoryService.deleteNode(virtContent3)

        // We deleted without using the service...
        wcmContentRepositoryService.wcmContentDependencyService.reload()
        
        // Now remove the node
        wcmContentRepositoryService.deleteNode(nodeA)

        // check that contentA was deleted
        assert "Content should not exist!", WcmContent.findByTitle('contentA') == null

        // check that B and C still exist (not cascaded delete)
        assert WcmContent.findByTitle('contentB')
        assert WcmContent.findByTitle('contentC')

        // check that template still exists
        assert WcmContent.findByTitle('template')

        def spaceAcA = wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA')
        assert spaceAcA == null

        // We use the old pre-deletion instance to look up cache info here, to make sure they are flushed
        
        assert wcmContentRepositoryService.wcmContentFingerprintService.getFingerprintFor(nodeA, false) == null
        assert wcmContentRepositoryService.wcmContentDependencyService.getDependencyPathsOf(nodeA).size() == 0
    }

    void testDeleteNodeB() {
        // Result Tree:
        //   a
        //   ----c
        //   ----d
        
       /*shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeB)
        }
       */

        // Remove the virtual ref from its parent
        nodeC.children.remove(virtContent1)
        assert nodeC.save(flush:true)
        nodeWiki.children.remove(virtContent2)
        assert nodeWiki.save(flush:true)

        // Now delete the virtual refs to B
        virtContent1.delete(flush: true)
        virtContent2.delete(flush: true)
        
        // We deleted without using the service...
        wcmContentRepositoryService.wcmContentDependencyService.reload()

        wcmContentRepositoryService.deleteNode(nodeB)
        nodeA.refresh()
        nodeC.refresh()
        nodeWiki.refresh()

        // check that there are two childen for contentA: contentC + contentD
        def references = nodeA.children

        assert 2 == references.size()
        assert references.find { it == nodeC }
        assert references.find { it == nodeWiki }
        
        // check that there are no children for contentC and contentD
        assert 0 == nodeC.children.size()
        assert 1 == nodeWiki.children.size()
    }

    void testDeleteMultipleNodesOrder1() {
        def ids = [nodeWiki.ident(), nodeC.ident(), nodeA.ident()]
        println "Deleting multi nodes 1 - A"
        wcmContentRepositoryService.deleteNodes(WcmVirtualContent.list())
        println "Deleting multi nodes 1 - B"
        wcmContentRepositoryService.removeAllReferencesTo([nodeA, nodeWiki, nodeC])
        println "Deleting multi nodes 1 - C"
        wcmContentRepositoryService.deleteNodes([nodeWiki, nodeC, nodeA], true)
        
        ids.each { id ->
            assert WcmContent.get(id) == null
        }
    }
    
    void testDeleteMultipleNodesOrder2() {
        def ids = [nodeWiki.ident(), nodeC.ident(), nodeA.ident()]
        println "Deleting multi nodes 2 - A"
        wcmContentRepositoryService.deleteNodes(WcmVirtualContent.list())

        println "Deleting multi nodes 2 - B"
        wcmContentRepositoryService.removeAllReferencesTo([nodeA, nodeWiki, nodeC])
        
        println "Deleting multi nodes 2 - C"
        wcmContentRepositoryService.deleteNodes([nodeA, nodeWiki, nodeC], true)
        
        ids.each { id ->
            assert WcmContent.get(id) == null
        }
    }

    void testDeleteVirtualContent() {
        // Result Tree:
        //   a
        //   ----c
        //   ----d

        wcmContentRepositoryService.deleteNode(virtContent1)

        nodeA = WcmContent.findByTitle('contentA')
        nodeC = WcmContent.findByTitle('contentC')
        nodeWiki = WcmContent.findByTitle('contentD')

        // Make sure the target of the virtual content is not deleted
        nodeB = WcmContent.findByTitle('contentB')
        assert nodeB

        // check that there are two childen for contentA: contentC + contentD
        def references = nodeA.children

        // There should still be 3 children (includiong B)
        assert 3 == references.size()
        assert references.find { it == nodeB }
        assert references.find { it == nodeC }
        assert references.find { it == nodeWiki }

        // check that there are no children for contentC and contentD
        assert 0 == nodeC.children.size()
    }

    void testDeleteReferenceAC() {
        // Result Tree:
        //   a
        //   ----b
        //   c
        //   ----b
        //   ----e
        //       ----b
        
        wcmContentRepositoryService.deleteLink(nodeC, nodeA)
        nodeA = WcmContent.findByTitle('contentA')
        nodeC = WcmContent.findByTitle('contentC')
        
        // check that nodeC do not have parent contentA
        def references = WcmVirtualContent.findAllWhere(target: nodeC)*.parent
        if (nodeC.parent) references << nodeC.parent
        assert 0 == references.size()

        // check that nodeA do not have child contentC
        assert nodeA.children.find { it == nodeC } == null
    }

    void testDeleteReferenceAB() {
        // Result Tree:
        //   a
        //   ---- c
        //       ---- b (1)
        //   ----d
        //       ----b (2)
        //   b
        
        wcmContentRepositoryService.deleteLink(nodeB, nodeA)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

        // check that there is only two child for contentA: contentC, contentD
        def references = nodeA.children
        assert 2 == references.size()
        assert references.find { it == nodeC }
        assert references.find { it == nodeWiki }
    }

    void testDeleteReferenceCB() {
        // Result Tree:
        //   a
        //   ----b
        //   ----c
        //   ----b (virtual copy 1)
        //   ----d
        //       ----b (virtual copy 2)

        wcmContentRepositoryService.deleteLink(WcmVirtualContent.findWhere(title: 'virtContent1'), nodeC)
        nodeA = WcmContent.findByTitle('contentA')
        nodeC = WcmContent.findByTitle('contentC')
        nodeB = WcmContent.findByTitle('contentB')
        
        // check that contentC does not have any children
        assert 0 == nodeC.children.size()

        assert 4 == nodeA.children.size()
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

        assert null != wcmContentRepositoryService.linkNode(nodeC, extraNode, 0)
        nodeC = WcmContent.findByTitle('contentC')

        // check that contentC has 2 parents
        def references = WcmVirtualContent.findAllWhere(target: nodeC)*.parent
        if (nodeC.parent) references << nodeC.parent
        assert 2 == references.size()
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
        assert null != wcmContentRepositoryService.linkNode(nodeB, extraNode, 0)
        nodeB = WcmContent.findByTitle('contentB')

        // check that contentB has 4 parents
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
        if (nodeB.parent) references << nodeB.parent
        assert 4 == references.size()

        assert 1 == extraNode.children.size()
        
    }

    void testMoveBtoRoot() {
        // Result Tree:
        //   a
        //   b
        //   ----c
        //       ----b (virtual copy)
        //   ----d
        //       ----b (virtual copy)

        // Force caching of the URI
        def beforeURI = nodeB.absoluteURI
        assert wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)

        def nodeBInfo = wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA/contentB')
        assert nodeBInfo.id.equals(nodeB.id)

        wcmContentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')
                
        // check that copies of contentB has two parents: contentC and extraContent
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
        assert 2 == references.size()
        assert references.find { it == nodeC }
        assert references.find { it == nodeWiki }

        assert nodeB.parent == null
        assert nodeA.children.contains(nodeB) == false

        // Make sure the content uri cache has been invalidated for old URI
        nodeBInfo = wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA/contentB')
        assert null == nodeBInfo

        // Do this AFTER checking cache explicitly, because findContentForPath will force caching of a "not found" entry
        println "Old URI for the node is [$beforeURI], new is ${nodeB.absoluteURI}"
        assert wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space) == null
        assert wcmContentRepositoryService.findContentForPath(nodeB.absoluteURI, nodeB.space)

        nodeBInfo = wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentB')
        assert nodeB.ident().equals(nodeBInfo.id)
    }

    void testMoveB1toRoot() {
        // Result Tree:
        //   b (virtual copy 1)
        //   a
        //   ----b
        //   ----c
        //   ----d
        //       ----b (virtual copy 2)
        // Force caching of the URI
        def beforeURI = nodeB.absoluteURI
        assert wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)

        wcmContentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

        // check that copies of contentB has two parents: contentC and extraContent
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
        assert 2 == references.size()
        assert references.find { it == nodeC }
        assert references.find { it == nodeWiki }

        assert nodeB.parent == null
        assert nodeA.children.contains(nodeB) == false

        // Make sure the content uri cache has been invalidated for old URI
        println "Old URI for the node is [$beforeURI], new is ${nodeB.absoluteURI}"
        assert wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space) == null
        assert wcmContentRepositoryService.findContentForPath(nodeB.absoluteURI, nodeB.space)
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
        //   ----b
        wcmContentRepositoryService.moveNode(nodeB, extraNode, 0)
        extraNode = WcmContent.findByTitle('newContent')
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

        // check that extraContent has child: contentB
        def references = extraNode.children
        assert 1 == references.size()
        assert references.find { it == nodeB }

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
        wcmContentRepositoryService.moveNode(extraNode, nodeC, 0)
        extraNode = WcmContent.findByTitle('newContent')
        nodeC = WcmContent.findByTitle('contentC')

        // check that extraContent has parent: contentC
        assert extraNode.parent == nodeC
        def references = nodeC.children
        assert 2 == references.size()

        assert references.find { it == WcmVirtualContent.findWhere(title: 'virtContent1')}
        assert references.find { it == extraNode}
    }

    void testContentDetails() {
        def content = WcmContent.findByTitle('contentA')
        def details = wcmContentRepositoryService.getContentDetails(content)
        assert 'org.weceem.html.WcmHTMLContent'.equals(details.className)
        assert 'contentA'.equals(details.title)
        assert 'admin'.equals(details.createdBy)
        assert details.createdOn
        // @todo Fix this, for some reason it is being changed, but not sure why yet
        //assertNull details.changedBy // it has not been changed
        //assertNull details.changedOn
        assert content.summary.equals(details.summary)
        assert 'org.weceem.html.WcmHTMLContent'.equals(details.contentType)
    }

    void insertNewNode() {
        extraNode = new WcmHTMLContent(title: 'newContent', aliasURI: 'newContent',
                content: 'sample newContent', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: WcmSpace.findWhere(name: 'jcatalog'), keywords: 'software',
                template: WcmTemplate.findWhere(title: 'template')).save(flush: true)
    }

    void testFindChildrenNoType() {
        def children = wcmContentRepositoryService.findChildren(nodeA)
        assert 3 == children.size()
        assert children.contains(nodeB)
        assert children.contains(nodeC)
        assert children.contains(nodeWiki)
    }

    void testFindDescendents() {
        def descendents = wcmContentRepositoryService.findDescendents(nodeA)
        assert 6 == descendents.size()
        assert descendents.contains(nodeB)
        assert descendents.contains(nodeC)
        assert descendents.contains(nodeWiki)
        assert descendents.contains(virtContent1)
        assert descendents.contains(virtContent2)
        assert descendents.contains(virtContent3)

        assert ([nodeB, nodeC, nodeWiki, virtContent1, virtContent2, virtContent3]*.absoluteURI == descendents*.absoluteURI)
    }

    void testFindDescendentsDepthFirst() {
        def descendents = wcmContentRepositoryService.findDescendentsDepthFirst(nodeA)
        assert 6 == descendents.size()
        assert descendents.contains(nodeB)
        assert descendents.contains(nodeC)
        assert descendents.contains(nodeWiki)
        assert descendents.contains(virtContent1)
        assert descendents.contains(virtContent2)
        assert descendents.contains(virtContent3)

        assert([nodeB, virtContent1, nodeC, virtContent2, virtContent3, nodeWiki ]*.absoluteURI == descendents*.absoluteURI)
    }

    void testFindChildrenWithType() {
        def children = wcmContentRepositoryService.findChildren(nodeA, [type:WcmWikiItem])
        assert 1 == children.size()
        assert children.contains(nodeWiki)

        children = wcmContentRepositoryService.findChildren(nodeA, [type:WcmHTMLContent])
        assert 2 == children.size()
        assert children.contains(nodeB)
        assert children.contains(nodeC)
    }

    void testFindParentsNoType() {
        def parents = wcmContentRepositoryService.findParents(nodeB)
        assert 3 == parents.size()
        assert parents.contains(nodeA)
        assert parents.contains(nodeC)
        assert parents.contains(nodeWiki)
    }

    void testFindParentsWithType() {
        def parents = wcmContentRepositoryService.findParents(nodeB, [type:WcmWikiItem])
        assert 1 == parents.size()
        assert parents.contains(nodeWiki)

        parents = wcmContentRepositoryService.findParents(nodeB, [type:WcmHTMLContent])
        assert 2 == parents.size()
        assert parents.contains(nodeA)
        assert parents.contains(nodeC)
    }
        

    void testCreateNode() {
        def newnode = new WcmHTMLContent(title: 'contentX', aliasURI: 'contentX',
                content: 'sample X content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)

        assert wcmContentRepositoryService.createNode(newnode, null)

        def newnode2 = new WcmHTMLContent(title: 'contentY', aliasURI: 'contentY',
                content: 'sample Y content', status: defStatus, 
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assert wcmContentRepositoryService.createNode(newnode2, nodeB)
        assert nodeB.equals(newnode2.parent)
        assert nodeB.children.contains(newnode2)
    }

    void testFindByRootURI() {
        def node = wcmContentRepositoryService.findRootContentByURI('contentA', spaceA)
        assert nodeA.equals(node)
    }

    void testFindChildrenSorted() {
        def nodes = wcmContentRepositoryService.findChildren(nodeA, [params:[sort:'title', order:'desc']])
        
        def expectedNodes = [nodeB, nodeC, nodeWiki]
        assert nodes.every { expectedNodes.contains(it) }
        def expectedTitles = expectedNodes*.title.sort({ a, b -> a.compareTo(b) }).reverse()
        nodes.eachWithIndex { obj, idx -> 
            assert obj.title.equals(expectedTitles[idx])
        }
        
    }

    void testFindAllRootContent() {
        def nodes = wcmContentRepositoryService.findAllRootContent(spaceA)
        println "nodes: $nodes"
        assert 6 == nodes.size()
        assert nodes.contains(nodeA)
        assert nodes.contains(template)
    }
    
    void testFindAllContent() {
        def nodes = wcmContentRepositoryService.findAllContent(spaceA)
        println "nodes: $nodes"
        assert 15 == nodes.size()
        assert nodes.contains(nodeA)
        assert nodes.contains(nodeB)
        assert nodes.contains(nodeC)
        assert nodes.contains(nodeWiki)
        assert nodes.contains(template)
        assert nodes.contains(virtContent1)
        assert nodes.contains(virtContent2)
        assert nodes.contains(virtContent3)
    }
    
    void testFindAllContentWithType() {
        def nodes = wcmContentRepositoryService.findAllContent(spaceA, [type: 'org.weceem.html.WcmHTMLContent'])
        println "nodes: $nodes"
        assert 9 == nodes.size()
        assert nodes.contains(nodeA)
        assert nodes.contains(nodeB)
        assert nodes.contains(nodeC)
    }
    
    void testFindAllContentWithStatus() {
        def status100 = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert status100.save(flush:true)
        def status200 = new WcmStatus(code: 200, description: "review", publicContent: true)
        assert status200.save(flush:true)
        nodeA.status = status100
        assert nodeA.save(flush: true)
        nodeC.status = status200
        assert nodeC.save(flush: true)
        
        def nodes = wcmContentRepositoryService.findAllContent(spaceA, [status: [100, 200]])
        println "nodes: $nodes"
        assert 2 == nodes.size()
        assert nodes.contains(nodeA)
        assert nodes.contains(nodeC)
    }
    
    void testCountContent() {
        assert (15 == wcmContentRepositoryService.countContent(spaceA))
    }
    
    void testCountContentWithType() {
        assert (9 == wcmContentRepositoryService.countContent(spaceA, [type: 'org.weceem.html.WcmHTMLContent']))
    }
    
    void testCountContentWithStatus() {
        def status100 = new WcmStatus(code: 100, description: "draft", publicContent: false)
        assert status100.save(flush:true)
        def status200 = new WcmStatus(code: 200, description: "review", publicContent: true)
        assert status200.save(flush:true)
        nodeA.status = status100
        assert nodeA.save(flush: true)
        nodeC.status = status200
        assert nodeC.save(flush: true)
        
        assert (2 == wcmContentRepositoryService.countContent(spaceA, [status: [100, 200]]))
    }


    void testFindReferencesTo(){
        def contentC = WcmContent.findByAliasURI("contentC")
        def contentA = WcmContent.findByAliasURI("contentA")
        def virtContent1 = WcmVirtualContent.findByAliasURI("virtContent1")
        def refs = wcmContentRepositoryService.findReferencesTo(contentC)
        for (ref in refs){
            assert (ref.referencingContent.aliasURI == contentA.aliasURI) || 
            (ref.referencingContent.aliasURI == virtContent1.aliasURI)
            assert ref.targetContent.aliasURI == contentC.aliasURI
            def refCont = ref.referencingContent."${ref.referringProperty}"
            if (refCont instanceof WcmContent){
                assert refCont.aliasURI == contentC.aliasURI
            }
            if (refCont instanceof Collection){
                assert contentC in refCont
            }
        }
    
    }

    void testCreateNodeFailsForInvalidContent() {
        def content = new WcmHTMLContent(title:'test')
        content.space = spaceA
        assert !content.validate()
        assert !wcmContentRepositoryService.createNode(content, null)
    }

    void testCreateFolderInEmptySpace() {
        def space = new WcmSpace(name:'dummy', aliasURI:'dummy')
        assert space.save()
        def content = new WcmFolder(title:'testing')
        content.space = space
        content.status = wcmContentRepositoryService.allPublicStatuses[0]
        def ok = wcmContentRepositoryService.createNode(content, null)
        println "content errors: ${content.errors}"
        assert ok
    }

    void testCreateNodeFailsForInvalidParams() {
        assert wcmContentRepositoryService.createNode('org.weceem.html.WcmHTMLContent', [space:spaceA]).hasErrors()
    }
    
    void testURIToIdCacheDoesNotLeakAcrossSpaces() {
        wcmContentRepositoryService.findContentForPath('contentA', spaceA)
        wcmContentRepositoryService.findContentForPath('contentA', spaceB)
        
        def spaceAcA = wcmContentRepositoryService.getCachedContentInfoFor(spaceA, 'contentA')
        assert nodeA.ident().equals(spaceAcA.id)
        def spaceBcA = wcmContentRepositoryService.getCachedContentInfoFor(spaceB, 'contentA')
        assert spaceBnodeA.ident().equals(spaceBcA.id)
    }
}
