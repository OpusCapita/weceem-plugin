<html>
  <head>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title><g:message code="space.title.import"/></title>
  </head>

  <body>
    <div class="body">
      <b class="header"><g:message code="space.title.import"/></b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>

      <g:form controller="space" method="post" action="startImport"
            enctype="multipart/form-data">
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
                <td><label for="importer"><g:message code="space.label.importType"/></label></td>
                <td>
                  <select id="importer" name="importer">
                    <g:each in="${importers}" var="imp">
                      <option value="${imp.key}">${imp.value.name}</option>
                    </g:each>
                  </select>
                </td>
              </tr>
              <tr class="prop">
                <td valign="top" class="name">
                  <label for="file"><g:message code="label.file"/></label>
                </td>
                <td valign="top" class="value">
                  <input id="file" type="file" name="file"/>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <input type="submit" class="button" value="${message(code: 'space.command.import')}"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </g:form>
    </div>
  </body>
</html>
