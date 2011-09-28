package org.weceem.export

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.converters.UnmarshallingContext

import org.weceem.content.*

/**
 * ImportExportConverter.
 *
 * @author Sergei Shushkevich
 */
class ImportExportConverter implements Converter {

    def rootClassName


    ImportExportConverter(rootClassName) {
        this.rootClassName = rootClassName
    }

    public boolean canConvert(Class clazz) {
        clazz.name != rootClassName && (clazz.equals(WcmSpace.class)
                || clazz.equals(WcmTemplate.class))
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        writer.startNode('className')
        writer.setValue(value.class.name)
        writer.endNode()
        writer.startNode('id')
        writer.setValue(value.id.toString())
        writer.endNode()
        if (value instanceof WcmTemplate) {
            writer.startNode('aliasURI')
            writer.setValue(value.aliasURI)
            writer.endNode()
            writer.startNode('spaceName')
            writer.setValue(value.space.name)
            writer.endNode()
        } else if (value instanceof WcmSpace) {
            writer.startNode('name')
            writer.setValue(value.name)
            writer.endNode()
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
            UnmarshallingContext context) {
        reader.moveDown()
        def className = reader.value
        reader.moveUp()
        reader.moveDown()
        def id = reader.value
        reader.moveUp()
        
        def result
        switch (className) {
            case WcmTemplate.class.name:
                reader.moveDown()
                def aliasURI = reader.value
                reader.moveUp()
                reader.moveDown()
                def spaceName = reader.value
                reader.moveUp()
                result = WcmTemplate.findWhere(aliasURI: aliasURI,
                        space: WcmSpace.findByName(spaceName))
                break

            case WcmSpace.class.name:
                reader.moveDown()
                def name = reader.value
                reader.moveUp()
                result = WcmSpace.findByName(name)
                break
 
            default:
                result = ApplicationHolder.application.getClassForName(className)?.get(id)
        }
        return result
    }
}
