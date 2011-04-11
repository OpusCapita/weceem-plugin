\<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="space.title.list"/></title>
  </head>

  <body>
    <div class="body">
      <h1><g:message code="space.title.list"/></h1>

      <div class="list">
        <table class="standard">
          <thead>
            <tr>
              <g:sortableColumn property="name" title="${message(code: 'space.header.name')}"/>
              <g:sortableColumn property="aliasURI" title="${message(code: 'space.header.aliasURI')}"/>
              <th><g:message code="header.operations"/></th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${spaceList}" status="i" var="space">
              <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                <td>${fieldValue(bean: space, field: 'name')}</td>
                <td>${fieldValue(bean: space, field: 'aliasURI')}</td>
                <td>
                  <g:link action="edit" class="button ui-corner-all" id="${space.id}"><g:message code="command.edit"/></g:link>
                  <g:link action="delete" class="button ui-corner-all" id="${space.id}"><g:message code="command.delete"/></g:link>
                  <g:link action="importSpace" class="button ui-corner-all" id="${space.id}"><g:message code="space.command.import"/></g:link>
                  <g:link action="exportSpace" class="button ui-corner-all" id="${space.id}"><g:message code="space.command.export"/></g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>

      <g:form controller="wcmSpace">
        <div class="nav">
          <br/>
          <span class="menuButton" style="padding-left:3px; margin-bottom:8px;">
            <g:actionSubmit action="create" value="${message(code: 'command.add')}" class="button ui-state-default ui-corner-all"/>
          </span>
          <br/>
        </div>
      </g:form>

    </div>
  </body>
</html>
