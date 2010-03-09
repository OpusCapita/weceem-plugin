package org.weceem.services

/**
 * Provides methods for WcmContent versions manipulations.
 *
 * @author Sergei Shushkevich
 */
class WcmContentVersionService {

    /**
     * Compares two versions of content.
     *
     * @param prevVersion
     * @param nextVersion
     */
    def compareVersions(prevVersion, nextVersion) {
        def prevContent = new XmlSlurper().parseText(prevVersion.objectContent)
        def nextContent = new XmlSlurper().parseText(nextVersion.objectContent)

        DiffUtils.diffString(prevContent.content.text().toString(), nextContent.content.text().toString())
    }
}
