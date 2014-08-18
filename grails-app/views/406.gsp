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
    <title>Sorry, this content is not intended for rendering!</title>

    <link rel="shortcut icon" href="${g.resource(plugin:'weceem', dir:'_weceem/images/favicon.ico')}"/>

    <link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'admin.css')}"/>
    <link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'admin-theme.css')}"/>
    <link rel="stylesheet" href="${g.resource(plugin:'weceem', dir: '_weceem/css',file:'weceem.css')}"/>

</head>

<body>

<div class="container">
  <div class="container">
    <div id="adminLogo" class="span-14"></div>
  </div>

  <h3>${flash.message ?: 'This content is not intended for rendering!'}</h3>

  <g:render plugin="weceem" template="/layouts/main/footer"/>
</div>

</body>
</html>
