<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Delete Space</title>
  </head>

  <body>
    <nav:set path="plugin.weceem.weceem_menu/administration" scope="plugin.weceem.weceem_menu"/>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1>Delete Space "${space.name.encodeAsHTML()}"?</h1>
        </div>
      </div>


      <g:if test="${flash.message}">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <div class="message">${flash.message}</div>
          </div>
        </div>
      </g:if>
      <g:hasErrors bean="${space}">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <div class="errors">
              <g:renderErrors bean="${space}" as="list"/>
            </div>
          </div>
        </div>
      </g:hasErrors>

      <div class="row">
        <div class="col-md-12 col-xs-12">
          <p>Do you really want to delete the space "${space.name.encodeAsHTML()}"? This will destroy all content in the space. There is no undo.</p>
          <g:link action="delete" class="btn btn-primary" id="${space.id}"><g:message code="command.confirm.delete"/></g:link>
          <g:link action="list" class="btn btn-default" id="${space.id}"><g:message code="command.cancel"/></g:link>
        </div>
      </div>
    </div>
  </body>
</html>
