/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.weceem.controllers


import grails.converters.JSON
import groovy.json.StreamingJsonBuilder
import org.gualdi.grails.plugins.ckeditor.utils.FileUtils
import org.gualdi.grails.plugins.ckeditor.utils.ImageUtils
import org.gualdi.grails.plugins.ckeditor.utils.MimeUtils
import org.gualdi.grails.plugins.ckeditor.utils.PathUtils
import org.gualdi.grails.plugins.ckeditor.CkeditorConfig

/**
 * WcmOpenFileManagerConnectorController class is the connector to CKeditor plugin.
 * It is the adjusted version of org.gualdi.grails.plugins.ckeditor.OpenFileManagerConnectorController class.
 * Also the class uses Utils classes from CKeditor plugin.
 *
 * @author Stephan Albers
 * @author July Antonicheva
 */
class WcmOpenFileManagerConnectorController {

    def messageSource

    def index = {
        render(plugin: "ckeditor", view: "/ofm", model: [configUrl: getConfigUrl()])
    }

    def config = {
        def ofmConfig = getOFMConfig()
        def finalConfig = [
            custom: [
                ofmBase: "${resource(dir: 'js/ofm', plugin: 'ckeditor')}",
                fileConnector: "${ofmConfig.fileConnector}",
                space: "${ofmConfig.space ?: ''}",
                type: "${ofmConfig.type}",
            ],
            options: [
                culture: "${ofmConfig.currentLocale}",
                lang: "grails",
                defaultViewMode: "${ofmConfig.viewMode}",
                autoload: true,
                showFullPath: false,
                showTitleAttr: false,
                browseOnly: false,
                showConfirmation: true,
                showThumbs: ofmConfig.showThumbs,
                generateThumbnails: false,
                searchBox: false,
                listFile: true,
                fileSorting: "TYPE_ASC",
                chars_only_latin: true,
                dateFormat: "d M Y H:i",
                serverRoot: false,
                fileRoot: '/',
                relPath: "${ofmConfig.baseUrl}",
                logger: false,
                capabilities:  ["select", "download", "rename", "delete"],
                plugins: []
            ],
            security: [
                allowChangeExtensions: false,
                uploadPolicy: "DISALLOW_ALL",
                uploadRestrictions: ofmConfig.uploadRestrictions
            ],
            upload: [
                overwrite: false,
                imagesOnly: false,
                fileSizeLimit: 'auto'
            ],
            exclude: [],
            images: [],
            videos: [],
            audios: [],
            edit: [
                enabled: false,
                lineNumbers: true,
                lineWrapping: true,
                codeHighlight: false,
                theme: "elegant",
                editExt: []
            ],
            extras: [
                extra_js: [],
                extra_js_async: true
            ],
            icons: [
                path: "${resource(dir: 'js/ofm/images/fileicons', plugin: 'ckeditor')}",
                directory: "_Open.png",
                default: "default.png"
            ]
        ];

        render finalConfig as JSON
    }

    private getConfigUrl() {
        def prefix = CkeditorConfig.connectorsPrefix

        def type = params.type
        def userSpace = params.space
        def showThumbs = params.showThumb
        def viewMode = params.viewMode

        def url = "${request.contextPath}/${prefix}/wcmofm/config?fileConnector=${request.contextPath}/${prefix}/wcmofm/filemanager&type=${type}${userSpace ? '&space=' + userSpace : ''}${showThumbs ? '&showThumbs=' + showThumbs : ''}${'&viewMode=' + viewMode}"

        return url
    }

