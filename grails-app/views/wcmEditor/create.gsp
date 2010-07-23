<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Create new content</title>
    <g:render template="editor_head" plugin="weceem"/>
  </head>
  
  <body>
    <div class="container">
        <div class="span-24 last">
          <h1>Create new <g:message code="${'content.type.name.'+params.type}" encodeAs="HTML"/></h1>

          <g:render template="editor" plugin="weceem"/>
        </div>
    </div>
  
  </body>
  
</html>
