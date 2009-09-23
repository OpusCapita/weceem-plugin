<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body >
    <div class="container span-24 last">
        <table class="standart">
            <thead>
                <tr>
                    <th width="15px"><g:message code="space.header.name"/></th>
                    <th width="15px"><g:message code="space.header.aliasURI"/></th>
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
    </div>
  </body>
</html>
