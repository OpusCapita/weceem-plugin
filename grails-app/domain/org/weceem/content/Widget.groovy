package org.weceem.content

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

/**
 * Widget class presents a piece of information on the screen with special
 * settings. Widget also has a template node with the path to the widget
 * template (GSP page).
 *
 * TODO: add properties fields for settings, like:
 *   Setting setting (a widget can define it's variables);
 *   Milliseconds cacheFor (all widgets having should be cached with their parameters);
 *   configurationParameter (show 5 latest news);
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 */
class Widget extends Content {

    static standaloneContent = false

    Integer width
    Integer height
    Integer color

    // 64Kb Unicode text with HTML/Wiki Markup
    String content
    
    public String getVersioningContent() { content }
    
    
    static mapping = {
        cache usage: 'nonstrict-read-write' 
    }
    
    static editors = {
        content(editor:'HtmlCode')
        width()
        height()
        color()
    }

    static transients = Content.transients
    
    static constraints = {
        content(nullable: false, maxSize: 65536)
        width(nullable: true)
        height(nullable: true)
        color(nullable: true)
    }
}
