/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.weceem.content

import org.weceem.files.*
import org.weceem.services.*

/**
 * WcmSpace class describes a user space.
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class WcmSpace implements Serializable {
    String name

    String aliasURI = '' // Default to blank eg / uri namespace
    
    static searchable = {
        root false // We con't want to search for WcmSpace instances

        only = ['id'] // used for finding content by space
    }
    
    static mapping = {
        cache usage: 'read-write'
        name index: 'space_name_Idx'
        aliasURI index: 'space_aliasURI_Idx'
    }

    static constraints = {
        name(nullable: false, blank: false, unique: true)
        aliasURI(nullable: true, blank: true, unique: true, size:0..80)
    }
    
    public String makeUploadName(){
        return (aliasURI == "") ? WcmContentRepositoryService.EMPTY_ALIAS_URI : aliasURI
    }

    boolean equals(o) {
        if (o == null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        WcmSpace wcmSpace = (WcmSpace) o

        if (name != wcmSpace.name) return false

        return true
    }

    int hashCode() {
        return name.hashCode()
    }
}
