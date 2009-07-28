<html>
  <head>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title>Create Space</title>
  </head>

  <body>

    <div class="body">
      <b class="header">Create Space</b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>
      <g:hasErrors bean="${space}">
        <div class="errors">
          <g:renderErrors bean="${space}" as="list"/>
        </div>
      </g:hasErrors>

      <g:form>
        <div class="dialog">
          <table>
            <tbody>
              <g:render plugin="weceem" template="/space/form" model="[space: space]"/>
              <tr class="prop">
                <td colspan="2">
                  <g:actionSubmit class="button" value="Save" action="save"/>&nbsp;
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