    private getOFMConfig() {
        def ofmConfig = [:]

        // Config object
        def config = grailsApplication.config.ckeditor

        // Base url
        def tmp
        def bUrl = PathUtils.getBaseUrl([space: params.space, type: params.type])
        if (config?.upload?.baseurl) {
            if (config?.upload?.baseurl.startsWith("http")) {
                tmp = PathUtils.checkSlashes(config?.upload?.baseurl, "R-")
                bUrl = tmp + PathUtils.checkSlashes(bUrl, "R-")
            }
            else {
                tmp = PathUtils.checkSlashes(config?.upload?.baseurl, "L+ R-")
                bUrl = "${request.contextPath}${tmp}" + PathUtils.checkSlashes(bUrl, "R-")
            }

        }
        else {
            tmp = PathUtils.checkSlashes(bUrl, "R-")
            bUrl = "${request.contextPath}/${tmp}"
        }

        ofmConfig.baseUrl = bUrl


        // File connector
        ofmConfig.fileConnector = params.fileConnector

        // Current space
        ofmConfig.space = params.space

        // Browser type
        ofmConfig.type = params.type

        // Locale
        def locale = request.getLocale().toString()[0..1]
        if (!(locale in CkeditorConfig.OFM_LOCALES)) {
            locale = 'en'
        }
        ofmConfig.currentLocale = locale

        // View mode
        ofmConfig.viewMode = params?.viewMode ?: "grid"

        // Show thumbs
        ofmConfig.showThumbs = params?.showThumbs ? params?.showThumbs == 'true' : false

        // Allowed extensions
        def extensionsConfig = getExtensionsConfig(ofmConfig.type)
        ofmConfig.uploadRestrictions = extensionsConfig.allowed.collect { "${it}" }

        ofmConfig
    }

    private getExtensionsConfig(type) {
        def config = grailsApplication.config.ckeditor.upload

        def resourceType = type?.toLowerCase()
        if (resourceType == 'file') {
            resourceType = 'link'
        }

        def allowed = config."${resourceType}".allowed ?: []
        def denied = config."${resourceType}".denied ?: []

        [allowed: allowed, denied: denied]
    }

    /**
     * Filemanager connector
     *
     */
    def fileManager = {
        log.debug "begin fileManager()"

        def mode = params.mode
        def type = params.type
        def space = params.space
        def showThumbs = params.showThumbs == 'true'

        def baseUrl = PathUtils.getBaseUrl(params)
        def baseDir = getBaseDir(baseUrl)

        def resp
        switch (mode) {
            case 'getinfo':
                resp = getInfo(baseDir, baseUrl, params.path)
                break

            case 'getfolder':
                streamFolder(response.outputStream, baseDir, baseUrl, params.path, showThumbs)
                log.debug "return fileManager()"
                return

            case 'rename':
                resp = rename(baseDir, params.old, params.'new', type)
                break

            case 'delete':
                resp = delete(baseDir, params.path)
                break

            case 'add':
                resp = add(baseDir, params.currentpath, type, request)
                break

            case 'addfolder':
                resp = addFolder(baseDir, params.path, params.name)
                break

            case 'download':
                resp = download(baseDir, params.path)
                break
        }

        log.debug "end fileManager()"

        if (resp) {
            render resp
        }
        else {
            return null
        }
    }

    def uploader = {
        log.debug "begin uploader()"

        def mode = params.mode
        def type = params.type
        def space = params.space
        def showThumbs = params.showThumbs == 'true'

        def baseUrl = PathUtils.getBaseUrl(params)
        def baseDir = getBaseDir(baseUrl)

        quickUpload(baseDir, baseUrl, "/", type, request)

        log.debug "end uploader()"

        return
    }

    private getBaseDir(baseUrl) {
        def config = grailsApplication.config.ckeditor

        def baseDir
        if (config?.upload?.baseurl) {
            baseDir = PathUtils.checkSlashes(config?.upload?.basedir, "L+ R-") + PathUtils.checkSlashes(baseUrl, "L+ R+")
        }
        else {
            baseDir = servletContext.getRealPath(baseUrl)
            baseDir = PathUtils.checkSlashes(baseDir, "R+")
        }

        def f = new File(baseDir)
        if (!f.exists()) {
            f.mkdirs()
        }

        return baseDir
    }

    private getInfo(baseDir, baseUrl, path) {
        def resp = getFileInfo(baseDir, baseUrl, path)
        return (resp as JSON).toString()
    }

    private streamFolder(outputStream, baseDir, baseUrl, path, showThumbs) {
        def writer = new PrintWriter(outputStream)
        def builder = new StreamingJsonBuilder(writer)
        builder {
            def currentDir = new File(baseDir + PathUtils.checkSlashes(path, "L- R+"))
            if (currentDir.exists()) {
                currentDir.eachFile { file ->
                    if (!file.name.startsWith('.')) {
                        def fname = path + file.name
                        "\"${fname}\""(
                                getFileInfo(baseDir, baseUrl, fname, showThumbs)
                        )
                    }
                }

            }
        }
        writer.close()
    }

