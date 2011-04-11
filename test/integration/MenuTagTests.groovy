import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.controllers.*
import org.weceem.content.*
import org.weceem.html.*
import org.weceem.tags.*

/**
 * ContentRepositoryTests class contains tests for tree operations from
 * wcmContentRepositoryService.
 *
 * These old tests BAD because they are not mocking the services, so they are testing the services and controller
 */
class MenuTagTests extends AbstractWeceemIntegrationTest {
    def statusA
    def statusB
    def spaceA
    def spaceB
    
    static transactional = true

    void setUp() {
        super.setUp()
        
        createContent {
            statusA = status(code: 400)
            statusB = status(code: 100, description: "draft", publicContent: false)
        }

        spaceA = new WcmSpace(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)

        spaceB = new WcmSpace(name: 'b', aliasURI: 'b')
        assert spaceB.save(flush: true)
    }
    
    void testBasicMenuWithChildrenOfCurrentPage() {
        def taglib = new WeceemTagLib()
       
        def parentA
        
        createContent {
            def childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1"
                content = "Child A1 content"
            }
            def childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2"
                content = "Child A2 content"
            }

            parentA = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Parent A"
                content = "Parent A content"
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)
        
            content(WcmHTMLContent) {
                status = statusA
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }
        }
        
        def space = spaceA
        
        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parentA)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, WcmContentController.makePageInfo(parentA.aliasURI, parentA))
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }
    
        def out = taglib.menu() 
        
        out = out.toString()
        
        println "Menu tag yielded: ${out}"
        /* Output should be like
        
        <ul>
        <li class="weceem-menu-level0  weceem-menu-first ">
        <a href="/a/node-1285335513358">Parent A</a>
            <ul>
                <li class="weceem-menu-level1  weceem-menu-first ">
                <a href="/a/node-1285335513358/node-1285335513064">Child A1</a></li>
                <li class="weceem-menu-level1   weceem-menu-last">
                <a href="/a/node-1285335513358/node-1285335513301">Child A2</a></li>
            </ul>
        </li>
        <li class="weceem-menu-level0   weceem-menu-last">
        <a href="/a/node-1285335513386">Parent B</a></li>
        </ul>
        
        */
        
        def markup = new XmlSlurper().parseText("<body>$out</body>")
        
        assertEquals 1, markup.ul.size()
        assertEquals 2, markup.ul.li.size()
        assertEquals "Parent A", markup.ul.li[0].a.text()
        assertEquals "Parent B", markup.ul.li[1].a.text()
        def pA = markup.ul.li[0]
        def lichildren = pA.dump()        
        println lichildren
        
        assertEquals 2, pA.'*'.li.size()
        assertEquals "Child A1", pA.'*'.li[0].a.text()
        assertEquals "Child A2", pA.'*'.li[1].a.text()
    }

    void testCustomMenuWithChildrenOfCurrentPage() {
        def taglib = new WeceemTagLib()
       
        def parentA
        
        createContent {
            def childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1"
                content = "Child A1 content"
            }
            def childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2"
                content = "Child A2 content"
            }

            parentA = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Parent A"
                content = "Parent A content"
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)
        
            content(WcmHTMLContent) {
                status = statusA
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }
        }
        
        def space = spaceA
        
        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parentA)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, WcmContentController.makePageInfo(parentA.aliasURI, parentA))
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }
    
        def r = taglib.menu(custom:true) { args ->
            def s = new StringBuilder()
            if (args.first) {
                s << "|FIRST-${args.level}|\n"
            }
            s << "NODE: ${args.menuNode.titleForHTML}\n"
            s << "ACTIVE: ${args.active}\n"
            s << args.nested
            if (args.last) {
                s << "|LAST-${args.level}|\n"
            }
            s
        } 
        
        r = r.toString()
        
        println "Menu tag yielded: ${r}"
        
        assertEquals """|FIRST-0|
NODE: Parent A
ACTIVE: true
|FIRST-1|
NODE: Child A1
ACTIVE: false
NODE: Child A2
ACTIVE: false
|LAST-1|
NODE: Parent B
ACTIVE: false
|LAST-0|
""", r
        
    }

    void testCustomMenuWithActiveChild() {
        def taglib = new WeceemTagLib()

        def parentA
        def childA2
        def childA1

        createContent {
            childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1"
                content = "Child A1 content"
            }
            childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2"
                content = "Child A2 content"
            }

            parentA = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Parent A"
                content = "Parent A content"
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)

            content(WcmHTMLContent) {
                status = statusA
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }
        }

        def space = spaceA

        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, childA2)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, WcmContentController.makePageInfo(parentA.aliasURI, parentA))
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }

        def r = taglib.menu(custom:true) { args ->
            def s = new StringBuilder()
            if (args.first) {
                s << "|FIRST-${args.level}|\n"
            }
            s << "NODE: ${args.menuNode.titleForHTML}\n"
            s << "ACTIVE: ${args.active}\n"
            s << args.nested
            if (args.last) {
                s << "|LAST-${args.level}|\n"
            }
            s
        } 

        r = r.toString()

        println "Menu tag yielded: ${r}"

        assertEquals """|FIRST-0|
NODE: Parent A
ACTIVE: false
|FIRST-1|
NODE: Child A1
ACTIVE: false
NODE: Child A2
ACTIVE: true
|LAST-1|
NODE: Parent B
ACTIVE: false
|LAST-0|
""", r

        def pi = WcmContentController.makePageInfo(parentA.aliasURI, childA1)
        // Now try again using lineage of childA1
        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, childA1)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, pi)   
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }
        
        println "Page info: ${pi.dump()}"
        r = taglib.menu(custom:true, node:parentA) { args ->
            def s = new StringBuilder()
            if (args.first) {
                s << "|FIRST-${args.level}|\n"
            }
            s << "NODE: ${args.menuNode.titleForHTML}\n"
            s << "ACTIVE: ${args.active}\n"
            s << args.nested
            if (args.last) {
                s << "|LAST-${args.level}|\n"
            }
            s
        } 

        r = r.toString()

        println "Menu tag yielded: ${r}"

        assertEquals """|FIRST-0|
NODE: Child A1
ACTIVE: true
NODE: Child A2
ACTIVE: false
|LAST-0|
""", r
    }

    void testDeepMenuWithChildrenOfCurrentPage() {
        def taglib = new WeceemTagLib()
       
        def parentA
        
        createContent {
            def childA1_1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1_1"
                content = "Child A1_1 content"
            }
            def childA1_2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1_2"
                content = "Child A1_2 content"
            }
            def childA2_1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2_1"
                content = "Child A2_1 content"
            }

            def childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1"
                content = "Child A1 content"
            }
            childA1.addToChildren(childA1_1)
            childA1.addToChildren(childA1_2)

            def childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2"
                content = "Child A2 content"
            }
            childA2.addToChildren(childA2_1)

            parentA = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Parent A"
                content = "Parent A content"
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)
        
            content(WcmHTMLContent) {
                status = statusA
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }
        }
        
        def space = spaceA
        
        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parentA)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, WcmContentController.makePageInfo(parentA.aliasURI, parentA))
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }
    
        def out = taglib.menu(levels:3) 
        
        out = out.toString()
        
        println "Menu tag yielded: ${out}"
        /* Output should be like
        
        <ul>
        <li class="weceem-menu-level0  weceem-menu-first ">
        <a href="/a/node-1285335513358">Parent A</a>
            <ul>
                <li class="weceem-menu-level1  weceem-menu-first ">
                <a href="/a/node-1285335513358/node-1285335513064">Child A1</a>
                    <ul>
                        <li class="weceem-menu-level1  weceem-menu-first ">
                        <a href="/a/node-1285335513358/node-1285335513064">Child A1_1</a></li>
                        <li class="weceem-menu-level1   weceem-menu-last">
                        <a href="/a/node-1285335513358/node-1285335513301">Child A1_2</a></li>
                    </ul>
                </li>
                <li class="weceem-menu-level1   weceem-menu-last">
                    <a href="/a/node-1285335513358/node-1285335513301">Child A2</a>
                    <ul>
                        <li class="weceem-menu-level1  weceem-menu-first ">
                        <a href="/a/node-1285335513358/node-1285335513064">Child A2_1</a></li>
                </li>
            </ul>
        </li>
        <li class="weceem-menu-level0   weceem-menu-last">
        <a href="/a/node-1285335513386">Parent B</a></li>
        </ul>
        
        */
        
        def markup = new XmlSlurper().parseText("<body>$out</body>")
        
        assertEquals 1, markup.ul.size()
        assertEquals 2, markup.ul.li.size()
        assertEquals "Parent A", markup.ul.li[0].a.text()
        assertEquals "Parent B", markup.ul.li[1].a.text()
        def pA = markup.ul.li[0]
        def lichildren = pA.dump()        
        println lichildren
        
        assertEquals 2, pA.'*'.li.size()
        assertEquals "Child A1", pA.'*'.li[0].a.text()
        assertEquals "Child A2", pA.'*'.li[1].a.text()        

        def subparent = pA.'*'.li[0].'*'
        assertEquals 2, subparent.li.size()
        assertEquals "Child A1_1", subparent.li[0].a.text()
        assertEquals "Child A1_2", subparent.li[1].a.text()        

        subparent = pA.'*'.li[1].'*'
        assertEquals 1, subparent.li.size()
        assertEquals "Child A2_1", subparent.li[0].a.text()
    }

    void testDeepMenuWithChildrenOfAnotherContentNode() {
        def taglib = new WeceemTagLib()

        def parentA
        def childA2
        
        createContent {
            def childA1_1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1_1"
                content = "Child A1_1 content"
            }
            def childA1_2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1_2"
                content = "Child A1_2 content"
            }
            def childA2_1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2_1"
                content = "Child A2_1 content"
            }

            def childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A1"
                content = "Child A1 content"
            }
            childA1.addToChildren(childA1_1)
            childA1.addToChildren(childA1_2)

            childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Child A2"
                content = "Child A2 content"
            }
            childA2.addToChildren(childA2_1)

            parentA = content(WcmHTMLContent) {
                space = spaceA
                status = statusA
                title = "Parent A"
                content = "Parent A content"
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)
        
            content(WcmHTMLContent) {
                status = statusA
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }
        }
        
        def space = spaceA
        
        taglib.request.with {
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_NODE, parentA)
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_PAGE, WcmContentController.makePageInfo(parentA.aliasURI, parentA))
            setAttribute(WcmContentController.REQUEST_ATTRIBUTE_SPACE, space.aliasURI)
        }
    
        def out = taglib.menu(node:childA2, levels:3) 
        
        out = out.toString()
        
        println "Menu tag yielded: ${out}"
        /* Output should be like
        
        <ul>
        <li class="weceem-menu-level0  weceem-menu-first ">
        <a href="/a/node-1285335513358">Parent A</a>
            <ul>
                <li class="weceem-menu-level1  weceem-menu-first ">
                <a href="/a/node-1285335513358/node-1285335513064">Child A1</a>
                    <ul>
                        <li class="weceem-menu-level1  weceem-menu-first ">
                        <a href="/a/node-1285335513358/node-1285335513064">Child A1_1</a></li>
                        <li class="weceem-menu-level1   weceem-menu-last">
                        <a href="/a/node-1285335513358/node-1285335513301">Child A1_2</a></li>
                    </ul>
                </li>
                <li class="weceem-menu-level1   weceem-menu-last">
                    <a href="/a/node-1285335513358/node-1285335513301">Child A2</a>
                    <ul>
                        <li class="weceem-menu-level1  weceem-menu-first ">
                        <a href="/a/node-1285335513358/node-1285335513064">Child A2_1</a></li>
                </li>
            </ul>
        </li>
        <li class="weceem-menu-level0   weceem-menu-last">
        <a href="/a/node-1285335513386">Parent B</a></li>
        </ul>
        
        */
        
        def markup = new XmlSlurper().parseText("<body>$out</body>")
        
        assertEquals 1, markup.ul.size()
        assertEquals 1, markup.ul.li.size()
        assertEquals "Child A2_1", markup.ul.li[0].a.text()
    }
}
