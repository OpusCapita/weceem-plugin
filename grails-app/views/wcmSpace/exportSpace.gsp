<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="space.title.export"/></title>
  </head>

  <body>
    <div class="span-24 last">
      <h1><g:message code="space.title.export" args="[space.name]" encodeAs="HTML"/></h1>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>

      <p>Please select a space to export</p>
      <g:form controller="wcmSpace" method="post" params="[id:space.id]"
            action="startExport">
        <div class="dialog">
          <table>
            <tbody>
              <tr>
                <td><label for="exporter"><g:message code="space.label.exportType"/></label></td>
                <td>
                  <select id="exporter" name="exporter">
                    <g:each in="${exporters}" var="exp">
                      <option value="${exp.key}">${exp.value.name}</option>
                    </g:each>
                  </select>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <input type="submit"class="button ui-state-default ui-corner-all" value="${message(code: 'space.command.export')}"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </g:form>
    </div>
  </body>
</html>
