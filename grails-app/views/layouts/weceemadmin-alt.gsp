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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:layoutTitle default="Weceem"/></title>
    <link rel="shortcut icon" href="${g.resource(plugin:'weceem', dir:'_weceem/images/favicon.ico')}"/>

    <nav:resources/>
    <g:javascript library="jquery" plugin="jquery"/>
    <plugin:isAvailable name="resources">
        <r:layoutResources/>
    </plugin:isAvailable>
    <jqui:resources themeCss="${g.resource(dir:'_weceem/jquery-ui/themes/cupertino', file:'jquery-ui-1.8.4.custom.css', plugin:'weceem')}"/>
    <blueprint:resources plugins="buttons"/>

	<!-- Blueprint CSS -->
	<link rel="stylesheet" href="${resource( dir: '_weceem/css/blueprint', file: 'screen.css')}" type="text/css" media="screen, projection" />
	<!--[if IE]><link rel="stylesheet" href="${resource( dir: '_weceem/css/blueprint', file: 'ie.css')}" type="text/css" media="screen, projection" /><![endif]-->		
	<!-- Import fancy-type plugin for the sample page. -->
	<link rel="stylesheet" href="${resource( dir: '_weceem/css/blueprint/plugins/fancy-type', file: 'screen.css')}" type="text/css" media="screen, projection /">
	<link rel="stylesheet" href="${resource( dir: '_weceem/css/blueprint/plugins/buttons', file: 'screen.css')}" type="text/css" media="screen, projection /">

    <link rel="stylesheet" href="${resource(dir: '_weceem/css',file:'admin.css')}"/>
    <link rel="stylesheet" href="${resource(dir: '_weceem/css',file:'admin-theme.css')}"/>

    <g:layoutHead/>
  </head>
  
  <body onload="${pageProperty(name:'body.onload')}" class="${pageProperty(name:'body.class')}">
  
  <div class="container">
    <h1>Alternative Admin UI</h1>
  
    <g:layoutBody/>
  </div>
  
      <plugin:isAvailable name="resources">
          <r:layoutResources/>
      </plugin:isAvailable>
  </body>
</html>
