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
    <img id="logo" src="${g.resource(dir:'_weceem/images/layout', file:'weceem-logo.png')}"/>
    <h1>Access denied</h1>
    <p>${accessDeniedMessage.encodeAsHTML()}</p>
  </body>
</html>