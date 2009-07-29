
import org.weceem.controllers.ContentController
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.tags.*

class WeceemTagLibTests extends grails.test.GrailsUnitTestCase {
    
    void testSpace() {
        mockTagLib(WeceemTagLib)
        def taglib = new WeceemTagLib()
        
        def spc = new Space()
        spc.name = 'testing'
        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_SPACE, spc)
        
        String.metaClass.encodeAsHTML = { return delegate }
        
        taglib.space([:])
        
        assertEquals "testing", taglib.out.toString()
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
            findChildren : { node, type, params ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
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
            findChildren : { node, type, params ->
                assertEquals HTMLContent, type
                [nodeA]
            }
        ]

        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
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
            findChildren : { node, type, params ->
                [nodeA, nodeB]
            }
        ]

        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_NODE, parent)
        
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
            findParents : { node, type, params ->
                assertEquals HTMLContent, type
                [parentB]
            }
        ]

        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
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
            findParents : { node, type, params ->
                [parent, parentB]
            }
        ]

        taglib.request.setAttribute(ContentController.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        taglib.contentRepositoryService = mockCRService
        
        taglib.eachParent([var:"c", filter:{ it.title.contains('A')}], { params -> return params.c.title + "|"})
        
        assertEquals "Parent A|", taglib.out.toString()
    }
}