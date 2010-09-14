//variable neede for search
var cacheParams = {};

//variable for the item being dragged
var currentDraggedItem = null;
var currentDropTarget = null;

var insertMarkerHalfHeight;
var treeTableOffset;

var expandTimeout = 1000;
var currentExpandTimerId;
var currentDropRefNode;
var currentDropMode; 

var rootNodeIndent = 10;
var nodeIndent = 30;
var DIALOG_WIDTH = '500px';

//variable to detect time when key was pressed in search box
var timeKeyPressed = null;


function sortByField(fieldname){
    cacheParams["isAsc"] = !cacheParams["isAsc"];
    cacheParams["sortField"] = fieldname;
    sendSearchRequest(cacheParams);
    $("#searchDiv>div>table>thead>tr>th").attr("class", (cacheParams["isAsc"] ? "asc" : "desc"));
}


function performSearch(){
    cacheParams["data"] = $("#data")[0].value;
    cacheParams["space"] = $('#spaceSelector').val();
    cacheParams["classFilter"] = ($("#advSearch").css("display") == "none" ? "none" : $("#classFilter")[0].value);
    cacheParams["fieldFilter"] = $("#fieldFilter")[0].value;
    cacheParams["fromDateFilter"] = $("#fromDate")[0].value;
    cacheParams["toDateFilter"] = $("#toDate")[0].value;
    cacheParams["statusFilter"] = $("#statusFilter")[0].value;
    sendSearchRequest(cacheParams);
}
	
function checkPerformSearch(){
    var currTime = new Date()
    if ((currTime.getTime() - timeKeyPressed.getTime()) > 1000){
        performSearch();
        timeKeyPressed = null;
    }else{
        setTimeout("checkPerformSearch()", 1100);
    }
}

function sendSearchRequest(searchParams){
    $("#treeDiv").css("display", "none");
    $("#searchDiv").css("display", "");
    $("#searchDiv > div > table > tbody").html("");
    $.post(resources["search.request"],
        searchParams,
        function(data){
            var response = data;
            var tr = $("<tr>");
            var td = $("<td>");
            for (i in response.result){
                var obj = response.result[i];
                var body = $("#searchDiv > div > table > tbody")
                var newTr = tr.clone();
                newTr.attr("id", "content-node-" + obj.id)
                var pageTd = td.clone();
                var statusTd = td.clone();
                var createTd = td.clone();
                var changeTd = td.clone();
                pageTd.html("<div class='item'><div class='ui-content-icon' style='display: inline-block'><img src='"+obj.iconHref+"'/></div>" + 
                "<h2 class='title'>" + "<a href=" + obj.href + ">" + obj.title + 
                "&nbsp;<span class='type'>(" + obj.aliasURI + " - " + obj.type + ")</span></a></h2>" + 
                "<div >Parent: "
                    + obj.parentURI + "</div></div>");
                statusTd.text(resources[obj.status]);
                createTd.text(obj.createdBy);
                changeTd.text(obj.changedOn);
                newTr.append(pageTd); newTr.append(statusTd);
                newTr.append(createTd); newTr.append(changeTd);
                body.append(newTr);
            }
            $('#advSearch').show('slow');
        });
}

function updateExpanders(){
    var parents = $("#treeTable>tbody>tr.parent"); //get all parents
    var leaves = $("#treeTable>tbody>tr:not(.parent,.inserter-before,.inserter-after)"); //get all leaves
    parents.each(function (id, it){
        if ($("#"+it.id+">td>span.expander").size() == 0){
            var expander ;
            if ($("span.expander").size() > 0){
                expander = $($("span.expander")[0]).clone();
            }else{
                expander = $('<span class="expander"/>');
            }
            expander.click(function (){
                $("#"+it.id).toggleBranch();
            })
            $("#"+it.id+">td:first").prepend(expander);
        }
        var id = /\d+/.exec(it.id);
        if ($(".child-of-content-node-"+id).size() == 0){
            $("#"+it.id).removeClass("parent");
            $("#"+it.id+">td:first>span.expander").remove();
        }
    });
    leaves.each(function (id, it){
        if ($("#"+it.id+">td>span.expander").size() > 0){
            $("#"+it.id+">td>span.expander").remove();
        }
    })
}

