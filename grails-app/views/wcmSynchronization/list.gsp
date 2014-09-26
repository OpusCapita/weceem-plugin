<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="synchronization.title"/></title>
  </head>
  <body>
    <nav:set path="plugin.weceem.weceem_menu/administration" scope="plugin.weceem.weceem_menu"/>
    <div class="container">
       <div class="row">
         <div class="col-md-12 col-xs-12">
            <h1>File Synchronization</h1>
         </div>
       </div>
       <div class="row">
         <div class="col-md-6 col-xs-6">
            <p>Synchronizing a space with the server filesystem will create new content nodes for any directories
               and files that exist in the filesystem but are not currently in the content repository.
            <br/>
            <br>
            You will be given the option to delete any content nodes
            that refer to server files or directories that no longer exist
            </p>
         </div>
         <div class="col-md-6 col-xs-6">
            To begin, please choose the space you would like to synchronize.<br/>
            <g:each in="${spaces}" status="i" var="space">
                <g:link controller="wcmSynchronization" action="synchronizationList" id="${space.id}" class="large">${space.name.encodeAsHTML()} (/${space.aliasURI.encodeAsHTML()})</g:link><br/>
            </g:each>
        </div>
       </div>
    </div>
  </body>
</html>
