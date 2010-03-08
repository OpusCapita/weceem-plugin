package org.weceem.controllers

/**
 * @author Sergei Shushkevich
 */
class WcmPortalController {

    static defaultAction = 'content'

    def content = { 
        redirect(controller:'wcmRepository', action:'treeTable')
    }
    
    def design = {
        render(view: 'design', plugin:'weceem')
    }

    def administration = {
        render(view: 'administration', plugin:'weceem')
    }
    
    def comingsoon = {
        render(view: 'comingsoon', plugin:'weceem')
    }
    
    def licenses = {
        render(view: 'licenses', plugin:'weceem')
    }
}