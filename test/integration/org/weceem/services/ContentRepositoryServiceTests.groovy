package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*

class ContentRepositoryServiceTests extends AbstractWeceemIntegrationTest {
    
    static transactional = true
    
    def nodeA
    def nodeB
    def nodeC
    def nodeWiki
    def spaceA
    def spaceB
    def template
    def defStatus
    def virtContent1
    def virtContent2
    def virtContent3
    
    def extraNode

    void setUp() {
        super.setUp()
        
        defStatus = new WcmStatus(code: 400, description: "published", publicContent: true)
        assert defStatus.save(flush:true)

        spaceA = new WcmSpace(name: 'jcatalog', aliasURI: 'jcatalog')
        assert spaceA.save(flush: true)
        spaceB = new WcmSpace(name: 'other', aliasURI: 'other')
        assert spaceB.save(flush: true)

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
                           
        // Tree structure:
        //
        //   a
        //   ----b (real)
        //   ----c
        //       ----b (virtual 1)
        //   ----d
        //       ----b (virtual 2)


        assert new WcmHTMLContent(title: 'Other Index', aliasURI: 'contentA',
                content: 'Other Index page', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceB, 
                orderIndex: 0).save()
        
        wcmContentRepositoryService.wcmContentDependencyService.reset()
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
        assertEquals 'anything', res.uri
        
        res = wcmContentRepositoryService.resolveSpaceAndURI('jcatalog/anything')
        assert res.space.id == spaceA.id
        assertEquals 'anything', res.uri

        res = wcmContentRepositoryService.resolveSpaceAndURI('other/anything')
        assert res.space.id == spaceB.id
        assertEquals 'anything', res.uri
    }

