package org.weceem.controllers

import org.weceem.services.ContentRepositoryService
import org.weceem.tags.WeceemTagLib

/**
 * Controller for rendering RSS/Atom feeds for child nodes of content
 */
class FeedsController {

    static defaultAction = 'rss'
    
    def contentRepositoryService
    
    private getFeedData() {
        def info = contentRepositoryService.resolveSpaceAndURI(params.uri)
        def space = info.space
        def uri = info.uri
        def node = contentRepositoryService.findContentForPath(uri, space)?.content
        if (node) {
            def nodes = contentRepositoryService.findChildren(node, [status:ContentRepositoryService.STATUS_ANY_PUBLISHED, max:25])
            return [parent: node, nodes: nodes, space:space]
        }
    }
    
    def rss = {
        def data = feedData
        if (!data) {
            render(status:404, text:"No feed available - no content at ${params.uri}")
            return
        }
        
        render( feedType:actionName, feedVersion: params.version) {
            title = data.parent.title
            description = data.parent.title
            link = g.createLink(controller:'content', action:'show', params:[uri: WeceemTagLib.makeFullContentURI(data.parent)], absolute:true)
            
            data.nodes.each { n ->
                entry {
                    title = n.title
                    publishedDate = n.publicationDate
                    link = g.createLink(controller:'content', action:'show', params:[uri: WeceemTagLib.makeFullContentURI(n)], absolute:true)
                    content(type:n.mimeType, value: n.summary ?: n.content)
                }
            }
        }
    }

    def atom = { 
        rss() // Looks weird, but actionName dynamics...
    }
}
