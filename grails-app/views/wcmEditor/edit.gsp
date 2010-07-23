<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title>Edit content - ${content.title.encodeAsHTML()}</title>
    <g:render template="editor_head" plugin="weceem"/>
  </head>
  
  <body>
  
    <div class="container">
        <div class="span-24 last">
            <h1>Edit <g:message code="${'content.type.name.'+content.class.name}" encodeAs="HTML"/></h1>

            <g:render template="editor" plugin="weceem"/>
        </div>
    </div>

  </body>
  
</html>
