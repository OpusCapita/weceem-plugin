<html>
  <head>
    <meta name="layout" content="${wcm.adminLayout().toString()}"/>
    <title><g:message code="content.title"/></title>
  </head>

  <body>
    <h1 class="headline"><g:message code="menu.content.header.preview"/></h1>
    <hr class="headline"/>
    <table class="portal">
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'spaces.gif')}" alt=""/>
          <p class="title">
            <g:link controller="preview">
              <g:message code="menu.content.previewSpace"/>
            </g:link>
          </p>
          <p class="description">
            <g:message code="menu.content.previewSpace.desc"/>
          </p>
        </td>
        <td></td>
      </tr>
    </table>
    <h1 class="headline"><g:message code="menu.content.header.repository"/></h1>
    <hr class="headline"/>
    <table class="portal">
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'tree.gif')}" alt=""/>
          <p class="title">
            <g:link controller="repository">
              <g:message code="menu.content.repository"/>
            </g:link>
          </p>
          <p class="description">
            <g:message code="menu.content.repository.desc"/>
          </p>
        </td>
        <td></td>
      </tr>
    </table>
    <h1 class="headline"><g:message code="menu.content.header.content"/></h1>
    <hr class="headline"/>
    <table class="portal">
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'document.gif')}" alt=""/>
          <p class="title">
            <g:link controller="hTMLContent" action="list">
              <g:message code="menu.content.htmlContent"/>
            </g:link>
          </p>
          <p class="description">
            <g:message code="menu.content.htmlContent.desc"/>
          </p>
        </td>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'document.gif')}" alt=""/>
          <p class="title">
            <a href="javascript: void(0)">
              <g:message code="menu.content.xmlData"/>
            </a>
          </p>
          <p class="description">
            <g:message code="menu.content.xmlData.desc"/>
          </p>
        </td>
      </tr>
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'blog.gif')}" alt=""/>
          <p class="title">
            <a href="javascript: void(0)">
              <g:message code="menu.content.blogs"/>
            </a>
          </p>
          <p class="description">
            <g:message code="menu.content.blogs.desc"/>
          </p>
        </td>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'document.gif')}" alt=""/>
          <p class="title">
            <g:link controller="wikiItem">
              <g:message code="menu.content.wiki"/>
            </g:link>
          </p>
          <p class="description">
            <g:message code="menu.content.wiki.desc"/>
          </p>
        </td>
      </tr>
    </table>
    <h1 class="headline"><g:message code="menu.content.header.versioning"/></h1>
    <hr class="headline"/>
    <table class="portal">
      <tr>
        <td>
          <img src="${createLinkTo(dir:pluginContextPath + '/images/weceem', file: 'versions.gif')}" alt=""/>
          <p class="title">
            <g:link controller="version">
              <g:message code="menu.content.versions"/>
            </g:link>
          </p>
          <p class="description">
            <g:message code="menu.content.versions.desc"/>
          </p>
        </td>
        <td></td>
      </tr>
    </table>
  </body>
</html>
