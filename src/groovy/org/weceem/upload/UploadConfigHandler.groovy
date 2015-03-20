package org.weceem.upload

interface UploadConfigHandler {
    def handleUploadConfig(File uploadDirValue, String uploadUrlValue)
}