package org.weceem.services

import org.springframework.beans.factory.InitializingBean

import grails.util.GrailsNameUtils

import org.weceem.content.WcmContent
import org.weceem.event.WeceemEvents
import org.weceem.event.WeceemDomainEvents
import org.weceem.event.EventMethod

/*
 * A service to send events to hooks used by plugins/internally
 * @author Scott Burch
*/

class WcmEventService implements InitializingBean {

    boolean transactional = false

    private listeners = []

    void afterPropertiesSet() {
        initMetaMethods()
        defineEvents(WeceemEvents)
        defineEvents(WeceemDomainEvents)
    }
    
    /*
     * Register a listener with a callback method
     */
    void addListener(listener) {
        assert listener.conformsTo(WeceemEvents)
        listeners << listener
    }

    /*
     * Unregister a listener
     */
    void removeListener(listener) {
        listeners.remove(listener)
    }

    /**
     * Call the event handler on any listeners
     */
    void event(EventMethod event, WcmContent contentNode) {
        assert event.definedIn(WeceemEvents)
        
        if (log.debugEnabled) {
            log.debug "Triggering notification event: ${event} for ${contentNode.absoluteURI}"
        }
        
        def listenerList
        synchronized (listeners) {
            listenerList = listeners.toArray()
        }
        
        def eventName = event
        listenerList.each { l ->
            if (l.conformsTo(event)) {
                event.invokeOn(l, [contentNode])
            }
        }
    }
    
    private void initMetaMethods() {
        Object.metaClass.conformsTo = { Class c -> 
            def obj = delegate
            c.eventMethods.any { m ->
                obj.conformsTo(m)
            }
        }
        Object.metaClass.conformsTo = { EventMethod m -> 
            delegate.metaClass.respondsTo(delegate, m.name, *m.argTypes) as Boolean
        }
    }

    void defineEvents(Class eventsClass) {
        def declarations = eventsClass.events
        declarations.delegate = new EventDSLDelegate(clazz:eventsClass)
        declarations()

        declarations.delegate.meths.each { protoMeth ->
            def mn = GrailsNameUtils.getClassNameRepresentation(protoMeth.name)
            log.debug "Registering method: get${mn} on ${eventsClass}"
            eventsClass.metaClass.static."get${mn}" = { -> protoMeth }
        }

        eventsClass.metaClass.static."getEventMethods" = { -> declarations.delegate.meths }
    }
}


class EventDSLDelegate {
    def meths = []
    Class clazz

    def methodMissing(String name, args) {
        assert args.size() <= 1
        def argTypes 
        if (args.size() == 1) {
            assert args[0] instanceof Closure

            argTypes = args[0].parameterTypes
        } else {
            argTypes = []
        }
        meths << new EventMethod(name, clazz, argTypes as Class[])
    }
}

