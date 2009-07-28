<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <fckeditor:editor userSpace="${content.space.name}" name="${name}" width="700" height="420" toolbar="Standard" fileBrowser="default">${value}</fckeditor:editor>
</bean:customField>
