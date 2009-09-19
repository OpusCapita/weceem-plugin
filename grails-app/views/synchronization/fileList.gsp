<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body >
  
    <g:form controller="synchronization">
        <g:if test="${createdContent.size() != 0}">
            <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all">
                ${dirnum} directories and ${filenum} files were added to the space ${space.name}
            </div>
            <div class="container span-24 last" style="text-align: center">
                <input type="button" class="ui-widget ui-state-default ui-corner-all"  onclick="$('#moreInfoDiv').toggle()" value="Show added content"></input>
            </div>
            <div id="moreInfoDiv" class="container span-24 last" style="display: none">
                <table class="standart">
                    <thead>
                        <tr>
                            <g:sortableColumn property="title" title="${message(code: 'synchronization.header.contentTitle')}"/>
                            <g:sortableColumn property="aliasURI" title="${message(code: 'synchronization.header.relativePath')}"/>
                        </tr>
                    </thead>
                    <tbody>
                         <g:each var="cont" in="${createdContent}">
                            <tr>
                                <td>${cont.title}</td>
                                <td>${cont.toRelativePath()}</td>
                            </tr>
                         </g:each>
                    </tbody>
                </table>
            </div>
        </g:if>
        <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all">
            The following nodes were found referencing files that no longer exist in the filesystem. To remove nodes click the checkboxes and then press Delete.
        </div>
        <div class="container span-24 last">
            <table class="standart">
                <thead>
                    <tr>
                        <g:sortableColumn property="title" title="${message(code: 'synchronization.header.contentTitle')}"/>
                        <g:sortableColumn property="aliasURI" title="${message(code: 'synchronization.header.relativePath')}"/>
                        <th width="15px">&nbsp;</th>
                    </tr>
                </thead>
                <tbody>
                     <g:each var="cont" in="${result}">
                        <tr>
                            <td>${cont.title}</td>
                            <td>${cont.toRelativePath()}</td>
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
