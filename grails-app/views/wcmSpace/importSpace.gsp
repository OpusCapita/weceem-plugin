<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="space.title.import"  args="[space.name]"/></title>
  </head>

  <body>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1><g:message code="space.title.import" args="[space.name]" encodeAs="HTML"/></h1>
        </div>
      </div>

      <g:if test="${flash.message}">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <div class="message">${flash.message}</div>
          </div>
        </div>
      </g:if>

      <g:form controller="wcmSpace" method="post" action="startImport"
            params="[id:space.id]"
            enctype="multipart/form-data">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <table>
              <tbody>
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
              </tbody>
            </table>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <input type="submit"  class="button" value="${message(code: 'space.command.import')}"/>
          </div>
        </div>
      </g:form>

    </div>
  </body>
</html>
