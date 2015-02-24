<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="space.title.list"/></title>
    <style>
      thead th {
        background: none repeat scroll 0 0 #c3d9ff;
      }
    </style>
  </head>

  <body>

  <nav:set path="plugin.weceem.weceem_menu/administration" scope="plugin.weceem.weceem_menu"/>
  <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1 class="headline"><g:message code="space.title.list"/></h1>
        </div>
      </div>

      <div class="row">
        <div class="col-md-12 col-xs-12">
        <table class="standard" style="width: 100%">
          <thead>
            <tr>
              <g:sortableColumn property="name" title="${message(code: 'space.header.name')}"/>
              <g:sortableColumn property="aliasURI" title="${message(code: 'space.header.aliasURI')}"/>
              <th class="page-column"><g:message code="header.operations"/></th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${spaceList}" status="i" var="space">
              <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                <td>${fieldValue(bean: space, field: 'name')}</td>
                <td>${fieldValue(bean: space, field: 'aliasURI')}</td>
                <td>
                  <g:link action="edit" class="button" id="${space.id}"><g:message code="command.edit"/></g:link>
                  <g:link action="confirmDelete" class="button" id="${space.id}"><g:message code="command.delete"/></g:link>
                  <g:link action="importSpace" class="button" id="${space.id}"><g:message code="space.command.import"/></g:link>
                  <g:link action="exportSpace" class="button" id="${space.id}"><g:message code="space.command.export"/></g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
        </div>
      </div>

      <g:form controller="wcmSpace">
        <div class="row">
          <div class="col-md-12"><br/>
            <span class="menuButton" style="padding-left:3px; margin-bottom:8px;">
              <g:actionSubmit action="create" value="${message(code: 'command.add')}" class="btn btn-primary"/>
            </span>
          </div>
        </div>
      </g:form>

    </div>
  </body>
</html>
