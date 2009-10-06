//variable neede for search
var cacheParams = {};

//variable for mouse Y coordinate
var mouseTop = null;
//variable for hovered item
var hoverItem = null;
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
    cacheParams["space"] = $('#spaceSelector')[0].options[$('#spaceSelector')[0].selectedIndex].text;
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
            var response = eval('(' + data + ')');
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
                pageTd.html("<div class='item'><div class='ui-icon ui-icon-document' style='display: inline-block'></div>" + 
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
                expander = $('<span class="expander" style="margin-left: -25px; padding-left: 25px;" />');
            }
            expander.click(function (){
                $("#"+it.id).toggleBranch();
                resetInserters();
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

function resetInserters(){
    $("tr[class*=inserter]:visible").css({'display': 'none'});
    $("tr[class*=inserter-after]").each(function (index, it){
        var id = getDecId($(it).attr('id'));
        if ($(".child-of-content-node-"+id).size() > 0){
            var style = $(".child-of-content-node-"+id+">td").attr('style');
            $("#inserter-after-"+id+">td").attr('style', style);
        }
    });
}

function viewSelected() {
    var nodes = getSelectedNodeIds()
    if (nodes.length == 0) {
        window.alert('You must select a node first')
        return
    }
    var node = nodes[0]
    window.open(resources["link.preview"]+"/"+node)
}

function deleteSelected() {
    var nodes = getSelectedNodeIds()
    if (nodes.length == 0) {
        window.alert('You must select a node first')
        return
    }
    var node = nodes[0]
    var title = $('#content-node-'+node+' h2.title').text() 
    $('#deleteContentNodeTitle').text(title)
	$('#deleteDialog').dialog('open')
}

function moveToSelected() {
    window.alert('Moving to another space is not implemented yet')
}

function duplicateSelected() {
    window.alert('Duplicate is not implemented yet')
}

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

function getParentId(element){
    var reg = new RegExp("child-of-content-node-\\d+");
    if ($(element).attr('class').match(reg) != null){
        return /\d+/.exec($(element).attr('class').match(reg)[0]);
    }else{
        return null;
    }
}

function getDecId(htmlid){
    return /\d+/.exec(htmlid)[0];
}



var draggableConf = {
        helper: "clone",
        opacity: .75,
        refreshPositions: true, // Performance?
        revert: "invalid",
        revertDuration: 300,
        scroll: true,
        drag: function(e, ui){
            var hoverItemId = getDecId(hoverItem.id);
            var itemTop = hoverItem.offsetTop;
            var itemButtom = itemTop + hoverItem.clientHeight;
            if (!$(hoverItem).is(".inserter-before") && !$(hoverItem).is(".inserter-after"))
            if (mouseTop <= (itemTop + 8) ){
                resetInserters();
                $("#inserter-before-" + hoverItemId).css('display', '');
            }else
            if (mouseTop >= (itemButtom - 8)){
                resetInserters();
                $("#inserter-after-" + hoverItemId).css('display', '');
            }else{
                resetInserters();
                $("#inserter-after-" + hoverItemId).css('display', '');
            }
        },
        start: function(e, ui){
            $(".selected").removeClass('selected');
        },
        stop: function(e, ui){
            resetInserters();
        }
        
    }

var droppableConf = {
        accept: "div.ui-content-icon",
        drop: function(e, ui) { 
            if ((this.id != "") &&
                (this.id != "empty-field") && 
                (getDecId(ui.helper.attr('id')) != getDecId(this.id))
            ) {
                var el = $("#" + this.id);
                if (el.is(".inserter-before") || el.is(".inserter-after")){
                    var pos = el.is('.inserter-after') ? 'after' : 'before'
                    if ((pos == 'after') && 
                        ($("#content-node-"+getDecId(el.attr('id'))).is('.parent'))
                            ){
                        pos = "to";
                        el = $("#content-node-"+getDecId(el.attr('id')));
                    }else{
                        var mainElId = getDecId(el.attr('id'));
                    }
                    var movable = $($(ui.draggable).parents("tr")[0]);
                    // @todo clean this up - slow to keep getting the node!
                    $('#confirmDialog').dialog('option', 'switch', pos);
                    $('#confirmDialog').dialog('option', 'source', movable);
                    $('#confirmDialog').dialog('option', 'target', el);
                    $('#confirmDialog').dialog('open');
                }else{
                    var type = $("#" + this.id + ">td:first>div>h2.title").attr("type");
                    if (resources["haveChildren"][type]){
                        // @todo clean this up - slow to keep getting the node!
                        $('#confirmDialog').dialog('option', 'switch', 'to');
                        $('#confirmDialog').dialog('option', 'source', $(ui.draggable).parents("tr")[0]);
                        $('#confirmDialog').dialog('option', 'target', $(this));
                        $('#confirmDialog').dialog('open');
                    }
                }
            }
            //resetInserters();
        },
        hoverClass: "accept",
        over: function(e, ui) {
          // Make the droppable branch expand when a draggable node is moved over it.
          if(this.id != ui.draggable.parents("tr")[0].id && !$(this).is(".expanded")) {
            $(this).expand();
          }
          hoverItem = $("#" + this.id)[0];
          
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
    // Insert branch into specific position: 
    // target - place to insert item after or before
    // place - can be 'after' or 'before' or 'to'
    $.fn.insertBranchTo = function(target, place){
        var node = $(this);
        var nodeid = getDecId(node.attr("id"));
        var trgid = getDecId($(target).attr("id"));
        if (place == "before")
            $("tr[id$=-"+nodeid+"]").insertBefore(target);
        else
        if (place == "after")
            $("tr[id$=-"+nodeid+"]").insertAfter(target);
        else
        if (place == "to"){
            $.each($("tr[id$=-"+nodeid+"]"), function(index, value){
                $(value).appendBranchTo(target);
                $("#inserter-after-"+trgid).insertAfter(target);
                $(value).removeClass("child-of-undefined").addClass("child-of-content-node-"+trgid);
            });
            $(target).addClass("parent");
            return ;
        }
        $.each($("tr[id$=-"+nodeid+"]"), function(index, value){
            toggleStyle($(value), $(target));
        })
        $.each($(".child-of-content-node-"+nodeid), function(index, value){
            $(value).appendBranchTo(node);
            $(value).removeClass("child-of-undefined").addClass("child-of-content-node-"+nodeid);
        });
        node.css("display", "");
    }
    // Handle selection of rows with click
    $('tr[id*=content-node-]').live('click', function(){
            var clickedNode = $(this)
            var rowNodes = $('tr[id*=content-node-]')
            var wasSel = clickedNode.hasClass('selected')
            $('tr[id*=content-node-]').removeClass('selected');
            if (!wasSel) {
                clickedNode.addClass('selected');
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
    
    
    $().mousemove(function (e){
        mouseTop = e.pageY - $("#treeTable")[0].offsetTop;
    });
  	$("#treeTable").treeTable({indent: 25});
  	$("span.expander").click(function (){resetInserters()});
    resetInserters();
    updateExpanders();
    
    $('div.ui-content-icon').draggable(draggableConf)
    
    $('.title').each(function() {
        $(this).parents("tr").droppable(droppableConf)
        });
    
	$('.ui-icon-info').click( function() { 
		var icon = $(this)
		var p = icon.parent()
		var i = icon.text()
		var dlg = $('#infoDialog'+i)
		dlg.dialog('open')
	})
	$('#deleteDialog').dialog( {
		autoOpen: false, 
		buttons: { 
			Yes: function () { 
			    $(this).dialog('close'); 
			    var nodeId = getSelectedNodeIds()[0]
                $.post(resources["link.deletenode"], 
                    {id: nodeId},
                    function(data) {
            		    if (data) {
            		        if (data.status == 403){
            		            $('#expiredDialog').dialog('open');
            		        } else
        		            if (data.result != 'success') {
        		                window.alert("Delete failed: "+data.error)
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
            case "to":
                var children = $(".child-of-content-node-"+trgid+"[id*=content-node-]>td:first>div>h2.title")
                index = 0;
                jQuery.each(children, function(index, value){
                    if ($(value).attr('orderindex') >= index){
                        index = $(value).attr('orderindex');
                    }
                });
                //index++;
                break;
        }
        return index;
    }
    
	$('#confirmDialog').dialog({
	    autoOpen: false,
	    modal: true,
	    buttons: {
	        "Cancel" : function(){$(this).dialog('close');},
	        "Move" : function(){
	            var swc = $(this).dialog('option', 'switch');
	            var src = $(this).dialog('option', 'source');
	            var trg = $(this).dialog('option', 'target');
	            var parentId = getParentId(trg);
	            var index = getInsertIndex(trg, swc);
                var tid = (swc == "to") ? getDecId($(trg).attr('id')) : (parentId == null ? -1 : parentId)
                $.post(resources["link.movenode"],
                    {sourceId: getDecId($(src).attr('id')), targetId: tid, index: index},
                    function (data){
                        var response = eval('(' + data + ')');
                        if (response['status'] == 403){
    	                    $('#expiredDialog').dialog('open');
    	                    return ;
        	            }
    	                if (response['result'] == "failure"){
    	                    alert(response['error']);
    	                }else{
    	                    $(src).insertBranchTo(trg, swc);
                            var indexes = response['indexes'];
                            jQuery.each(indexes, function(key, val){
                               $("#content-node-" + key + ">td:first>div>h2.title").attr('orderindex', val);
                            });
                            updateExpanders();
                            resetInserters();
    	                }
	                }
                );
	            $(this).dialog('close');
	        },
	        "Virtual Copy" : function(){
	            var swc = $(this).dialog('option', 'switch');
	            var src = $(this).dialog('option', 'source');
	            var trg = $(this).dialog('option', 'target');
	            var parentId = getParentId(trg);
	            var index = getInsertIndex(trg, swc);
	            var tid = (swc == "to") ? getDecId($(trg).attr('id')) : (parentId == null ? -1 : parentId)
	            var inserterAfter = $("#inserter-after-" + getDecId($(src).attr('id'))[0]).clone(); 
	            inserterAfter.appendTo($("#treeTable>tbody"));
                var inserterBefore = $("#inserter-before-" + getDecId($(src).attr('id'))[0]).clone();
                inserterBefore.appendTo($("#treeTable>tbody"));
                inserterAfter.droppable(droppableConf);
                inserterBefore.droppable(droppableConf);
                $.post(resources["link.copynode"],
        	            {sourceId: getDecId($(src).attr('id')), targetId: tid, index: index},
        	            function (data){
        	                var response = eval('(' + data + ')');
        	                if (response['status'] == 403){
        	                    $('#expiredDialog').dialog('open');
        	                    return ;
        	                }
        	                if (response['result'] == "failure"){
        	                    alert(response['error']);
        	                }else{
        	                    var srcCopy = $(src).clone(); srcCopy.appendTo($("#treeTable>tbody"));
        	                    $(inserterAfter).attr('id', 'inserter-after-' + response['id']);
        	                    $(inserterBefore).attr('id', 'inserter-before-' + response['id']);
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
        	                    resetInserters();
        	                    updateExpanders();
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

