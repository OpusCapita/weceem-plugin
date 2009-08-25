
import grails.test.*
import org.weceem.content.Content

import org.weceem.services.EventService

class EventServiceTests extends GroovyTestCase {

    def eventService
    def grailsApplication

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testContentAddedEvent() {
        def callback = false
        // First without a listener
        eventService.contentAdded(new Content(), [])
        // Then with a listener
        EventService.metaClass.'static'.onWeceemContentAdded = {a, b -> callback = a instanceof Content && b.size() == 0}
        eventService.contentAdded(new Content(), [])
        assertTrue callback
    }
}

