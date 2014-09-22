<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Edit Space</title>
  </head>

  <body>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1>Edit Space</h1>
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

      <g:form method="post">
        <input type="hidden" name="id" value="${space?.id}"/>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <table>
              <tbody>
                <g:render plugin="weceem" template="/wcmSpace/form" model="[space: space]"/>
              </tbody>
            </table>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <g:actionSubmit class="button" value="Save" action="update"/>&nbsp;
            <g:actionSubmit class="button" value="Back" action="list"/>
          </div>
        </div>
      </g:form>

    </div>
  </body>
</html>
