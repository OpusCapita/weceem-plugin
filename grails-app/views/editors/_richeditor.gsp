<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <wcm:wcmeditor userSpace="${content.space.makeUploadName()}"
                   name="${name}"
                   width="100%" height="500"
                   showThumbs="true"
                   toolbar="HTMLEditor">${value?.encodeAsHTML()}
    </wcm:wcmeditor>
</bean:customField>
