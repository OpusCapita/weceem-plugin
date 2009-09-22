<div class="container"> 
    <div id="adminLogo" class="span-14"></div>
    
    <div id="iconbar" class="span-10 last prepend-top" style="text-align: right">
        <span title="Current User">
          Welcome, <wcm:loggedInUserName/>
        </span>
        <g:link url="${wcm.userProfileEditUrl().encodeAsHTML()}" class="ui-state-default ui-corner-all button">Profile</g:link>
        <g:link url="${wcm.userLogOutUrl().encodeAsHTML()}" class="ui-state-default ui-corner-all button">Log Out</g:link>
        <a href="http://weceem.org/weceem/Documentation" class="ui-state-default ui-corner-all button">Help</a>
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

