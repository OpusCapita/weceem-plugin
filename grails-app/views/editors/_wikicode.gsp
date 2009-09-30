<script type="text/javascript">
    $(function(){
        $('#tabs-${name}').tabs({
            select: function(event, ui){
                var tabid = ui.tab.id;
                var richEditor = FCKeditorAPI.GetInstance("${name}");
                switch (tabid){
                    case "tab-${name}-1":
                        $.post("${createLink(action: 'convertToWiki', controller: 'editor')}",
                            {text: richEditor.GetXHTML(true)},
                            function (data){
        	                    var response = eval('(' + data + ')');
        	                   $("#wikiEditor")[0].value = response.result;
        	                });
                        break;
                    case "tab-${name}-2":
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
