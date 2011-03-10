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
    def childA2
    def parentA
    def rootNode1
    
    void setUp() {
        super.setUp()
                
        WcmContentDependencyService.metaClass.getLog = { ->
            [debugEnabled: true, debug: { s -> println s } ]
        }
        
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
        
        assertEquals ([childA1, childA2]*.id.sort(), wcmContentDependencyService.getDependenciesOf(templateA)*.id.sort())

        assertEquals ([templateA]*.id.sort(), wcmContentDependencyService.getContentDependentOn(childA1)*.id.sort())
        assertEquals ([templateA]*.id.sort(), wcmContentDependencyService.getContentDependentOn(childA2)*.id.sort())
        assertEquals ([]*.id.sort(), wcmContentDependencyService.getContentDependentOn(parentA)*.id.sort())

        assertEquals ([parentA, childA1]*.id.sort(), wcmContentDependencyService.getContentDependentOn(templateA)*.id.sort())

        assertEquals ([childA2]*.id.sort(), wcmContentDependencyService.getContentDependentOn(templateB)*.id.sort())

        assertEquals 0, wcmContentDependencyService.getContentDependentOn(parentA).size()
    }
    
    void testFingerprintChangesOnTemplatedContentAndTemplateWhenTemplateChanges() {

        def oldTemplateFP = wcmContentDependencyService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP
        def oldParentAFP = wcmContentDependencyService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentDependencyService.getFingerprintForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP
        def oldChildA1FP = wcmContentDependencyService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA2FP = wcmContentDependencyService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        dumpInfo()

        println "-"*20
        
        templateA.content = "bla bla bla"
        templateA.save(flush:true)
        
        wcmContentDependencyService.updateFingerprintFor(templateA)
        
        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentDependencyService.getFingerprintFor(templateA)
        def newParentAFP = wcmContentDependencyService.getFingerprintFor(parentA)
        def newParentTreeAFP = wcmContentDependencyService.getFingerprintForDescendentsOf(parentA)
        def newChildA1FP = wcmContentDependencyService.getFingerprintFor(childA1)
        def newChildA2FP = wcmContentDependencyService.getFingerprintFor(childA2)

        dumpInfo()
        
        assertNotNull newTemplateFP
        assertTrue oldTemplateFP != newTemplateFP

        // These must also change fingerprint as they dependen on the template
        assertNotNull newParentAFP
        assertTrue oldParentAFP != newParentAFP
        assertNotNull newChildA1FP
        assertTrue oldChildA1FP != newChildA1FP

        // parent tree should have changed, one of the children uses the template
        assertNotNull newParentTreeAFP
        assertTrue oldParentATreeFP != newParentTreeAFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newTemplateFP, wcmContentDependencyService.getFingerprintFor(templateA)
    }

    void testFingerprintChangesOnNonTemplatedContent() {

        def oldRootFP = wcmContentDependencyService.getFingerprintFor(rootNode1)
        
        rootNode1.content = "bla bla bla"
        rootNode1.save(flush:true)
        
        wcmContentDependencyService.updateFingerprintFor(rootNode1)
        
        // Now we expect finger prints for template and its dependents to be updated
        def newRootFP = wcmContentDependencyService.getFingerprintFor(rootNode1)

        assertNotNull newRootFP
        assertTrue oldRootFP != newRootFP

        // Test invariance of the fingerprint
        assertEquals newRootFP, wcmContentDependencyService.getFingerprintFor(rootNode1)
    }

    void testFingerprintChangesOnParentWithTemplateThatDependsOnDescendentsWhenDescendentChanges() {
        
        def oldTemplateFP = wcmContentDependencyService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP
        def oldParentAFP = wcmContentDependencyService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentDependencyService.getFingerprintForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP
        def oldChildA1FP = wcmContentDependencyService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA2FP = wcmContentDependencyService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        childA1.content = "this should cascade changes"
        childA1.save(flush:true)
        
        println "We're updating A1 FP now..."
        dumpInfo()
        wcmContentDependencyService.updateFingerprintFor(childA1)
        println "We're done updating A1 FP now."
        dumpInfo()
        
        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentDependencyService.getFingerprintFor(templateA)
        def newParentAFP = wcmContentDependencyService.getFingerprintFor(parentA)
        def newParentATreeFP = wcmContentDependencyService.getFingerprintForDescendentsOf(parentA)
        def newChildA1FP = wcmContentDependencyService.getFingerprintFor(childA1)
        def newChildA2FP = wcmContentDependencyService.getFingerprintFor(childA2)

        // Node we modified must change
        assertNotNull newChildA1FP
        assertTrue "The child fingerprint did not change", oldChildA1FP != newChildA1FP

        // Template must have changed, it depends on it
        assertNotNull newTemplateFP
        assertTrue "The template fingerprint did not change", oldTemplateFP != newTemplateFP

        // The parent and tree must also be changed
        assertNotNull newParentAFP
        assertTrue "The parent fingerprint did not change", oldParentAFP != newParentAFP
        assertNotNull newParentATreeFP
        assertTrue "The parent tree fingerprint did not change", oldParentATreeFP != newParentATreeFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newChildA1FP, wcmContentDependencyService.getFingerprintFor(childA1)        
    }
    
    void dumpInfo() {
        wcmContentDependencyService.dumpDependencyInfo(true)
        wcmContentDependencyService.dumpFingerprintInfo(true)
    }
}
