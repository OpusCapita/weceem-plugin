<script language="javascript" type="text/javascript">
function styleButtons() {
    $('button.removeTag').button({
        icons: {
            primary: 'ui-icon-closethick'
        },
        text: false
    });
}

$(function() {
    
    styleButtons();

    $('button.addTag').button({icons: {
        primary: 'ui-icon-plus'
    }});
    
    $('#tagsfield_${name} .addTag').click( function(event) {
        event.preventDefault();
        
        var dataElem = $("input[name='${name}']");
        var existingTags = dataElem.val().split(',');
        var displayTagsParent = $("#tagsfield_${name} .existingTagList");
        var newTagsElem = $("input[name='newTags_${name}']");
        var newTags = newTagsElem.val().split(',');
        var exists = false;
        $.each(newTags, function(index, t) {
            t = $.trim(t).toLowerCase();
            var exists = false
            for (i = 0; i < existingTags.length; i++) {
                if (existingTags[i] == t) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                existingTags[existingTags.length] = t;
                $('<div class="existingTag"><span class="tagtext">'+t+'</span><button class="removeTag">Remove</button></div>').appendTo(displayTagsParent);
                styleButtons();
            }
        })
        dataElem.val(existingTags.join(','));
        newTagsElem.val('');
    });
    $('#tagsfield_${name} .removeTag').live('click', function(event) {
        event.preventDefault();
        var tagParentDiv = $(event.target).parentsUntil('.existingTagList');
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