<tr id="content-node-${c.id}" class="datarow ${ c.parent ? 'child-of-content-node-'+c.parent.id : ''}">
	<td>
	  <div class="item">
            <wcm:renderContentItemIcon type="${c}" id="content-icon-${c.id  }"/>
			<h2 orderindex="${c.orderIndex == null ? 0 : c.orderIndex}" type="${c.class.name}" class="title">
			    <g:link controller="wcmEditor" action="edit" id="${c.id}">
			     ${c.title.encodeAsHTML()}        <span class="type">( /${c.aliasURI.encodeAsURL().encodeAsHTML()} - <g:message code="content.item.name.${c.class.name}"/>)</span>
			    </g:link>
			</h2>
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

<g:if test="${c.children.size()}">
	<g:each in="${c.children}" var="child">
		<g:render plugin="weceem" template="newtreeTableNode" model="[c:child]"/>
	</g:each>
</g:if>
