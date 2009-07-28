<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="admin"/>
    <title>Edit content</title>
    <g:javascript src="weceem/editor.js"/>
    <g:render template="editor_head" plugin="weceem"/>
  </head>
  
  <body>
  
    <div class="container">
        <div class="span-24 last">
            <h1>Edit <g:message code="${'content.type.name.'+content.class.name}" encodeAs="HTML"/> content</h1>

            <g:render template="editor" plugin="weceem"/>
        </div>
    </div>

  </body>
  
</html>
