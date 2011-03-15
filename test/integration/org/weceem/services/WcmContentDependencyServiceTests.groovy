package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.content.*
import org.weceem.html.WcmHTMLContent

class WcmContentDependencyServiceTests extends AbstractWeceemIntegrationTest {

    def statusPublished
    def statusDraft
    def spaceA
    def templateA
    def templateB
    def childA1
    def grandchildA1
    def childA2
    def parentA
    def rootNode1
    
    def wcmContentDependencyService

    void setUp() {
        super.setUp()
                
        WcmContentDependencyService.metaClass.getLog = { ->
            [debugEnabled: true, debug: { s -> println s } ]
        }
        
        // Flush previous test data
        wcmContentDependencyService.reset()

        createContent {
            statusPublished = status(code: 400)
            statusDraft = status(code: 100, description: "draft", publicContent: false)
        }

        spaceA = new WcmSpace(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)

        createContent {
            templateA = content(WcmTemplate) {
                title = 'template'
                aliasURI = 'templateA'
                space = spaceA
                status = statusPublished
                content = 'template content'
                contentDependencies = 'parent-a/**'
            }

            templateB = content(WcmTemplate) {
                title = 'template'
                aliasURI = 'templateB'
                space = spaceA
                status = statusPublished
                content = 'template content'
            }

            childA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusPublished
                title = "Child A1"
                content = "Child A1 content"
            }

            grandchildA1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusPublished
                title = "Grandchild A1"
                content = "Grandchild A1 content"
            }

            childA1.addToChildren(grandchildA1)
            
            childA2 = content(WcmHTMLContent) {
                space = spaceA
                status = statusPublished
                title = "Child A2"
                content = "Child A2 content"
                template = templateB
            }

            parentA = content(WcmHTMLContent) {
                space = spaceA
                aliasURI = 'parent-a'
                status = statusPublished
                title = "Parent A"
                content = "Parent A content"
                template = templateA
            }
            parentA.addToChildren(childA1)
            parentA.addToChildren(childA2)

            content(WcmHTMLContent) {
                status = statusPublished
                space = spaceA
                title = "Parent B"
                content = "Parent B content"
            }

        
            rootNode1 = content(WcmHTMLContent) {
                space = spaceA
                status = statusPublished
                title = "Root node 1"
                content = "Root node 1 content"
            }
        }
        
        wcmContentDependencyService.reload()
    }
    
    void testDependenciesLoadCorrectly() {

        dumpInfo()

        assertEquals (['parent-a/**'], wcmContentDependencyService.getDependencyPathsOf(templateA))
        assertEquals (['templateA'], wcmContentDependencyService.getDependencyPathsOf(parentA))
        assertEquals (['templateA'], wcmContentDependencyService.getDependencyPathsOf(childA1))
        assertEquals (['templateB'], wcmContentDependencyService.getDependencyPathsOf(childA2))

        dumpInfo()
        
        def templDeps = wcmContentDependencyService.getContentDependentOn(templateA)
        println "TemplateA deps: ${templDeps*.absoluteURI} / ${templDeps*.id}"
        println "Expected TemplateA deps: ${[childA1, grandchildA1, parentA]*.absoluteURI} / ${[childA1, grandchildA1, parentA]*.id}"
        assertEquals ([childA1, grandchildA1, parentA]*.id.sort(), templDeps*.id.sort())

        def child1Deps = wcmContentDependencyService.getContentDependentOn(childA1)
        println "child1Deps deps: ${child1Deps*.absoluteURI} / ${child1Deps*.id}"
        assertEquals ([templateA, parentA, grandchildA1]*.id.sort(), child1Deps*.id.sort())
        assertEquals ([templateA, parentA, grandchildA1, childA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(childA2)*.id.sort())
        assertEquals ([]*.id.sort(), wcmContentDependencyService.getContentDependentOn(parentA)*.id.sort())

        assertEquals ([parentA, childA1, grandchildA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(templateA)*.id.sort())

        assertEquals ([parentA, childA1, grandchildA1, templateA, childA2]*.id.sort(), wcmContentDependencyService.getContentDependentOn(templateB)*.id.sort())

        assertEquals 0, wcmContentDependencyService.getContentDependentOn(parentA).size()
    }
    

    void testGetDependenciesOf() {

        dumpInfo()

        def templDeps = wcmContentDependencyService.getDependenciesOf(templateA)
        println "TemplateA deps: ${templDeps*.absoluteURI}"
        // TemplateA indirectly depends on templateB because childA2 is dependent on changes to templateB
        assertEquals ([childA1, childA2, grandchildA1, templateB]*.id.sort(), templDeps*.id.sort())
        
        assertEquals ([templateA, childA2, grandchildA1, childA1, templateB]*.id.sort(), wcmContentDependencyService.getDependenciesOf(parentA)*.id.sort())
        assertEquals ([templateA, grandchildA1, childA2, templateB]*.id.sort(), wcmContentDependencyService.getDependenciesOf(childA1)*.id.sort())
        assertEquals ([templateB]*.id.sort(), wcmContentDependencyService.getDependenciesOf(childA2)*.id.sort())
    }

    void testGetContentDependentOn() {

        dumpInfo()

        assertEquals ([parentA, childA1, grandchildA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(templateA)*.id.sort())
        assertEquals ([]*.id.sort(), wcmContentDependencyService.getContentDependentOn(parentA)*.id.sort())
        assertEquals ([templateA, parentA, grandchildA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(childA1)*.id.sort())
        assertEquals ([templateA, parentA, childA1, grandchildA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(childA2)*.id.sort())
    }

    void testDependencyInfoDoesNotClashAcrossSpacesForSameURI() {
        assert false
    }
    
    void dumpInfo() {
        wcmContentDependencyService.dumpDependencyInfo(true)
    }
}
