<%--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <title><g:layoutTitle default="Weceem"/></title>
    <link rel="shortcut icon" href="${resource(dir:wcm.pluginCtxPath() +'/_weceem/images/favicon.ico')}"/>

    <nav:resources/>

	<link rel="stylesheet" href="${resource(dir:wcm.pluginCtxPath() +'/_weceem/js/jquery-ui-1.7.1/css/cupertino', file:'jquery-ui-1.7.1.custom.css')}" type="text/css"/>
	<script type="text/javascript" src="${g.resource(dir: wcm.pluginCtxPath() +'/_weceem/js/jquery-ui-1.7.1/js', file:'jquery-1.3.2.min.js')}"></script>
	<script type="text/javascript" src="${g.resource(dir: wcm.pluginCtxPath() +'/_weceem/js/jquery-ui-1.7.1/js', file:'jquery-ui-1.7.1.custom.min.js')}"></script>
	<link href="${resource(dir:wcm.pluginCtxPath() +'/_weceem/js/fg-menu', file:'fg.menu.css')}" rel="stylesheet" type="text/css" />
	<link rel="stylesheet" href="${resource(dir: wcm.pluginCtxPath() +'/_weceem/css',file:'weceem.css')}"/>
	<script type="text/javascript" src="${g.resource(dir: wcm.pluginCtxPath() +'/_weceem/js/fg-menu', file:'fg.menu.js')}"></script>

	<!-- Blueprint CSS -->
	<link rel="stylesheet" href="${resource( dir: wcm.pluginCtxPath() +'/_weceem/css/blueprint', file: 'screen.css')}" type="text/css" media="screen, projection" />
	<!--[if IE]><link rel="stylesheet" href="${resource( dir: wcm.pluginCtxPath() +'/_weceem/css/blueprint', file: 'ie.css')}" type="text/css" media="screen, projection" /><![endif]-->		
	<!-- Import fancy-type plugin for the sample page. -->
	<link rel="stylesheet" href="${resource( dir: wcm.pluginCtxPath() +'/_weceem/css/blueprint/plugins/fancy-type', file: 'screen.css')}" type="text/css" media="screen, projection /">
	<link rel="stylesheet" href="${resource( dir: wcm.pluginCtxPath() +'/_weceem/css/blueprint/plugins/buttons', file: 'screen.css')}" type="text/css" media="screen, projection /">

    <link rel="stylesheet" href="${resource(dir: wcm.pluginCtxPath() +'/_weceem/css',file:'admin.css')}"/>
    <link rel="stylesheet" href="${resource(dir: wcm.pluginCtxPath() +'/_weceem/css',file:'admin-theme.css')}"/>

    <g:layoutHead/>
  </head>
  
  <body onload="${pageProperty(name:'body.onload')}" class="${pageProperty(name:'body.class')}">
  
  <div class="container">
    <g:if test="${!params.externalCall}">
        <g:render plugin="weceem" template="/layouts/main/header"/>
    </g:if>
  
    <g:layoutBody/>
    
    <g:if test="${!params.externalCall}">
        <g:render plugin="weceem" template="/layouts/main/footer"/>
    </g:if>
  </div>
  
  </body>
</html>
