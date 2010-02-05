package org.weceem.controllers

import org.weceem.services.ContentRepositoryService
import org.weceem.tags.WeceemTagLib

/**
 * Controller for rendering archive of year/month/day of given content URI
 */
class ArchiveController {

    static defaultAction = 'list'
    
    def contentRepositoryService

    private getArchiveData() {
        def fullURIParts = params.uri.tokenize('/')
        
        if (log.debugEnabled) {
            log.debug "Archive uri parts: ${fullURIParts}"
        }
        
        // Gather up the trailing number parts, if any
        def contentURIParts = []
        def numberParts = []
        def target = numberParts
        // Go backwards through uri parts and put into number parts, and switch to uri parts when no longer numbers
        fullURIParts.reverse().each { part ->
            if (!part.isInteger()) {
                target = contentURIParts
            }
            target << part
        }
        // Now we have list of either nothing, "year", "year, month" or "year, month, day"
        numberParts = numberParts.reverse() // we want year to be the first not the last
        def contentURI = contentURIParts.reverse().join('/')
        def year = numberParts.size() > 0 ? numberParts[0].toInteger() : null
        def month = numberParts.size() > 1 ? numberParts[1].toInteger() : null
        def day = numberParts.size() > 2 ? numberParts[2].toInteger() : null

        assert year
        assert month
        
        def info = contentRepositoryService.resolveSpaceAndURI(contentURI)
        def space = info.space
        def uri = info.uri
        def node = contentRepositoryService.findContentForPath(uri, space)?.content
        if (node) {
            def dates = day != null ?
                contentRepositoryService.calculateDayStartEndDates(day, month, year) :
                contentRepositoryService.calculateMonthStartEndDates(month, year)
                
            def max = params.int('max') ?: 25
            
            def nodes = contentRepositoryService.findContentForTimePeriod(node,
                dates.start,
                dates.end,
                [status:ContentRepositoryService.STATUS_ANY_PUBLISHED, max:Math.min(max, 100)])
            return [parent: node, nodes: nodes, space:space, startDate:dates.start, endDate:dates.end]
        }
    }

    def list = {
        def data = archiveData
        if (!data) {
            render(status:404, text:"No archive available - no content at ${params.uri}")
            return
        }

        render text:data.nodes.collect({it.content}).join('-----------------\n')
    }
}