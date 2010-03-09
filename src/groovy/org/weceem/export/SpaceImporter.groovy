package org.weceem.export

import org.weceem.content.*

/**
 * SpaceImporter.
 *
 * @author Sergei Shushkevich
 */
interface SpaceImporter {

    void execute(WcmSpace space, File file)

    String getName()
}
