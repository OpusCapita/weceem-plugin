package org.weceem.controllers

/**
 * Controller for rendering search results
 */
class WcmSearchController {

    static defaultAction = 'search'
    
    def wcmContentRepositoryService

    static DEFAULT_RESULTS_PER_PAGE = 25
    
    private getSearchData() {
        def info = wcmContentRepositoryService.resolveSpaceAndURI(params.uri)
        def space = info.space
        def uri = info.uri
        if (log.debugEnabled) {
            log.debug "Searching in space [$space] under URI [$uri]"
        }
        if (space) {
            def searchHits
            def searchType = params.mode ?: 'text'
            switch (searchType) {
                case 'tag':
                    searchHits = wcmContentRepositoryService.searchForPublicContentByTag(params.query, space, uri, 
                        [types:params.types, offset:params.int('offset'), max: Math.min(100, params.int('max') ?: DEFAULT_RESULTS_PER_PAGE)])
                    if (log.debugEnabled) {
                        log.debug "Seach by tag ${params.query} results: ${searchHits}"
                    }
                    break
                case 'text':
                default:
                    searchHits = wcmContentRepositoryService.searchForPublicContent(params.query, space, uri, 
                        [types:params.types, offset:params.int('offset'), max: Math.min(100, params.int('max') ?: DEFAULT_RESULTS_PER_PAGE)])
                    break
            }
            return [
                space:space, 
                results:searchHits
            ]
        }
    }

    def search = {
        def data = searchData // Get the search results
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