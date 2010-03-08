package org.weceem.controllers

import com.thoughtworks.xstream.XStream
import java.text.SimpleDateFormat

import org.weceem.content.*
import org.weceem.export.*
import org.weceem.versioning.*

/**
 * @author Sergei Shushkevich
 */
class WcmVersionController {

    def pagingService
    def xstreamConfigurer
    def contentVersionService

    def index = {
        redirect(action: list, params: params)
    }

    def list = {
        def model = [spaceList: Space.list(), contentTypeList: getAvailableContentTypes()]
        def dateFormat = new SimpleDateFormat('yyyy-MM-dd')
        def pagingResult = pagingService.search(ContentVersion.class, params) {c ->
            if (params.space) {
                c.eq('spaceName', params.space)
            }
            if (params.contentType) {
                c.eq('objectClassName', params.contentType)
            }
            if (params.createdFrom) {
                c.ge('createdOn', dateFormat.parse(params.createdFrom))
            }
            if (params.createdTo) {
                c.lt('createdOn', dateFormat.parse(params.createdTo) + 1)
            }
            if (params.username) {
                c.eq('createdBy', params.username)
            }
        }
        model.putAll pagingResult

        model
    }

    def restore = {
        def contentVersion = ContentVersion.get(params.id)
        if (contentVersion) {
            // todo:
            // handle situations when:
            // - content for restoring was deleted, but another with same contentHeader and spaceName was created
            //   (also need to handle other situations when changes were performed for title or space (i.e. unique key);
            // - referenced domain objects was deleted (e.g. Space)
            def xstream = new XStream()
            xstream.registerConverter(new DomainConverter(contentVersion.objectClassName))
            def content = Content.get(contentVersion.objectKey)
            if (content) {
                // Using bindData to work around Grails 1.2m2 bugs, change to .properties when 1.2-RC1 is live
                bindData(content, getRestoredProperties(xstream.fromXML(contentVersion.objectContent)) )
                content.save()
            } else {
                // if Content was deleted
                def versionedContent = xstream.fromXML(contentVersion.objectContent)
                content = versionedContent.class.newInstance(getRestoredProperties(versionedContent))
                content.save()
                ContentVersion.executeUpdate(
                        "update ContentVersion cv set cv.objectKey = ? where cv.objectKey = ?",
                        [content.id, contentVersion.objectKey])
            }
            flash.message = "Content with title '${content.title}' restored to ${contentVersion.revision} revision."
        }
        redirect(action: list)
    }

    def changes = {
        def contentVersion = ContentVersion.get(params.id)
        def currentContent = Content.get(contentVersion.objectKey)

        // if Content was deleted
        if (!currentContent) {
            flash.message = "Content with title '${contentVersion.contentHeader}' can't be compared with current version because it was deleted."
            redirect(action: list)
        }

        def oldContent = new XmlSlurper().parseText(contentVersion.objectContent)

        def latestRevision = ContentVersion.createCriteria().get {
            eq('objectKey', contentVersion.objectKey)
            projections {
                max('revision')
            }
        }

        ['content': DiffUtils.diffString(oldContent.content.text().toString(), currentContent.contentAsText),
        'oVersion': contentVersion.revision, 'cVersion': latestRevision + 1,
        'oCreatedBy': contentVersion.createdBy, 'cChangedBy': currentContent.changedBy,
        'oCreatedOn': contentVersion.createdOn, 'cChangedOn': currentContent.changedOn]
    }

    /**
     * Renders changes for specified Content versions.
     *
     * params.fromVersion
     * params.toVersion
     */
    def showVersionsChanges = {
        def fromVersion = ContentVersion.get(params.fromVersion)
        def toVersion = ContentVersion.get(params.toVersion)

        def compareResult = contentVersionService.compareVersions(fromVersion, toVersion)

        render(view: 'changes', model: [content: compareResult,
                oVersion: fromVersion.revision, cVersion: toVersion.revision,
                oCreatedBy: fromVersion.createdBy, cChangedBy: toVersion.createdBy,
                oCreatedOn: fromVersion.createdOn, cChangedOn: toVersion.createdOn])
    }

    private def getRestoredProperties(versionedContent) {
        versionedContent.properties.findAll {key, value ->
            key != 'beforeUpdate' && key != 'beforeDelete'
        }
    }

    private def getAvailableContentTypes() {
        def contentClass = grailsAttributes.grailsApplication.getArtefact('Domain', 'Content')
        def result = contentClass.subClasses.collect {
            it.name
        }
        return result.sort()
    }
}