function loadPage(url) {
    window.location.reload(true);
    window.location.href = url
}

function changeSpace() {
    var sel = $('#spaceSelector').get(0)
    var spacename = sel.options[sel.selectedIndex].text
    loadPage(resources["link.treetable"] + "?space=" + spacename)
}

/**
 * Get the node ids of all currently selected nodes
 */
function getSelectedNodeIds() {
    var nodeIds = $.map( $('tr.selected'), function (item, idx) {
        var idattr = $(item).attr('id')
        var id = /\d+/.exec(idattr)
        return id
    })
    return nodeIds
}

function hideInserter() {
    var mrk = $('#insert-marker');
    mrk.queue('concurrentfx', function() { 
        mrk.fadeOut( function() {
            mrk.hide();
            $('div.table').removeClass('crop-x');
        });
        mrk.dequeue('concurrentfx');
    });
    mrk.dequeue('concurrentfx');
}

function viewSelected() {
    var nodes = getSelectedNodeIds()
    if (nodes.length == 0) {
        errorMessage('You must select a node first')
        return
    }
    var node = nodes[0]
    window.open(resources["link.preview"]+"/"+node)
}

function deleteSelected() {
    var nodes = getSelectedNodeIds()
    if (nodes.length == 0) {
        errorMessage('You must select a node first')
        return
    }
    var node = nodes[0]
    var title = $('#content-node-'+node+' h2.title').text() 
    $('#deleteContentNodeTitle').text(title)
	$('#deleteDialog').dialog('open')
}

function moveToSelected() {
    errorMessage('Moving to another space is not implemented yet')
}

function duplicateSelected() {
    errorMessage('Duplicate is not implemented yet')
}

function errorMessage(str) {
    $('#errorDialogMessage').text(str);
    $('#errorDialog').dialog({ 
        buttons: { "Ok": function() { $(this).dialog("close"); } },
	    width: DIALOG_WIDTH,
        modal: true 
    });
}

/*
function toggleStyle(element, neighbour){
    var reg = new RegExp("child-of-content-node-\\d+");
    var parentClass = null;
    if ($(neighbour).attr('class').match(reg) != null){
        parentClass = neighbour.attr('class').match(reg)[0];
    }
    if ($(element).attr('class').match(reg) != null){
        element.removeClass(element.attr('class').match(reg)[0]);
    }
    if (parentClass != null){
        element.addClass(parentClass);
    }
    var newstyle = $("#" + $(neighbour).attr('id') + " > td:first").attr('style');
    if (newstyle == null) newstyle = "";
    $("#" + $(element).attr('id') + " > td:first").attr("style", newstyle);
    return parentClass != null ? /\d+/.exec(parentClass)[0] : null;
}
*/

function getParentId(element){
    var reg = new RegExp("child-of-content-node-\\d+");
    var elem = $(element).first();
    var matcher = $(elem).attr('class').match(reg)
    if (matcher != null){
        return /\d+/.exec(matcher[0])[0];
    }else{
        return null;
    }
}

function getDecId(htmlid){
    return /\d+/.exec(htmlid)[0];
}

function debug(s) {
    //console.log(s);
}

function showInserterAfter(row, indentedLikeItem) {
    var item = $('div.item', row);
    if (!indentedLikeItem || (indentedLikeItem.length == 0)) {
        indentedLikeItem = row;
    }
    var itemForIndent = $('div.item', indentedLikeItem);
    var indentItemPos = itemForIndent.position();
    var pos = item.parent().position();
    var top = pos.top + row.outerHeight(true) - insertMarkerHalfHeight;
    revealInserter(indentItemPos.left, top);
}

