<html>
  <head>
    <meta name="layout" content="admin"/>
    <title>Edit Space</title>
  </head>

  <body>
    <div class="body">
      <b class="header">Edit Space</b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>
      <g:hasErrors bean="${space}">
        <div class="errors">
          <g:renderErrors bean="${space}" as="list"/>
        </div>
      </g:hasErrors>

      <g:form method="post">
        <input type="hidden" name="id" value="${space?.id}"/>
        <div class="dialog">
          <table>
            <tbody>
              <g:render plugin="weceem" template="/space/form" model="[space: space]"/>
              <tr class="prop">
                <td colspan="2">
                  <g:actionSubmit class="button" value="Save" action="update"/>&nbsp;
                  <g:actionSubmit class="button" value="Back" action="list"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </g:form>
    </div>
  </body>
</html>
