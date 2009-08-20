<html>
<head >
	<meta name="layout" content="admin"/>
	<g:javascript src="treeTable/javascripts/jquery.treeTable.js"/>
	<g:javascript src="treeTable/javascripts/core.treeTable.js"/>
	<link href="${createLinkTo(dir:pluginContextPath + '/js/treeTable/stylesheets', file:'jquery.treeTable.css')}" rel="stylesheet" type="text/css" />

<style type="text/css">
	td span.ui-icon { display: inline;}
	th {text-align: left}
	span.ui-icon-circle-triangle-e { float: left; margin-left: -2.6em; }
	.nodeinfo { font-size: 85%;}
	.nodeinfoDialog { display:none;}
	span.ui-icon-info { float: left; margin-right: 0.8em; }
	span.ui-icon-circle-plus { float: left; margin-right: 0.5em; }
	span.ui-icon-circle-minus { float: left; }
	a:hover { text-decoration: underline;}
	table.treeTable tr:hover {background-color: #9EB3BF}
	ul.childList { font-size: 1em}
	.ui-state-highlight { height: 1.5em; line-height: 1.2em; }

    h2.title + span.type { margin-left: 10px;}
</style>
<script type="text/javascript">

var resources = {};

function init(){
    var haveChildren = {};
    <g:each var="data" in="${haveChildren}">
      haveChildren["${data.key}"] = ${data.value};
    </g:each>
    resources["haveChildren"] = haveChildren;
    resources["content.button.create"] = "${message(code:'content.button.create', encodeAs:'JavaScript')}";
    resources["content.button.cancel"] = "${message(code:'content.button.cancel', encodeAs:'JavaScript')}";
    resources["link.movenode"] = "${createLink(action: 'moveNode', controller: 'repository')}";
    resources["link.copynode"] = "${createLink(action: 'copyNode', controller: 'repository')}";
    resources["link.deletenode"] = "${createLink(action: 'deleteNode', controller: 'repository')}";
    resources["link.treetable"] = "${createLink(action: 'treeTable', controller: 'repository')}";
    resources["link.preview"] = "${createLink(action: 'preview', controller: 'repository')}";
    <g:each var="status" in="${org.weceem.content.Status.list()}">
      resources["${status.description}"] = "${message(code: 'content.status.' + status.description, encodeAs:'JavaScript')}"
    </g:each>
}

$(function(){
    init();
    initTreeTable();
    $("#fromDate").datepicker();
    $("#toDate").datepicker();
    $("#search_btn").click(function(){
        $("#treeDiv").css("display", "none");
        $("#searchDiv").css("display", "");
        var sel = $('#spaceSelector').get(0)
        var spacename = sel.options[sel.selectedIndex].text
        $.post("${createLink(action: 'searchRequest', controller: 'repository')}",
            {data: $("#data")[0].value, space: spacename},
            function(data){
                var response = eval('(' + data + ')');
                var tr = $("<tr>");
                var td = $("<td>");
                for (i in response.result){
                    var obj = response.result[i];
                    var body = $("#searchDiv > table > tbody")
                    var newTr = tr.clone();
                    var pageTd = td.clone();
                    var statusTd = td.clone();
                    var createTd = td.clone();
                    var changeTd = td.clone();
                    pageTd.html("<div class='ui-icon ui-icon-document' style='display: inline-block'></div>" + 
                    "<a href='#' style='position:relative; top: -12px;'>" + 
                    "<h2 class='title'>" + obj.title + 
                    "</h2><span class='type'>(/" + obj.aliasURI + " - " + obj.type + ")</span></a>" + 
                    "<div style='position: relative; left: 25px; top: -25px'>Parent: <a href='#'>" + obj.parent + "/" + obj.aliasURI + "</a></div>");
                    statusTd.text(resources[obj.status]);
                    createTd.text(obj.createdBy);
                    changeTd.text(obj.changedOn);
                    newTr.append(pageTd); newTr.append(statusTd);
                    newTr.append(createTd); newTr.append(changeTd);
                    body.append(newTr);
                }
            });
    });
    $("#clear_btn").click(function(){
        $("#treeDiv").css("display", "");
        $("#searchDiv").css("display", "none");
        $("#data")[0].value = "";
        $("#searchDiv > table > tbody").html("");
    });
})
</script>
</head>
<body>

    <div class="span-24 last">
        <div class="container">
            <div class="span-12"><g:render plugin="weceem" template="repository-buttons"/></div>
            <div class="span-4">
            	Space: <g:select id="spaceSelector" name="space" from="${spaces}" optionKey="id" optionValue="name" onchange="changeSpace()" value="${space.id}"/>
            </div>
            <form controller="repository">
            	<div style="float:right" class="span-8 last">
            	  <span id="search_btn" class="sbox_l"></span><span class="sbox"><input type="text" name="data" id="data" /></span><span id="clear_btn" class="sbox_r"></span>
            	  <span class="button_l"></span><span class="button"><g:submitButton name="adv" value="Advanced" id="adv"/></span><span class="button_r"></span>
            	</div>
            	<div id="advSearch" style="display:none" class="span-24 last">
            			Advanced options:<br/>
            			Content type: <g:select from="${grailsApplication.domainClasses.findAll({org.weceem.content.Content.isAssignableFrom(it.clazz)}).sort({a,b->a.name.compareTo(b.name)})}" optionKey="name" optionValue="name"/>
            			Filter by date <g:select from="[[id:'createdOn', value:'created'], [id:'changedOn', value:'changed']]"  optionKey="id" optionValue="value"/> from <input id="fromDate" type="text"/> to <input id="toDate" type="text"/>
            	</div>
            </form>
            </div>

            <div id="treeDiv" class="span-24 last">
                <table id="treeTable">
                  <thead style="border-spacing: 10px 10px">
                    <tr>
                      <th align="left">Page</th>
                      <th align="left">Status</th>
                      <th align="left">Created By</th>
                      <th align="left">Last changed</th>
                      <th>&nbsp;</th>
                    </tr>
                  </thead>
                  <tbody>
                	<g:each in="${content.sort()}" var="c">
                		<g:render plugin="weceem" template="newtreeTableNode" model="[c:c]"/>
		
                	</g:each>
                  </tbody>
                </table>
            </div>
            
            <div id="searchDiv" class="span-24 last" style="display: none">
                <table>
                    <thead style="border-spacing: 10px 10px">
                      <tr>
                        <th align="left">Page</th>
                        <th align="left">Status</th>
                        <th align="left">Created By</th>
                        <th align="left">Last changed</th>
                        <th>&nbsp;</th>
                      </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
            </div>

            <div class="span-24 last prepend-top"><g:render plugin="weceem" template="repository-buttons"/></div>

<div id="createNewDialog" class="ui-helper-hidden" title="${message(code:'content.title.create')}">
    <g:form controller="editor" action="create" method="GET">
        <input id="parentid" name="parent.id" type="hidden"></input>
        <label for="createNewType"><g:message code="content.label.type" encodeAs="HTML"/></label><br/>
        <g:select id="createNewType" name="type" from="${contentTypes.sort { message(code:'content.type.name.'+it) } }" optionValue="${ { message(code:'content.type.name.'+it) } }"/>
        <input id="spaceField" type="hidden" name="space.id" value="${params.space.id}">
    </g:form>
</div>

<div id="deleteDialog" title="Do you want to delete this node?" class="ui-helper-hidden">
	Deleting the content "<span id="deleteContentNodeTitle"></span>" will delete all its child nodes - you cannot undo this.
	Continue with delete?
</div>

<div id="moreActionsMenu" class="ui-helper-hidden">
    <ul>
        <li><a href="#" class="viewAction"><g:message code="command.view" encodeAs="HTML"/></a></li>
        <li><a href="#" class="deleteAction"><g:message code="command.delete" encodeAs="HTML"/></a></li>
        <li><a href="#" class="moveToSpaceAction"><g:message code="command.moveToSpace" encodeAs="HTML"/></a></li>
        <li><a href="#" class="duplicateAction"><g:message code="command.duplicate" encodeAs="HTML"/></a></li>
    </ul>
</div>

<div id="confirmDialog" title="Confirm your action" class="ui-helper-hidden">
	Please, choose one from the following actions:
</div>

<div id="expiredDialog" title="Session Expired" class="ui-helper-hidden">
	${ message(code:'message.session.expired') }
</div>

</body>
</html>
