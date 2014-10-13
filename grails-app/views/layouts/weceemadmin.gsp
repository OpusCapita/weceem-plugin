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
<%@page defaultCodec="none" expressionCodec="none" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:layoutTitle default="Weceem"/></title>

    <link rel="shortcut icon" href="${g.resource(plugin:'weceem', dir:'_weceem/images/favicon.ico')}"/>

    <!-- Including Bootstrap resources -->
    <link rel="stylesheet" href="${g.resource(plugin:'twitter-bootstrap', dir: 'css',file:'bootstrap.min.css')}"/>
    <link rel="stylesheet" href="${g.resource(plugin:'twitter-bootstrap', dir: 'css',file:'bootstrap-theme.min.css')}"/>

    <g:javascript library="jquery" plugin="jquery"/>
    <plugin:isAvailable name="resources">
        <r:layoutResources/>
    </plugin:isAvailable>


    <jqui:resources themeCss="${g.resource(dir:'_weceem/jquery-ui/themes/cupertino', file:'jquery-ui-1.8.4.custom.css', plugin:'weceem')}"/>

	<link href="${g.resource(plugin:'weceem', dir:'_weceem/js/fg-menu', file:'fg.menu.css')}" rel="stylesheet" type="text/css" />
	<script type="text/javascript" src="${g.resource(plugin:'weceem', dir: '_weceem/js/fg-menu', file:'fg.menu.js')}"></script>

    <link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'admin.css')}"/>
    <link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'admin-theme.css')}"/>
	<link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'weceem.css')}"/>
    <g:layoutHead/>

  </head>
  
  <body onload="${pageProperty(name:'body.onload')}" class="${pageProperty(name:'body.class')}">

  <div class="container">
    <g:if test="${!params.externalCall}">
      <div class="row">
        <g:render plugin="weceem" template="/layouts/main/header"/>
      </div>
    </g:if>

    <div class="row">
      <g:layoutBody/>
    </div>

    <g:if test="${!params.externalCall}">
      <div class="row">
        <div class="col-md-12 col-xs-12">
          <g:render plugin="weceem" template="/layouts/main/footer"/>
        </div>
      </div>
    </g:if>
  </div>
  
      <plugin:isAvailable name="resources">
          <r:layoutResources/>
      </plugin:isAvailable>

  </body>
</html>
