<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="admin"/>
    <title>Content Changes</title>
    <style type="text/css">
    div.added, span.added {
      background-color: #99FF99;
      font-weight: bolder;
    }
    div.added {
      border-left: 3px solid #336600;
      padding-left: 5px;
    }
    div.deleted, span.deleted {
      background-color: #FF9999;
      text-decoration: line-through;
    }
    div.deleted {
      border-left: 3px solid #990000;
      padding-left: 5px;
    }
    td.block {
      border: 1px solid #CCCCCC;
      padding: 5px;
    }
    </style>
  </head>
  <body>
    <div class="body">
      <b class="header">Content Changes</b>
      <hr class="headline" style="margin-left: 0"/>

      <table width="100%">
        <tr>
          <td>
            <table>
              <tr>
                <td class="block" align="center">
                  <b>Version ${oVersion}</b> by &quot;${oCreatedBy}&quot;
                  <br/>
                  on ${oCreatedOn}
                </td>
                <td align="center">
                  <b>compared with</b>
                </td>
                <td class="block" align="center">
                  <b>Version ${cVersion}</b> by &quot;${cChangedBy}&quot;
                  <br/>
                  on ${cChangedOn}
                </td>
              </tr>
            </table>
          </td>
          <td align="right">
            <table><tr><td class="block">
              <div style="font-weight: bold; padding-bottom: 3px;">Key</div>
              <div class="added">word was added</div>
              <div class="deleted">word was deleted</div>
            </td></tr></table>
          </td>
        </tr>
      </table>

      <div style="margin-top: 20px; border: 1px solid #CCCCCC; padding: 5px;">
        ${content}
      </div>

    </div>
  </body>
</html>
