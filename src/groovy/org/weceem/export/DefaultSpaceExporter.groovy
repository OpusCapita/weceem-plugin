package org.weceem.export

import java.text.SimpleDateFormat

import com.thoughtworks.xstream.XStream

import org.weceem.content.*
import org.weceem.files.*

/**
 * WeceemSpaceExporter.
 *
 * @author Sergei Shushkevich
 */
class DefaultSpaceExporter implements SpaceExporter {

    File execute(WcmSpace space) {
        def ts = new SimpleDateFormat('yyMMddHHmmssSSS').format(new Date())
        def filesDir = org.weceem.services.WcmContentRepositoryService.getUploadPath(space)

        def baseDir = new File("${filesDir.absolutePath}/export-${ts}")
        baseDir.mkdir()

        def file = new File("${baseDir.absolutePath}/content.xml")

        def xstream = new XStream()
        omitFields xstream

        def contentList = WcmContent.findAll(
                "from WcmContent c where c.space = ? and c.class != ?",
                [space, WcmTemplate.class.name])
        def templateList = []
        def parentsList = []
        contentList.each {content ->
            if (content.metaClass.hasProperty(content, 'template')
                    && content.template) {
                templateList << content.template
            }
            if (!content.parent) {
                parentsList << content
            }
        }
        templateList = templateList.unique()
        file << '<content>'
        if (templateList) {
            // 1. export spaces (required for templates)
            xstream.registerConverter(new ImportExportConverter(WcmSpace.class.name))
            WcmSpace.findAll("from Space s where s.id in (${(templateList.collect {it.space.id}).join(',')})")
                    .each {sp ->
                file << xstream.toXML(sp)
            }

            // 2. export templates (required for content)
            xstream = new XStream()
            omitFields xstream
            xstream.registerConverter(new ImportExportConverter(WcmTemplate.class.name))
            templateList.each {template ->
                file << xstream.toXML(template)
            }
        }
        // 3. export other content
        parentsList.each {content ->
            xstream = new XStream()
            omitFields xstream
            xstream.registerConverter(new ImportExportConverter(content.class.name))

            file << xstream.toXML(content)            
        }
        (contentList-parentsList).each {content ->
            xstream = new XStream()
            omitFields xstream
            xstream.setMode(XStream.NO_REFERENCES)
            xstream.registerConverter(new ImportExportConverter(content.class.name))
            file << xstream.toXML(content)
        }

        file << '</content>'
        def ant = new AntBuilder()

        ant.copy(todir: "${baseDir.absolutePath}/files", failonerror: false) {
            fileset(dir: "${filesDir.absolutePath}/${space.name}")
        }

        def tmp = File.createTempFile("export", ".zip")
        tmp.delete()
        ant.zip(destfile: tmp.absolutePath, basedir: baseDir.absolutePath)
        ant.delete(dir: baseDir.absolutePath)
        return tmp
    }

    String getMimeType() {
        'application/zip'
    }

    String getName() {
        'Weceem 0.1 (ZIP)'
    }

    protected void omitFields(xstream) {
        xstream.omitField(WcmContent.class, 'children')
        xstream.omitField(WcmContent.class, 'beforeInsert')
        xstream.omitField(WcmContent.class, 'beforeUpdate')
        xstream.omitField(WcmContent.class, 'beforeDelete')
    }
}
