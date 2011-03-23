package org.weceem.services

class UpdateFailedException extends ContentRepositoryException {
    UpdateFailedException(String message) {
        super(message)
    }
}