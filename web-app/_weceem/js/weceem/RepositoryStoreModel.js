dojo.provide("weceem.RepositoryStoreModel");

dojo.require("dijit.Tree");

dojo.declare("weceem.RepositoryStoreModel", dijit.tree.TreeStoreModel, {
  // summary
  //    Extends dijit.tree.TreeStoreModel to provide lazy loading functionality
  //    for tree nodes.
  //
  // author
  //    Sergei Shushkevich


  // loadChildrenUrl: String
  //    URL for loading node's children items.
  loadChildrenUrl: "",


  mayHaveChildren: function(/*dojo.data.Item*/ item) {
    // summary
    //    Tells if an item has or may have children.

    return (this.store.getValue(item, "path") == "$root$"
            || this.store.getValue(item, "hasChildren") == true);
  },

  getChildren: function(/*dojo.data.Item*/ parentItem, /*function(items)*/ callback, /*function*/ onError) {
    // summary
    //    Lazy loads children from "loadChildrenUrl" and calls onComplete()
    //    with array of child items of given parent item.

    if (parentItem.children) {
      callback(parentItem.children);
    } else if (this.mayHaveChildren(parentItem)) {
      var self = this;
      var parent = parentItem;
      var defaultCallback = callback;

      var gotData = function(data) {
        for (var i = 0; i < data.length; i++) {
          self.newItem(data[i], parent);
        }
        defaultCallback(parent.children);
      };

      dojo.xhrPost({
        url: this.loadChildrenUrl,
        content: {
          contentPath: this.store.getValue(parentItem, "path")
        },
        handleAs: "json-comment-optional",
        load: gotData,
        error: onError
      });

    } else {
      this.inherited(arguments);
    }
  }
});
