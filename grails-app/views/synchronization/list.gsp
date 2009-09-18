<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body >
  
    <g:form controller="synchronization">
        <div class="container span-24 last">
            <table class="standart">
                <thead>
                    <tr>
                        <g:sortableColumn property="name" title="${message(code: 'space.header.name')}"/>
                        <g:sortableColumn property="aliasURI" title="${message(code: 'space.header.aliasURI')}"/>
                        <th width="15px"><g:message code="header.operation"/></th>
                    </tr>
                </thead>
                <tbody>
                     <g:each in="${spaces}" status="i" var="space">
                      <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        <td>${fieldValue(bean: space, field: 'name')}</td>
                        <td>${fieldValue(bean: space, field: 'aliasURI')}</td>
                        <td>
                          <g:link controller="synchronization" action="synchronizationList" id="${space.id}"><g:message code="command.sync"/></g:link>
                        </td>
                      </tr>
                    </g:each>
                </tbody>
            </table>
            <div class="span-24 last">
                <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="Delete" controller="synchronization" action="delete"/>
            </div>
        </div>
    </g:form>
  
  </body>
</html>
