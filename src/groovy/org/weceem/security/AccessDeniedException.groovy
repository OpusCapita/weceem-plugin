package org.weceem.security

class AccessDeniedException extends RuntimeException {
    AccessDeniedException(String msg) {
        super(msg)
    }
}