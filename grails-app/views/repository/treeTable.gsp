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
}

$(function(){
    init();
    initTreeTable();
})
</script>
</head>
<body>

    <div class="span-24 last">
        <div class="container">
            <div class="span-12"><g:render plugin="weceem" template="repository-buttons"/></div>
            <div class="span-12 last">
            	Space: <g:select id="spaceSelector" name="space" from="${spaces}" optionKey="id" optionValue="name" onchange="changeSpace()" value="${space.id}"/>
            	
                <%-- We have not finished implementing search/filter so we remove it for now
                <form controller="repository">
                	<div style="float:right"><span class="sbox_l"></span><span class="sbox"><input type="text" name="date" id="date" /></span><span class="sbox_r"></span><span class="button_l"></span><span class="button"><g:submitButton name="adv" value="Advanced" id="adv"/></span><span class="button_r"></span></div>
                	<div id="advSearch" style="display:none">
                			Advanced options:<br/>
                			Content type: <g:select from="${grailsApplication.domainClasses.findAll({org.weceem.content.Content.isAssignableFrom(it.clazz)}).sort({a,b->a.name.compareTo(b.name)})}" optionKey="name" optionValue="name"/><br/>
                			Filter by date <g:select from="[[id:'createdOn', value:'created'], [id:'changedOn', value:'changed']]"  optionKey="id" optionValue="value"/> from <g:datePicker precision="day"/> to <g:datePicker  precision="day"/>
                	</div>
                </form>
                 --%>
            </div>

            <div class="span-24 last">
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
