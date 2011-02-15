<bean:customField id="editor_${name.encodeAsHTML()}" beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <div class="codemirror-editor">
        <textarea id="editor_${name.encodeAsHTML()}" name="${name.encodeAsHTML()}" style="height: 400px; width: 100%;">${value?.encodeAsHTML()}</textarea>
    </div>
</bean:customField>
