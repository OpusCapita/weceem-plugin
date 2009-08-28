
import grails.test.*
import org.weceem.services.EventService
import org.weceem.event.Events

import org.weceem.content.Content

import org.weceem.services.EventService

class EventServiceTests extends grails.test.GrailsUnitTestCase {

    def listenerCalled = false
    def eventService

    protected void setUp() {
        super.setUp()
        eventService = new EventService()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testContentAddedEvent() {
        // first test without a listener
        eventService.contentAdded(new Content(), [])

        // then test with a listener
        eventService.registerListener(Events.CONTENT_ADDED, this)
        eventService.contentAdded(new Content(), [])
        assertTrue listenerCalled

        // remove listener and test again
        eventService.unregisterListener(Events.CONTENT_ADDED, this)
        listenerCalled = false
        eventService.contentAdded(new Content(), [])
        assertFalse listenerCalled

    }

    void onWeceemContentAdded(Content content, params) {
      listenerCalled = content instanceof Content && params.size == 0
    }
}

