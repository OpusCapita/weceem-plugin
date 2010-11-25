import groovy.mock.interceptor.MockFor

import org.weceem.controllers.WcmContentController
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.tags.*

import org.weceem.services.*
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

class WeceemTagLibTests extends grails.test.GrailsUnitTestCase {
    
    void testSpace() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def spc = new WcmSpace()
        spc.name = 'testing'
        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, spc)
        
        String.metaClass.encodeAsHTML = { return delegate }
        
        taglib.space([:])
        
        assertEquals "testing", taglib.out.toString()
    }


    void testCreateLink() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()

        def g = new MockFor(ApplicationTagLib)
        g.demand.createLink {hash ->
          assertEquals "wcmContent", hash.controller
          assertEquals "show", hash.action
        }

        taglib.metaClass.g = g.proxyInstance()

        def node = new WcmHTMLContent(aliasURI:'someNode', space: new WcmSpace(name:'default', aliasURI:'default'))
        taglib.wcmContentRepositoryService = [findContentForPath : { path, space -> [content: node]}]
        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, node.space)
        taglib.createLink(path: 'someNode', null)
    }

  void testCountChildren() {
    mockTagLib(WeceemTagLib)
    def taglib = new WeceemTagLib()

    def parent = new WcmHTMLContent()

    def mockCRService = new MockFor(WcmContentRepositoryService)
    mockCRService.demand.countChildren {node, args ->
      assertEquals parent, node
    }

    taglib.wcmContentRepositoryService = mockCRService.proxyInstance()
    taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)

    try {
      taglib.countChildren([path:"some/path", node: parent])
      fail "Expected exception with path and node attributes"
    } catch(e) {
        // we wanted an exception
    }

    taglib.countChildren([node: parent])
  }

  void testEachChildWithNode() {
    mockTagLib(WeceemTagLib)
    def taglib = new WeceemTagLib()

    def parent = new WcmHTMLContent(title: 'parent')

    def anotherNode = new WcmHTMLContent(title: 'another')

    def mockCRService = new MockFor(WcmContentRepositoryService)
    mockCRService.demand.findChildren { node, args ->
      assertEquals parent, node
    }

    taglib.wcmContentRepositoryService = mockCRService.proxyInstance()

    taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, anotherNode)

    try {
      taglib.eachChild([path:"some/path", node: parent], {})
      fail "Expected exception with path and node attributes"
    } catch(e) {
      assert e.message =~ "You cannot specify both node and path attributes"
    }

    taglib.eachChild([node: parent], {})
  }


    void testEachChildWithoutFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WcmHTMLContent()
        parent.title = 'Parent'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'
        
        def nodeB = new WcmHTMLContent()
        nodeB.title = 'Node B'

        def mockCRService = [
            findChildren : { node, args ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.wcmContentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c"], { params -> return params.c.title + "|"})
        
        assertEquals "Node A|Node B|", taglib.out.toString()
    }
    
    
    void testEachChildWithType() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WcmHTMLContent()
        parent.title = 'Parent'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findChildren : { node, args ->
                assertEquals WcmHTMLContent, args.type
                [nodeA]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.wcmContentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c", type:WcmHTMLContent], { params -> return params.c.title + "|"})
        
        assertEquals "Node A|", taglib.out.toString()
    }
    
    void testEachChildWithFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WcmHTMLContent()
        parent.title = 'Parent'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'

        def nodeB = new WcmWikiItem()
        nodeB.title = 'Wiki B'

        def mockCRService = [
            findChildren : { node, args ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.wcmContentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c", filter:{ it.title.contains('Wiki')}], { params -> return params.c.title + "|"})
        
        assertEquals "Wiki B|", taglib.out.toString()
    }
    
    void testEachParentWithType() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WcmWikiItem()
        parent.title = 'Parent A'

        def parentB = new WcmHTMLContent()
        parentB.title = 'Parent B'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findParents : { node, args ->
                assertEquals WcmHTMLContent, args.type
                [parentB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        taglib.wcmContentRepositoryService = mockCRService
        
        taglib.eachParent([var:"c", type:WcmHTMLContent], { params -> return params.c.title + "|"})
        
        assertEquals "Parent B|", taglib.out.toString()
    }
    
    void testEachParentWithFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WcmWikiItem()
        parent.title = 'Parent A'

        def parentB = new WcmHTMLContent()
        parentB.title = 'Parent B'
    
        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'

        def mockCRService = [
            findParents : { node, args ->
                [parent, parentB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        taglib.wcmContentRepositoryService = mockCRService
        
        taglib.eachParent([var:"c", filter:{ it.title.contains('A')}], { params -> return params.c.title + "|"})
        
        assertEquals "Parent A|", taglib.out.toString()
    }
    
}