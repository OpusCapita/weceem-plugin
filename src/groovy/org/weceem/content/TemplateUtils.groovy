package org.weceem.content

class TemplateUtils {
    static getTemplateForContent(WcmContent content) {
        def template = content.metaClass.hasProperty(content, 'template') ? content.template : null
        if ((template == null)
            && (content.metaClass.hasProperty(content, 'template'))
            && (content.parent != null)) {
            return getTemplateForContent(content.parent)
        } else {
            return template
        }
    }
}