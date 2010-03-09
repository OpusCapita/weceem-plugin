import org.weceem.services.WcmEventService
import org.weceem.event.Events

import org.weceem.content.WcmContent

class EventServiceTests extends grails.test.GrailsUnitTestCase {

  def listenerCalled = false
  def eventService

  protected void setUp() {
    super.setUp()
    eventService = new WcmEventService()
    listenerCalled = false
  }

  protected void tearDown() {
    super.tearDown()
  }

  void testAfterContentAddedEvent() {
    // first test without a listener
    eventService.afterContentAdded(new WcmContent())

    // then test with a listener
    eventService.registerListener(Events.AFTER_CONTENT_ADDED, this)
    eventService.afterContentAdded(new WcmContent())
    assertTrue listenerCalled

    // remove listener and test again
    eventService.unregisterListener(Events.AFTER_CONTENT_ADDED, this)
    listenerCalled = false
    eventService.afterContentAdded(new WcmContent())
    assertFalse listenerCalled
  }

  void testAfterContentUpdatedEvent() {
    // first test without a listener
    eventService.afterContentUpdated(new WcmContent())

    // then test with a listener
    eventService.registerListener(Events.AFTER_CONTENT_UPDATED, this)
    eventService.afterContentUpdated(new WcmContent())
    assertTrue listenerCalled

    // remove listener and test again
    eventService.unregisterListener(Events.AFTER_CONTENT_UPDATED, this)
    listenerCalled = false
    eventService.afterContentUpdated(new WcmContent())
    assertFalse listenerCalled
  }

  void testAfterContentRemovedEvent() {
    // first test without a listener
    eventService.afterContentRemoved(new WcmContent())

    // then test with a listener
    eventService.registerListener(Events.AFTER_CONTENT_REMOVED, this)
    eventService.afterContentRemoved(new WcmContent())
    assertTrue listenerCalled

    // remove listener and test again
    eventService.unregisterListener(Events.AFTER_CONTENT_REMOVED, this)
    listenerCalled = false
    eventService.afterContentRemoved(new WcmContent())
    assertFalse listenerCalled
  }

  void afterWeceemContentUpdated(WcmContent content) {
    listenerCalled = true
  }

  void afterWeceemContentAdded(WcmContent content) {
    listenerCalled = true
  }

  void afterWeceemContentRemoved(WcmContent content) {
    listenerCalled = true
  }
}

