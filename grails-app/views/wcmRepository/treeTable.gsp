<html>
<head >
	<meta name="layout" content="${wcm.adminLayout().toString()}"/>
	<title>Weceem - ${space.name.encodeAsHTML()}</title>
	<script type="text/javascript" src="${g.resource(plugin:'weceem', dir: '_weceem/js/treeTable/javascripts', file:'jquery.treeTable.js')}"></script>
	<script type="text/javascript" src="${g.resource(plugin:'weceem', dir: '_weceem/js', file:'jquery.hotkeys.js')}"></script>
	<script type="text/javascript" src="${g.resource(plugin:'weceem', dir: '_weceem/js/treeTable/javascripts/', file:'core.treeTable.js')}"></script>
	<link href="${g.resource(plugin:'weceem', dir: '_weceem/js/treeTable/stylesheets', file:'jquery.treeTable.css')}" rel="stylesheet" type="text/css" />
	<link href="${g.resource(plugin:'weceem', dir: '_weceem/css', file:'contentRepository.css')}" rel="stylesheet" type="text/css" />

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
    resources["link.movenode"] = "${createLink(action: 'moveNode', controller: 'wcmRepository')}";
    resources["link.copynode"] = "${createLink(action: 'copyNode', controller: 'wcmRepository')}";
    resources["link.deletenode"] = "${createLink(action: 'deleteNode', controller: 'wcmRepository')}";
    resources["link.treetable"] = "${createLink(action: 'treeTable', controller: 'wcmRepository')}";
    resources["link.preview"] = "${createLink(action: 'preview', controller: 'wcmRepository')}";
    resources["search.request"] = "${createLink(action: 'searchRequest', controller: 'wcmRepository')}";
    <g:each var="status" in="${org.weceem.content.WcmStatus.list()}">
      resources["${status.description}"] = "${message(code: 'content.status.' + status.description, encodeAs:'JavaScript')}"
    </g:each>
    
    cacheParams["isAsc"] = true;
    cacheParams["sortField"] = "title";
}

