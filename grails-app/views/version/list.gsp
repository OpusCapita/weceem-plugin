<html>
  <head>
    <meta name="layout" content="admin"/>
    <title><g:message code="contentVersion.title.list"/></title>
    <link href="${createLinkTo(dir:pluginContextPath + '/js/dijit/themes/tundra', file: 'tundra.css')}" media="screen" rel="stylesheet"
        type="text/css"/>
    <script djConfig="parseOnLoad:true, isDebug:false, usePlainJson:true" src="${createLinkTo(dir:pluginContextPath + '/js/dojo', file: 'dojo.js')}"
        type="text/javascript"></script>
    <script type="text/javascript" src="${createLinkTo(dir:pluginContextPath + '/js/weceem', file: 'common.js')}"></script>
    <script type="text/javascript">
      dojo.require("dojo.parser");
      dojo.require("dijit.form.Form");
      dojo.require("dijit.form.FilteringSelect");
      dojo.require("dijit.form.DateTextBox");
      dojo.require("dijit.form.TextBox");
      dojo.require("dijit.form.Button");
    </script>
  </head>

  <body class="tundra">
    <div class="body">
      <b class="header"><g:message code="contentVersion.title.list"/></b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>

      <g:form name="searchForm" action="list" dojoType="dijit.form.Form">
        <table>
          <tr>
            <td>
              <label for="contentType">
                <g:message code="contentVersion.label.spaceName"/>
              </label>
            </td>
            <td colspan="3">
              <select id="space" dojoType="dijit.form.FilteringSelect"
                  name="space" autoComplete="true" style="width: 160px;"
                  value="${params.space}">
                <option value=""></option>
                <g:each in="${spaceList}" var="space">
                  <option value="${space.name}">${space.name}</option>
                </g:each>
              </select>
            </td>
          </tr>
          <tr>
            <td>
              <label for="contentType">
                <g:message code="contentVersion.label.objectClassName"/>
              </label>
            </td>
            <td colspan="3">
              <select id="contentType" dojoType="dijit.form.FilteringSelect"
                  name="contentType" autoComplete="true" style="width: 160px;"
                  value="${params.contentType}">
                <option value=""></option>
                <g:each in="${contentTypeList}" var="contentType">
                  <option value="${contentType}">${contentType}</option>
                </g:each>
              </select>
            </td>
          </tr>
          <tr>
            <td>
              <label for="createdFrom">
                <g:message code="label.created"/>
                <g:message code="label.from"/>
              </label>
            </td>
            <td>
              <input id="createdFrom" type="text" dojoType="dijit.form.DateTextBox"
                  name="createdFrom" style="width: 100px;" serialize="serializeDate"
                  constraints="{datePattern: 'dd/MM/yyyy', strict: true}"
                  value="${params.createdFrom}"/>
            </td>
            <td>
              <label for="createdTo">
                <g:message code="label.to"/>
              </label>
            </td>
            <td>
              <input id="createdTo" type="text" dojoType="dijit.form.DateTextBox"
                  name="createdTo" style="width: 100px;" serialize="serializeDate"
                  constraints="{datePattern: 'dd/MM/yyyy', strict: true}"
                  value="${params.createdTo}"/>
            </td>
          </tr>
          <tr>
            <td>
              <label for="username">
                <g:message code="label.createdBy"/>
              </label>
            </td>
            <td colspan="3">
              <input id="username" type="text" dojoType="dijit.form.TextBox"
                  name="username" style="width: 160px;"
                  value="${params.username}"/>
              <g:easySearch name="user"/>
            </td>
          </tr>
          <tr>
            <td colspan="4">
              <button dojoType="dijit.form.Button" type="submit">
                <g:message code="command.search"/>
              </button>
              <button dojoType="dijit.form.Button" type="reset">
                <g:message code="command.reset"/>
              </button>
            </td>
          </tr>
        </table>
      </g:form>

      <g:set var="searchKeys" value="['space', 'contentType', 'createdFrom', 'createdTo', 'username']"/>

      <div class="list">
        <table class="standard">
          <thead>
            <tr>
              <g:sortableColumn property="contentHeader"
                  title="${message(code: 'contentVersion.header.contentHeader')}"
                  params="${params.subMap(searchKeys)}"/>
              <th><g:message code="contentVersion.header.spaceName"/></th>
              <th><g:message code="contentVersion.header.revision"/></th>
              <g:sortableColumn property="createdBy"
                  title="${message(code: 'header.createdBy')}"
                  params="${params.subMap(searchKeys)}"/>
              <g:sortableColumn property="createdOn"
                  title="${message(code: 'header.createdOn')}"
                  params="${params.subMap(searchKeys)}"/>
              <g:sortableColumn property="objectKey"
                  title="${message(code: 'contentVersion.header.objectKey')}"
                  params="${params.subMap(searchKeys)}"/>
              <th width="15px"><g:message code="header.operations"/></th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${items}" var="version" status="i">
              <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                <td>${version.contentTitle?.encodeAsHTML()}</td>
                <td>${version.spaceName?.encodeAsHTML()}</td>
                <td>${version.revision?.encodeAsHTML()}</td>
                <td>${version.createdBy?.encodeAsHTML()}</td>
                <td>${version.createdOn?.encodeAsHTML()}</td>
                <td>${version.objectKey?.encodeAsHTML()}</td>
                <td nowrap="nowrap">
                  <g:link action="changes" id="${version.id}">
                    <g:message code="contentVersion.command.changes"/>
                  </g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
      <div class="paginateButtons">
        <g:paginate total="${totalCount}" params="${params.subMap(searchKeys)}"/>
      </div>
    </div>
  </body>
</html>
