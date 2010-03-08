package org.weceem.controllers

import org.weceem.content.Space

class WcmContentSubmissionController {

    static allowedMethods = [submit:'POST' /*, update:'POST', delete:'POST'*/]
    
    def weceemSecurityService
    def contentRepositoryService
    
    def submit = { 
        def formPath = params.remove('formPath')
        def successPath = params.remove('successPath')
        def spaceId = params.remove('spaceId')
        def parentId = params.remove('parentId')
        def type = params.remove('type')
        def space = Space.get(spaceId.toString().toLong())
        
        def content = contentRepositoryService.createUserSubmittedContent(space, parentId, type, params, request)
        // redirect to/render content with model populated, and link to new content included
        if (content.hasErrors()) {
            if (log.debugEnabled) {
                log.debug "Rendering original content form at [$formPath] in space [${space.name}] due to form errors: ${content.errors}"
            }
            // The rendering functions expect a space
            request[WcmContentController.REQUEST_ATTRIBUTE_PREPARED_MODEL] = [submittedContent:content]
            def newparams = [uri:formPath]
            content.discard()
            flash[WcmContentController.FLASH_MESSAGE] = "contentSubmission.content.has.errors"
            forward(controller:'wcmContent', action:'show', params:newparams)
        } else {
            if (log.debugEnabled) {
                log.debug "Redirecting to [$successPath] after successful content submission"
            }
            flash[WcmContentController.FLASH_MESSAGE] = content.status.publicContent ?
                "contentSubmission.content.accepted.published" : 
                "contentSubmission.content.accepted.not.published"
            redirect(controller:'wcmContent', action:'show', params:[uri:space.aliasURI+'/'+successPath])
        }
    }
}
