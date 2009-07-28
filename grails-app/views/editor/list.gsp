<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/content"/></content>
    <content tag="tab">content</content>
    <title>HTMLContent List</title>
  </head>
  <body>
    <div class="body">
      <b class="header">HTMLContent List</b>

      <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
      </g:if>

      <g:form controller="hTMLContent">
        <div class="nav">
          <br/>
          <span class="menuButton" style="padding-left:3px; margin-bottom:8px;">
            <g:actionSubmit action="create" value="Add" class="button"/>
          </span>
          <br/>
        </div>
      </g:form>

      <div class="list">
        <table class="standard">
          <thead>
            <tr>
              <g:sortableColumn property="title" title="Title"/>
              <th>Space</th>
              <th>Language</th>
              <g:sortableColumn property="createdBy" title="Created By"/>
              <g:sortableColumn property="createdOn" title="Created On"/>
              <g:sortableColumn property="changedBy" title="Changed By"/>
              <g:sortableColumn property="changedOn" title="Changed On"/>
              <th width="15px">Operation</th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${HTMLContentList}" status="i" var="HTMLContent">
              <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                <td>${HTMLContent.title?.encodeAsHTML()}</td>
                <td>${HTMLContent.space?.name?.encodeAsHTML()}</td>
                <td>${HTMLContent.language?.encodeAsHTML()}</td>
                <td>${HTMLContent.createdBy?.encodeAsHTML()}</td>
                <td>${HTMLContent.createdOn?.encodeAsHTML()}</td>
                <td>${HTMLContent.changedBy?.encodeAsHTML()}</td>
                <td>${HTMLContent.changedOn?.encodeAsHTML()}</td>
                <td>
                  <g:link action="edit" id="${HTMLContent.id}" title="Edit">Edit</g:link>&nbsp;&nbsp;
                  <g:link action="delete" id ="${HTMLContent.id}" title="Delete">Delete</g:link>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
      <div class="paginateButtons">
        <g:paginate total="${org.weceem.html.HTMLContent.count()}"/>
      </div>
    </div>
  </body>
</html>
