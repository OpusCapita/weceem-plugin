package org.weceem.controllers

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
        if (log.debugEnabled) {
            log.debug "Searching in space [$space] under URI [$uri]"
        }
        if (space) {
            return [
                space:space, 
                results:contentRepositoryService.searchForPublicContent(params.query, space, uri, 
                    [offset:params.int('offset'), max: Math.min(100, params.int('max') ?: DEFAULT_RESULTS_PER_PAGE)])
            ]
        }
    }

    def search = {
        def data = searchData
        if (!data) {
            log.warn "There were no search results for [${params.query}]"
            render(status:404, text:"No search results available - no content at ${params.uri}")
            return
        }

        request[WcmContentController.REQUEST_ATTRIBUTE_PREPARED_MODEL] = [searchResults:data.results]
        def uri = params.resultsPath ?: data.space.aliasURI+'/views/search-results'
        params.clear()
        params.uri = uri
        forward(controller:'wcmContent', action:'show', params:params)
    }
}