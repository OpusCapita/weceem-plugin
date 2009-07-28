<div id="pdfDialog" dojoType="dijit.Dialog" style="display: none;"
    title="${message(code: 'contentRepository.title.convertToPdf')}">
  <div><g:message code="contentRepository.message.confirm.convertToPdf"/></div>
  <br>
  <div align="center">
    <button dojoType="dijit.form.Button" type="button" onclick="convertToPDF(true)">
      <g:message code="contentRepository.action.pdfHierarchy"/>
    </button>
    <button dojoType="dijit.form.Button" type="button" onclick="convertToPDF(false)">
      <g:message code="contentRepository.action.pdfContent"/>
    </button>
    <button dojoType="dijit.form.Button" type="submit">
      <g:message code="command.cancel"/>
    </button>
  </div>
</div>