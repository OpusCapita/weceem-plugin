package org.weceem.export

import org.weceem.content.*

/**
 * SpaceExporter.
 *
 * @author Sergei Shushkevich
 */
interface SpaceExporter {

    File execute(WcmSpace space)

    String getMimeType()

    String getName()
}
