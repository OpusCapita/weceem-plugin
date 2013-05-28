<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Create Space</title>
  </head>

  <body>

    <div class="body">
      <h1>Create Space</h1>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>
      <g:hasErrors bean="${space}">
        <div class="errors">
          <g:renderErrors bean="${space}" as="list"/>
        </div>
      </g:hasErrors>

      <g:form>
        <div class="dialog">
          <table>
            <tbody>
              <g:render plugin="weceem" template="/wcmSpace/form" model="[space: space]"/>
              <tr class="prop">
                <td valign="top" class="aliasURI">
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
              <tr class="prop">
                <td colspan="2">
                  <g:actionSubmit class="button" value="Save" action="save"/>&nbsp;
                  <g:actionSubmit class="button" value="Back" action="list"/>
                </td>
              </tr>
              </tbody>
          </table>
        </div>
      </g:form>
    </div>
  </body>
</html>
