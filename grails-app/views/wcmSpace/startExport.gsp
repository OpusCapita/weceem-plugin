<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <meta http-equiv="Refresh" content="1;URL=${createLink(action: 'performExport', params: [space:params.id, exporter:params.exporter])}">
    <title><g:message code="space.title.export"/></title>
  </head>

  <body>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1><g:message code="space.title.export" args="[space.name]"/></h1>
        </div>
      </div>

      <br/><br/>
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <div><g:message code="message.export.downloading"/></div>
        </div>
      </div>
    </div>
  </body>
</html>
