<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <span class="field-readonly">${value?.name.encodeAsHTML()}</span>
    <input type="hidden" name="${name.encodeAsHTML() + '.id'}" value="${value?.id.encodeAsHTML()}"/>
</bean:customField>
