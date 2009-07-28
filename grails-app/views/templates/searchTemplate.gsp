<!--
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<html>
  <head>
    <meta name="layout" content="main"></meta>
    <title>Search in ${space}: ${query}</title>
    <style type="text/css">
      div#headerBlock {
        vertical-align: top;
        position: relative;
        width: 100%;
        clear: both;
      }
      div#tabsBlock {
        position: relative;
        width: 100%;
        clear: both;
      }
      div#menueBlock {
        float: left;
        width: 24%;
        margin-right: 2px;
        margin-left: 2px;
        margin-top: 2px;
      }
      div#contentNodeBlock {
        float: left;
        width: 65%;
        height: 100%;
        margin-right: 2px;
        margin-left: 2px;
        margin-top: 2px;
      }
      .highlight {
        font-weight: bold;
      }
      .hitEntry {
        margin-top: 5px;
        border-bottom: 1px dotted black;
      }
      .hitTitle {
        font-size: larger;
        margin: 0px;
      }
      .hitTitle a {
        text-decoration: none;
      }
      .hitTitle a:hover {
        text-decoration: underline;
      }
      .hitInfo {
        font-size: smaller;
        color: #6e7d8e;
      }
    </style>
  </head>

  <body>
    <div id="content">
      <g:block id="headerBlock">
      </g:block>

      <g:block id="tabsBlock">
      </g:block>

      <g:block id="menueBlock">
      </g:block>


      <div id="search" style="margin-top: 5em;">
        <g:if test="${results != null}">

          <div id="searchCount">
            <g:if test="${results.total > 0}">
              Displaying <b>${1 + results.offset} - ${results.offset + results.max}</b>
              of <b>${results.total}</b> matches
            </g:if>
            <g:else>
              No matches found.
            </g:else>
          </div>
          <hr/>


          <g:each var="result" in="${results.results}" status="i">

            <div class='hit'>

              <div class='hitEntry'>
                <div class='hitTitle'>
                  <g:link controller="weceem" action="show" id="${result.aliasURI}"
                      params="[space: result.space.name]">
                    ${result.title}
                  </g:link>
                </div>
                <div class='hitInfo'>
                  ${result.createdOn}
                </div>
                <p class='hitBody'>
                  ${results.highlights[i] ?: "..."}
                </p>
              </div>
            </div>

          </g:each>


          <div class="archivePaginate">
            <g:paginate controller="search" action="${space}" total="${results.total}" max="10" params="[ query: params.query]"/>
          </div>

        </g:if>
      </div>

    </div>
  </body>
</html>