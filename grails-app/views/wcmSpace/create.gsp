<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Create Space</title>
  </head>

  <body>
    <nav:set path="plugin.weceem.weceem_menu/administration" scope="plugin.weceem.weceem_menu"/>
    <div class="container">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <h1>Create Space</h1>
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
          <div class="errors">
            <g:renderErrors bean="${space}" as="list"/>
          </div>
        </div>
      </g:hasErrors>

      <g:form>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <table>
              <tbody>
               <g:render plugin="weceem" template="/wcmSpace/form" model="[space: space]"/>
                <tr class="prop">
                  <td valign="top">
                    <label for="name">Use template:</label>
                  </td>
                  <td valign="top" class="value">
                    <g:select name="templateName" from="${templates}"
                      optionValue="${ { v -> g.message(code:'weceem.template.name.'+v.key) } }"
                      optionKey="key"
                      value="${selectedTemplate}"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <g:actionSubmit class="btn btn-primary" value="Save" action="save"/>&nbsp;
            <g:actionSubmit class="btn btn-default" value="Back" action="list"/>
          </div>
        </div>
      </g:form>


    </div>
  </body>
</html>