function showInserterBefore(row) {
    var item = $('div.item', row);
    var indentItemPos = item.position();
    var pos = item.parent().position();
    var top = pos.top - insertMarkerHalfHeight;
    revealInserter(indentItemPos.left, top);
}

function showInserterAsChildOf(row) {
    var item = $('div.item', row);
    var itempos = item.position();
    var pos = item.parent().position();
    var top = pos.top + row.outerHeight(true) - insertMarkerHalfHeight;
    var l = itempos.left + nodeIndent;
    revealInserter(l, top);
}

function revealInserter(left, top) {
    var mrk = $('#insert-marker');
    var curPos = mrk.offset();
    $('div.table').addClass('crop-x');
    if ((left != curPos.left) || (top != curPos.top)) {
        if (mrk.is(':visible')) {
            // We might have an in-progress move animation already
            mrk.clearQueue();
            mrk.dequeue();
            // Now add the new one
            mrk.animate( {'left':left, 'top':top}, { duration:50 });
        } else {
            mrk.css( {'left':left, 'top':top});
            mrk.queue('concurrentfx', function() { 
                mrk.fadeIn();
                mrk.dequeue('concurrentfx');
            });
            mrk.dequeue('concurrentfx');
        }
    }
}

function inserterAtPrecedingSibling(targetItem) {
    setCurrentDropPoint(targetItem, "before"); // this is effectively a NOP
    showInserterBefore(targetItem);
    debug("Insert before");
}

function getPreviousSiblingRow(targetItem) {
    var tgt = targetItem;
    var childclassmatch = /child\-of\-content\-node\-\d+/.exec(tgt.attr('class'));
    debug('childclassmatch: '+childclassmatch);
    if (childclassmatch != null) {
        tgt = tgt.prevAll('.'+childclassmatch[0]+':first:visible');
    } else {
        tgt = tgt.prevAll('tr.datarow:not([class*=child-of-content-node-]):first:visible');
    }
    // Default to row before current if no smart matching
    if (!tgt.length) {
        tgt = targetItem.prev('tr.datarow:visible');
    }    
    debug('Previous sibling is: '+tgt+', '+tgt.attr('id'));
    return tgt;
}

function getNextSiblingRow(targetItem) {
    var tgt = targetItem;
    var childclassmatch = /child\-of\-content\-node\-\d+/.exec(tgt.attr('class'));
    debug('childclassmatch: '+childclassmatch);
    if (childclassmatch != null) {
        tgt = tgt.nextAll('.'+childclassmatch[0]+':first');
    } else {
        tgt = tgt.nextAll('tr.datarow:not([class*=child-of-content-node-]):first:visible');
    }
    debug('Next sibling is: '+tgt+', '+tgt.attr('id'));
    return tgt;
}

function inserterAtFollowingSibling(targetItem) {
    var childClass = 'child-of-'+targetItem.attr('id')
    var hasChildren = targetItem.nextAll('tr.'+childClass+":first:visible");
    // If we have children we need to skip them (can't insert self into child list)
    if (hasChildren.length > 0) {
        var tgt = getNextSiblingRow(targetItem);
        // If there is no next sibling, pick the next non-child row whatever, it will be 
        // a sibling of the parent
        debug("following sib: "+tgt.length);
        var indentTarget
        if (!tgt.length) {
            tgt = targetItem.nextAll('tr.datarow:not(.'+childClass+'):first:visible');
            indentTarget = targetItem;
            debug("following sib non-child: "+tgt.length);
        }    

        // If we are the last row in the table...
        if (!tgt.length) {
            debug("following sib using after self");
            tgt = targetItem.nextAll('tr.'+childClass+":last:visible");

            setCurrentDropPoint(targetItem, "after");

            // Indent to targetItem's indent level
            showInserterAfter(tgt, $(indentTarget));
        } else {
            debug("following sib using before: "+tgt);

            setCurrentDropPoint(tgt, "before");

            showInserterBefore($(tgt));
        }
    } else {
        // We have no children so it will go after us

        setCurrentDropPoint(targetItem, "after");

        showInserterAfter($(targetItem));
    }
    debug("Insert after");
}

