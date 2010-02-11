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

    static DEFAULT_RESULTS_PER_PAGE = 25
    
    private getSearchData() {
        def info = contentRepositoryService.resolveSpaceAndURI(params.uri)
        def space = info.space
        def uri = info.uri
        if (space) {
            return contentRepositoryService.searchForPublicContent(params.query, space, uri, 
                [offset:params.int('offset'), max: Math.min(100, params.int('max') ?: DEFAULT_RESULTS_PER_PAGE)])
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