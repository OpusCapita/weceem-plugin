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

    <nav:menu scope="plugin.weceem.weceem_admin" custom="true">
        <div class="row margin-bottom-10">
            <div class="col-md-2 col-xs-2">
                <img src="${g.resource(plugin:'weceem', dir:'_weceem/images/weceem/admin/icons', file: item.name +'.gif')}" alt="${message(code:'admin.title.'+item.name , encodeAs:'HTML')}"/> <br/>
                <p:callTag tag="g:link" attrs="${linkArgs + [class:active ? 'active' : '']}">
                    <span>
                        <nav:title item="${item}"/>
                    </span>
                </p:callTag>
            </div>
            <div class="col-md-10 col-xs-10 text">
                <h2 class="title"><g:message code="${'admin.function.description.'+item.name}" encodeAs="HTML"/></h2>
            </div>
        </div>
    </nav:menu>

    <nav:menu scope="weceem_admin" custom="true">
        <div class="row margin-bottom-10">
            <div class="col-md-2 col-xs-2">
                <img src="${g.resource(plugin:'none', dir:'_weceem/images/icons', file: item.name+'.gif')}" alt="${message(code:'admin.title.'+item.name, encodeAs:'HTML')}"/> <br/>
                <p:callTag tag="g:link" attrs="${linkArgs + [class:active ? 'active' : '']}">
                    <span>
                        <nav:title item="${item}"/>
                    </span>
                </p:callTag>
            </div>
            <div class="col-md-10 col-xs-10 text">
                <h2 class="title"><g:message code="${'admin.function.description.'+item.name}" encodeAs="HTML"/></h2>
            </div>
        </div>
    </nav:menu>

</div>

</body>
</html>
