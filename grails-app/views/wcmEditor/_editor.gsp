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


<g:uploadForm controller="wcmEditor" action="${content.id ? 'update' : 'save'}" method="post">

    <g:if test="${content.id}">
        <input type="hidden" name="id" value="${params.id.encodeAsHTML()}" xmlns="http://www.w3.org/1999/html"/>
    </g:if>
    <g:else>
        <input type="hidden" name="type" value="${params.type.encodeAsHTML()}"/>
    </g:else>

    <g:grep in="${editableProperties}" var="prop" filter="${ { p -> p.group == null }}">
        <div class="row">
            <div class="col-md-12 col-xs-12">
                <%= (wcm."editorField${prop.editor}"(bean:content, property:prop.property)).encodeAsRaw() %>
            </div>
        </div>
    </g:grep>

    <div class="row">
        <div class="col-md-12 col-xs-12">
            <g:actionSubmit class="button" value="${message(code:'content.button.saveAndContinue', default:'Save and continue editing')}" action="${content.id ? 'updateContinue' : 'saveContinue'}"/>
            <g:actionSubmit class="button" value="${message(code:'content.button.save', default:'Save')}" action="${content.id ? 'update' : 'save'}"/>
            <g:if test="${weceemEditorMode != 'create'}">
                <g:actionSubmit class="button" value="${message(code:'content.button.preview', default:'Preview')}" action="preview"/>
                <input style="display:none" type="submit" id="preview-action-submitter" name="_action_preview" value="preview"/> <%-- Needed to invoke onsubmit on form during our preview submit phase --%>
            </g:if>
            <g:link action="cancel" controller="wcmEditor" style="color: black; text-decoration: none;"><input type="button" value="${message(code:'content.button.cancel', default:'Cancel')}" class="button"/></g:link>
        </div>
    </div>

    <div class="row" style="margin-top:20px;">
        <div class="col-md-12 col-xs-12">
            <div id="panels">
                <g:set var="groupNames" value="${(editableProperties*.group).findAll({ it }).unique()}"/>

                <g:each in="${groupNames}" var="gn">
                    <div class="row">
                        <div class="col-md-12 col-xs-12">
                            <h2 class="sectionLabel"><a href="#"><g:message code="editor.group.heading.${gn}" default="${gn}" encodeAs="HTML"/></a></h2>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-12 col-xs-12">
                            <div id="editor-${gn.encodeAsHTML()}" class="editorpanel">
                                <g:grep in="${editableProperties}" var="prop" filter="${ { p -> p.group == gn} }">
                                    <div class="row">
                                        <div class="col-md-12 col-xs-12">
                                            <%= (wcm."editorField${prop.editor}"(bean:content, property:prop.property)).encodeAsRaw() %>
                                        </div>
                                    </div>
                                </g:grep>
                            </div>
                        </div>
                    </div>
                </g:each>

                <div class="row">
                    <div class="col-md-12 col-xs-12">
                        <h2 class="sectionLabel"><a href="#"><g:message code="editor.group.heading.changes" default="Change history"/></a></h2>
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-12 col-xs-12">
                        <div id="editor-changes" class="editorpanel">
                            <div class="prepend-1 span-22 last">
                                <div>
                                    <g:each in="${changeHistory}" var="change">
                                        <g:link target="weceem_history" controller="wcmEditor" action="showRevision" id="${change.id}"><wcm:humanDate date="${change.createdOn}"/> by ${change.createdBy.encodeAsHTML()}</g:link><br/>
                                    </g:each>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-12 col-xs-12">
                        <h2 class="sectionLabel"><a href="#"><g:message code="editor.group.heading.children" default="Parent &amp; Children"/></a></h2>
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-12 col-xs-12">
                        <div id="editor-changes" class="editorpanel">
                            <div class="row">
                                <div class="col-md-2 col-xs-2">
                                    <label>Parent:</label>
                                </div>
                                <div class="col-md-10 col-xs-10">
                                    <g:if test="${content.parent}">
                                        ${content.parent.title.encodeAsHTML()} (${message(code:'content.item.name.'+wcm.getClassName(node:content.parent), encodeAs:'HTML')})
                                        <input type="hidden" name="parent.id" value="${content.parent.id}"/>
                                    </g:if><br/>
                                </div>
                            </div>

                        <%-- Only show children if we are editing --%>
                            <g:if test="${content.id}">
                                <div class="row">
                                    <div class="col-md-2 col-xs-2">
                                        <label>Children:</label>
                                    </div>
                                    <div class="col-md-10 col-xs-10">
                                        <g:if test="${content.children?.size()}">
                                            <ul>
                                                <g:each in="${content.children}" var="child">
                                                    <li>${child.title.encodeAsHTML()} (${message(code:'content.item.name.'+wcm.getClassName(node:child), encodeAs:'HTML')})</li>
                                                </g:each>
                                            </ul>
                                        </g:if>
                                    </div>
                                </div>
                            </g:if>

                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</g:uploadForm>