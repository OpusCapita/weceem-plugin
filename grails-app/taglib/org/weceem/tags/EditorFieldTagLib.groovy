package org.weceem.tags

import org.weceem.content.Content
import org.weceem.content.Space
import org.weceem.content.Status
import org.weceem.content.Template
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup

class EditorFieldTagLib {
    
    static namespace = "wcm"
    
    TagLibraryLookup gspTagLibraryLookup // Tag lookup bean from Grails injection
    
    def editorFieldString = { attrs ->
        out << bean.input(beanName:'content', property:attrs.property, noLabel:true)
    }

    def editorFieldTitle = { attrs ->
        out << bean.input(beanName:'content', property:attrs.property, noLabel:true, 'class':"big")
    }

    def editorFieldReadOnly = { attrs ->
        def v = pageScope.content[attrs.property]
        if (v) v = v.encodeAsHTML()
        out << "<span class=\"field-readonly\">${ v ?: '' }</span>"
    }
    
    def editorFieldReadOnlyDate = { attrs ->
        out << "<span class=\"field-readonly\">${g.formatDate(date:pageScope.content[attrs.property], format:'d MMM yyyy HH:mm:ss')}</span>"
    }
    
    def editorFieldDate = { attrs ->
        out << bean.date(beanName:'content', property:attrs.property, noLabel:true)
    }

    def editorFieldTemplate = { attrs ->
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            noSelection: ['':'- No template -'],
            from:{ Template.findAllBySpace( pageScope.content.space, [sort:'title']) }, optionValue:'title', optionKey:'id')
    }

    def editorFieldStatus = { attrs ->
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            from:{ Status.listOrderByCode() }, optionValue: { v -> g.message(code:'content.status.'+v.description) }, optionKey:'id')
    }

    def editorFieldSpace = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/space', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorFieldContent = { attrs ->
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true,
            noSelection: ['':'- No content -'],
            from:{ Content.findAllBySpace( pageScope.content.space, [sort:'title']).findAll( { c -> !c.is(pageScope.content) }) }, 
                optionValue:'title', optionKey:'id')
    }

    def editorFieldInteger = { attrs ->
        out << bean.input(beanName:'content', property:attrs.property, noLabel:true)
    }

    def editorFieldContentFileUpload = { attrs ->
        if (pageScope.content.fileSize) {
            out << "<span class=\"field-readonly\">File already uploaded (${pageScope.content.fileSize} bytes)</span>"
        } else {
            out << "<input type=\"file\" name=\"${attrs.property}\"/>"
        }
    }

    def editorFieldRichHTML = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/richeditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }
    
    def editorFieldWikiCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/wikieditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    def editorFieldHtmlCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codeeditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }
    
    def editorResourcesHtmlCode = { attrs ->
        includeEditArea()
        out << """
        <script language="javascript" type="text/javascript">
          editAreaLoader.init({
              id : "editor_${attrs.property}",
              syntax: "html",
              allow_toggle: false,
              start_highlight: true
          });
        </script> 
        """
    }
    
    def editorFieldCssCode = { attrs ->
        // Workaround for Grails 1.1.x bug invoking tags with body as method - have to use a template instead
        out << g.render(template:'/editors/codeeditor', plugin:'weceem', 
            model:[name:attrs.property, value:pageScope.content[attrs.property]])
    }

    void includeEditArea() {
        if (!request['weceem.editor.editarea.js.included']) {
            out << """
            <script language="javascript" type="text/javascript" src="${resource(dir: pluginContextPath+'/js/editarea', plugin:'weceem', file: 'edit_area_full.js')}"></script>
            """
            request['weceem.editor.editarea.js.included'] = true
        }
    }

    def editorResourcesCssCode = { attrs ->
        includeEditArea()
        out << """
        <script language="javascript" type="text/javascript">
          editAreaLoader.init({
              id : "editor_${attrs.property}",
              syntax: "css",
              allow_toggle: false,
              start_highlight: true
          });
        </script> 
        """
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
        out << bean.select(beanName:'content', property:attrs.property, noLabel:true, from: langs, optionKey:'id', optionValue:'text')
    }
}
