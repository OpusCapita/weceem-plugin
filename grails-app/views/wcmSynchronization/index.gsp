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
          <h1><g:message code="synchronization.title"/></h1>
        </div>
      </div>
      <div class="row">
        <div class="col-md-6 col-xs-6">
          <p>Synchronizing a space with the server filesystem will create new content nodes for any directories and
          files that exist in the filesystem but are not currently in the content repository.
          </p>
          <p> You will be given the option to delete any content nodes that refer to server files or directories that
          no longer exist
          </p>
          <p>Please choose the space you would like to synchronize</p>
        </div>
      </div>
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <input type="button" class="btn btn-primary"
          value="${message(code: 'synchronization.action.start')}"
          onclick="document.location.href = '${createLink(action: 'list')}'"/>
        </div>
      </div>
    </div>
  </body>
</html>
