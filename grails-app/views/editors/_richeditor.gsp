<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <ckeditor:editor userSpace="${content.space.makeUploadName()}" 
        name="${name}" 
        width="700" height="420"
        showThumbs="true" 
        toolbar="HTMLEditor">${value}</ckeditor:editor>
</bean:customField>
