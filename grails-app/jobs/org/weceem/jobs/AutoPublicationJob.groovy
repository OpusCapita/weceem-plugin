package org.weceem.jobs

import org.weceem.content.WcmContent

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
        
        WcmContent.withTransaction { txn ->
            try {
                def n = wcmContentRepositoryService.publishPendingContent()
                if (log.infoEnabled && n) {
                    log.info "Auto-published $n content nodes"
                }

                n = wcmContentRepositoryService.archiveStaleContent()
                if (log.infoEnabled && n) {
                    log.info "Auto-archived $n content nodes"
                }
            } catch (Throwable t) {
                // Need to discard any modified objects here?
                txn.setRollbackOnly()
                if (log.errorEnabled) {
                    log.error "Auto publication job failed", t
                }
                throw t
            }
        }
        
        if (log.debugEnabled) {
            log.debug "Auto publication job done."
        }
    }
}
