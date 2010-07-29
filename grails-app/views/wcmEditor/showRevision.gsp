<html>
  <head>
    <meta name="layout" content="weceemadmin-contenthistory"/>
    <title>Change History</title>
    <g:render template="editor_head" plugin="weceem"/>
  </head>
  
  <body>
    <div class="container">
        <div class="span-24 last">
          <h1>Revision ${historyItem.revision} of ${currentContent.title.encodeAsHTML()} (/${currentContent.absoluteURI.encodeAsHTML()})</h1>
          <h2><g:formatDate date="${historyItem.createdOn}" format="yyyy/MM/dd 'at' hh:mm:ss"/></h2>

          <h2>Previously the content was:</h2>
          <pre>
              ${content}
          </pre>
          <h2>Properties were:</h2>
          <table>
              <tr><th>Name</th><th>Value</th></tr>
              <g:each in="${contentProperties}" var="p">
              <tr><td>${p.key.encodeAsHTML()}</td><td>${p.value?.encodeAsHTML()}</td></tr>
              </g:each>
          </table>
        </div>
    </div>
  
  </body>
  
</html>
