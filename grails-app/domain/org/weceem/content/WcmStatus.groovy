package org.weceem.content

/**
 * The status of a content node
 * This is designed to be extensible in a simple way - a numeric value is used
 * with the default states being spread out in the numeric range, so that
 * some future alterations to the workflow can be made easily without data migration
 */
class WcmStatus implements Serializable {
    Integer code
    String description // This should be an i18n message code
    Boolean publicContent // Indicates if content in this status can be viewed by unauthenticated users
    
    static searchable = {
        root false // Don't want to get search hits for WcmStatus
        only = ['id', 'publicContent']

        publicContent excludeFromAll: true
    }    
    
    static mapping = {
        cache usage: 'read-write' 
    }

    static constraints = {
        code(nullable: false)
        description(nullable: false, size:1..80) 
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        WcmStatus wcmStatus = (WcmStatus) o

        if (code != wcmStatus.code) return false

        return true
    }

    int hashCode() {
        return code.hashCode()
    }
}
