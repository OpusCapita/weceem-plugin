<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="administration.title"/></title>
  </head>

  <body>
      <div class="container">
          <div class="span-24 last">
              <h1 class="headline"><g:message code="administration.title"/></h1>
          </div>

          <nav:eachItem group="weceem.plugin.admin" var="n">
              <div class="span-24 last container">
                  <div class="span-3">
                      <img src="${g.resource(plugin:'weceem', dir:'_weceem/images/weceem/admin/icons', file: n.title+'.gif')}" alt="${message(code:'admin.title.'+n.title, encodeAs:'HTML')}"/>
                      <p class="title">
                        <g:link controller="${n.controller}" action="${n.action}"><g:message code="${'admin.function.title.'+n.title}" encodeAs="HTML"/></g:link>
                      </p>
                  </div>
                  <div class="span-20 prepend-1 last">
                      <h2><g:message code="${'admin.function.description.'+n.title}" encodeAs="HTML"/></h2>
                  </div>
              </div>
          </nav:eachItem>

          <nav:eachItem group="weceem.app.admin" var="n">
              <div class="span-24 last container">
                  <div class="span-3">
                      <img src="${g.resource(plugin:'none', dir:'_weceem/images/icons', file: n.title+'.gif')}" alt="${message(code:'admin.title.'+n.title, encodeAs:'HTML')}"/>
                      <p class="title">
                        <g:link controller="${n.controller}" action="${n.action}"><g:message code="${'admin.function.title.'+n.title}" encodeAs="HTML"/></g:link>
                      </p>
                  </div>
                  <div class="span-20 prepend-1 last">
                      <h2><g:message code="${'admin.function.description.'+n.title}" encodeAs="HTML"/></h2>
                  </div>
              </div>
          </nav:eachItem>
      </div>

  </body>
</html>
