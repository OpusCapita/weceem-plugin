<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Delete Space</title>
  </head>

  <body>

    <div class="body">
      <h1>Delete Space "${space.name.encodeAsHTML()}"?</h1>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>
      <g:hasErrors bean="${space}">
        <div class="errors">
          <g:renderErrors bean="${space}" as="list"/>
        </div>
      </g:hasErrors>

      <p>Do you really want to delete the space "${space.name.encodeAsHTML()}"? This will destroy all content in the space. There is no undo.</p>
      <g:link action="delete" class="button ui-corner-all" id="${space.id}"><g:message code="command.confirm.delete"/></g:link>
      <g:link action="list" class="button ui-corner-all" id="${space.id}"><g:message code="command.cancel"/></g:link>
    </div>
  </body>
</html>
