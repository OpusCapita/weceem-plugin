import org.weceem.services.WcmEventService
import org.weceem.event.WeceemEvent

import org.weceem.content.WcmContent

class EventServiceTests extends grails.test.GrailsUnitTestCase {

    def listenerCalled = false
    def eventService

    protected void setUp() {
        super.setUp()
        mockLogging(WcmEventService)
        eventService = new WcmEventService()
        listenerCalled = false
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testEventWithNoListeners() {
        eventService.event(WeceemEvent.contentDidGetCreated, new WcmContent())
    }
    
    void testContentDidGetCreatedEvent() {
        // then test with a listener
        eventService.addListener(this)
        eventService.event(WeceemEvent.contentDidGetCreated, new WcmContent())
        assertTrue listenerCalled

        // remove listener and test again
        eventService.removeListener(this)
        listenerCalled = false
        eventService.event(WeceemEvent.contentDidGetCreated, new WcmContent())
        assertFalse listenerCalled
    }

    void contentDidGetCreated(WcmContent content) {
        listenerCalled = true
    }
}

