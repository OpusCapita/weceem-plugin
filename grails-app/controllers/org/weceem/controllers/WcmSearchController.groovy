package org.weceem.controllers

import org.weceem.services.ContentRepositoryService
import org.weceem.controllers.ContentController
import org.weceem.tags.WeceemTagLib

/**
 * Controller for rendering search results
 */
class WcmSearchController {

    static defaultAction = 'search'
    
    def contentRepositoryService

    private getSearchData() {
        def info = contentRepositoryService.resolveSpaceAndURI(params.uri)
        def space = info.space
        def uri = info.uri
        if (space) {
            return contentRepositoryService.searchForContent(params.query, space, true, uri)
        }
    }

    def search = {
        def data = searchData
        if (!data) {
            log.warn "There were no search results for [${params.query}]"
            render(status:404, text:"No search results available - no content at ${params.uri}")
            return
        }

        request[ContentController.REQUEST_ATTRIBUTE_PREPARED_MODEL] = [searchResults:data]
        def uri = params.resultsPath ?: 'views/search-results'
        params.clear()
        params.uri = uri
        forward(controller:'content', action:'show')
    }
}