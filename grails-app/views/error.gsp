<html>
  <head>
      <title>Weceem CMS Error</title>
      <style type="text/css">
            .message {
                border: 1px solid black;
                padding: 5px;
                background-color:#E9E9E9;
            }
            h3 {
                margin-top: 20px;
            }
            .stack {
                border: 1px solid black;
                padding: 5px;
                overflow:auto;
                height: 300px;
            }
            .snippet {
                padding: 5px;
                background-color:white;
                border:1px solid black;
                margin:3px;
                font-family:courier;
            }
            #logo {
                padding-bottom: 20px;
                display: block;
            }
      </style>
  </head>

  <body>
    <img id="logo" src="${g.resource(dir:'_weceem/images/layout', file:'weceem-logo.png')}"/><h1>An error occurred</h1>
    <g:if test="${exception}">
        <h2>${exception.message.encodeAsHTML()}</h2>
    </g:if>
    
    <h3>Details</h3>
    <div class="message">
        <strong>Error ${request.'javax.servlet.error.status_code'.encodeAsHTML()}:</strong> ${request.'javax.servlet.error.message'.encodeAsHTML()}<br/>
        <g:if test="${exception}">
            <strong>Exception Message:</strong> ${exception.message?.encodeAsHTML()} <br />
            <strong>Caused by:</strong> ${exception.cause?.message?.encodeAsHTML()} <br />
            <strong>Class:</strong> ${exception.className}.encodeAsHTML() <br />
            <strong>At Line:</strong> [${exception.lineNumber}.encodeAsHTML()] <br />
            <strong>Code Snippet:</strong><br />
            <div class="snippet">
                <g:each var="cs" in="${exception.codeSnippet}">
                    ${cs?.encodeAsHTML()}<br />
                </g:each>
            </div>
        </g:if>
    </div>
    <g:if test="${exception}">
        <h3>Stack Trace</h3>
        <div class="stack">
          <pre><g:each in="${exception.stackTraceLines}">${it.encodeAsHTML()}<br/></g:each></pre>
        </div>
    </g:if>
  </body>
</html>