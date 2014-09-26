<div class="container">
    <div class="row">
        <div class="col-md-6" id="adminLogo" >&nbsp;</div>
        <div class="col-md-6" style="font-size: 12px; vertical-align: top; text-align: right; padding-top: 18px;">
          <span title="Current User">
            <g:message code="admin.user.current" args="${[wcm.loggedInUserName().encodeAsHTML(), wcm.loggedInUserEmail().encodeAsHTML()]}"/>
          </span>
          <g:link url="${wcm.userProfileEditUrl().encodeAsHTML()}"><g:message code="admin.user.profile"/></g:link> |
            <a href="http://www.weceem.org/"><g:message code="admin.help.label"/></a> |
          <g:link url="${wcm.userLogOutUrl().encodeAsHTML()}"><g:message code="admin.user.logout"/></g:link>
        </div>
    </div>

    <div id="navigation" class="row" style="margin:0px;">
        <div class="col-md-12 col-xs-12">
            <nav:menu scope="plugin.weceem.weceem_menu" custom="true" class="nav nav-pills" >
                <li class="${active ? 'active': ''}">
                    <p:callTag tag="g:link"
                               attrs="${linkArgs + [class: 'active' ? 'active' : '']}">
                        <span>
                            <nav:title item="${item}"/>
                        </span>
                    </p:callTag>
             </li>
            </nav:menu>
        </div>
    </div>

    <div class="row">
      <div class="col-md-12 col-xs-12">
        <g:if test="${flash.message}">
          <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-highlight ui-corner-all"><g:message code="${flash.message.encodeAsHTML()}"/></div>
        </g:if>
        <g:if test="${flash.error}">
          <div class="message span-22 prepend-1 append-1 prepent-top append-bottom last ui-state-error ui-corner-all">${flash.error}</div>
        </g:if>
      </div>
    </div>
 </div>