<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="content.title.edit" default="Edit"/> <g:message code="${'content.type.name.'+content.class.name}" encodeAs="HTML"/> - ${content.title.encodeAsHTML()}</title>
    <g:render template="editor_head" plugin="weceem"/>
  </head>
  
  <body>
  
    <div class="container">
        <div class="span-24 last">
            <h1><g:message code="content.title.edit" default="Edit"/> <g:message code="${'content.type.name.'+content.class.name}" encodeAs="HTML"/></h1>

            <g:render template="editor" plugin="weceem"/>
        </div>
    </div>

  </body>
  
</html>
