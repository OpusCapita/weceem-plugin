<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <meta http-equiv="Refresh" content="1;URL=${createLink(action: 'performExport', params: [space:params.id, exporter:params.exporter])}">
    <title><g:message code="space.title.export"/></title>
  </head>

  <body>
    <div class="body">
      <h1><g:message code="space.title.export" args="[space.name]"/></h1>

      <br/><br/>
      <div><g:message code="message.export.downloading"/></div>
    </div>
  </body>
</html>