function inserterAsChild(targetItem) {
    setCurrentDropPoint(targetItem, "child");
    showInserterAsChildOf(targetItem);
    debug("Insert as child");
}

function setCurrentDropPoint(row, mode) {
    if ((currentDropRefNode != row) || 
        (currentDropMode != mode)) {
        clearExpandTimer();   
    }
    currentDropRefNode = row;
    currentDropMode = mode; 
}

function showDropInsertionPoint(targetItem, ui) {
    if (currentDropTarget && currentDropTarget != currentDraggedItem) {
        debug("Drop target: "+$(currentDropTarget).attr('id'));
        
        var dropOffset = currentDropTarget.position();
        var mouseLeft = ui.position.left; 
        var mouseY = ui.position.top; 
        
        // Workaround for firefox not setting offsetParent of helper correct
        if (ui.helper.offsetParent().is('body')) {
            mouseLeft -= ui.helper.parent().offset().left;
            mouseY -= ui.helper.parent().offset().top;
        }
        
        debug("Calc'd pos: "+mouseLeft+', '+mouseY);
        debug("Pos: "+ui.position.left+', '+ui.position.top);
        debug("Ofs: "+ui.offset.left+', '+ui.offset.top);
        debug("helper: "+ui.helper);
        debug("helper pos L: "+$(ui.helper).position().left);
        debug("helper ofs L: "+$(ui.helper).offset().left);
        debug("helper parent: "+$(ui.helper).parent().attr('id'));
        debug("helper ofs parent: "+$(ui.helper).offsetParent().nodeName);
        var targetItemMidPoint = dropOffset.top + ($(currentDropTarget).height() >> 1);
        var targetItemLeft = $('div.item', currentDropTarget).position().left;
        debug("targetItemLeft: "+targetItemLeft+", itemtop "+dropOffset.top+", Midp: "+targetItemMidPoint);
        var isIndented = mouseLeft >= targetItemLeft+nodeIndent;
        var isAboveMidPoint = mouseY + (ui.helper.height()>>1) < targetItemMidPoint;
        // Select previous or next sibling at this parentage level
        if (isAboveMidPoint && !isIndented) {
            // This is really a NOP. Hmm.
            inserterAtPrecedingSibling(currentDropTarget);
        } else {
            if (!isIndented) {
                inserterAtFollowingSibling(currentDropTarget);
            } else {
                // @todo We have to see how indented they are, and based on the indent of previous
                // row allow them to drop it at any supported indent level
                inserterAsChild(currentDropTarget);
            }
        }
    }
}

function confirmDragDropOperation(opts) {
    var dlg = $('#confirmDialog');
    dlg.data('switch', opts['switch']);
    dlg.data('source', opts.source);
    dlg.data('target', opts.target);
    dlg.dialog('open');
}

function performDrop(e, ui) {
    clearExpandTimer(); // make sure elems don't expand while we wait
    var src = currentDraggedItem.parents('.datarow:first');
    if (currentDropRefNode) {
        if ((currentDropMode == 'after') || (currentDropMode == 'before')) {
            confirmDragDropOperation( {'switch':currentDropMode, 'source':src, 'target':currentDropRefNode} )
        } else {
            var type = $('h2.title',currentDropRefNode).attr("type");
            if (resources["haveChildren"][type]){
                confirmDragDropOperation( {'switch':currentDropMode, 'source':src, 'target':currentDropRefNode} )
            }
        }
    }
}

function highlightChangedRow(item) {
    setSelection(item, true);
}

function setSelection(item, animate) {
    clearSelection();
    item.addClass('selected', animate ? 500 : 0);
}

