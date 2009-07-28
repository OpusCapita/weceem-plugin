package org.weceem.export

import org.weceem.content.*

/**
 * SpaceImporter.
 *
 * @author Sergei Shushkevich
 */
interface SpaceImporter {

    void execute(Space space, File file)

    String getName()
}
