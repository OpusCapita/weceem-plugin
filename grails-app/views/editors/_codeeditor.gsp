<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <textarea id="editor_${name.encodeAsHTML()}" name="${name.encodeAsHTML()}" style="height: 400px; width: 100%;">${value?.encodeAsHTML()}</textarea>
</bean:customField>

