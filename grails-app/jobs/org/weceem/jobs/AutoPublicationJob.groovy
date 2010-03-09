package org.weceem.jobs


class AutoPublicationJob {
    def wcmContentRepositoryService

    static triggers = {
        simple name: 'autopub', startDelay: 60000, repeatInterval: 60000  
    }
      
    static group = 'weceem'
    
    /**
     * @todo This is sub-optimal, we should just remember the date of the next publication at startup/edit
     * and schedule a task to do this for then
     */
    def execute() {
        if (log.debugEnabled) {
            log.debug "Auto publication job running..."
        }
        def n = wcmContentRepositoryService.publishPendingContent()
        if (log.infoEnabled && n) {
            log.info "Auto-published $n content nodes"
        }
    }
}
