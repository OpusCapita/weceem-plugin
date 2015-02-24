<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="space.title.export"  args="[space.name]"/></title>
  </head>

  <body>
    <nav:set path="plugin.weceem.weceem_menu/administration" scope="plugin.weceem.weceem_menu"/>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1><g:message code="space.title.export" args="[space.name]" encodeAs="HTML"/></h1>
        </div>
      </div>

      <g:if test="${flash.message}">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <div class="message">${flash.message}</div>
          </div>
        </div>
      </g:if>

      <g:form controller="wcmSpace" method="post" params="[id:space.id]"
            action="startExport">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <table>
              <tbody>
                <tr>
                  <td colspan="2">Please select a space to export
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
              </tbody>
            </table>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <input type="submit" class="btn btn-primary" value="${message(code: 'space.command.export')}"/>
          </div>
        </div>
      </g:form>

    </div>
  </body>
</html>
