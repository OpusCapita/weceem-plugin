<table class="details" style="width: 100%;">
  <tr>
    <td colspan="4" class="header">Content Parent/Children</td>
  </tr>
  <g:if test="${!parents && !children}">
    <td colspan="4">Selected content hasn't parent and children.</td>
  </g:if>
  <g:each in="${parents}" var="parent">
    <tr>
      <td>
        <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'file_16.gif')}" alt=""
            title="Parent Content"/>
      </td>
      <td>
        ${parent.title}
      </td>
      <td>
        ${parent.class.name}
      </td>
      <td>
        <g:formatDate format="dd/MM/yyyy" value="${parent.createdOn}"/>
        ${parent.createdBy}
      </td>
    </tr>
  </g:each>
  <g:each in="${children}" var="child">
    <tr>
      <td>
        <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'file_multiple_16.gif')}" alt=""
            title="Child Content"/>
      </td>
      <td>
        ${child.title}
      </td>
      <td>
        ${child.class.name}
      </td>
      <td>
        <g:formatDate format="dd/MM/yyyy" value="${child.createdOn}"/>
        ${child.createdBy}
      </td>
    </tr>
  </g:each>
  <tr>
    <td colspan="4" class="header">Related Content</td>
  </tr>
  <g:if test="${!relatedContents}">
    <td colspan="4">Related content not found.</td>
  </g:if>
  <g:each in="${relatedContents}" var="relatedContent">
    <tr>
      <td>
        <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'web_16.gif')}" alt=""
            title="Link"/>
      </td>
      <td>
        ${relatedContent.title}
      </td>
      <td>
        ${relatedContent.class.name}
      </td>
      <td>
        <g:formatDate format="dd/MM/yyyy" value="${relatedContent.createdOn}"/>
        ${relatedContent.createdBy}
      </td>
    </tr>
  </g:each>
</table>
