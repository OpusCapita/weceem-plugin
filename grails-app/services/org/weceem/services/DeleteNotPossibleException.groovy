package org.weceem.services

class DeleteNotPossibleException extends ContentRepositoryException {
    DeleteNotPossibleException(String message) {
        super(message)
    }
}