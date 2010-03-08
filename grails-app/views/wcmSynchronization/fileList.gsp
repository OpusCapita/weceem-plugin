<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body >
  
      <div class="span-24 last">
        <h1>File Synchronization of space ${space.name.encodeAsHTML()} complete.</h1>
      </div>

    <g:form controller="wcmSynchronization">
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
                            <th width="15px"><g:message code="synchronization.header.contentTitle"/></th>
                            <th width="15px"><g:message code="synchronization.header.relativePath"/></th>
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
        <g:if test="${result.size()}">
        
            <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all">
                The following nodes were found referencing files that no longer exist in the filesystem. To remove nodes click the checkboxes and then press Delete.
            </div>
            <div class="container span-24 last">
                <table class="standart">
                    <thead>
                        <tr>
                            <th width="15px"><g:message code="synchronization.header.contentTitle"/></th>
                            <th width="15px"><g:message code="synchronization.header.relativePath"/></th>
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
                    <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="Delete selected" controller="wcmSynchronization" action="delete"/>
                    <g:actionSubmit class="ui-widget ui-state-default ui-corner-all" value="Don't delete anything" controller="wcmSynchronization" action="done"/>
                </div>
            </div>
        </g:if>
        <g:else>
            Good news! Your repository has no content nodes referencing non-existent server files.<br/>
            <g:link controller="wcmRepository" class="button">OK</g:link>
        </g:else>
    </g:form>
  
  </body>
</html>
