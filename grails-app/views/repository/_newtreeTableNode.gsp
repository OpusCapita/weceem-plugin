<tr id="inserter-before-${c.id}" class="inserter-before ${ c.parent ? 'child-of-content-node-'+c.parent.id : ''} delimeter-${c.id}">
  <td colspan="5" ><div class="title"></div></td>
</tr>

<tr id="content-node-${c.id}" class="${ c.parent ? 'child-of-content-node-'+c.parent.id : ''}">
	<td>
	  <div class="item">
			<% 
			switch (c.class.name) {
				case 'ContentFile':icontype="image"; break; 
				case 'ContentDirectory':icontype="folder-collapsed"; break; 
				case 'StyleSheet':icontype="script"; break; 
				case 'Template':icontype="script"; break; 
				case 'Widget':icontype="script"; break; 
				default: icontype="document";
			}
			%>
			<div class="ui-icon ui-icon-${icontype}"></div>
			<g:link controller="editor" action="edit" id="${c.id}">
			 <h2 orderindex="${c.orderIndex == null ? 0 : c.orderIndex}" type="${c.toName()}" class="title">${c.title.encodeAsHTML()}        <span class="type">( /${c.aliasURI.encodeAsHTML()} - <g:message code="content.item.name.${c.toName()}"/>)</span>
			 </h2>
			</g:link>
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
	<td>
		<div id="infoDialog${c.id}" class="nodeinfoDialog" title="${c.title.encodeAsHTML()}">
			URI: <g:link controller="weceem" action="show" id="${c.id}"><span class="uri">${c.aliasURI.encodeAsHTML()}</span></g:link> 
			<br/>Created <wcm:humanDate date="${c.createdOn}"/> by <g:link action="viewChangesByAuthor" class="author">${c.createdBy.encodeAsHTML()}</g:link>
			<g:if test="${c.changedBy}">, changed <wcm:humanDate date="${c.changedOn}"/> by <g:link action="viewChangesByAuthor" class="author">${c.changedBy.encodeAsHTML()}</g:link></g:if>
		</div>
	</td>
</tr>

<g:if test="${c.children.size()}">
	<g:each in="${c.children}" var="child">
		<g:render  plugin="weceem" template="newtreeTableNode" model="[c:child]"/>
	</g:each>
</g:if>
<!--<tr id="" class="child-of-content-node-${c.id}">
  <td colspan="5"></td>
</tr>-->

<tr id="inserter-after-${c.id}" class="inserter-after ${ c.parent ? 'child-of-content-node-'+c.parent.id : ''} delimeter-${c.id}">
  <td colspan="5" ><div class="title"></div></td>
</tr>
