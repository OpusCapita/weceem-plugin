package org.weceem.event

import grails.util.GrailsNameUtils

abstract class EventManager {
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
        
        def declarations = c.protocol
        declarations.delegate = new EventManagerDelegate(clazz:c)
        declarations()
        
        declarations.delegate.meths.each { protoMeth ->
            def mn = GrailsNameUtils.getClassNameRepresentation(protoMeth.name)
            println "Registering method: get${mn}"
            c.metaClass.static."get${mn}" = { -> protoMeth }
        }
        
        c.metaClass.static."getEventMethods" = { -> declarations.delegate.meths }
    }
}

class EventManagerDelegate {
    def meths = []
    Class clazz
    
    def methodMissing(String name, args) {
        assert args.every { a -> a instanceof Class }
        meths << new EventMethod(name, clazz, args as Class[])
    }
}
