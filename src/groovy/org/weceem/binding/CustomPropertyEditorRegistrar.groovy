package org.weceem.binding

import java.util.Date 
import java.text.SimpleDateFormat 
import org.springframework.beans.PropertyEditorRegistrar 
import org.springframework.beans.PropertyEditorRegistry

class CustomPropertyEditorRegistrar implements PropertyEditorRegistrar {

    public void registerCustomEditors(PropertyEditorRegistry registry) { 
        // Register the special date/time editor for publishFrom/publishUntil only on our classes otherwise
        // user's own application will get our editor
        registry.registerCustomEditor(Date, 'publishFrom', new DateAndTimeDateEditor(new SimpleDateFormat("yyyy/MM/dd HH:mm"), true)); 
        registry.registerCustomEditor(Date, 'publishUntil', new DateAndTimeDateEditor(new SimpleDateFormat("yyyy/MM/dd HH:mm"), true)); 
    } 
}