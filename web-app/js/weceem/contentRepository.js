// icons classes for tree nodes
function getIcon(item, opened) {
    if (item) {
        var type = dijit.byId("tree").model.store.getValue(item, "type").toString();
        if (type == "org.weceem.content.Block") {
            return "blockLeaf";
        } else if (type == "org.weceem.blog.BlogEntry") {
            return "blogLeaf";
        } else if (type == "org.weceem.forum.ForumEntry") {
            return "forumLeaf";
        } else if (type == "org.weceem.html.HTMLContent") {
            return "htmlLeaf";
        } else if (type == "org.weceem.css.StyleSheet") {
            return "stylesheetLeaf";
        } else if (type == "org.weceem.wiki.WikiItem") {
            return "wikiLeaf";
        } else if (type == "org.weceem.content.Template") {
            return "templateLeaf";
        } else if (type != "org.weceem.content.Space" && type != "contentType" && type != "org.weceem.files.ContentDirectory") {
            return "defaultLeaf";
        }
    }
    return opened ? "dijitFolderOpened" : "dijitFolderClosed";
}

// load Details for selected Content
function loadDetails(callback) {
    hideDetails();
    if (selectedContentPath && selectedContentPath.split("/").length >= 3) {
        dojo.byId("loading").style.visibility = "visible";
        dojo.xhrPost({
            url: "loadNodeDetails",
            content: {
                contentPath: selectedContentPath
            },
            handleAs: "text",
            load: callback
        });
    }       
}

// display Details block(s)
function showDetails(response) {
    var contentTable = dojo.byId("contentDetails")
    contentTable.innerHTML = "<table class='details'><tbody>"+response+"</tbody></table>"
    contentTable.style.display = ""
    dojo.byId("loading").style.display = "none";
}

// hide Details block(s)
function hideDetails() {
    document.getElementById("contentDetails").style.display = "none";
    document.getElementById("relatedContent").style.display = "none";
    document.getElementById("recentChanges").style.display = "none";
}

// execute DnD
function performDndOperation(copy, dialogId) {
    dijit.byId(dialogId).hide()
    var tree = dijit.byId("tree");
    var dndSource = tree.dndController;
    dndSource.dndDropParams.perform = true;
    dndSource.dndDropParams.copy = copy;
    dndSource.onDndDrop();
}

// load RelatedContent for selected Content
function loadRelatedContent() {
    if (selectedContentPath) {
        dojo.xhrPost({
            url: "loadRelatedContentDetails",
            content: {
                contentPath: selectedContentPath
            },
            handleAs: "text",
            load: function(data) {
                document.getElementById("relatedContent").innerHTML = data;
                document.getElementById("relatedContent").style.display = "";
            }
        });
    }
}

// load RecentChanges for selected Content
function loadRecentChanges() {
    if (selectedContentPath) {
        dojo.xhrPost({
            url: "loadRecentChangesDetails",
            content: {
                contentPath: selectedContentPath
            },
            handleAs: "text",
            load: function(data) {
                document.getElementById("recentChanges").innerHTML = data;
                document.getElementById("recentChanges").style.display = "";
            }
        });
    }
}

// display InsertContent dialog
function showInsertContentDialog() {
    // reset dialog's fields
    dojo.byId("parentPathField").value = "";
    dojo.byId("parentField").innerHTML = "";
    dojo.byId("parentFieldRow").style.display = "none";
    if (selectedItem) {
        var store = dijit.byId("tree").model.store;
        var contentPath = new String(store.getValue(selectedItem, "path"));
        var attributes = contentPath.split("/");
        dijit.byId("spaceField").setValue(attributes[0]);
        if (attributes.length == 2) {
            var contentType = attributes[1];
            if (contentType == "Files") contentType = "org.weceem.files.ContentFile";
            dijit.byId("contentTypeField").setValue(contentType);
        } else if (attributes.length > 2) {
            dijit.byId("contentTypeField").setValue(store.getValue(selectedItem, "type"));
            if (store.getValue(selectedItem, "canHaveChildren") == true) {
                dojo.byId("parentPathField").value = contentPath;
                dojo.byId("parentField").innerHTML = store.getValue(selectedItem, "label");
                dojo.byId("parentFieldRow").style.display = "";
            }
        }
    }
    dijit.byId('insertContentDialog').show()
}

