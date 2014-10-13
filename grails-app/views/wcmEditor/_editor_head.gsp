<script type="text/javascript" src="${g.resource(plugin:'weceem', dir:'_weceem/js/weceem', file:'editor.js')}"></script>
<%-- Render any head resources needed by fields, all on one line to avoid whitespace --%>
<g:each in="${editableProperties}" var="prop">
    <wcm:ifTagExists namespace="wcm" tag="${'editorResources'+prop.editor}">
        <%= (wcm."editorResources${prop.editor}"(bean:content, property:prop.property)).encodeAsRaw() %>
    </wcm:ifTagExists>
</g:each>