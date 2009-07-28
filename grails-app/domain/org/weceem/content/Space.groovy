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

/**
 * Space class describes a user space.
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class Space {
    String name

    String aliasURI = '' // Default to blank eg / uri namespace
    
    static searchable = {
        only = ['name']
    }
    
    static mapping = {
        cache usage: 'nonstrict-read-write'
        name index: 'space_name_Idx'
        aliasURI index: 'space_aliasURI_Idx'
    }

    static constraints = {
        name(nullable: false, blank: false, unique: true)
        aliasURI(nullable: false, blank: true, unique: true, size:0..80)
    }
}
