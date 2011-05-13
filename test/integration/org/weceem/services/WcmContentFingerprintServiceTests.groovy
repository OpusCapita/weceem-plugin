package org.weceem.services

import org.weceem.AbstractWeceemIntegrationTest

import org.weceem.content.*
import org.weceem.html.WcmHTMLContent

class WcmContentFingerprintServiceTests extends AbstractWeceemIntegrationTest {

    def statusPublished
    def statusDraft
    def spaceA
    def templateA
    def templateB
    def childA1
    def childA1comment1
    def childA1comment2
    def childA2
    def parentA
    def rootNode1
    
    def wcmContentFingerprintService
    def wcmContentDependencyService
    
    void setUp() {
        
        super.setUp()
    
        WcmContentFingerprintService.metaClass.getLog = { ->
            [debugEnabled: true, debug: { s -> println s } ]
        }
        
        wcmContentFingerprintService.reset()
        wcmContentDependencyService.reset()

        createContent {
            statusPublished = status(code: 400)
            statusDraft = status(code: 100, description: "draft", publicContent: false)
        }

        spaceA = new WcmSpace(name: 'a', aliasURI: 'a')
        assert spaceA.save(flush: true)

    }

    void initBlogStyleRepo() {
        initRepo('parent-a/**')
    }
    
    void initNonDependentTemplateARepo() {
        initRepo('')
    }
    
    void initDeepBlogRepo() {
        initRepo('parent-a/**', true)
    }

    void initRepo(templateADeps, deep = false) {
        createContent {
            templateA = content(WcmTemplate) {
                title = 'template'
                aliasURI = 'templateA'
                space = spaceA
                status = statusPublished
                content = 'template content'
                contentDependencies = templateADeps
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

        if (deep) {
            createContent {
                // We create comments as HTML as comment does not have template property so no deps
                childA1comment1 = content(WcmHTMLContent) {
                    space = spaceA
                    status = statusPublished
                    title = "Child A1 comment 1"
                    content = "Child A1 comment 1 text"
                    template = templateA
                }
                childA1comment2 = content(WcmHTMLContent) {
                    space = spaceA
                    status = statusPublished
                    title = "Child A1 comment 2"
                    content = "Child A1 comment 2 text"
                    template = templateB
                }
                
                childA1.addToChildren(childA1comment1)
                childA1.addToChildren(childA1comment2)
            }
        }
        wcmContentDependencyService.reset()
    }

    void testFingerprintChangesOnTemplatedContentAndTemplateWhenNonDependentTemplateChanges() {
        initNonDependentTemplateARepo()

        def oldTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP
        def oldParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP
        def oldChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        dumpInfo()

        println "-"*20
        
        templateA.content = "bla bla bla"
        templateA.save(flush:true)
        
        wcmContentFingerprintService.updateFingerprintFor(templateA)
        
        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        def newParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        def newParentTreeAFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        def newChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        def newChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)

        dumpInfo()
        
        assertNotNull newTemplateFP
        assertTrue oldTemplateFP != newTemplateFP

        // These must also change fingerprint as they dependen on the template
        assertNotNull newParentAFP
        assertTrue oldParentAFP != newParentAFP
        assertNotNull newChildA1FP
        assertTrue oldChildA1FP != newChildA1FP

        // parent tree should have changed, one of the children uses the template
        println "Old parent tree: $oldParentATreeFP - new: $newParentTreeAFP"
        assertNotNull newParentTreeAFP
        assertTrue oldParentATreeFP != newParentTreeAFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newTemplateFP, wcmContentFingerprintService.getFingerprintFor(templateA)

        // If we invalidate it and regenerate it, is it the same?
        wcmContentFingerprintService.invalidateFingerprintFor(templateA)

        // Test invariance of the fingerprint
        assertEquals newTemplateFP, wcmContentFingerprintService.getFingerprintFor(templateA)
    }
    
    void testFingerprintChangesOnTemplatedContentAndTemplateWhenCircularRefTemplateChanges() {
        initBlogStyleRepo()

        def oldTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP
        def oldParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP
        def oldChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        dumpInfo()

        println "-"*20
        
        templateA.content = "bla bla bla"
        templateA.save(flush:true)
        
        wcmContentFingerprintService.updateFingerprintFor(templateA)
        
        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        def newParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        def newParentTreeAFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        def newChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        def newChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)

        dumpInfo()
        
        assertNotNull newTemplateFP
        assertTrue oldTemplateFP != newTemplateFP

        // These must also change fingerprint as they dependen on the template
        assertNotNull newParentAFP
        assertTrue oldParentAFP != newParentAFP
        assertNotNull newChildA1FP
        assertTrue oldChildA1FP != newChildA1FP

        // parent tree should have changed, one of the children uses the template
        println "Old parent tree: $oldParentATreeFP - new: $newParentTreeAFP"
        assertNotNull newParentTreeAFP
        assertTrue oldParentATreeFP != newParentTreeAFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newTemplateFP, wcmContentFingerprintService.getFingerprintFor(templateA)

        // If we invalidate it and regenerate it, is it the same?
        wcmContentFingerprintService.invalidateFingerprintFor(templateA)

        // Test invariance of the fingerprint
        assertEquals newTemplateFP, wcmContentFingerprintService.getFingerprintFor(templateA)
    }

    void testFingerprintChangesOnNonTemplatedContent() {
        initBlogStyleRepo()
        
        def oldRootFP = wcmContentFingerprintService.getFingerprintFor(rootNode1)
        
        rootNode1.content = "bla bla bla"
        rootNode1.save(flush:true)
        
        wcmContentFingerprintService.updateFingerprintFor(rootNode1)
        
        // Now we expect finger prints for template and its dependents to be updated
        def newRootFP = wcmContentFingerprintService.getFingerprintFor(rootNode1)

        assertNotNull newRootFP
        assertTrue oldRootFP != newRootFP

        // Test invariance of the fingerprint
        assertEquals newRootFP, wcmContentFingerprintService.getFingerprintFor(rootNode1)
    }

