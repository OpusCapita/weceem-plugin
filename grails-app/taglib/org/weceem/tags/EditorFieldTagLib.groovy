package org.weceem.tags

import org.weceem.content.WcmContent
import org.weceem.files.WcmContentFileDB
import org.gualdi.grails.plugins.ckeditor.Ckeditor
import grails.util.Holders
import org.gualdi.grails.plugins.ckeditor.utils.PathUtils
import org.weceem.content.WcmStatus
import org.weceem.content.WcmTemplate
import org.weceem.script.WcmScript
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.apache.commons.lang.WordUtils

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
        def dval = d?.format('yyyy/MM/dd')
        sb << g.textField(name:attrs.property+'_date', size:10, maxLength:10, value:dval)
        sb << " @ "
        def hr = d?.format('HH')
        def min = d?.format('mm')
        sb << g.textField(name:attrs.property+'_hour', size:2, maxLength:2, value:hr, 'class':'date-editor-hour')
        sb << " : "
        sb << g.textField(name:attrs.property+'_minute', size:2, maxLength:2, value:min, 'class':'date-editor-minute')

        out << bean.customField(beanName:'content', property:attrs.property, noLabel:true) {
            out << sb
        }

        out << g.javascript([:]) {
"""
\$(function(){ 
    \$('#${attrs.property.encodeAsJavaScript()}_date').datepicker({ 
        dateFormat: 'yy/mm/dd',
        onSelect: function(dateText, inst) {
            \$(this).siblings('.date-editor-hour').val('00');
            \$(this).siblings('.date-editor-minute').val('00');
        }
    })
})
"""
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

    def wcmeditor = { attrs, body ->
        def editor = new Ckeditor(request, attrs, body())
        out << renderWeceemEditor(editor)
    }

    def renderWeceemEditor(editor) {
        StringBuffer outb = new StringBuffer()

        if (!editor.config.append) {
            outb << """<textarea id="${editor.config.instanceId}" name="${editor.config.instanceName}">${editor.initialValue?.encodeAsHTML()}</textarea>\n"""
        }
        outb << """<script type="text/javascript">\n"""

        if (editor.config.removeInstance) {
            outb << """if (CKEDITOR.instances['${editor.config.instanceId}']){CKEDITOR.remove(CKEDITOR.instances['${editor.config.instanceId}']);}\n"""
        }

        outb << """CKEDITOR."""
        if (editor.config.append) {
            outb << """appendTo"""
        }
        else {
            outb << """replace"""
        }
        outb << """('${editor.config.instanceId}'"""
        outb << getEditorConfiguration(editor)
        //  outb << editor.config.configuration
        outb << """);\n"""
        outb << """</script>\n"""

        return outb.toString()
    }

    def getEditorConfiguration(editor) {
        def ckconfig = Holders.config.ckeditor

        def customConfig = ckconfig?.config
        if (customConfig && !editor.config.config["customConfig"]) {
            customConfig = PathUtils.checkSlashes(customConfig, "L- R-", true)
            editor.config.config["customConfig"] = "'${editor.config.contextPath}/${customConfig}'"
        }

        // Collect browser settings per media type
        editor.config.resourceTypes.each { t ->
            def type = WordUtils.capitalize(t)
            def typeForConnector = "${type == 'Link' ? 'File' : type}"

            if (ckconfig?.upload?."${t}"?.browser) {
                editor.config.config["filebrowser${type}BrowseUrl"] = "'${getWeceemBrowseUrl(editor.config, typeForConnector, editor.config.userSpace, editor.config.fileBrowser,  editor.config.showThumbs, editor.config.viewMode)}'"
            }
            if (ckconfig?.upload?."${t}"?.upload) {
                editor.config.config["filebrowser${type}UploadUrl"] = "'${getWeceemUploadUrl(editor.config, typeForConnector, editor.config.userSpace)}'"
            }
        }

        // Config options
        def configs = []
        editor.config.config.each {k, v ->
            if (!editor.config.localConfig[k]) {
                configs << "${k}: ${v}"
            }
        }
        editor.config.localConfig.each {k, v ->
            configs << "${k}: ${v}"
        }

        if (ckconfig?.fullPage) {
            configs << "fullPage: ${ckconfig?.fullPage}"
        }

        StringBuffer configuration = new StringBuffer()
        if (configs.size()) {
            configuration << """, {\n"""
            configuration << configs.join(",\n")
            configuration << """}\n"""
        }

        return configuration
    }

    def getWeceemUploadUrl(config, type, userSpace) {
        return "${config.contextPath}/${config.getConnectorsPrefix()}/wcmofm/uploader?type=${type}${userSpace ? '&space='+ userSpace : ''}"
    }

    def getWeceemBrowseUrl(config, type, userSpace, fileBrowser, showThumbs, viewMode) {
        def browserUrl
        def prefix = config.getConnectorsPrefix()
        browserUrl = "${config.contextPath}/${prefix}/wcmofm?fileConnector=${config.contextPath}/${prefix}/wcmofm/filemanager&type=${type}${userSpace ? '&space='+ userSpace : ''}${showThumbs ? '&showThumbs='+ showThumbs : ''}${'&viewMode='+ viewMode}"

        return browserUrl
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
