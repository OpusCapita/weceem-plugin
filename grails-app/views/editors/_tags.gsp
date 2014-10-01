<div id="tagsfield_${name}" style="position: relative; float:left; vertical-align: top; margin-top: 0px;">
    <div class="existingTagList">
        <g:each in="${content[name]}" var="t">
            <div class="existingTag"><span class="tagtext">${t.encodeAsHTML()}</span><button class="removeTag">x</button></div>
        </g:each>
    </div>
    <div>
        <input name="newTags_${name}" value=""/><button class="addTag" style="float:none;display:inline">Add</button>
        <input name="${name}" type="hidden" value="${content[name]?.join(',').encodeAsHTML()},"/><!-- leave extra comma in -->
    </div>
</div>
