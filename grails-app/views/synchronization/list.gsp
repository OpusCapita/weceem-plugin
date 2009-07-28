<html>
  <head>
    <meta name="layout" content="admin"/>
    <content tag="menu"><g:render plugin="weceem" template="/layouts/menu/administration"/></content>
    <content tag="tab">administration</content>
    <title><g:message code="synchronization.title"/></title>
    <style type="text/css">
      @import "${createLinkTo(dir:pluginContextPath + '/js/dojox/grid/_grid', file: 'tundraGrid.css')}";
      @import "${createLinkTo(dir:pluginContextPath + '/js/dijit/themes/tundra', file: 'tundra.css')}";
      @import "${createLinkTo(dir:pluginContextPath + '/css', file: 'contentRepository.css')}";
      @import "${createLinkTo(dir:pluginContextPath + '/js/weceem/css', file: 'widgets.css')}";
    </style>
    <script djConfig="parseOnLoad:true, isDebug:false, usePlainJson:true"
        src="${createLinkTo(dir:pluginContextPath + '/js/dojo', file: 'dojo.js')}"
        type="text/javascript"></script>
    <script type="text/javascript"
        src="${createLinkTo(dir:pluginContextPath + '/js/weceem', file: 'contentRepository.js')}"></script>
    <script language="JavaScript" type="text/javascript">
      dojo.require("dojo.parser");
      dojo.require("dojo.data.ItemFileWriteStore");
      dojo.require("dojox.grid.Grid");
      dojo.require("dijit.form.Form");
      dojo.require("dijit.form.FilteringSelect");
      dojo.require("dijit.form.Button");
      dojo.require("dijit.Dialog");
      dojo.require("weceem.LoadingViewLock");

      var orphanedContentView = {
        cells: [[
          {name: "${message(code: 'synchronization.header.contentTitle')}", field: "title", width: "150px"},
          {name: "${message(code: 'synchronization.header.contentType')}", field: "type", width: "100px"},
          {name: "${message(code: 'synchronization.header.space')}", field: "space", width: "100px"},
          {name: " ", value: "<button>${message(code: 'command.delete')}</button>", styles: 'text-align:center;'},
          {name: " ", value: "<button>${message(code: 'command.link')}</button>", styles: 'text-align:center;'}
        ]]
      };
      var orphanedContentLayout = [ orphanedContentView ];

      var orphanedFilesView = {
        cells: [[
          {name: "${message(code: 'synchronization.header.fileName')}", field: "title", width: "150px"},
          {name: "${message(code: 'synchronization.header.type')}", field: "type", width: "100px"},
          {name: "${message(code: 'synchronization.header.relativePath')}", field: "path", width: "250px"},
          {name: " ", value: "<button>${message(code: 'command.create')}</button>", styles: 'text-align:center;'}
        ]]
      };
      var orphanedFilesLayout = [ orphanedFilesView ];

      function serverError(message) {
        var msg = message ? message : "${message(code: 'error.synchronization.common')}";
        alert(msg);
        dijit.byId("viewLock").unlockView();
      }
    </script>
  </head>

  <body class="tundra">
    <div id="viewLock" dojoType="weceem.LoadingViewLock"
        message="${message(code: 'message.loading')}"></div>
    <%-- todo: use the same store for both grids (fix model's query) --%>
    <div dojoType="dojo.data.ItemFileWriteStore" jsId="syncStore"
        url="synchronizationList"></div>
    <div dojoType="dojo.data.ItemFileWriteStore" jsId="syncStore2"
        url="synchronizationList"></div>

    <div dojoType="dojox.grid.data.DojoData" jsId="contentModel"
        rowsPerPage="100" store="syncStore" query="{namespace:'content'}"></div>
    <div><b>${message(code: 'synchronization.title.content')}</b></div>
    <div id="contentGrid" dojoType="dojox.Grid" model="contentModel" structure="orphanedContentLayout"
        style="height: 250px; width: 100%;">
      <script type="dojo/connect" event="onCellClick" args="e">
        var id = e.grid.model.getRow(e.rowIndex).name;
        if (e.cell.index == 3
            && confirm("${message(code: 'synchronization.message.confirm.delete')}")) {
          dijit.byId("viewLock").lockView();
          dojo.xhrPost({
            url: "deleteContent",
            content: {
              id: id
            },
            handleAs: "json-comment-optional",
            load: function(response, ioArgs) {
              if (response.success) {
                e.grid.model.remove([e.rowIndex]);
                dijit.byId("viewLock").unlockView();
              } else {
                serverError(response.error);
              }
            },
            error: function(response, ioArgs) {
              serverError();
            }
          });
        } else if (e.cell.index == 4) {
          dojo.byId("contentLinkId").value = id;
          dijit.byId("linkDialog").show();
        }
      </script>
    </div>

    <br/>

    <div dojoType="dojox.grid.data.DojoData" jsId="filesModel"
        rowsPerPage="100" store="syncStore2" query="{namespace:'files'}"></div>
    <div><b>${message(code: 'synchronization.title.files')}</b></div>
    <div id="filesGrid" dojoType="dojox.Grid" model="filesModel" structure="orphanedFilesLayout"
        style="height: 250px; width: 100%;">
      <script type="dojo/connect" event="onCellClick" args="e">
        if (e.cell.index == 3
            && confirm("${message(code: 'synchronization.message.confirm.create')}")) {
          var name = e.grid.model.getRow(e.rowIndex).name;
          var path = e.grid.model.getRow(e.rowIndex).path;
          dijit.byId("viewLock").lockView();
          dojo.xhrPost({
            url: "createContentFile",
            content: {
              path: path
            },
            handleAs: "json-comment-optional",
            load: function(response, ioArgs) {
              if (response.success) {
                e.grid.model.remove([e.rowIndex]);
                dijit.byId("linkFilePath").store.deleteItem(
                    dijit.byId("linkFilePath").store._getItemByIdentity(name));
                dijit.byId("viewLock").unlockView();
              } else {
                serverError(response.error);
              }
            },
            error: function(response, ioArgs) {
              serverError();
            }
          });
        }
      </script>
    </div>

    <div id="linkDialog" dojoType="dijit.Dialog" style="display: none;">
      <form action="#" dojoType="dijit.form.Form" onsubmit="return false;">
        <script type="dojo/connect" event="onSubmit" args="e">
          if (this.isValid()) {
            var self = this;
            var filePath = dijit.byId("linkFilePath").store.getValue(
                    dijit.byId("linkFilePath").item, "path");
            dijit.byId("linkDialog").hide();
            dijit.byId("viewLock").lockView();
            dojo.xhrPost({
              url: "linkContentFile",
              content: {
                id: dojo.byId("contentLinkId").value,
                path: filePath
              },
              handleAs: "json-comment-optional",
              load: function(response, ioArgs) {
                if (response.success) {
                  var rowIndex = dijit.byId("contentGrid").model._rowIdentities[dojo.byId("contentLinkId").value];
                  dijit.byId("contentGrid").model.remove([rowIndex]);
                  var targetRowIndex = dijit.byId("filesGrid").model._rowIdentities[filePath];
                  dijit.byId("filesGrid").model.remove([targetRowIndex]);
                  dijit.byId("linkFilePath").store.deleteItem(
                      dijit.byId("linkFilePath").item);

                  dijit.byId("viewLock").unlockView();
                } else {
                  serverError(response.error);
                }
              },
              error: function(response, ioArgs) {
                serverError();
              }
            });
          }
          return false;
        </script>
        <input id="contentLinkId" type="hidden" name="id" value=""/>
        <label for="linkFilePath"><g:message code="synchronization.label.targetFile"/></label>
        <br/>
        <input id="linkFilePath" dojoType="dijit.form.FilteringSelect"
            store="syncStore2" query="{namespace:'files'}"
            searchAttr="title" labelAttr="path" name="path" autocomplete="true"/>
        <br/><br/>
        <div align="center">
          <button type="submit" dojoType="dijit.form.Button">
            <g:message code="command.ok"/>
          </button>
          <button type="button" dojoType="dijit.form.Button"
              onclick="dijit.byId('linkDialog').hide()">
            <g:message code="command.cancel"/>
          </button>
        </div>
      </form>
    </div>

  </body>
</html>
