<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body>
    
      <div class="container">
          <div class="span-24 last">
            <h1>File Synchronization</h1>
          </div>
          <div class="span-10">
            <p>Synchronizing a space with the server filesystem will create new content nodes for any directories
               and files that exist in the filesystem but are not currently in the content repository.
            <br/>
            <br>
            You will be given the option to delete any content nodes
            that refer to server files or directories that no longer exist
            </p>
          
        </div>
        <div class="prepend-2 span-10 append-2 last">
            To begin, please choose the space you would like to synchronize.<br/>
            <g:each in="${spaces}" status="i" var="space">
                <g:link controller="synchronization" action="synchronizationList" id="${space.id}" class="large">${space.name.encodeAsHTML()} (/${space.aliasURI.encodeAsHTML()})</g:link><br/>
            </g:each>
        </div>
    </div>
  </body>
</html>
