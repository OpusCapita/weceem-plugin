import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.mock.interceptor.MockFor

import org.weceem.controllers.WcmContentController
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.wiki.*
import org.weceem.tags.*

import org.weceem.services.*
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

@TestFor(WeceemTagLib)
@Mock(WcmHTMLContent)
class WeceemTagLibTests {

    void testSpace() {
        def spc = new WcmSpace()
        spc.name = 'testing'
        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_SPACE, spc)
        
        Object.metaClass.encodeAsHTML = { return delegate }
        
        def output = applyTemplate("<wcm:space/>")

        assertEquals "testing", output
    }


    void testCreateLink() {
        def g = new MockFor(ApplicationTagLib)
        g.demand.createLink {hash ->
          assertEquals "wcmContent", hash.controller
          assertEquals "show", hash.action
        }

        tagLib.metaClass.g = g.proxyInstance()

        def node = new WcmHTMLContent(aliasURI:'someNode', space: new WcmSpace(name:'default', aliasURI:'default'))
        tagLib.wcmContentRepositoryService = [findContentForPath : { path, space -> [content: node]}]
        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_SPACE, node.space)
        tagLib.createLink(path: 'someNode', null)
    }

    void testLink() {
        def g = new MockFor(ApplicationTagLib)
        g.demand.createLink {hash ->
          assertEquals "wcmContent", hash.controller
          assertEquals "show", hash.action
        }

        tagLib.metaClass.g = g.proxyInstance()

        def node = new WcmHTMLContent(aliasURI:'someNode', space: new WcmSpace(name:'default', aliasURI:'default'))
        tagLib.wcmContentRepositoryService = [findContentForPath : { path, space -> [content: node]}]
        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_SPACE, node.space)
        def output = applyTemplate("<wcm:link path='someNode'>Click</wcm:link>")

        assert output.contains("<a href=")
        assert output.contains(">Click<")
        assert output.contains("</a>")
        assert !output.contains('path')
        assert !output.contains('controller')
        assert !output.contains('action')
        assert !output.contains('uri')

    }

    void testCountChildren() {
        def parent = new WcmHTMLContent()

        def mockCRService = new MockFor(WcmContentRepositoryService)
        mockCRService.demand.countChildren {node, args ->
          assertEquals parent, node
        }

        tagLib.wcmContentRepositoryService = mockCRService.proxyInstance()
        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, parent)

        try {
          tagLib.countChildren([path:"some/path", node: parent])
          fail "Expected exception with path and node attributes"
        } catch(e) {
            // we wanted an exception
        }

        // @todo Need to test the output!!!?
        tagLib.countChildren([node: parent])
    }

    void testEachChildWithNode() {
        def parent = new WcmHTMLContent(title: 'parent')

        def anotherNode = new WcmHTMLContent(title: 'another')

        def mockCRService = new MockFor(WcmContentRepositoryService)
        mockCRService.demand.findChildren { node, args ->
            assertEquals parent, node
        }

        tagLib.wcmContentRepositoryService = mockCRService.proxyInstance()

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, anotherNode)

        try {
            tagLib.eachChild([path: "some/path", node: parent], {})
            fail "Expected exception with path and node attributes"
        } catch (e) {
            assert e.message =~ "You cannot specify both node and path attributes"
        }

        // @todo Need to test the output!!!?
        tagLib.eachChild([node: parent], {})
    }


    void testEachChildWithoutFilter() {

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

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, parent)
        
        tagLib.wcmContentRepositoryService = mockCRService

        def o = applyTemplate("<wcm:eachChild var=\"c\">\${c.title}|</wcm:eachChild>")
        
        assertEquals "Node A|Node B|", o
    }
    
    
    void testEachChildWithType() {
        def parent = new WcmHTMLContent()
        parent.title = 'Parent'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findChildren : { node, args ->
                assertEquals "WcmHTMLContent", args.type
                [nodeA]
            }
        ]

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, parent)
        
        tagLib.wcmContentRepositoryService = mockCRService

        def o = applyTemplate("<wcm:eachChild var=\"c\" type=\"WcmHTMLContent\">\${c.title}|</wcm:eachChild>")
        
        assertEquals "Node A|", o
    }
    
    void testEachChildWithFilter() {
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

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, parent)
        
        tagLib.wcmContentRepositoryService = mockCRService

        def o = applyTemplate("<wcm:eachChild var=\"c\" filter=\"\${ { it.title.contains('Wiki') } }\">\${c.title}|</wcm:eachChild>")
        
        assertEquals "Wiki B|", o
    }
    
    void testEachParentWithType() {
        def parent = new WcmWikiItem()
        parent.title = 'Parent A'

        def parentB = new WcmHTMLContent()
        parentB.title = 'Parent B'

        def nodeA = new WcmHTMLContent()
        nodeA.title = 'Node A'
        
        def mockCRService = [
            findParents : { node, args ->
                assertEquals "WcmHTMLContent", args.type
                [parentB]
            }
        ]

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        tagLib.wcmContentRepositoryService = mockCRService

        def o = applyTemplate("<wcm:eachParent var=\"c\" type=\"WcmHTMLContent\">\${c.title}|</wcm:eachParent>")
        
        assertEquals "Parent B|", o
    }
    
    void testEachParentWithFilter() {
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

        tagLib.request.setAttribute(RenderEngine.REQUEST_ATTRIBUTE_NODE, nodeA)
        
        tagLib.wcmContentRepositoryService = mockCRService

        def o = applyTemplate("<wcm:eachParent var=\"c\" filter=\"\${ { it.title.contains('A') } }\">\${c.title}|</wcm:eachParent>")
        
        assertEquals "Parent A|", o
    }

    
    void testSummarize() {

        def testData = [
            [40, 'Immigration Help For Grails Developers', "Immigration Help For Grails Developers"],
            [10, 'Immigration Help For Grails Developers', "Immigra..."],
            [5, 'Immigration Help For Grails Developers', "Im..."],
            [40, 'Immigration Help For Grails Developers XXXXXXXXXX', "Immigration Help For Grails ..."],
            [40, 'Immigration', "Immigration"]
        ]
        
        testData.each { d ->
            def o = applyTemplate("<wcm:summarize length=\"${d[0]}\">${d[1]}</wcm:summarize>")
            println "Testing with data: ${d}"
            println "Tag output is $o (length: ${o.size()})"
            assertTrue d[0] >= o.size()
            assertEquals d[2], o
        }
    }

    void testDataFeedWithMaxGreaterThanEntries() {

        def mockCacheService = [
            getOrPutValue : { String cacheName, String key, Closure creator ->
                """
<rss>
    <channel>
        <item>
            <title>Test item 1</title>
            <link>none</link>
        </item>
        <item>
            <title>Test item 2</title>
            <link>none</link>
        </item>
    </channel>
</rss>"""
            }
        ]
        
        tagLib.wcmCacheService = mockCacheService
        
        def output = applyTemplate("<wcm:dataFeed url='http://localhost/dummyfeed' max='10'/>")

        def o = output
        assertTrue o.contains('Test item 1')
        assertTrue o.contains('Test item 2')
        
    }
}