dojo.provide("weceem.RepositoryDndSource");
dojo.require("dijit._tree.dndSource");
dojo.require("dijit.Dialog");

dojo.declare("weceem.RepositoryDndSource", dijit._tree.dndSource, {
  // summary:
  //    Extends dijit._tree.dndSource to provide specific behaviour.
  //
  // author
  //    Sergei Shushkevich

  _underlay: null,
  _fadeIn: null,
  _fadeOut: null,
  duration: 200,

  _dndSourceNode: null,
  _dndTargetNode: null,
  _accepted: false,

  dndDropParams: new Object(),

  constructor: function(tree, params){
    this.singular = true;
    this._underlay = new dijit.DialogUnderlay({
      id: this.id + "_underlay"
    });
    var node = document.getElementById("loading");
    this._fadeIn = dojo.fx.combine(
            [dojo.fadeIn({
              node: node,
              duration: this.duration,
              onBegin: function() { node.style.visibility = "visible"; }
            }),
              dojo.fadeIn({
                node: this._underlay.domNode,
                duration: this.duration,
                onBegin: dojo.hitch(this._underlay, "show")
              })]);
    this._fadeOut = dojo.fx.combine(
            [dojo.fadeOut({
              node: node,
              duration: this.duration,
              onEnd: function() { node.style.visibility = "hidden"; }
            }),
              dojo.fadeOut({
                node: this._underlay.domNode,
                duration: this.duration,
                onEnd: dojo.hitch(this._underlay, "hide")
              })]);
  },

  copyState: function(keyPressed){
    return false;
  },

  checkAcceptance: function(source, nodes) {
    // summary:
    //    Checks if the transferred item is Content node.
    // source: Object
    //    the source which provides items
    // nodes: Array
    //    the list of transferred items

    var node = dijit.getEnclosingWidget(nodes[0]);
    if (node) {
      var type = this.tree.model.store.getValue(node.item, "type");
      if (type != "space" && type != "contentType") {
        this._accepted = true;
        this._dndSourceNode = node;
        return true;
      }
    }
    return false;
  },

  checkItemAcceptance: function(node, source) {
    // summary:
    //    Checks ability to drop item.

    this._dndTargetNode = dijit.getEnclosingWidget(node);

    // disallow drop into itself or parent node
    if (!this._accepted || !this._dndTargetNode
        || (this._dndSourceNode === this._dndTargetNode)
        || (this._dndSourceNode.getParent() === this._dndTargetNode)) {
      return false;
    }

    var store = this.tree.model.store;

    var targetType = store.getValue(this._dndTargetNode.item, "type");
    // disallow drop into Space node
    if (targetType == "space") {
      return false;
    }

    var sourcePath = store.getValue(this._dndSourceNode.item, "path").toString();
    var targetPath = store.getValue(this._dndTargetNode.item, "path").toString();
    // disallow drop into any child node
    if (targetPath.indexOf(sourcePath) == 0
        && targetPath.charAt(sourcePath.length) == "/") {
      return false;
    }

    var sourceAttributes = sourcePath.split("/");
    var targetAttributes = targetPath.split("/");
    // disallow D&D between different Spaces
    if (sourceAttributes[0] != targetAttributes[0]) {
      return false;
    }

    // check target node acceptance
    if (targetType != "contentType"
        && store.getValue(this._dndTargetNode.item, "canHaveChildren") != true) {
      return false;
    }

    var sourceType = store.getValue(this._dndSourceNode.item, "type");
    // allow drop into ContentDirectory only for ContentFile and it's subclasses
    if (sourceType != "org.weceem.files.ContentFile" && sourceType != "org.weceem.files.ContentDirectory"
        && targetType == "org.weceem.files.ContentDirectory") {
      return false;
    }

    return true;
  },

  onDndDrop: function(source, nodes, copy){
    // summary:
    //   Topic event processor for /dnd/drop, called to finish the DnD operation..
    //   Updates data store items according to where node was dragged from and dropped
    //   to. The tree will then respond to those data store updates and redraw itself.
    // source: Object: the source which provides items
    // nodes: Array: the list of transferred items
    // copy: Boolean: copy items, if true, move items otherwise

    // Need a reference to the store to call back to its structures.
    var self = this;

    if (this.dndDropParams.perform) {
      this._fadeIn.play();
      var params = this.dndDropParams;

      // Callback for handling a successful load.
      var dataUpdated = function(data) {
        if (data.result && data.result == "success") {
          // reload page (refresh whole tree):
          document.location.href = "tree";
        } else {
          dndError(data.error);
        }
        if (self._fadeIn.status() == "playing") {
          self._fadeIn.stop();
        }
        self._fadeOut.play();
      };
      // Callback for any errors that occur during load.
      var serverError = function() {
        dndError();
        if (self._fadeIn.status() == "playing") {
          self._fadeIn.stop();
        }
        self._fadeOut.play();
      };
      // Update data on the server side
      var url;
      if (params.copy) {
        url = "copyNode";
      } else {
        url = "moveNode";
      }
      var d = dojo.xhrPost({
        url: url,
        content: {
          sourcePath: params.treeStore.getValue(params.childItem, "path"),
          targetPath: params.treeStore.getValue(params.newParentItem, "path")
        },
        handleAs: "json-comment-optional"
      });
      d.addCallback(dataUpdated);
      d.addErrback(serverError);
      // reset params object
      this.dndDropParams = new Object();
    } else if (this.containerState == "Over") {
      this.dndDropParams.tree = this.tree;
      this.dndDropParams.treeModel = this.tree.model
      this.dndDropParams.treeStore = this.tree.model.store;
      this.dndDropParams.target = this.current;
      this.dndDropParams.targetWidget = dijit.getEnclosingWidget(this.dndDropParams.target);
      this.dndDropParams.newParentItem = (this.dndDropParams.targetWidget && this.dndDropParams.targetWidget.item) || this.tree.item;
      this.dndDropParams.childTreeNode = dijit.getEnclosingWidget(nodes[0]);
      this.dndDropParams.childItem = this.dndDropParams.childTreeNode.item;
      this.dndDropParams.oldParentItem = this.dndDropParams.childTreeNode.getParent().item;

      var store = this.tree.model.store;
      var srcType = store.getValue(this._dndSourceNode.item, "type");
      var srcParentType = store.getValue(this._dndSourceNode.getParent().item, "type");
      var trgType = store.getValue(this._dndTargetNode.item, "type");
      var trgTypeName = store.getValue(this._dndTargetNode.item, "path").split("/")[1];

      // if drag file/directory to directory, move reference and move file/directory on FS
      if ((srcType == 'org.weceem.files.ContentFile' || srcType == 'org.weceem.files.ContentDirectory')
          && (trgType == "org.weceem.files.ContentFile" || trgType == "org.weceem.files.ContentDirectory")) {
        dijit.byId("moveReferenceDialog").show();
      // if drag files/directory from "Files" node to any other Content type, create new reference
      } else if ((srcType == 'org.weceem.files.ContentFile' || srcType == 'org.weceem.files.ContentDirectory')
          && (srcParentType == 'org.weceem.files.ContentFile' || srcParentType == 'org.weceem.files.ContentDirectory' || srcParentType == 'contentType')
          && (trgType != "org.weceem.files.ContentFile" && trgType != "org.weceem.files.ContentDirectory" && trgType != "contentType")) {
        dijit.byId('copyReferenceDialog').show();
      // if drag file/directory into "Files" node, remove reference and move file/directory on FS
      } else if ((srcType == 'org.weceem.files.ContentFile' || srcType == 'org.weceem.files.ContentDirectory')
          && trgType == "contentType" && trgTypeName == "Files") {
        dijit.byId("moveReferenceDialog").show();
      } else if (this.dndDropParams.oldParentItem.path[0].split("/").length == 2) {
        // if drag from root to any other node
        dijit.byId('newReferenceDialog').show();
      } else if (this.dndDropParams.newParentItem.path[0].split("/").length == 2) {
        // if make as root
        dijit.byId('deleteReferenceDialog').show();
      } else {
        dijit.byId('performDndDialog').show();
      }
    }
    this.onDndCancel();
  },

  onDndCancel: function() {
    // summary:
    //    Topic event processor for /dnd/cancel, called to cancel the DnD operation.

    this._dndSourceNode = null;
    this._dndTargetNode = null;
    this._accepted = false;
    this.inherited(arguments);
  }
});
