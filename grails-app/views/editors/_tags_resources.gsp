<script language="javascript" type="text/javascript">
$(function() {
    
    $('#tagsfield_${name} .addTag').click( function(event) {
        event.preventDefault();
        
        var dataElem = $("input[name='${name}']");
        var displayTagsParent = $("#tagsfield_${name} .existingTagList");
        var newTagsElem = $("input[name='newTags_${name}']");
        var newTags = newTagsElem.val().split(',');
        var newValue = dataElem.val();
        $.each(newTags, function(index, t) {
            t = $.trim(t).toLowerCase();
            newValue += t + ',';
            $('<div class="existingTag"><span class="tagtext">'+t+'</span><button class="removeTag">x</button></div>').appendTo(displayTagsParent);
        })
        dataElem.val(newValue);
        newTagsElem.val('');
    });
    $('#tagsfield_${name} .removeTag').live('click', function(event) {
        event.preventDefault();
        var tagParentDiv = $(event.target).parent();
        var tagToRemove = $('.tagtext', tagParentDiv).text();
        $(tagParentDiv).fadeOut(500, function() {
            $(this).remove();
        });
        var dataElem = $("input[name='${name}']");
        var currentTags = dataElem.val().split(',');
        var newVal = '';
        $.each(currentTags, function(index, t) {
            t = $.trim(t).toLowerCase();
            if (t != tagToRemove) {
                newVal += t + ','
            }
        });
        dataElem.val(newVal);
    });
});
</script>