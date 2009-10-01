<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="space.title.export"/></title>
  </head>

  <body>
    <div class="body">
      <b class="header"><g:message code="space.title.export"/></b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>

      <g:form controller="space" method="post" action="startExport">
        <div class="dialog">
          <table>
            <tbody>
              <tr class="prop">
                <td valign="top" class="name">
                  <label for="space"><g:message code="space.label.space"/></label>
                </td>
                <td valign="top" class="value">
                  <g:select from="${org.weceem.content.Space.list()}" name="space" optionKey="id" optionValue="name"/>
                </td>
              </tr>
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
                  <input type="submit" class="button" value="${message(code: 'space.command.export')}"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </g:form>
    </div>
  </body>
</html>
