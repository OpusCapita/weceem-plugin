<div class="container"> 
    <div id="adminLogo" ></div>
    <div id="iconbar" >
        <span title="Current User">
          Current User: <wcm:loggedInUserName/>
        </span>
      <g:link controller="register" action="edit"><img src="${createLinkTo(dir:wcm.pluginCtxPath() +'/images/layout',file:'user16.gif')}"/></g:link>
      <a href="#"><img src="${createLinkTo(dir:wcm.pluginCtxPath() +'/images/layout',file:'help16.gif')}"/></a>
      <g:link controller="logout"><img src="${createLinkTo(dir:wcm.pluginCtxPath() +'/images/layout',file:'logout16.gif')}"/></g:link>
    </div>

    <div id="navigation" class="span-24 last">
        <nav:render group="weceem" actionMatch="true"/>
    </div>

    
    <g:if test="${flash.message}">
      <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all">${flash.message.encodeAsHTML()}</div>
    </g:if>
    <g:if test="${flash.error}">
      <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-error ui-corner-all">${flash.error}</div>
    </g:if>

</div>

