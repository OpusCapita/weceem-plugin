package weceem

import com.grailsrocks.functionaltest.*

class ConcurrentEditFunctionalTests extends BrowserTestCase  {
    void testSaveWidgetWhileRenderingPageUsingIt() {

        javaScriptEnabled = false

        def uriToGet = "index"
        def serverURL = baseURL
        
        // Create space
        get('/wcm/admin/space/create')
        form {
            name = "Default"
            aliasURI = ''
            click "_action_save"
        }
        
        assertStatus 200
        assertContentContains 'created'
        
        get('/wcm/admin')
        assertStatus 200
        assertContentContains 'welcome'

        // Edit the widget
        def editLink = byXPath("//a[contains(text(), 'Common header')]")
        editLink.click()

        // In the editor
        assertStatus 200
        assertContentContains 'edit widget'

        
        form {
            title = "UPDATED: Common header"
            
            // Now we do the GET to simulate concurrent request
            def u = new URL(serverURL+uriToGet)
            concurrentGet(u)
            
            click "_action_update"
        }
        
        // Back at repo view?
        assertStatus 200
        assertContentContains 'welcome'

        Thread.sleep(10000)
    }
    
    void concurrentGet(u) {
        println "Doing concurrent GET to $u"
        50.times {
            Thread.start {
                println "Output of concurrent GET to $u was: "+u.text
            }
        }
    }
}
