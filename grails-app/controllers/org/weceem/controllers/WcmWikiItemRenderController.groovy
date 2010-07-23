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
package org.weceem.controllers

import net.java.textilej.parser.builder.HtmlDocumentBuilder
import net.java.textilej.parser.MarkupParser
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import com.jcatalog.wiki.WeceemDialect

class WcmWikiItemRenderController {
    def show = {
        def wikiWriter = new StringWriter()
        def builder = new HtmlDocumentBuilder(wikiWriter)
        builder.emitAsDocument = false
        def parser = new MarkupParser(new WeceemDialect(), builder)
        parser.parse(contentText)
        def replaced = processMacro(wikiWriter.toString())
        // @todo we are doing nothing with this
        contentText = processEmptyMacro(replaced)        
    }

    private def processMacro(content) {
        content.replaceAll(/\{(.*?)(?::(.*?))?\}(.*?)\{\1\}/) {Object[] match ->
            def tagName = match[1]
            if (!grailsApplication.getArtefactForFeature(
                    TagLibArtefactHandler.TYPE, 'macro:' + tagName)) {
                return match[0]
            }
            def tagBody = match[3]
            def tagAttrs = processMacroAttributes(match[2])
            return macro."$tagName"(tagAttrs) { tagBody }
        }
    }

    private def processEmptyMacro(content) {
        content.replaceAll(/\{(.*?)(?::(.*?))?\}/) {Object[] match ->
            def tagName = match[1]
            if (!grailsApplication.getArtefactForFeature(
                    TagLibArtefactHandler.TYPE, 'emacro:' + tagName)) {
                return match[0]
            }
            def tagAttrs = processMacroAttributes(match[2])
            return emacro."$tagName"(tagAttrs)
        }
    }

    private def processMacroAttributes(String attrs) {
        if (!attrs) return [:]
        def result = [:]
        attrs.eachMatch(/(\w+)=([^|]+)/) {match ->
            result[match[1]] = match[2]
        }
        return result
    }
}