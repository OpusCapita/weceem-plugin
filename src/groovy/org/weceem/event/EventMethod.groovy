package org.weceem.event

class EventMethod {
    final Class declaredIn
    final String name
    final Class[] argTypes
    
    EventMethod(String name, Class declaredIn, Class[] argTypes) {
        this.@name = name
        this.@declaredIn = declaredIn
        this.@argTypes = argTypes
    }
    
    boolean definedIn(Class c) { declaredIn.isAssignableFrom(c) }
    
    def invokeOn(target, Object[] args) {
        if (args) {
            target."${name}"(*args)
        } else {
            target."${name}"()
        }
    }
    
    String toString() { "Event ${declaredIn.name}.${name} (${argTypes*.name.join(', ')})"}
}