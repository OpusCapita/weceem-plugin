<div class="container"> 
    <div id="adminLogo" class="span-14"></div>
    
    <div id="iconbar" class="span-10 last prepend-top" style="text-align: right">
        <span title="Current User">
          Welcome, ${wcm.loggedInUserName().encodeAsHTML()} (${wcm.loggedInUserEmail().encodeAsHTML()})
        </span>
        <g:link url="${wcm.userProfileEditUrl().encodeAsHTML()}">Profile</g:link> |
        <a href="http://www.weceem.org/">Help</a> |
        <g:link url="${wcm.userLogOutUrl().encodeAsHTML()}">Log Out</g:link>
    </div>

    <div id="navigation" class="span-24 last">
        <nav:render group="weceem" actionMatch="true"/>
    </div>

    
    <g:if test="${flash.message}">
      <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all"><g:message code="${flash.message}" encodeAs="HTML"/></div>
    </g:if>
    <g:if test="${flash.error}">
      <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-error ui-corner-all">${flash.error}</div>
    </g:if>

</div>

