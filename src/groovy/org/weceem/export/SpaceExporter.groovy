package org.weceem.export

import org.weceem.content.*

/**
 * SpaceExporter.
 *
 * @author Sergei Shushkevich
 */
interface SpaceExporter {

    File execute(Space space)

    String getMimeType()

    String getName()
}