    private getFileInfo(baseDir, baseUrl, path, showThumbs = true) {
        def currentObject = baseDir + PathUtils.checkSlashes(path, "L- R-")
        def file = new File(currentObject)

        def width = ''
        def height = ''
        def fileSize = 0
        def preview
        def fileType
        def properties
        if (file.isDirectory()) {
            path = PathUtils.checkSlashes(path, "L+ R+", true)
            preview = g.resource(dir: "js/ofm/images/fileicons", file: "_Open.png", plugin: "ckeditor")
            fileType = 'dir'
            properties = [
                    'Date Created': '',
                    'Date Modified': '',
                    'Width': '',
                    'Height': '',
                    'Size': ''
            ]
        }
        else {
            def fileParts = PathUtils.splitFilename(file.name)
            fileType = fileParts.ext?.toString()?.toLowerCase()
            fileSize = file.length()

            preview = g.resource(dir: "js/ofm/images/fileicons", file: "${fileParts.ext.toLowerCase()}.png", plugin: "ckeditor")
            if (fileType in CkeditorConfig.OFM_IMAGE_EXTS) {
                if (showThumbs) {
                    def config = grailsApplication.config.ckeditor
                    if (config?.upload?.baseurl) {
                        if (config?.upload?.baseurl.startsWith("http")) {
                            preview = PathUtils.checkSlashes(config?.upload?.baseurl, "R-") + baseUrl + path
                        }
                        else {
                            preview = g.resource(file: PathUtils.checkSlashes(config?.upload?.baseurl, "L+ R-") + baseUrl + path )
                        }
                    }
                    else {
                        preview = g.resource(file: baseUrl + path)
                    }
                }
                def imgDim = ImageUtils.calculateImageDimension(file, fileType)
                if (imgDim) {
                    width = imgDim.width
                    height = imgDim.height
                }
            }

            properties = [
                    'Date Created': '',
                    'Date Modified': new Date(file.lastModified()).format("dd-MM-yyyy HH:mm:ss"),
                    'Width': width,
                    'Height': height,
                    'Size': fileSize
            ]
        }

        def resp = [
                'Path': path,
                'Filename': file.name,
                'File Type': fileType,
                'Preview': "${preview}",
                'Properties': properties,
                'Error': '',
                'Code': 0
        ]

        return resp
    }

    private rename(baseDir, oldName, newName, type) {
        def oldFile = new File(baseDir + PathUtils.checkSlashes(oldName, "L-"))
        def newFile = new File(oldFile.parent, newName)

        def isDirectory = oldFile.isDirectory()

        def path
        if (isDirectory) {
            path = PathUtils.getFilePath(PathUtils.checkSlashes(oldName, "R-"))
        }
        else {
            path = PathUtils.getFilePath(oldName)
        }

        def resp
        if (PathUtils.isSafePath(baseDir, newFile)) {
            if (!newFile.exists()) {
                if (isDirectory || FileUtils.isFileAllowed(newName, type)) {
                    try {
                        if (oldFile.renameTo(newFile)) {
                            def tmpJSON = [
                                    'Old Path': oldName,
                                    'Old Name': oldFile.name,
                                    'New Path': path + newFile.name + (isDirectory ? '/' : ''),
                                    'New Name': newFile.name,
                                    'Error': '',
                                    'Code': 0
                            ]

                            resp = (tmpJSON as JSON).toString()
                        }
                        else {
                            resp = error('ofm.invalidFilename', 'Invalid file name')
                        }
                    }
                    catch (SecurityException se) {
                        resp = error('ofm.noPermissions', 'No permissions')
                    }
                }
                else {
                    resp = error('ofm.invalidFilename', 'Invalid file name');
                }
            }
            else {
                resp = error('ofm.fileAlreadyExists', 'File exists')
            }
        }
        else {
            resp = error('ofm.noPermissions', 'No permissions')
        }

        return resp
    }

