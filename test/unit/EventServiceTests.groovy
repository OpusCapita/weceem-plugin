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
    listenerCalled = false
  }

  protected void tearDown() {
    super.tearDown()
  }

  void testAfterContentAddedEvent() {
    // first test without a listener
    eventService.afterContentAdded(new Content(), [])

    // then test with a listener
    eventService.registerListener(Events.AFTER_CONTENT_ADDED, this)
    eventService.afterContentAdded(new Content(), [])
    assertTrue listenerCalled

    // remove listener and test again
    eventService.unregisterListener(Events.AFTER_CONTENT_ADDED, this)
    listenerCalled = false
    eventService.afterContentAdded(new Content(), [])
    assertFalse listenerCalled
  }

  void testAfterContentUpdatedEvent() {
    // first test without a listener
    eventService.afterContentUpdated(new Content(), [])

    // then test with a listener
    eventService.registerListener(Events.AFTER_CONTENT_UPDATED, this)
    eventService.afterContentUpdated(new Content(), [])
    assertTrue listenerCalled

    // remove listener and test again
    eventService.unregisterListener(Events.AFTER_CONTENT_UPDATED, this)
    listenerCalled = false
    eventService.afterContentUpdated(new Content(), [])
    assertFalse listenerCalled
  }



  void afterWeceemContentUpdated(Content content, params) {
    listenerCalled = content instanceof Content && params.size == 0
  }

  void afterWeceemContentAdded(Content content, params) {
    listenerCalled = content instanceof Content && params.size == 0
  }
}