function insertContent(fields) {
    if (dijit.byId("spaceField").isValid()
            && dijit.byId("contentTypeField").isValid()) {
        if (fields.contentTypeField != 'org.weceem.files.ContentFile'
                && fields.contentTypeField != 'org.weceem.files.ContentDirectory') {
            var uri = "newContent?space.id=" + encodeURI(fields.space)
                    + "&contentType=" + encodeURI(fields.contentTypeField)
                    + "&parentPath=" + encodeURI(dojo.byId("parentPathField").value);
            document.location.href = uri;
        } else {
            if (fields.contentTypeField == 'org.weceem.files.ContentFile') {
                dojo.byId("newFileSpace").value = fields.space;
                dojo.byId("newFileParent").value = dojo.byId("parentPathField").value;
                dijit.byId("uploadFileDialog").show();
            } else {
                dojo.byId("newDirectorySpace").value = fields.space;
                dojo.byId("newDirectoryParent").value = dojo.byId("parentPathField").value;
                dijit.byId("createDirectoryDialog").show();
            }
        }
    } else {
        dijit.byId('insertContentDialog').show()
    }
}

function convertToPDF(isHierarchy) {
    dijit.byId('pdfDialog').hide();
    var url = "pdfView?path=" + encodeURI(selectedItem.path[0]);
    if (isHierarchy) {
        url += "&isHierarchy=true";
    }
    document.location.href = url;
}

function validateChangesForm() {
    var changesForm = document.getElementById("changesForm");
    var inputs = changesForm.getElementsByTagName("input");
    var values = new Array();
    for (var i = 0; i < inputs.length; i++) {
        if (inputs[i].type == "checkbox" && inputs[i].checked) {
            values[values.length] =
            inputs[i].id.substr(inputs[i].id.lastIndexOf('_') + 1);
        }
    }
    if (values.length != 2) {
        alert("You must select 2 versions to compare.");
        return false;
    } else {
        values = values.sort(function(a, b) {
            return a - b
        });
        document.getElementById("fromVersion").value = values[0];
        document.getElementById("toVersion").value = values[1];
        return true;
    }
}

function deleteNode(justReference, confirmationMessage) {
    if (confirm(confirmationMessage)) {
        if (selectedItem) {
            var tree = dijit.byId("tree");
            var store = tree.model.store;
            var fadeIn = tree.dndController._fadeIn;
            var fadeOut = tree.dndController._fadeOut;
            fadeIn.play();

            var itemDeleted = function(data) {
                hideDetails();
                if (fadeIn.status() == "playing") {
                    fadeIn.stop();
                }
                fadeOut.play();
                // reload page (refresh whole tree):
                document.location.href = "tree";
            }
            var serverError = function() {
                alert("Error occurred during node deleting.");
                if (fadeIn.status() == "playing") {
                    fadeIn.stop();
                }
                fadeOut.play();
            }
            var url;
            if (justReference) {
                url = "deleteReference";
            } else {
                url = "deleteNode";
            }
            var d = dojo.xhrPost({
                url: url,
                content: {
                    contentPath: store.getValue(selectedItem, "path")
                },
                handleAs: "json-comment-optional"
            });
            d.addCallback(itemDeleted);
            d.addErrback(serverError);
        }
    }
}

function showDeleteNodeDialog() {
    resetDeleteNodeDialog();
    dojo.byId("deleteNodeContentPath").value = dijit.byId("tree").model.store
            .getValue(selectedItem, "path");
    dijit.byId("deleteNodeDialog").show();

    dojo.xhrPost({
        url: "deleteNodeInfo",
        form: dojo.byId("deleteNodeForm"),
        hadnleAs: "text",
        load: function(response, ioArgs) {
            dojo.byId("deleteNodeDialogMessage").innerHTML = response;
            dojo.byId("deleteNodeDialogLoading").style.display = "none";
            dojo.byId("deleteNodeDialogMessage").style.display = "";
            dojo.byId("deleteNodeDialogButtons").style.display = "";
        }
    });
}

function resetDeleteNodeDialog() {
    dojo.byId("deleteNodeContentPath").value = "";
    dojo.byId("deleteNodeDialogLoading").style.display = "";
    dojo.byId("deleteNodeDialogMessage").style.display = "none";
    dojo.byId("deleteNodeDialogButtons").style.display = "none";
}

function showRenameNodeDialog() {
    resetRenameNodeDialog();
    dojo.byId("renameNodeContentPath").value = dijit.byId("tree").model.store
            .getValue(selectedItem, "path");
    dijit.byId("renameNodeDialog").show();
}

function resetRenameNodeDialog() {
    dojo.byId("renameNodeContentPath").value = "";
    dojo.byId("renameNodeTo").value = "";
}

function dndError(message) {
    var msg = message ? message : "Error occurred during Drag & Drop.";
    alert(msg);
}
