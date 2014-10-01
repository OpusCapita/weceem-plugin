<script language="javascript" type="text/javascript">
    $(function(){
        $('#tabs-${name}').tabs({
            beforeActivate: function(event, ui){
                var tab = $(ui.newTab).children().first().attr('id')
                var richEditor = CKEDITOR.instances['fckeditor']
                switch (tab){
                    case "tab-${name}-1":
                        $.post("${createLink(action: 'convertToWiki', controller: 'wcmEditor')}",
                            {text: richEditor.getData()},
                            function (data){
        	                    var response = eval(data);
                               console.log($("#wikiEditor"))
        	                   $("#wikiEditor")[0].value = response.result;
        	                });
                        break;
                    case "tab-${name}-2":
                        $.post("${createLink(action: 'convertToHtml', controller: 'wcmEditor')}",
                            {text: $("#wikiEditor")[0].value},
                            function (data){
        	                    var response = eval(data);
                                richEditor.setData(response.result);
        	                });
                        break;
                    default:
                        break;
                }
            }
        });
    })
</script>
