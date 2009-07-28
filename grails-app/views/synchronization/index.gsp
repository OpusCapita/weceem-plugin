<html>
  <head>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title><g:message code="synchronization.title"/></title>
  </head>

  <body>
    <b class="header"><g:message code="synchronization.title"/></b>

    <br/>
    <br/>
    <div>
      <input type="button" class="button"
          value="${message(code: 'synchronization.action.start')}"
          onclick="document.location.href = '${createLink(action: 'list')}'"/>
    </div>
  </body>
</html>
