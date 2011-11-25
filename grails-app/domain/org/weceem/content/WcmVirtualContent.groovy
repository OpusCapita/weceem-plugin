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
 *  WcmVirtualContent class.
 *
 * @author Stephan Albers
 * @author July Karpey
 */
class WcmVirtualContent extends WcmContent {
    WcmContent target
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "virtual-content-32.png"]

    static searchable = {
        alias WcmVirtualContent.name.replaceAll("\\.", '_')
        
        only = ['title', 'status']
    }

    Map getVersioningProperties() { 
        super.getVersioningProperties() + [ 
            target:target?.ident()
        ] 
    }

    /**
     * Override the normal dependency mechanism and auto-depend on the target
     */
    @Override
    String getHardDependencies() {
        target ? super.hardDependencies + ',' + target?.absoluteURI : super.hardDependencies
    }
    
    boolean contentShouldAcceptChildren() { false }

    static constraints = {
        target(nullable: true)
    }
    
    static mapping = {
        //target lazy:false // we never want proxies for this, but in Grails < 1.2 final, this gives us bad proxies
    }
    
    static editors = {
        contentDependencies hidden: true
    }
}