    private delete(baseDir, path) {
        def file = new File(baseDir + PathUtils.checkSlashes(path, "L-"))

        def resp
        if (PathUtils.isSafePath(baseDir, file)) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    try {
                        def deleteClosure
                        deleteClosure = {
                            it.eachDir(deleteClosure)
                            it.eachFile {
                                it.delete()
                            }
                        }
                        deleteClosure file
                        file.delete()

                        def tmpJSON = [
                                'Path': path,
                                'Error': '',
                                'Code': 0
                        ]

                        resp = (tmpJSON as JSON).toString()
                    }
                    catch (SecurityException se) {
                        resp = error('ofm.noPermissions', 'No permissions')
                    }
                }
                else {
                    try {
                        if (file.delete()) {
                            def tmpJSON = [
                                    'Path': path,
                                    'Error': '',
                                    'Code': 0
                            ]

                            resp = (tmpJSON as JSON).toString()
                        }
                        else {
                            resp = error('ofm.invalidFilename', 'Invalid file name')
                        }
                    }
                    catch (SecurityException se) {
                        resp = error('ofm.noPermissions', 'No permissions')
                    }
                }
            }
            else {
                resp = error('ofm.fileDoesNotExists', 'File does not exists')
            }
        }
        else {
            resp = error('ofm.noPermissions', 'No permissions')
        }

        return resp
    }

    private add(baseDir, currentPath, type, request) {
        def overwrite = grailsApplication.config.ckeditor.upload.overwrite ?: false

        def file
        try {
            file = request.getFile("newfile")
        }
        catch (Exception e) {
            file = null
        }

        def resp
        if (!file) {
            resp = error('ofm.invalidFilename', 'Invalid file', true)
        }
        else {
            def uploadPath = new File(baseDir + PathUtils.checkSlashes(currentPath, "L- R+"))
            def newName = file.originalFilename

            def f = PathUtils.splitFilename(newName)
            if (FileUtils.isAllowed(f.ext, type)) {
                def fileToSave = new File(uploadPath, newName)
                if (!overwrite) {
                    def idx = 1
                    while (fileToSave.exists()) {
                        newName = "${f.name}(${idx}).${f.ext}"
                        fileToSave = new File(uploadPath, newName)
                        idx++
                    }
                }
                file.transferTo(fileToSave)

                def tmpJSON = [
                        'Path': currentPath,
                        'Name': newName,
                        'Error': '',
                        'Code': 0
                ]
                resp = (tmpJSON as JSON).toString()
                resp = "<textarea>${resp}</textarea>"
            }
            else {
                resp = error('ofm.invalidFileType', 'Invalid file type', true)
            }
        }

        return resp
    }

    private addFolder(baseDir, path, name) {
        def newDir = new File(baseDir + PathUtils.checkSlashes(path, "L- R+") + name)

        def resp
        if (newDir.exists()) {
            resp = error('ofm.directoryAlreadyExists', 'Directory already exists!')
        }
        else {
            try {
                if (newDir.mkdir()) {
                    def tmpJSON = [
                            'Parent': path,
                            'Name': name,
                            'Error': '',
                            'Code': 0
                    ]
                    resp = (tmpJSON as JSON).toString()
                }
                else {
                    resp = error('ofm.invalidFolderName', 'Invalid folder name')
                }
            }
            catch (SecurityException se) {
                resp = error('ofm.noPermissions', 'No permissions')
            }
        }

        return resp
    }

    private download(baseDir, path) {
        def file = new File(baseDir + PathUtils.checkSlashes(path, "L-"))

        def encodedFilename = URLEncoder.encode(file.name, "UTF-8")
        def filename = ""
        if (encodedFilename.indexOf('%') == -1) {
            filename = "filename=\"${file.name}\""
        }
        else {
            def userAgent = request.getHeader("User-Agent")
            if (userAgent =~ /MSIE [4-8]/) {
                // IE < 9 do not support RFC 6266 (RFC 2231/RFC 5987)
                filename = "filename=\"${encodedFilename}\""
            }
            else {
                // Use RFC 6266 (RFC 2231/RFC 5987)
                filename = "filename*=UTF-8''${encodedFilename}"
            }
        }

        response.setHeader("Content-Type", "application/octet-stream")
        response.setHeader("Content-Disposition", "attachment; ${filename}")
        response.setHeader("Content-Length", "${file.size()}")
        response.setHeader("Content-Transfer-Encoding", "Binary");

        def os = response.outputStream

        byte[] buff = null
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))
        try {
            buff = new byte[2048]
            int bytesRead = 0
            while ((bytesRead = bis.read(buff, 0, buff.size())) != -1) {
                os.write(buff, 0, bytesRead)
            }
        }
        finally {
            bis.close()
            os.flush()
            os.close()
        }

        return null
    }

    def show = {
        def config = grailsApplication.config.ckeditor
        def filename = PathUtils.checkSlashes(config?.upload?.basedir, "L+ R+") + params.filepath
        def ext = PathUtils.splitFilename(params.filepath).ext

        def contentType = MimeUtils.getMimeTypeByExt(ext)
        def file = new File(filename)

        response.setHeader("Content-Type", contentType)
        response.setHeader("Content-Length", "${file.size()}")

        def os = response.outputStream

        byte[] buff = null
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))
        try {
            buff = new byte[2048]
            int bytesRead = 0
            while ((bytesRead = bis.read(buff, 0, buff.size())) != -1) {
                os.write(buff, 0, bytesRead)
            }
        }
        finally {
            bis.close()
            os.flush()
            os.close()
        }

        return null
    }

    private quickUpload(baseDir, baseUrl, currentPath, type, request) {
        def config = grailsApplication.config.ckeditor
        def overwrite = config.upload.overwrite ?: false

        def resType = type?.toLowerCase()
        if (resType == 'file') {
            resType = 'link'
        }

        def isUploadEnabled = config.upload."${resType}".upload

        def errorMsg = ""

        def idxSpec = ""
        def f

        if (isUploadEnabled) {
            if (request.method != "POST") {
                errorMsg = simpleError("ofm.invalidCall", "Invalid call")
            }
            else {
                def file = request.getFile("upload")
                if (!file) {
                    errorMsg = simpleError("ofm.invalidFile", "Invalid file")
                }
                else {
                    def uploadPath = new File(baseDir + PathUtils.checkSlashes(currentPath, "L- R+"))
                    def newName = file.originalFilename

                    f = PathUtils.splitFilename(newName)
                    if (FileUtils.isAllowed(f.ext, type)) {
                        def fileToSave = new File(uploadPath, newName)
                        if (!overwrite) {
                            def idx = 1
                            while (fileToSave.exists()) {
                                idxSpec = "(${idx})"
                                newName = "${f.name}${idxSpec}.${f.ext}"
                                fileToSave = new File(uploadPath, newName)
                                idx++
                            }
                        }
                        file.transferTo(fileToSave)
                    }
                    else {
                        errorMsg = simpleError("ofm.invalidFileType", "Invalid file type")
                    }
                }
            }
        }
        else {
            errorMsg = simpleError("ofm.uploadsDisabled", "Uploads disabled")
        }

        def fname = ''
        if (!errorMsg) {
            def encodedFilename = URLEncoder.encode(f.name, "UTF-8")
            if (encodedFilename.indexOf('%') == -1) {
                encodedFilename = f.name
            }

            def tmpUrl = config?.upload?.baseurl
            if (tmpUrl) {
                baseUrl = "${PathUtils.checkSlashes(tmpUrl, "L- R-", true)}/${PathUtils.checkSlashes(baseUrl, "L- R-", true)}"
                if (baseUrl.startsWith("http")) {
                    fname = "${baseUrl}/${encodedFilename}${idxSpec}.${f.ext}"
                } else {
                    fname = "${request.contextPath}/${baseUrl}/${encodedFilename}${idxSpec}.${f.ext}"
                }
            } else {
                fname = "${request.contextPath}/${baseUrl}/${encodedFilename}${idxSpec}.${f.ext}"
            }
        }

        response.setHeader("Cache-Control", "no-cache")
        render(contentType: "text/html", encoding: "UTF-8") {
            script(type: "text/javascript", "window.parent.CKEDITOR.tools.callFunction(${params.CKEditorFuncNum}, '${fname}', '${errorMsg}');")
        }
    }

    private error(key, message, useTextarea = false) {
        def msg
        try {
            msg = messageSource.getMessage(key, null, request.getLocale())
        }
        catch (org.springframework.context.NoSuchMessageException nsme) {
            msg = message
        }
        def error = ['Error': msg, 'Code': -1]
        def jsonError = (error as JSON).toString()
        if (useTextarea) {
            jsonError = "<textarea>${jsonError}</textarea>"
        }

        return jsonError
    }

    private simpleError(key, message) {
        def msg
        try {
            msg = messageSource.getMessage(key, null, request.getLocale())
        }
        catch (org.springframework.context.NoSuchMessageException nsme) {
            msg = message
        }

        return msg
    }
}
