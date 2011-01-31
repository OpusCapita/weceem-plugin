package org.weceem.event

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import grails.util.GrailsNameUtils

abstract class EventManager {
    
    static Log log = LogFactory.getLog(EventManager)

    private static initialized
    
    static init() {
        Object.metaClass.conformsTo << { Class c -> 
            def obj = delegate
            c.eventMethods.any { m ->
                obj.conformsTo(m)
            }
        }
        Object.metaClass.conformsTo << { EventMethod m -> 
            delegate.metaClass.respondsTo(delegate, m.name, *m.argTypes) as Boolean
        }
        initialized = true
    }
    
    static define(Class c) {
        if (!initialized) {
            init()
        }
        
        def declarations = c.events
        declarations.delegate = new EventManagerDelegate(clazz:c)
        declarations()
        
        declarations.delegate.meths.each { protoMeth ->
            def mn = GrailsNameUtils.getClassNameRepresentation(protoMeth.name)
            log.debug "Registering method: get${mn} on ${c}"
            c.metaClass.static."get${mn}" = { -> protoMeth }
        }
        
        c.metaClass.static."getEventMethods" = { -> declarations.delegate.meths }
    }
}

class EventManagerDelegate {
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
