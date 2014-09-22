<tr id="content-node-${c.id}" class="datarow ${ c.parent ? 'child-of-content-node-'+c.parent.id : ''} ${ c.parent == null ? '' : 'ui-helper-hidden'}">
	<td>
	  <div class="item">
          <div class="row">
            <div class="col-md-12">
               <wcm:renderContentItemIcon node="${c}"  id="content-icon-${c.id}"/>
               <div style="position: relative; padding-left: 30px;">
                <h2 class="title">
                    <g:link controller="wcmEditor" action="edit" id="${c.id}">${c.title.encodeAsHTML()} <span class="type"> (/${c.aliasURI.encodeAsHTML()} - <g:message code="content.item.name.${wcm.getClassName(node:c)}"/>)</span>
                    </g:link>
                </h2>
              </div>
            </div>
          </div>
      </div>
	</td>
	<td>
		<g:message code="${'content.status.'+c.status.description}" encodeAs="HTML"/>
	</td>
	<td>
	    ${c.createdBy?.encodeAsHTML()}
	</td>
	<td>
	  <g:if test="${c.changedOn}">
	    <wcm:humanDate date="${c.changedOn}"/>
		</g:if> 
		<g:else>
		  <g:message code="message.null.changedOn" encodeAs="HTML"/>
		</g:else>
	</td>
</tr>

<jq:jquery>
    jQuery('#content-node-${c.id}').data('orderindex', ${c.orderIndex == null ? 0 : c.orderIndex});
    jQuery('#content-node-${c.id}').data('type',"${wcm.getClassName(node:c).encodeAsJavaScript()}"); 
</jq:jquery>

<g:if test="${c.children.size()}">
	<g:each in="${c.children}" var="child">
		<g:render plugin="weceem" template="newtreeTableNode" model="[c:child]"/>
	</g:each>
</g:if>
