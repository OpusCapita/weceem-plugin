package org.weceem.tags

import org.weceem.content.WcmContent
import org.weceem.files.WcmContentFileDB
import org.weceem.content.WcmStatus
import org.weceem.content.WcmTemplate
import org.weceem.script.WcmScript
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup

class EditorFieldTagLib {
    
    static namespace = "wcm"
    
    def proxyHandler
    TagLibraryLookup gspTagLibraryLookup // Tag lookup bean from Grails injection
    
    def editorLabel = { attrs ->
        def args = argsWithRequiredOverride(attrs, [beanName:'content', property:attrs.property, labelKey:'content.label.'+attrs.property])
        out << bean.label(args)
    }

    def editorFieldString = { attrs ->
        def args = argsWithRequiredOverride(attrs, [beanName:'content', property:attrs.property, noLabel:true])
        out << bean.input(args)
    }

    def editorFieldSelectInList = { attrs ->
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true)
    }

    def editorFieldLongString = { attrs ->
        out << bean.textArea(beanName:'content', property:attrs.property, rows:3, cols:40, noLabel:true)
    }

    def editorFieldBoolean = { attrs ->
        out << bean.checkBox(beanName:'content', property:attrs.property, noLabel:true)
    }

    def argsWithRequiredOverride(attrs, args) {
        def con = pageScope.content
        if (con.metaClass.hasProperty(proxyHandler.unwrapIfProxy(con).class, 'overrideRequired')) {
            def r = con.overrideRequired[attrs.property]
            if (r != null) {
                if (!r) {
                    args.requiredField = ' '
                }
            }
        }
        args
    }

    def editorFieldTitle = { attrs ->
        def args = argsWithRequiredOverride(attrs, [beanName:'content', property:attrs.property, noLabel:true, 'class':"big"])
        out << bean.input(args)
    }

    def editorFieldTags = { attrs ->
        out << g.render(template:'/editors/tags', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorResourcesTags = { attrs ->
        out << g.render(template:'/editors/tags_resources', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorFieldReadOnly = { attrs ->
        def v = pageScope.content[attrs.property]
        if (v) v = v.encodeAsHTML()
        out << "<span class=\"field-readonly\">${ v ?: '' }</span>"
    }
    
    def editorFieldReadOnlyURI = { attrs ->
        def v = pageScope.content[attrs.property]
        if (v) v = v.encodeAsURL().encodeAsHTML()
        out << "<span class=\"field-readonly\">${ v ?: '' }</span>"
    }
    
    def editorFieldModifiedBy = { attrs ->
        // Only for xxxxBy fields
        assert attrs.property.endsWith('By')
        
        def byProp = pageScope.content[attrs.property]
        def v = ''
        if (byProp) {
            def onProp = pageScope.content[attrs.property[0..-3]+'On']
            v = "${byProp?.encodeAsHTML()} on ${g.formatDate(date:onProp, format:'d MMM yyyy \'at\' HH:mm:ss')}"
        }
        out << "<span class=\"field-readonly\">${v}</span>"
    }
    
    def editorFieldReadOnlyDate = { attrs ->
        out << "<span class=\"field-readonly\">${g.formatDate(date:pageScope.content[attrs.property], format:'d MMM yyyy HH:mm:ss')}</span>"
    }
    
    def editorFieldDate = { attrs ->
        StringBuilder sb = new StringBuilder()
        def d = pageScope.content[attrs.property]
        sb << g.datePicker(name: attrs.property, value: d, default: "none", precision: "minute", noSelection: [ '':''])
            out << bean.customField(beanName:'content', property:attrs.property, noLabel:true) {
            out << sb
        }
    }
/*
    def editorFieldDateTime = { attrs ->
        out << bean.input(beanName:'content', property:attrs.property, noLabel:true)
        out << g.javascript([:]) {
"""
\$(function(){ \$('#${attrs.property.encodeAsJavaScript()}').datepicker() })
"""
        }
    }
*/
    def editorFieldWcmTemplate = { attrs ->
        def templates = WcmTemplate.findAllBySpace( pageScope.content.space, [sort:'title'])
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            noSelection: ['':'- No template (inherit) -'],
            from: templates, optionValue:'title', optionKey:'id')
    }

    def editorFieldCacheMaxAge = { attrs ->
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            optionKey: { it == null ? '' : it },
            optionValue: { g.message(code:'editor.max.age.seconds.'+(it == null ? 'template' : it) ) } )
    }

    def editorFieldWcmScript = { attrs ->
        def templates = WcmScript.findAllBySpace( pageScope.content.space, [sort:'title'])
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            noSelection: ['':'- Select a script -'],
            from: templates, optionValue:{ it.title + " (${it.absoluteURI})"}, optionKey:'id')
    }

    def editorFieldWcmStatus = { attrs ->
        def statuses = WcmStatus.listOrderByCode()
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            from: statuses, optionValue: { v -> g.message(code:'content.status.'+v.description) }, optionKey:'id')
    }

    def editorFieldWcmSpace = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/space', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorFieldWcmContent = { attrs ->
        def contents =  WcmContent.findAllBySpace( pageScope.content.space, [sort:'title']).findAll( { c -> !c.is(pageScope.content) })
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            noSelection: ['':'- No content -'],
            from: contents, optionValue: { o -> "${o.title} (${o.absoluteURI})" }, optionKey:'id')
    }

    def editorFieldInteger = { attrs ->
        out << bean.input(beanName:'content', property:attrs.property, noLabel:true)
    }

    def editorFieldContentFileUpload = { attrs ->
        if (pageScope.content.fileSize) {
            out << "<span class=\"field-readonly\">File already uploaded (${pageScope.content.fileSize} bytes)</span>"
        } 
		if (!pageScope.content.fileSize || 
			pageScope.content.metaClass.hasProperty(proxyHandler.unwrapIfProxy(pageScope.content).class, 'allowUpDate') && pageScope.content.allowUpDate) {
            out << "<input type=\"file\" name=\"${attrs.property}\"/>"
        }
    }

    def editorFieldRichHTML = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/richeditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }
    
    void includeCKEditor() {
        if (!request['weceem.editor.ckeditor.js.included']) {
            out << ckeditor.resources()
            out << ckeditor.config(var:"toolbar_HTMLEditor") {
"""
        [
            ['Maximize'],
            ['Cut','Copy','Paste','PasteText','PasteFromWord','-','Print', 'SpellChecker', 'Scayt'],
            ['Undo','Redo','-','Find','Replace','-','SelectAll','RemoveFormat'],
            ['NewPage'],
            ['Source','ShowBlocks','-','About'],
            '/',
            ['Link','Unlink','Anchor'],
            ['Image','Flash','Table','HorizontalRule','SpecialChar','PageBreak'],
            ['Outdent','Indent','Blockquote','CreateDiv'],
            ['Subscript','Superscript'],
            ['TextColor','BGColor'],
            ['BidiLtr', 'BidiRtl'],
            '/',
            ['Styles','Format','Font','FontSize'],
            ['Bold','Italic','Underline','Strike','-','NumberedList','BulletedList','-'],
            ['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock']
        ]
"""
            }
            request['weceem.editor.ckeditor.js.included'] = true
        }
    }

    def editorResourcesRichHTML = { attrs ->
        includeCKEditor()
    }
    
    def editorFieldHTMLContent = { attrs ->
        if (pageScope.content.metaClass.hasProperty(pageScope.content, 'allowGSP') && pageScope.content.allowGSP) {
            out << editorFieldHtmlCode(attrs)
        } else {
            out << editorFieldRichHTML(attrs)
        }
    }
    
    def editorFieldWikiCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/wikieditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorFieldHtmlCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codemirror', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
        
        out << """
        <script language="javascript" type="text/javascript">
        var editor_${attrs.property} = CodeMirror.fromTextArea("editor_${attrs.property.encodeAsJavaScript()}", {
          parserfile: ["parsexml.js"],
          path: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/js/').encodeAsJavaScript()}",
          stylesheet: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/css', file:'xmlcolors.css').encodeAsJavaScript()}",
          textWrapping: false
        });
        </script>
        """
    }

    def editorFieldJSCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codemirror', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
        
        out << """
        <script language="javascript" type="text/javascript">
        var editor_${attrs.property} = CodeMirror.fromTextArea("editor_${attrs.property.encodeAsJavaScript()}", {
          parserfile: ["tokenizejavascript.js", "parsejavascript.js"],
          path: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/js/').encodeAsJavaScript()}",
          stylesheet: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/css', file:'jscolors.css').encodeAsJavaScript()}",
          textWrapping: false
        });
        </script>
        """
    }
    
    def editorFieldGroovyCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codemirror', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
        
        out << """
        <script language="javascript" type="text/javascript">
        var editor_${attrs.property} = CodeMirror.fromTextArea("editor_${attrs.property.encodeAsJavaScript()}", {
          parserfile: ["tokenizegroovy.js", "parsegroovy.js"],
          path: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/js/').encodeAsJavaScript()}",
          stylesheet: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/css', file:'groovycolors.css').encodeAsJavaScript()}",
          textWrapping: false
        });
        </script>
        """
    }
    
    def editorResourcesHtmlCode = { attrs ->
        includeCodeMirror()
    }

    def includeCodeMirror() {
        if (!request['weceem.editor.codemirror.js.included']) {
            out << g.render(template:'/editors/codemirror_resources', plugin:'weceem')
            request['weceem.editor.codemirror.js.included'] = true
        }
    }

    def editorResourcesHTMLContent = { attrs ->
        if (pageScope.content.metaClass.hasProperty(pageScope.content, 'allowGSP') && pageScope.content.allowGSP) {
            out << editorResourcesHtmlCode(attrs)
        } else {
            out << editorResourcesRichHTML(attrs)
        }
    }
    
    def editorResourcesJSCode = { attrs ->
        includeCodeMirror()
    }
    
    def editorResourcesGroovyCode = { attrs ->
        includeCodeMirror()
    }

    def editorFieldCssCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codemirror', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
        
        out << """
        <script language="javascript" type="text/javascript">
        var editor_${attrs.property} = CodeMirror.fromTextArea("editor_${attrs.property.encodeAsJavaScript()}", {
          parserfile: ["parsecss.js"],
          path: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/js/').encodeAsJavaScript()}",
          stylesheet: "${g.resource(plugin:'weceem', dir:'_weceem/codemirror/css', file:'csscolors.css').encodeAsJavaScript()}",
          textWrapping: false
        });
        </script>
        """
    }

    def editorResourcesCssCode = { attrs ->
        includeCodeMirror()
    }
    
    def editorResourcesWikiCode = { attrs ->
        out << g.render(template: '/editors/wikicode', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }
    
    def ifTagExists = { attrs, body ->
        if (gspTagLibraryLookup.lookupTagLibrary(attrs.namespace, attrs.tag)) {
            out << body()
        }
    }
    
    def editorFieldLanguageList = { attrs ->
        // @todo Cache this
        def langs = Locale.availableLocales.collect {
            [id:it.ISO3Language, text: it.displayLanguage]
        }
        langs = langs.unique { it.id }
        
        def defaultLanguage = langs.find { it.id == "eng" }
        
        langs = langs.grep { it.id != defaultLanguage.id }
        
        langs = langs.sort { a, b -> 
            a.text.compareTo(b.text)
        }
        langs.add(0, defaultLanguage)        
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true, 
            from: langs, optionKey:'id', optionValue:'text')
    }
}
