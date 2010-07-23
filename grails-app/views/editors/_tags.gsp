<div id="tagsfield_${name}">
    <div class="existingTagList">
        <g:each in="${content[name]}" var="t">
            <div class="existingTag"><span class="tagtext">${t.encodeAsHTML()}</span><button class="removeTag">x</button></div>
        </g:each>
    </div>
    <div>
        <input name="newTags_${name}" value=""/><button class="addTag" style="float:none;display:inline">Add tags</button>
        <input name="${name}" type="hidden" value="${content[name]?.join(',').encodeAsHTML()},"/><!-- leave extra comma in -->
    </div>
</div>