function clearSelection() {
    var rowNodes = $('tr[id*=content-node-]')
    $('tr[id*=content-node-]').removeClass('selected');
}

function startDrag(e, ui) {
    currentDropTarget = null
    currentDraggedItem = $(e);
    setSelection(currentDraggedItem)
}

function stopDrag(e, ui) {
}

function cancelDrag() {
    
}

function dragHovering(elem, ui) {
    showDropInsertionPoint(elem, ui);
    
    // Make the droppable branch expand when a draggable node is moved over it AND only if it has children
    if ( currentDropTarget && 
            (currentDropTarget != currentDraggedItem) && 
            !currentDropTarget.is('.expanded') &&
            (currentDropMode == 'child')) {
        // set timer to open it
        // we don't do auto-expand 
        setTimerToExpand(currentDropTarget);    
    }
}

function clearExpandTimer() {
    clearTimeout(currentExpandTimerId);
    currentExpandTimerId = null;
}

function expandRow(row) {
    row.expand();
    // Now animate the inserter again at the correct place
}

function setTimerToExpand(tgt) {
    clearExpandTimer();
    currentExpandTimerId = setTimeout("expandRow($('#"+tgt.attr('id')+"'));", expandTimeout);
}

var draggableConf = {
    helper: 'clone',
    appendTo: '#treeDiv div.table table', 
    opacity: .75,
    refreshPositions: true,
    revert: "invalid",
    revertDuration: 300,
    distance: 3,
    scroll: true,
    zIndex: 1000,
    handle: 'div.ui-content-icon',
    drag: function(e, ui){
        dragHovering(this, ui);
    },
    start: function(e, ui){
        startDrag(this, ui);
    },
    stop: function(e, ui){
        stopDrag();
    }
    
}

var droppableConf = {
    accept: "div.item",
    drop: function(e, ui) { 
        performDrop(e, ui);
    },
    over: function(e, ui) {
        currentDropTarget = $(this);
    }
}

function removeNode(id) {
    $('#content-node-'+id).slideUp('slow', function () { 
        $(this).remove() 
        $('.child-of-content-node-'+id).remove()
    })
}

