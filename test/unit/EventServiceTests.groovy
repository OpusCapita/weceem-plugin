import org.weceem.services.WcmEventService
import org.weceem.event.WeceemEvents

import org.weceem.content.WcmContent

class EventServiceTests extends grails.test.GrailsUnitTestCase {

    def listenerCalled = false
    def eventService

    protected void setUp() {
        super.setUp()
        mockLogging(WcmEventService)

        eventService = new WcmEventService()
        eventService.afterPropertiesSet()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testEventWithNoListeners() {
        eventService.event(WeceemEvents.contentDidGetCreated, new WcmContent())
    }
    
    void testContentDidGetCreatedEvent() {
        // then test with a listener
        def l = new TestListener()
        eventService.addListener(l)
        eventService.event(WeceemEvents.contentDidGetCreated, new WcmContent())
        assertTrue l.called

        // remove listener and test again
        eventService.removeListener(l)  
        l.called = false
        eventService.event(WeceemEvents.contentDidGetCreated, new WcmContent())
        assertFalse l.called
    }
}

class TestListener {
    boolean called

    void contentDidGetCreated(WcmContent content) {
        called = true
    }
}