$(function(){
    init();
    initTreeTable();
})
</script>
</head>
<body>
  <nav:set path="plugin.weceem.weceem_menu/content" scope="plugin.weceem.weceem_menu"/>
  <div class="container" id="repository-toolbar">
    <div class="row">
      <div class="col-md-4 col-xs-4">
        <g:render plugin="weceem" template="repository-buttons"/>
      </div>
      <div class="col-md-4 col-xs-4" style="margin-top: 10px;">
        <label><g:message code="space.label.space" default="Space:"/></label>
         <g:select id="spaceSelector" name="space" from="${spaces}"
				 optionKey="id" optionValue="name" onchange="changeSpace()" value="${space.id}"/>
      </div>
      <div class="col-md-4 col-xs-4" id="repository-searchbox"  style="margin-top: 10px;">
        <span id="search_btn" class="sbox_l"></span>
        <span class="sbox"><input type="text" name="data" id="data" /></span>
        <span id="clear_btn" class="sbox_r"></span>
      </div>
    </div>

    <form controller="wcmRepository">
      <div id="advSearch" style="display:none">
          <div class="row">
            <div class="col-md-12">
              <i>You can filter results by</i>
              <br/>
              type <select id="classFilter">
                                                    	    <option value="none">All</option>
                                                    	    <g:each in="${grailsApplication.domainClasses.findAll{org.weceem.content.WcmContent.isAssignableFrom(it.clazz) && (it.clazz != org.weceem.content.WcmContent)}.sort({a,b->a.name.compareTo(b.name)})}">
                                                    	        <option value="${it.fullName}"><g:message code="content.item.name.${it.fullName}" encodeAs="HTML"/></option>
                                                    	    </g:each>
                                                    	</select>
              status <g:select name="statusFilter" id="statusFilter" from="${[['description': 'all', 'code': 0]] + org.weceem.content.WcmStatus.list()}" optionKey="code" optionValue="description" />
              date <g:select name="fieldFilter" id="fieldFilter" from="[[id:'createdOn', value:'created'], [id:'changedOn', value:'changed']]"  optionKey="id" optionValue="value"/> from <input id="fromDate" type="text"/> to <input id="toDate" type="text"/>
              </div>
          </div>
        </div>
    </form>


    <div class="row">

      <div id="treeDiv" class="col-md-12 col-xs-12">
              <div class="table treeTable">
                  <div id="insert-marker"  class="ui-helper-hidden">
                      <img src="${g.resource(plugin:'weceem', dir:'_weceem/images/weceem', file:'inserter.gif')}"/>
                  </div>
                  
                <table id="treeTable">
                  <thead>
                    <tr>
                      <th align="left" class="page-column"><g:message code="header.content" default="Page"/></th>
                      <th align="left"><g:message code="header.status" default="Status"/></th>
                      <th align="left"><g:message code="header.createdBy" default="Created by"/></th>
                      <th align="left"><g:message code="header.lastChanged" default="Last changed"/></th>
                    </tr>
                  </thead>
                  <tbody>

                      
                	<g:each in="${content.sort()}" var="c">
                		<g:render plugin="weceem" template="newtreeTableNode" model="[c:c]"/>
                	</g:each>
                  </tbody>
                </table>
             </div>
      </div>
    </div>

    <div class="row">

      <div id="searchDiv" style="display: none" class="col-md-12 col-xs-12 margin-top-15">

             <div class="table treeTable">
                    <table id="searchResultTable" class="treeTable">
                        <thead>
                          <tr>
                            <th align="left" class="asc"><a href="#" onclick="sortByField('title')"><g:message code="header.content" default="Page"/>&nbsp;&nbsp;</a></th>
                            <th align="left" class="asc"><a href="#" onclick="sortByField('status.description')"><g:message code="header.status" default="Status"/>&nbsp;&nbsp;</a></th>
                            <th align="left" class="asc"><a href="#" onclick="sortByField('createdBy')"><g:message code="header.createdBy" default="Created by"/>&nbsp;&nbsp;</a></th>
                            <th align="left" class="asc"><a href="#" onclick="sortByField('changedOn')"><g:message code="header.lastChanged" default="Last changed"/>&nbsp;&nbsp;</a></th>
                          </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
             </div>
      </div>
    </div>

    <div class="row margin-top-15">
        <div class="col-md-12 col-xs-12">
            <g:render plugin="weceem" template="repository-buttons"/>
        </div>
    </div>
  </div>

<div id="createNewDialog" class="ui-helper-hidden" title="${message(code:'content.title.create', encodeAs:"HTML")}">
    <g:form controller="wcmEditor" action="create" method="GET">
        <input id="parentid" name="parent.id" type="hidden"/>
        <div id="parentInformation" style="display: inline;">
            <b>${message(code:'content.label.parentContent', encodeAs:"HTML")}:</b>&nbsp;
            <label id="newTypeParent"></label>
        </div>
        <p><label for="createNewType"><g:message code="content.label.type" encodeAs="HTML"/></label></p>
        <g:select id="createNewType" name="type" from="${contentTypes.sort { message(code:'content.type.name.'+it) } }" optionValue="${ { message(code:'content.type.name.'+it) } }"/>
        <input id="spaceField" type="hidden" name="space.id" value="${params.space.id}">
    </g:form>
</div>

<div id="deleteDialog" title="Do you want to delete this node?" class="ui-helper-hidden">
	<p>Deleting the content "<span id="deleteContentNodeTitle"></span>" - you cannot undo this.</p>
	<p>Continue with delete?</p>
</div>

<div id="errorDialog" title="An error occurred" class="ui-helper-hidden">
	<p>Sorry - there was a problem!</p>
	<p id="errorDialogMessage"></p>
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
	<p>What would you like to do with this content?</p>
</div>

<div id="expiredDialog" title="Session Expired" class="ui-helper-hidden">
	<p><g:message code="message.session.expired" encodeAs="HTML"/></p>
</div>

</body>
</html>
