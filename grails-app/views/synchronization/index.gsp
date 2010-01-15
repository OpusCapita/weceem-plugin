<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="synchronization.title"/></title>
  </head>

  <body>
    <b class="header"><g:message code="synchronization.title"/></b>

    <p>
        Synchronizing a space with the server filesystem will create new content nodes for any directories
        and files that exist in the filesystem but are not currently in the content repository.
    </p>
    <p> You will be given the option to delete any content nodes
        that refer to server files or directories that no longer exist
    </p>
    <p>Please choose the space you would like to synchronize""</p>
    <div>
      <input type="button" class="button"
          value="${message(code: 'synchronization.action.start')}"
          onclick="document.location.href = '${createLink(action: 'list')}'"/>
    </div>
  </body>
</html>
