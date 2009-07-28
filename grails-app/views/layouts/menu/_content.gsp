<dl id="menuList">
  <dt><g:message code="menu.content.header.preview"/></dt>
  <dd>
    <ul>
      <li><g:link controller="preview"><g:message code="menu.content.previewSpace"/></g:link></li>
    </ul>
  </dd>
  <dt><g:message code="menu.content.header.repository"/></dt>
  <dd>
    <ul>
      <li>
        <g:link controller="contentRepository">
          <g:message code="menu.content.repository"/>
        </g:link>
      </li>
    </ul>
  </dd>
  <dt><g:message code="menu.content.header.content"/></dt>
  <dd>
    <ul>
      <li>
        <g:link controller="hTMLContent" action="list">
          <g:message code="menu.content.htmlContent"/>
        </g:link>
      </li>
      <li>
        <a href="javascript: void(0)">
          <g:message code="menu.content.xmlData"/>
        </a>
      </li>
      <li>
        <a href="javascript: void(0)">
          <g:message code="menu.content.blogs"/>
        </a>
      </li>
      <li>
        <g:link controller="wikiItem">
          <g:message code="menu.content.wiki"/>
        </g:link>
      </li>
    </ul>
  </dd>
  <dt><g:message code="menu.content.header.versioning"/></dt>
  <dd>
    <ul>
      <li>
        <g:link controller="contentVersion">
          <g:message code="menu.content.versions"/>
        </g:link>
      </li>
    </ul>
  </dd>
</dl>