<html>
<head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="administration.title"/></title>
</head>

<body>
<div class="container">
    <div class="row">
      <div class="col-md-12 col-xs-12">
        <h1><g:message code="administration.title"/></h1>
      </div>
    </div>

    <nav:eachItem group="weceem.plugin.admin" var="n">
        <div class="row">
            <div class="col-md-2 col-xs-2">
                <img src="${g.resource(plugin:'weceem', dir:'_weceem/images/weceem/admin/icons', file: n.title+'.gif')}" alt="${message(code:'admin.title.'+n.title, encodeAs:'HTML')}"/>
                <p class="title">
                    <g:link controller="${n.controller}" action="${n.action}"><g:message code="${'admin.function.title.'+n.title}" encodeAs="HTML"/></g:link>
                </p>
            </div>
            <div class="col-md-10 col-xs-10 text">
                <h2 class="title"><g:message code="${'admin.function.description.'+n.title}" encodeAs="HTML"/></h2>
            </div>
        </div>
    </nav:eachItem>

    <nav:eachItem group="weceem.app.admin" var="n">
        <div class="row">
            <div class="col-md-2 col-xs-2">
                <img src="${g.resource(plugin:'none', dir:'_weceem/images/icons', file: n.title+'.gif')}" alt="${message(code:'admin.title.'+n.title, encodeAs:'HTML')}"/>
                <p class="title">
                    <g:link controller="${n.controller}" action="${n.action}"><g:message code="${'admin.function.title.'+n.title}" encodeAs="HTML"/></g:link>
                </p>
            </div>
            <div class="col-md-10 col-xs-10">
                <h2><g:message code="${'admin.function.description.'+n.title}" encodeAs="HTML"/></h2>
            </div>
        </div>
    </nav:eachItem>
</div>

</body>
</html>
