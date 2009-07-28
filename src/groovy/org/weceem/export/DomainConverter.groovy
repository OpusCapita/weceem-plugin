package org.weceem.export

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.converters.UnmarshallingContext
import org.codehaus.groovy.grails.commons.ApplicationHolder

import org.weceem.content.*

/**
 * Simply outputs primary key for domain classes to the stream.
 *
 * @author Sergei Shushkevich
 */
class DomainConverter implements Converter {

    String rootClassName

    DomainConverter(String rootClassName) {
        this.rootClassName = rootClassName
    }

    public boolean canConvert(Class clazz) {
        clazz.name != rootClassName && (clazz.equals(Space.class) || clazz.equals(Template.class) || clazz.equals(Language.class))
    }

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        writer.startNode('className')
        writer.setValue(value.class.name)
        writer.endNode()
        writer.startNode('id')
        writer.setValue(value.id as String)
        writer.endNode()
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown()
        def className = reader.value
        reader.moveUp()
        reader.moveDown()
        def id = reader.value
        reader.moveUp()
        return ApplicationHolder.application.getClassForName(className).get(id)
    }
}