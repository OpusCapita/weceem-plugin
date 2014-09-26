<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="content.title.create" default="Create new"/> - <g:message code="${'content.type.name.'+params.type}" encodeAs="HTML"/></title>
    <g:render template="editor_head" plugin="weceem"/>

  </head>
  
  <body>
    <nav:set path="plugin.weceem.weceem_menu/content" scope="plugin.weceem.weceem_menu"/>
    <div class="container">
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <h1><g:message code="content.title.create" default="Create new"/> - <g:message code="${'content.type.name.'+params.type}" encodeAs="HTML"/></h1>
          </div>
        </div>
        <div class="row">
          <div class="col-md-12 col-xs-12">
            <g:render template="editor" plugin="weceem" model="[weceemEditorMode:'create']" />
          </div>
        </div>
    </div>
  
  </body>
  
</html>
