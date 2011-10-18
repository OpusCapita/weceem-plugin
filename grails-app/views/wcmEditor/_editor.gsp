<div class="container">
<g:javascript>
<%-- I dont want to do this here but have no choice until we can include resources in header --%>
$( function() {
    // Highlight any accordion headers that have errors inside them
    $('div .ui-state-error', $('#panels')).parentsUntil('#panels').prev('.ui-accordion-header').addClass('ui-state-error');
    var originalFormOnSubmits = $('.preview-button').parents('form').attr('onsubmit');

    // Set up the preview action
    $('.preview-button').click( function(event) {
        var form = $(event.target).parents('form');
        form.attr('target', '_preview');
        var prevURL = '${g.createLink(controller:'wcmEditor', action:'preview', id:content.id).encodeAsJavaScript()}';
        form.attr('action', prevURL);
        
        // Now submit form using workaround for onsubmit calls
        $('#preview-action-submitter').click();
        
        // Reset the form target so save works as expected
        form.attr('target', '');
        form.attr('action', prevURL);
    
        // Don't have a double-submit
        event.preventDefault();
    });
});
</g:javascript>

    <bean:errorClass>ui-state-error</bean:errorClass>

    <g:uploadForm controller="wcmEditor" action="${content.id ? 'update' : 'save'}" method="post">

        <g:if test="${content.id}">
            <input type="hidden" name="id" value="${params.id.encodeAsHTML()}"/>
        </g:if>    
        <g:else>
            <input type="hidden" name="type" value="${params.type.encodeAsHTML()}"/>
        </g:else>
        
        <div class="span-24 last">
            <div>
        <g:grep in="${editableProperties}" var="prop" filter="${ { p -> p.group == null }}">
                <div class="clear span-4">
                    <wcm:editorLabel bean="${content}" property="${prop.property}"/>
                </div>
                <div class="field prepend-1 span-18 last">
                    <% println wcm."editorField${prop.editor}"(bean:content, property:prop.property) %>
                </div>
        </g:grep>
            </div>
        </div>

        <div class="clear span-24 last">
            <div class="prepend-top append-bottom editorsaveactions">
                <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="${message(code:'content.button.saveAndContinue', default:'Save and continue editing')}" action="${content.id ? 'updateContinue' : 'saveContinue'}"/>
                <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="${message(code:'content.button.save', default:'Save')}" action="${content.id ? 'update' : 'save'}"/>
                <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" class="ui-widget ui-state-default ui-corner-all preview-button" value="${message(code:'content.button.preview', default:'Preview')}" action="preview"/>
                <input style="display:none" type="submit" id="preview-action-submitter" name="_action_preview" value="preview"/> <%-- Needed to invoke onsubmit on form during our preview submit phase --%>
                <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="${message(code:'content.button.cancel', default:'Cancel')}" action="${content.id ? 'update' : 'save'}" action="cancel"/>
            </div>
        </div>
        
        <div class="span-24 last" id="panels">
            <g:set var="groupNames" value="${(editableProperties*.group).findAll({ it }).unique()}"/>
            <g:each in="${groupNames}" var="gn">
                <h2><a href="#"><g:message code="editor.group.heading.${gn}" default="${gn}" encodeAs="HTML"/></a></h2>
                <div id="editor-${gn.encodeAsHTML()}" class="editorpanel">
                    <g:grep in="${editableProperties}" var="prop" filter="${ { p -> p.group == gn} }">
                        <div class="clear prepend-1 span-4">
                            <bean:label beanName="content" property="${prop.property}" labelKey="${'content.label.'+prop.property}"/>
                        </div>
                        <div class="field prepend-1 span-17 last">
                            <% println wcm."editorField${prop.editor}"(bean:content, property:prop.property) %>
                        </div>
                    </g:grep>
                </div>
            </g:each>

            <h2><a href="#"><g:message code="editor.group.heading.changes" default="Change history"/></a></h2>
            <div id="editor-changes" class="editorpanel">
                <div class="prepend-1 span-22 last">
                    <div>
                    <g:each in="${changeHistory}" var="change">
                        <g:link target="weceem_history" controller="wcmEditor" action="showRevision" id="${change.id}"><wcm:humanDate date="${change.createdOn}"/> by ${change.createdBy.encodeAsHTML()}</g:link><br/>
                    </g:each>
                    </div>
                </div>
            </div>

            <h2><a href="#"><g:message code="editor.group.heading.children" default="Parent &amp; Children"/></a></h2>
            <div id="editor-family" class="editorpanel">
                <div class="clear prepend-1 span-2">
                    <label>Parent:</label>
                </div>
                <div class="prepend-1 span-20 last">
                    <g:if test="${content.parent}">
                        ${content.parent.title.encodeAsHTML()} (${message(code:'content.item.name.'+content.parent.class.name, encodeAs:'HTML')})
                        <input type="hidden" name="parent.id" value="${content.parent.id}"/>
                    </g:if><br/>
                </div>
                <%-- Only show children if we are editing --%>
                <g:if test="${content.id}">
                    <div class="clear prepend-1 span-2">
                        <label>Children:</label>
                    </div>
                    <div class="prepend-1 span-21 last">
                        <g:if test="${content.children?.size()}">
                            <ul>
                            <g:each in="${content.children}" var="child">
                                <li>${child.title.encodeAsHTML()} (${message(code:'content.item.name.'+child.class.name, encodeAs:'HTML')})</li>
                            </g:each>
                            </ul>
                        </g:if>
                    </div>
                </g:if>
            </div>
        </div>
  </g:uploadForm>
</div>  

