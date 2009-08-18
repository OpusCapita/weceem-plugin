<script type="text/javascript">
    $(function(){
        $('#tabs').tabs({
            select: function(event, ui){
                var tabid = ui.tab.id;
                var richEditor = FCKeditorAPI.GetInstance("${name}");
                switch (tabid){
                    case "tab-1":
                        $.post("${createLink(action: 'convertToWiki', controller: 'editor')}",
                            {text: richEditor.GetXHTML(true)},
                            function (data){
        	                    var response = eval('(' + data + ')');
        	                   $("#wikiEditor")[0].value = response.result;
        	                });
                        break;
                    case "tab-2":
                        $.post("${createLink(action: 'convertToHtml', controller: 'editor')}",
                            {text: $("#wikiEditor")[0].value},
                            function (data){
        	                    var response = eval('(' + data + ')');
                                richEditor.SetData(response.result);
        	                });
                        break;
                    default:
                        break;
                }
            }
        });
    })
</script>
<bean:customField beanName="content" property="${name}" labelKey="${'content.label.'+name}" noLabel="true">
    <div id="tabs">
        <ul>
            <li ><a id="tab-1" href="#tabs-1">Text Editor</a></li>
            <li ><a id="tab-2" href="#tabs-2">Preview</a></li>
        </ul>
        <div id="tabs-1">
             <textarea id="wikiEditor" 
                          style="border: none; height: 420; width: 700;"
                          >${content?.content}</textarea>
        </div>
        <div id="tabs-2">
            <fckeditor:editor id="fckeditor" userSpace="${content.space.name}" name="${name}" width="700" height="420" toolbar="Standard" fileBrowser="default">${value}</fckeditor:editor>
        </div>
    </div>  
</bean:customField>
