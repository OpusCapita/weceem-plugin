package org.weceem.servlet

import javax.servlet.http.HttpSessionAttributeListener
import javax.servlet.http.HttpSessionBindingEvent

import org.weceem.controllers.WcmContentController

class SessionChangeListener implements HttpSessionAttributeListener {
    void attributeAdded(HttpSessionBindingEvent se) {
        changed(se)
    }
    void attributeRemoved(HttpSessionBindingEvent se) {
        changed(se)
    }
    void attributeReplaced(HttpSessionBindingEvent se)  {
        changed(se)
    }
    
    void changed(HttpSessionBindingEvent se) {
        if (se.name != WcmContentController.SESSION_TIMESTAMP_KEY) {
            def t = System.currentTimeMillis()
            try {
                se.session.setAttribute(WcmContentController.SESSION_TIMESTAMP_KEY, t)
            } catch (IllegalStateException e) {
                // swallow this 'cos you can't find out if session has been invalidated beforehand
            }
        }
    }
}