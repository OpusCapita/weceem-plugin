<html>
  <head>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title><g:message code="space.title.list"/></title>
  </head>

  <body>
    <div class="body">
      <b class="header"><g:message code="space.title.list"/></b>

      <g:form controller="space">
        <div class="nav">
          <br/>
          <span class="menuButton" style="padding-left:3px; margin-bottom:8px;">
            <g:actionSubmit action="create" value="${message(code: 'command.add')}" class="button"/>
          </span>
          <br/>
        </div>
      </g:form>

      <div class="list">
        <table class="standard">
          <thead>
            <tr>
              <g:sortableColumn property="name" title="${message(code: 'space.header.name')}"/>
              <g:sortableColumn property="aliasURI" title="${message(code: 'space.header.aliasURI')}"/>
              <th width="15px"><g:message code="header.operations"/></th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${spaceList}" status="i" var="space">
              <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                <td>${fieldValue(bean: space, field: 'name')}</td>
                <td>${fieldValue(bean: space, field: 'aliasURI')}</td>
                <td>
                  <g:link action="edit" id="${space.id}"><g:message code="command.edit"/></g:link>
                  <g:link action="delete" id ="${space.id}"><g:message code="command.delete"/></g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
      <div class="paginateButtons">
        <g:paginate total="${org.weceem.content.Space.count()}"/>
      </div>
      <br/>
      <div>
        <g:link action="importSpace">
            <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'fileimport_24.gif')}"
                alt="" style="vertical-align: middle;"/>
            <span><g:message code="space.link.import"/></span>
        </g:link>
        <g:link action="exportSpace">
            <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'fileexport_24.gif')}"
                alt="" style="vertical-align: middle;"/>
            <span><g:message code="space.link.export"/></span>
        </g:link>
      </div>
    </div>
  </body>
</html>
