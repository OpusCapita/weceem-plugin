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
        
        def spc = new Space()
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
        assertEquals "content", hash.controller
        assertEquals "show", hash.action
        }

        taglib.metaClass.g = g.proxyInstance()

        def node = new HTMLContent(aliasURI:'someNode', space: new Space(name:'default', aliasURI:'default'))
        taglib.contentRepositoryService = [findContentForPath : { path, space -> [content: node]}]
        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, node.space)
        taglib.createLink(path: 'someNode', null)
    }

  void testCountChildren() {
    mockTagLib(WeceemTagLib)
    def taglib = new WeceemTagLib()

    def parent = new HTMLContent()

    def mockCRService = new MockFor(ContentRepositoryService)
    mockCRService.demand.countChildren {node, args ->
      assertEquals parent, node
    }

    taglib.contentRepositoryService = mockCRService.proxyInstance()
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

    def parent = new HTMLContent(title: 'parent')

    def anotherNode = new HTMLContent(title: 'another')

    def mockCRService = new MockFor(ContentRepositoryService)
    mockCRService.demand.findChildren { node, args ->
      assertEquals parent, node
    }

    taglib.contentRepositoryService = mockCRService.proxyInstance()

    taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, anotherNode)

    try {
      taglib.eachChild([path:"some/path", node: parent], {})
      fail "Expected exception with path and node attributes"
    } catch(e) {
      assert e.message =~ "can not specify ${WeceemTagLib.ATTR_NODE} and ${WeceemTagLib.ATTR_PATH} attributes"
    }

    taglib.eachChild([node: parent], {})
  }


    void testEachChildWithoutFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new HTMLContent()
        parent.title = 'Parent'

        def nodeA = new HTMLContent()
        nodeA.title = 'Node A'
        
        def nodeB = new HTMLContent()
        nodeB.title = 'Node B'

        def mockCRService = [
            findChildren : { node, args ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c"], { params -> return params.c.title + "|"})
        
        assertEquals "Node A|Node B|", taglib.out.toString()
    }
    
    
    void testEachChildWithType() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new HTMLContent()
        parent.title = 'Parent'

        def nodeA = new HTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findChildren : { node, args ->
                assertEquals HTMLContent, args.type
                [nodeA]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c", type:HTMLContent], { params -> return params.c.title + "|"})
        
        assertEquals "Node A|", taglib.out.toString()
    }
    
    void testEachChildWithFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new HTMLContent()
        parent.title = 'Parent'

        def nodeA = new HTMLContent()
        nodeA.title = 'Node A'

        def nodeB = new WikiItem()
        nodeB.title = 'Wiki B'

        def mockCRService = [
            findChildren : { node, args ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachChild([var:"c", filter:{ it.title.contains('Wiki')}], { params -> return params.c.title + "|"})
        
        assertEquals "Wiki B|", taglib.out.toString()
    }
    
    void testEachParentWithType() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WikiItem()
        parent.title = 'Parent A'

        def parentB = new HTMLContent()
        parentB.title = 'Parent B'

        def nodeA = new HTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findParents : { node, args ->
                assertEquals HTMLContent, args.type
                [parentB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachParent([var:"c", type:HTMLContent], { params -> return params.c.title + "|"})
        
        assertEquals "Parent B|", taglib.out.toString()
    }
    
    void testEachParentWithFilter() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def parent = new WikiItem()
        parent.title = 'Parent A'

        def parentB = new HTMLContent()
        parentB.title = 'Parent B'
    
        def nodeA = new HTMLContent()
        nodeA.title = 'Node A'

        def mockCRService = [
            findParents : { node, args ->
                [parent, parentB]
            }
        ]

        taglib.request.setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachParent([var:"c", filter:{ it.title.contains('A')}], { params -> return params.c.title + "|"})
        
        assertEquals "Parent A|", taglib.out.toString()
    }
}