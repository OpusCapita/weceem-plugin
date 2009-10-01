<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="design.title"/></title>
  </head>

  <body>
    <h1 class="headline"><g:message code="menu.design.header.templates"/></h1>
    <table class="portal">
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'template.gif')}" alt=""/>
          <p class="title">
            <g:link controller="template">
              <g:message code="menu.design.templates"/>
            </g:link>
          </p>
          <p class="description"><g:message code="menu.design.templates.desc"/></p>
        </td>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'blocks.gif')}" alt=""/>
          <p class="title">
            <g:link controller="block">
              <g:message code="menu.design.widgets"/>
            </g:link>
          </p>
          <p class="description"><g:message code="menu.design.widgets.desc"/></p>
        </td>
      </tr>
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'stylesheet.gif')}" alt=""/>
          <p class="title">
            <g:link controller="styleSheet">
              <g:message code="menu.design.stylesheet"/>
            </g:link>
          </p>
          <p class="description"><g:message code="menu.design.stylesheet.desc"/></p>
        </td>
        <td></td>
      </tr>
    </table>
  </body>
</html>
