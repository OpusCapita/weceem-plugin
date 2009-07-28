package org.weceem.services

import com.thoughtworks.xstream.XStream

/**
 * Provides methods for Content versions manipulations.
 *
 * @author Sergei Shushkevich
 */
class ContentVersionService {

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
