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
 *  VirtualContent class.
 *
 * @author Stephan Albers
 * @author July Karpey
 */
class VirtualContent extends Content {
    Content target
    
    static searchable = {
        alias VirtualContent.name.replaceAll("\\.", '_')
        
        only = ['title', 'status']
    }

    Map getVersioningProperties() { 
        super.getVersioningProperties() + [ 
            target:target?.ident()
        ] 
    }
    
    Boolean canHaveChildren() { false }

    static constraints = {
        target(nullable: false)
    }
    
    static mapping = {
        //target lazy:false // we never want proxies for this, but in Grails < 1.2 final, this gives us bad proxies
    }
}