/*--------------------------------------------*/
function initTreeTable() {
    insertMarkerHalfHeight = parseInt($('#insert-marker img').css('height')) >> 1;
    var treeTable = $('#treeTable');
    treeTableOffset = {
        left:treeTable.outerWidth()-treeTable.width(),
        top:treeTable.outerHeight()-treeTable.height()
    };
    
    // Insert branch into specific position: 
    // target - place to insert item after or before
    // place - can be 'after' or 'before' or 'child'
    $.fn.insertBranchTo = function(target, place){
        var node = $(this);
        var id = node.attr("id");
        var nodeid = getDecId(id);
        var trgid = getDecId($(target).attr("id"));
        if (place == "before")
            node.insertBranchBefore(target);
        else
        if (place == "after")
            node.insertBranchAfter(target);
        else
        if (place == "child"){
            node.appendBranchTo($(target));
            $(target).addClass('parent'); // So we can easily update expanders
            return;
        }
    }

    // Handle selection of rows with click
    $('tr[id*=content-node-]').live('click', function(){
            var clickedNode = $(this)
            var wasSel = clickedNode.hasClass('selected')
            clearSelection();
            if (!wasSel) {
                setSelection(clickedNode);
            }
    });
    
    $("#data").keypress(function(e){
        if (timeKeyPressed == null){
            timeKeyPressed = new Date();
            checkPerformSearch();
        }else{
            timeKeyPressed = new Date();
        }
    });
    
    
  	$("#treeTable").treeTable({indent: nodeIndent});
    
    $('div.item').draggable(draggableConf);
    
    $('#treeTable tr.datarow').droppable(droppableConf);
    
	$('.ui-icon-info').click( function() { 
		var icon = $(this)
		var p = icon.parent()
		var i = icon.text()
		var dlg = $('#infoDialog'+i)
		dlg.dialog('open')
	})
	$('#deleteDialog').dialog( {
		autoOpen: false, 
	    width: DIALOG_WIDTH,
		buttons: { 
			Yes: function () { 
			    $(this).dialog('close'); 
			    var nodeId = getSelectedNodeIds()[0]
                $.post(resources["link.deletenode"], 
                    {id: nodeId},
                    function(data) {
            		    if (data) {
            		        if (data.status == 403) {
            		            $('#expiredDialog').dialog('open');
            		        } else
        		            if (data.result != 'success') {
        		                errorMessage("Delete failed: "+data.error)
        		            } else {
        		                removeNode(nodeId)
        		            }
        		        }
                    }, 'json')
			}, 
			No: function () { 
			    $(this).dialog('close') 
			} 
		}
	})
	$('.ui-icon-circle-minus').click( function() { 
        deleteSelected()
	})
	
	$('#createNewDialog').dialog({
	    autoOpen: false,
	    modal: true,
	    width: DIALOG_WIDTH,
	    buttons: {
	        Create : function () {
	            var parentid = getSelectedNodeIds()
	            $("#parentid").attr("value", parentid)
	            $('#createNewDialog form').submit()
	            $(this).dialog('close')
	        },
	        Cancel : function () {
	            $(this).dialog('close')
	        }
	    }
	})
	
	$('button.createNew').click( function() {
	    $('#createNewDialog').dialog('open')
	})
	
    var moreActionsMenu = $('#moreActionsMenu').html()
    
	$('.moreActions').each( function () {
	    var button = $(this)
	    $(this).menu({
		    content: moreActionsMenu,		
		    flyOut: true,
		    itemSelected: function(node) {
		        if ($(node).hasClass('deleteAction')) {
		            deleteSelected()
		        } else if ($(node).hasClass('viewAction')) {
		            viewSelected()
		        } else if ($(node).hasClass('moveToSpaceAction')) {
		            moveToSelected()
		        } else if ($(node).hasClass('duplicateAction')) {
		            duplicateSelected()
		        }
                $(button).click()
		    }
	    })
	})
	
	$("#expiredDialog").dialog({
        autoOpen: false,
        modal: true,
	    width: DIALOG_WIDTH,
        buttons: { "Ok" : function(){$(this).dialog('close');}}
    });
    
    function getInsertIndex(target, position){
        var trgid = getDecId($(target).attr('id'));
        var index;
        switch (position){
            case "before":
                index = $("#content-node-" + trgid + ">td:first>div>h2.title").attr("orderindex");
                break;
            case "after":
                if (!$("#"+$(target).attr('id')+"+tr").size()) {
                    index = parseInt($("#content-node-" + trgid + ">td:first>div>h2.title").
                    attr("orderindex")) + 1;
                }else{
                    var nextid = getDecId($("#"+$(target).attr('id')+"+tr").attr('id'))
                    index = $("#content-node-"+nextid+">td:first>div>h2.title").attr("orderindex");
                }
                break;
            case "child":
                var children = $(".child-of-content-node-"+trgid+"[id*=content-node-]>td:first>div>h2.title")
                index = 0;
                jQuery.each(children, function(index, value){
                    if ($(value).attr('orderindex') >= index){
                        index = $(value).attr('orderindex');
                    }
                });
                break;
        }
        return index;
    }
    
	$('#confirmDialog').dialog({
	    autoOpen: false,
	    modal: true,
	    width: DIALOG_WIDTH,
	    close: function () {
	        hideInserter();
	    },
	    buttons: {
	        "Cancel" : function() {
	            $(this).dialog('close');
            },
	        "Move" : function(){
	            var swc = $(this).data('switch');
	            var src = $(this).data('source');
	            var trg = $(this).data('target');
	            var parentId = getParentId(trg);
	            var index = getInsertIndex(trg, swc);
                var tid = (swc == "child") ? getDecId($(trg).attr('id')) : (parentId == null ? -1 : parentId)
                $.post(resources["link.movenode"],
                    {sourceId: getDecId($(src).attr('id')), targetId: tid, index: index},
                    function (data){
                        var response = data;
                        if (response['status'] == 403){
    	                    $('#expiredDialog').dialog('open');
    	                    return ;
        	            }
                        hideInserter();
    	                if (response['result'] == "failure"){
    	                    errorMessage(response['error']);
    	                }else{
    	                    $(src).insertBranchTo(trg, swc);
                            var indexes = response['indexes'];
                            jQuery.each(indexes, function(key, val){
                               $("#content-node-" + key + ">td:first>div>h2.title").attr('orderindex', val);
                            });
                            updateExpanders();
                            currentDropRefNode.expand();
                            highlightChangedRow(src);
    	                }
	                }
                );
	            $(this).dialog('close');
	        },
	        "Virtual Copy" : function(){
	            var swc = $(this).data('switch');
	            var src = $(this).data('source');
	            var trg = $(this).data('target');
	            var parentId = getParentId(trg);
	            var index = getInsertIndex(trg, swc);
	            var tid = (swc == "child") ? getDecId($(trg).attr('id')) : (parentId == null ? -1 : parentId)
	            var inserterAfter = $("#inserter-after-" + getDecId($(src).attr('id'))[0]).clone(); 
	            inserterAfter.appendTo($("#treeTable>tbody"));
                var inserterBefore = $("#inserter-before-" + getDecId($(src).attr('id'))[0]).clone();
                inserterBefore.appendTo($("#treeTable>tbody"));
                inserterAfter.droppable(droppableConf);
                inserterBefore.droppable(droppableConf);
                $.post(resources["link.copynode"],
        	            {sourceId: getDecId($(src).attr('id')), targetId: tid, index: index},
        	            function (data){
        	                var response = data;
        	                if (response['status'] == 403){
        	                    $('#expiredDialog').dialog('open');
        	                    return ;
        	                }
                            hideInserter();
        	                if (response['result'] == "failure"){
        	                    errorMessage(response['error']);
        	                }else{
        	                    var srcCopy = $(src).clone(); 
        	                    srcCopy.appendTo($("#treeTable>tbody"));
        	                    $(srcCopy).attr('id', 'content-node-' + response['id']);
        	                    $(srcCopy).insertBranchTo(trg, swc);
        	                    $('#' + srcCopy.attr('id') + '>td>div>div.ui-content-icon').draggable(draggableConf);
        	                    $('#' + srcCopy.attr('id') + '>td>div>h2.title').attr('type', response['ctype']);
        	                    srcCopy.droppable(droppableConf);
        	                    $('#' + srcCopy.attr('id') + '>td>div>h2>a>span.type').html(' (Virtual Content)');
                                var indexes = response['indexes'];
                                jQuery.each(indexes, function(key, val){
                                   $("#content-node-" + key + ">td:first>div>h2.title").attr('orderindex', val);
                                });
        	                    updateExpanders();
                                currentDropRefNode.expand();
                                highlightChangedRow(srcCopy);
        	                }
        	            });
    	        $(this).dialog('close');
	        }
	    }
	});
	
	
// Search initialization
    $("#fromDate").datepicker();
    $("#toDate").datepicker();
    $("#search_btn").click(function(){performSearch()});
    $("#clear_btn").click(function(){
        cacheParams["sortField"] = "title";
        cacheParams["isAsc"] = true;
        $("#treeDiv").css("display", "");
        $("#searchDiv").css("display", "none");
        $("#data")[0].value = "";
        $("#fromDate")[0].value = "";
        $("#toDate")[0].value = "";
        $('#advSearch').hide('slow');
        $("#searchDiv > div > table > tbody").html("");
    });

}

