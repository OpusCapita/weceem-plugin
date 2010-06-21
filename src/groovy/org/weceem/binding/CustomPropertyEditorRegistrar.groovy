package org.weceem.binding

import java.util.Date 
import java.text.SimpleDateFormat 
import org.springframework.beans.PropertyEditorRegistrar 
import org.springframework.beans.PropertyEditorRegistry

import org.weceem.content.*

class CustomPropertyEditorRegistrar implements PropertyEditorRegistrar {
    public void registerCustomEditors(PropertyEditorRegistry registry) { 
        registry.registerCustomEditor(null, 'publishFrom', new DateAndTimeDateEditor(new SimpleDateFormat("yyyy/MM/dd HH:mm"), true)); 
        registry.registerCustomEditor(null, 'publishUntil', new DateAndTimeDateEditor(new SimpleDateFormat("yyyy/MM/dd HH:mm"), true)); 
    } 
}