    void testFingerprintChangesOnParentWithTemplateThatDependsOnDescendentsWhenDescendentChanges() {
        initBlogStyleRepo()
        
        def oldTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP
        def oldParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP
        def oldChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        childA1.content = "this should cascade changes"
        childA1.save(flush:true)
        
        println "We're updating A1 FP now..."
        dumpInfo()
        wcmContentFingerprintService.updateFingerprintFor(childA1)
        println "We're done updating A1 FP now."
        dumpInfo()
        
        // The parent and tree must also be changed
        def newParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull newParentAFP
        assertTrue "The parent fingerprint did not change", oldParentAFP != newParentAFP

        def newParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull newParentATreeFP
        assertTrue "The parent tree fingerprint did not change", oldParentATreeFP != newParentATreeFP

        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        def newChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        def newChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)

        // Node we modified must change
        assertNotNull newChildA1FP
        assertTrue "The child fingerprint did not change", oldChildA1FP != newChildA1FP

        // Template must have changed, it depends on it
        assertNotNull newTemplateFP
        assertTrue "The template fingerprint did not change", oldTemplateFP != newTemplateFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newChildA1FP, wcmContentFingerprintService.getFingerprintFor(childA1)        
    }
    
    void testFingerprintChangesOnParentWithTemplateThatDependsOnDescendentsWhenDeepDescendentChanges() {
        initDeepBlogRepo()
        
        def oldTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        assertNotNull oldTemplateFP

        def oldParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull oldParentAFP
        def oldParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull oldParentATreeFP

        def oldChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        assertNotNull oldChildA1FP
        def oldChildA1TreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(childA1)
        assertNotNull oldChildA1TreeFP

        def oldChildA1comment1FP = wcmContentFingerprintService.getFingerprintFor(childA1comment1)
        assertNotNull oldChildA1comment1FP
        def oldChildA1comment2FP = wcmContentFingerprintService.getFingerprintFor(childA1comment2)
        assertNotNull oldChildA1comment2FP

        def oldChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)
        assertNotNull oldChildA2FP
        
        childA1comment1.content = "this should cascade changes up the graph"
        childA1comment1.save(flush:true)
        
        println "We're updating A1 comment1 FP now..."
        dumpInfo()
        wcmContentFingerprintService.updateFingerprintFor(childA1comment1)
        println "We're done updating A1 comment1 FP now."
        dumpInfo()
        
        // The grandparent and tree must also be changed
        def newParentAFP = wcmContentFingerprintService.getFingerprintFor(parentA)
        assertNotNull newParentAFP
        assertTrue "The grandparent fingerprint did not change", oldParentAFP != newParentAFP

        // Grandparent tree check...
        def newParentATreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(parentA)
        assertNotNull newParentATreeFP
        assertTrue "The grandparent tree fingerprint did not change", oldParentATreeFP != newParentATreeFP

        // parent tree check...
        def newChildA1TreeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(childA1)
        assertNotNull newChildA1TreeFP
        assertTrue "The parent tree fingerprint did not change", oldChildA1TreeFP != newChildA1TreeFP

        // Now we expect finger prints for template and its dependents to be updated
        def newTemplateFP = wcmContentFingerprintService.getFingerprintFor(templateA)
        def newChildA1FP = wcmContentFingerprintService.getFingerprintFor(childA1)
        def newChildA1comment1FP = wcmContentFingerprintService.getFingerprintFor(childA1comment1)
        def newChildA1comment2FP = wcmContentFingerprintService.getFingerprintFor(childA1comment2)
        def newChildA2FP = wcmContentFingerprintService.getFingerprintFor(childA2)

        // Node we modified must change
        assertNotNull newChildA1comment1FP
        assertTrue "The child fingerprint did not change", oldChildA1comment1FP != newChildA1comment1FP

        // Sibling must not have changed, uses templateB so not dep on sibling
        assertNotNull newChildA1comment2FP
        assertTrue "The sibling fingerprint changed", oldChildA1comment2FP == newChildA1comment2FP

        // Parent modified must change
        assertNotNull newChildA1FP
        assertTrue "The parent fingerprint did not change", oldChildA1FP != newChildA1FP

        // Template must have changed, it depends on it
        assertNotNull newTemplateFP
        assertTrue "The template fingerprint did not change", oldTemplateFP != newTemplateFP

        // ChildA2 must not have changed, it uses a different template
        assertEquals newChildA2FP, oldChildA2FP
        
        // Test invariance of the fingerprint
        assertEquals newChildA1comment1FP, wcmContentFingerprintService.getFingerprintFor(childA1comment1)        
    }
    
    void testDeepFingerprintCalculationFromCold() {
        initDeepBlogRepo()
        
        def commentFP = wcmContentFingerprintService.getFingerprintFor(childA1comment1)
        assertNotNull commentFP
    }
    
    void testDeepTreeFingerprintCalculationFromCold() {
        initDeepBlogRepo()
        
        def n = childA1comment1
        while (n = n.parent) {
            def treeFP = wcmContentFingerprintService.getTreeHashForDescendentsOf(n)
            assertNotNull treeFP
        }
    }
    
    void dumpInfo() {
        wcmContentDependencyService.dumpDependencyInfo(true)
        wcmContentFingerprintService.dumpFingerprintInfo(true)
    }
}
