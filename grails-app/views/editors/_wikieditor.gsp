    <div id="tabs-${name}">
        <ul>
            <li ><a id="tab-${name}-1" href="#tabs-${name}-1">Text Editor</a></li>
            <li ><a id="tab-${name}-2" href="#tabs-${name}-2">Preview</a></li>
        </ul>
        <div id="tabs-${name}-1">
             <textarea id="wikiEditor" rows="20"
                          style="border: none; height: 420; width: 100%;">${content?.content}</textarea>
        </div>
        <div id="tabs-${name}-2">
            <ckeditor:editor id="fckeditor" userSpace="${content.space.name}" name="${name}"
                width="100%" height="420" showThumbs="true" toolbar="Basic">${value}</ckeditor:editor>
        </div>
    </div>  

