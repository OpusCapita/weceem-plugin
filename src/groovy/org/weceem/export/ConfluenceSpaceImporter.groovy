package org.weceem.export

import javax.xml.parsers.SAXParserFactory
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.apache.commons.logging.Log

import org.weceem.content.*
import org.weceem.wiki.* // Nasty

/**
 * ConfluenceSpaceImporter.
 *
 * @author Sergei Shushkevich
 */
class ConfluenceSpaceImporter implements SpaceImporter {

    Log log = LogFactory.getLog(DefaultSpaceImporter)

    public void execute(WcmSpace space, File file) {
        def grailsApp = ApplicationHolder.application

        def defStatus = WcmStatus.findByPublicContent(true)
        def parser = SAXParserFactory.newInstance().newSAXParser()
        def handler = new SAXConfluenceParser()
        try {
            parser.parse(new FileInputStream(file), handler)
        } catch (Exception e) {
            log.error( "Unable to import Confluence file ${file}", e)
            throw new ImportException("Uploaded file can't be parsed. Check it and try again.")
        }
        // merge pages and bodies
        handler.pages.eachWithIndex {page, i ->
            page.body = handler.bodies[i]
        }
        // group pages by title
        def pageGroups = handler.pages.groupBy { it.title }
        pageGroups.each {k, v ->
            def wi = new WcmWikiItem(title: k, space: space, status: defStatus)
            wi.createAliasURI(null)
            def latestItem = v.max { Long.valueOf(it.version) }
            wi.content = latestItem.body

            def wikiItem = WcmWikiItem.findWhere(aliasURI: wi.aliasURI, space: space)
            if (wikiItem) {
                // @todo remove this and revert to x.properties = y after Grails 1.2-RC1
                grailsApp.mainContext.wcmContentRepositoryService.hackedBindData(wikiItem, wi.properties)
            } else wikiItem = wi
            wikiItem.save(flush: true)
        }
    }

    public String getName() {
        'Confluence (XML)'
    }
}
