<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body >
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1>File Synchronization of space ${space.name.encodeAsHTML()} complete.</h1>
        </div>
      </div>

      <g:form controller="wcmSynchronization">
        <g:if test="${createdContent.size() != 0}">
          <div class="row">
            <div class="col-md-12 col-xs-12">
              <div class="message ui-state-highlight ui-corner-all">
                ${dirnum} directories and ${filenum} files were added to the space ${space.name}
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-md-12 col-xs-12">
                <input type="button" class="button"  onclick="$('#moreInfoDiv').toggle()" value="Show added content"/>
            </div>
          </div>
          <div class="row">
            <div id="moreInfoDiv" class="col-md-12 col-xs-12" style="display: none">
                <table class="standard">
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
          </div>
        </g:if>

        <g:if test="${missingFiles.size()}">
          <div class="row">
            <div class="col-md-12 col-xs-12">
              <div class="message ui-state-highlight ui-corner-all">
                The following nodes were found referencing files that no longer exist in the filesystem. To remove nodes click the checkboxes and then press Delete.
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-md-12 col-xs-12">
                <table class="standard">
                    <thead>
                        <tr>
                            <th width="15px"><g:message code="synchronization.header.contentTitle"/></th>
                            <th width="15px"><g:message code="synchronization.header.relativePath"/></th>
                            <th width="15px">&nbsp;</th>
                        </tr>
                    </thead>
                    <tbody>
                         <g:each var="cont" in="${missingFiles}">
                            <tr>
                                <td>${cont.title}</td>
                                <td>${cont.toRelativePath()}</td>
                                <td><g:if test="${missingFilesHardRefs[cont.ident()]}">
                                    Cannot be deleted, is referenced
                                    </g:if>
                                    <g:else>
                                        <g:checkBox name="delete-${cont.id}" value="${false}" />
                                    </g:else>
                                    </td>
                            </tr>
                         </g:each>
                    </tbody>
                </table>
            </div>
          </div>
          <div class="row">
                <div class="col-md-12 col-xs-12">
                    <g:actionSubmit class="button" value="Delete selected" controller="wcmSynchronization" action="delete"/>
                    <g:actionSubmit class="button" value="Don't delete anything" controller="wcmSynchronization" action="done"/>
                </div>
          </div>
        </g:if>
        <g:else>
          <div class="row">
            <div class="col-md-12 col-xs-12">
              Good news! Your repository has no content nodes referencing non-existent server files.<br/>
              <g:link controller="wcmRepository" class="button">OK</g:link>
            </div>
          </div>
        </g:else>
      </g:form>
    </div>
  </body>
</html>
