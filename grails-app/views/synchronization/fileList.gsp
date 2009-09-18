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
                        <g:sortableColumn property="title" title="${message(code: 'synchronization.header.contentTitle')}"/>
                        <g:sortableColumn property="aliasURI" title="${message(code: 'synchronization.header.relativePath')}"/>
                        <g:sortableColumn property="space.name" title="${message(code: 'synchronization.header.space')}"/>
                        <th width="15px">&nbsp;</th>
                    </tr>
                </thead>
                <tbody>
                     <g:each var="cont" in="${result}">
                        <tr>
                            <td>${cont.title}</td>
                            <td>${cont.toRelativePath()}</td>
                            <td>${cont.space.name}</td>
                            <td><g:checkBox name="delete-${cont.id}" value="${false}" /></td>
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