    void testFindContentForPath() {
        // Without cache
        assertEquals nodeA.id, wcmContentRepositoryService.findContentForPath('contentA', spaceA, false).content.id
        assertEquals nodeB.id, wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA, false).content.id
        assertFalse nodeA.id == wcmContentRepositoryService.findContentForPath('contentA', spaceB, false).content.id
        assertNull wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceB, false)

        // With cache, once to load, second to hit
        2.times {
            assertEquals nodeA.id, wcmContentRepositoryService.findContentForPath('contentA', spaceA).content.id
            assertEquals nodeB.id, wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id
            assertFalse nodeA.id == wcmContentRepositoryService.findContentForPath('contentA', spaceB).content.id
            assertNull wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceB)
        }
    }
    
    void testUpdatingNodesMaintainsURICacheIntegrity() {
        assertEquals nodeA.id, wcmContentRepositoryService.findContentForPath('contentA', spaceA).content.id
        assertEquals nodeB.id, wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA).content.id

        def result = wcmContentRepositoryService.updateNode(nodeA.id.toString(), [aliasURI:'changed-alias'])
        assertTrue(!result.notFound)
        assertTrue(!result.errors)
        
        assertNull wcmContentRepositoryService.findContentForPath('contentA', spaceA)
        assertNull wcmContentRepositoryService.findContentForPath('contentA/contentB', spaceA)
        assertEquals nodeA.id, wcmContentRepositoryService.findContentForPath('changed-alias', spaceA).content.id
        assertEquals nodeB.id, wcmContentRepositoryService.findContentForPath('changed-alias/contentB', spaceA).content.id
    }    
    
    void testDeletePreventedIfHaschildren() {
        shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeA)
        }
    }

    void testDeletePreventedIfVirtualContentReferencesIt() {
        def kids = nodeA.children.collect { it }
        for (child in kids){
            nodeA.children.remove(child)
            child.parent = null
        }
        nodeA.children.clear()
        assert nodeA.save(flush:true)

        shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeA)
        }
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
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

        // check that contentA was deleted
        assertTrue "Content should not exist!", WcmContent.findByTitle('contentA') == null

        // check that B and C still exist (not cascaded delete)
        assertNotNull "Content B should exist!", WcmContent.findByTitle('contentB')
        assertNotNull "Content C should exist!", WcmContent.findByTitle('contentC')

        // check that template still exists
        assertNotNull "Template should exist!", WcmContent.findByTitle('template')
    }

    void testDeleteNodeB() {
        // Result Tree:
        //   a
        //   ----c
        //   ----d
        
        shouldFail(DeleteNotPossibleException) {
            wcmContentRepositoryService.deleteNode(nodeB)
        }

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

        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }
        
        // check that there are no children for contentC and contentD
        assertEquals 0, nodeC.children.size()
        assertEquals 1, nodeWiki.children.size()
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
        
        wcmContentRepositoryService.deleteLink(nodeC, nodeA)
        nodeA = WcmContent.findByTitle('contentA')
        nodeC = WcmContent.findByTitle('contentC')
        
        // check that nodeC do not have parent contentA
        def references = WcmVirtualContent.findAllWhere(target: nodeC)*.parent
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
        
        wcmContentRepositoryService.deleteLink(nodeB, nodeA)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

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

        wcmContentRepositoryService.deleteLink(WcmVirtualContent.findWhere(title: 'virtContent1'), nodeC)
        nodeA = WcmContent.findByTitle('contentA')
        nodeC = WcmContent.findByTitle('contentC')
        nodeB = WcmContent.findByTitle('contentB')
        
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

        assert null != wcmContentRepositoryService.linkNode(nodeC, extraNode, 0)
        nodeC = WcmContent.findByTitle('contentC')

        // check that contentC has 2 parents
        def references = WcmVirtualContent.findAllWhere(target: nodeC)*.parent
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
        assert null != wcmContentRepositoryService.linkNode(nodeB, extraNode, 0)
        nodeB = WcmContent.findByTitle('contentB')

        // check that contentB has 4 parents
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
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

        // Force caching of the URI
        def beforeURI = nodeB.absoluteURI
        assertNotNull wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)

        wcmContentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')
                
        // check that copies of contentB has two parents: contentC and extraContent
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }

        assertNull nodeB.parent
        assert nodeA.children.contains(nodeB) == false

        // Make sure the content uri cache has been invalidated for old URI
        println "Old URI for the node is [$beforeURI], new is ${nodeB.absoluteURI}"
        assertNull wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)
        assertNotNull wcmContentRepositoryService.findContentForPath(nodeB.absoluteURI, nodeB.space)
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
        assertNotNull wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)

        wcmContentRepositoryService.moveNode(nodeB, null, 0)
        nodeA = WcmContent.findByTitle('contentA')
        nodeB = WcmContent.findByTitle('contentB')

        // check that copies of contentB has two parents: contentC and extraContent
        def references = WcmVirtualContent.findAllWhere(target: nodeB)*.parent
        assertEquals 2, references.size()
        assertNotNull references.find { it == nodeC }
        assertNotNull references.find { it == nodeWiki }

        assertNull nodeB.parent
        assert nodeA.children.contains(nodeB) == false

        // Make sure the content uri cache has been invalidated for old URI
        println "Old URI for the node is [$beforeURI], new is ${nodeB.absoluteURI}"
        assertNull wcmContentRepositoryService.findContentForPath(beforeURI, nodeB.space)
        assertNotNull wcmContentRepositoryService.findContentForPath(nodeB.absoluteURI, nodeB.space)
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
        wcmContentRepositoryService.moveNode(extraNode, nodeC, 0)
        extraNode = WcmContent.findByTitle('newContent')
        nodeC = WcmContent.findByTitle('contentC')

        // check that extraContent has parent: contentC
        assert extraNode.parent == nodeC
        def references = nodeC.children
        assertEquals 2, references.size()

        assertNotNull references.find { it == WcmVirtualContent.findWhere(title: 'virtContent1')}
        assertNotNull references.find { it == extraNode}
    }

    void testContentDetails() {
        def content = WcmContent.findByTitle('contentA')
        def details = wcmContentRepositoryService.getContentDetails(content)
        assertEquals 'org.weceem.html.WcmHTMLContent', details.className
        assertEquals 'contentA', details.title
        assertEquals 'admin', details.createdBy
        assertNotNull details.createdOn
        // @todo Fix this, for some reason it is being changed, but not sure why yet
        //assertNull details.changedBy // it has not been changed
        //assertNull details.changedOn
        assertEquals content.summary, details.summary
        assertEquals 'org.weceem.html.WcmHTMLContent', details.contentType
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
        assertEquals 3, children.size()
        assertTrue children.contains(nodeB)
        assertTrue children.contains(nodeC)
        assertTrue children.contains(nodeWiki)
    }

    void testFindDescendents() {
        def descendents = wcmContentRepositoryService.findDescendents(nodeA)
        assertEquals 6, descendents.size()
        assertTrue descendents.contains(nodeB)
        assertTrue descendents.contains(nodeC)
        assertTrue descendents.contains(nodeWiki)
        assertTrue descendents.contains(virtContent1)
        assertTrue descendents.contains(virtContent2)
        assertTrue descendents.contains(virtContent3)
    }

    void testFindChildrenWithType() {
        def children = wcmContentRepositoryService.findChildren(nodeA, [type:WcmWikiItem])
        assertEquals 1, children.size()
        assertTrue children.contains(nodeWiki)

        children = wcmContentRepositoryService.findChildren(nodeA, [type:WcmHTMLContent])
        assertEquals 2, children.size()
        assertTrue children.contains(nodeB)
        assertTrue children.contains(nodeC)
    }

    void testFindParentsNoType() {
        def parents = wcmContentRepositoryService.findParents(nodeB)
        assertEquals 3, parents.size()
        assertTrue parents.contains(nodeA)
        assertTrue parents.contains(nodeC)
        assertTrue parents.contains(nodeWiki)
    }

    void testFindParentsWithType() {
        def parents = wcmContentRepositoryService.findParents(nodeB, [type:WcmWikiItem])
        assertEquals 1, parents.size()
        assertTrue parents.contains(nodeWiki)

        parents = wcmContentRepositoryService.findParents(nodeB, [type:WcmHTMLContent])
        assertEquals 2, parents.size()
        assertTrue parents.contains(nodeA)
        assertTrue parents.contains(nodeC)
    }
        

    void testCreateNode() {
        def newnode = new WcmHTMLContent(title: 'contentX', aliasURI: 'contentX',
                content: 'sample X content', status: defStatus,
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)

        assertTrue wcmContentRepositoryService.createNode(newnode, null)

        def newnode2 = new WcmHTMLContent(title: 'contentY', aliasURI: 'contentY',
                content: 'sample Y content', status: defStatus, 
                createdBy: 'admin', createdOn: new Date(),
                space: spaceA, keywords: 'software',
                template: template, orderIndex: 1)
        assertTrue wcmContentRepositoryService.createNode(newnode2, nodeB)
        assertEquals nodeB, newnode2.parent
        assertTrue nodeB.children.contains(newnode2)
    }

    void testFindByRootURI() {
        def node = wcmContentRepositoryService.findRootContentByURI('contentA', spaceA)
        assertEquals nodeA, node
    }

    void testFindChildrenSorted() {
        def nodes = wcmContentRepositoryService.findChildren(nodeA, [params:[sort:'title', order:'desc']])
        
        def expectedNodes = [nodeB, nodeC, nodeWiki]
        assertTrue nodes.every { expectedNodes.contains(it) }
        def expectedTitles = expectedNodes*.title.sort({ a, b -> a.compareTo(b) }).reverse()
        nodes.eachWithIndex { obj, idx -> 
            assertEquals obj.title, expectedTitles[idx]
        }
        
    }

    void testFindAllRootContent() {
        def nodes = wcmContentRepositoryService.findAllRootContent(spaceA)
        println "nodes: $nodes"
        assertEquals 2, nodes.size()
        assertTrue nodes.contains(nodeA)
        assertTrue nodes.contains(template)
    }
    
    void testFindAllContent() {
        def nodes = wcmContentRepositoryService.findAllContent(spaceA)
        println "nodes: $nodes"
        assertEquals 8, nodes.size()
        assertTrue nodes.contains(nodeA)
        assertTrue nodes.contains(nodeB)
        assertTrue nodes.contains(nodeC)
        assertTrue nodes.contains(nodeWiki)
        assertTrue nodes.contains(template)
        assertTrue nodes.contains(virtContent1)
        assertTrue nodes.contains(virtContent2)
        assertTrue nodes.contains(virtContent3)
    }
    
    void testFindAllContentWithType() {
        def nodes = wcmContentRepositoryService.findAllContent(spaceA, [type: 'org.weceem.html.WcmHTMLContent'])
        println "nodes: $nodes"
        assertEquals 3, nodes.size()
        assertTrue nodes.contains(nodeA)
        assertTrue nodes.contains(nodeB)
        assertTrue nodes.contains(nodeC)
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
        assertEquals 2, nodes.size()
        assertTrue nodes.contains(nodeA)
        assertTrue nodes.contains(nodeC)
    }
    
    void testCountContent() {
        assertEquals(8, wcmContentRepositoryService.countContent(spaceA))
    }
    
    void testCountContentWithType() {
        assertEquals(3, wcmContentRepositoryService.countContent(spaceA, [type: 'org.weceem.html.WcmHTMLContent']))
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
        
        assertEquals(2, wcmContentRepositoryService.countContent(spaceA, [status: [100, 200]]))
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
        def content = new WcmHTMLContent()
        content.space = spaceA
        assertFalse content.validate()
        assertFalse wcmContentRepositoryService.createNode(content, null)
    }

    void testCreateFolderInEmptySpace() {
        def space = new WcmSpace(name:'dummy', aliasURI:'dummy')
        assert space.save()
        def content = new WcmFolder(title:'testing')
        content.space = space
        content.status = wcmContentRepositoryService.allPublicStatuses[0]
        def ok = wcmContentRepositoryService.createNode(content, null)
        println "content errors: ${content.errors}"
        assertTrue ok
    }

    void testCreateNodeFailsForInvalidParams() {
        assertTrue wcmContentRepositoryService.createNode('org.weceem.html.WcmHTMLContent', [space:spaceA]).hasErrors()
    }
}
