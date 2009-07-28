<html>
  <head>
    <meta name="layout" content="admin"/>
    <meta http-equiv=Refresh content="1;URL=${createLink(action: 'performExport', params: params)}">
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title><g:message code="space.title.export"/></title>
  </head>

  <body>
    <div class="body">
      <b class="header"><g:message code="space.title.export"/></b>

      <br/><br/>
      <div><g:message code="message.export.downloading"/></div>
    </div>
  </body>
</html>
