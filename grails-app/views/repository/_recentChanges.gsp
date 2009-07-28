<g:form name="changesForm" controller="contentVersion">
  <g:hiddenField name="fromVersion"/>
  <g:hiddenField name="toVersion"/>
  <table class="details" style="width: 100%;">
    <tr>
      <td colspan="5" class="header">Changes</td>
    </tr>
    <g:if test="${changes}">
      <tr>
        <th></th>
        <th>Revision</th>
        <th>Created By</th>
        <th>Created On</th>
        <th>Operations</th>
      </tr>
    </g:if>
    <g:else>
      <td colspan="5">There are no changes for selected content.</td>
    </g:else>
    <g:each in="${changes}" var="version">
      <tr>
        <td>
          <input id="version_${version.ident()}" type="checkbox"/>
        </td>
        <td align="center">
          ${version.revision}
        </td>
        <td>
          ${version.createdBy}
        </td>
        <td>
          ${version.createdOn}
        </td>
        <td>
          <g:link controller="contentVersion" action="restore" id="${version.id}">Restore</g:link>
          <g:link controller="contentVersion" action="changes" id="${version.id}">Changes</g:link>
        </td>
      </tr>
    </g:each>
    <g:if test="${changes && changes.size() > 1}">
      <tr>
        <td colspan="5" align="center">
          <g:actionSubmit action="showVersionsChanges" class="button"
              value="Compare selected versions" onclick="return validateChangesForm();"/>
        </td>
      </tr>
    </g:if>
  </table>
</g:form>