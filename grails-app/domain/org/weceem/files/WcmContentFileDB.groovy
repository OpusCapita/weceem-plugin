package org.weceem.files

import org.springframework.web.multipart.MultipartFile

import org.codehaus.groovy.grails.plugins.codecs.Base64Codec;
import org.codehaus.groovy.grails.web.context.ServletContextHolder

import org.weceem.content.*

import org.weceem.util.MimeUtils

/**
 * WcmContentFileDB, a "file" which is stored in the data base using base64 encoding.
 * Don't use it for large content. It is intended for logos etc.
 *
 * @author Oliver Mihatsch
 */
class WcmContentFileDB extends WcmContent {

	static searchable = {
		only = ['title', 'status', 'space', 'aliasURI', 'parent']
	}

	String fileMimeType
	Long fileSize = 0

	String content
	byte[] rawContent
	
	MultipartFile uploadedFile

	static allowUpDate = true // allow editors to update this file
	    
	static transients = WcmContent.transients + ['uploadedFile', 'rawContent']

	static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "server-file-32.png"]

	static overrideRequired = WcmContent.overrideRequired + [ 'title': false ]

	static constraints = {
        content(nullable: false, maxSize: WcmContent.MAX_CONTENT_SIZE)
		// @todo this is ugly, WcmContentDirectory should never have one, and all files SHOULD
		fileMimeType(nullable:true, blank:true)
	}

	static editors = {
		title()
		aliasURI(group: 'extra')
		uploadedFile(editor:'ContentFileUpload')
		fileMimeType(group:'extra')
		fileSize(group:'extra', editor: 'ReadOnly')
		status()
		content(hidden: true)
	}

	void setUploadedFile(MultipartFile file) {
		// don't overwrite anything, if no upload file specified
		if (file && !file.isEmpty()) {
			this.@uploadedFile = file
			setRawContent(file.bytes)
			if (!title) {
				title = file.originalFilename
			}
			if (!aliasURI) {
				aliasURI = file.originalFilename
			}
		}
	}

	def setRawContent(byte[] rawContent) {
		this.@rawContent = rawContent
		if (rawContent == null) {
			content = null;
    		fileSize = 0
		}
		else {
			fileSize = rawContent.length
			content = rawContent.encodeBase64().toString()
		}
	}
	
	byte[] getRawContent() {
		if (rawContent == null && content != null) {
			this.@rawContent = content.decodeBase64()
		}
		rawContent
	}
	
    /**
     * Must be overridden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { 
		int count = 0;
		StringBuilder out = new StringBuilder();
		out << "Base64:"
		if (content == null) {
			out << " null"
		}
		else {
			int len = 60
			int pos = 0;
			while (pos >= 0) {
				out << '\n'
				if (content.length() - pos > len) {
					out << content.substring(pos, pos + len)
					pos += len
				}
				else {
					out << content.substring(pos)
					pos = -1
				}
			}
		}
		out.toString()
	}

	// Get the servlet container to serve the file
	static handleRequest = { content ->
		renderFileDB(content.rawContent, content.fileMimeType)
	}

	boolean contentShouldAcceptChildren() { false }
	
	/**
	 * Handle the create event to set the mime type.
	 */
	boolean contentWillBeCreated(WcmContent parentContent) {
		if (uploadedFile && !fileMimeType) {
			fileMimeType = uploadedFile.contentType ?: MimeUtils.getDefaultMimeType(uploadedFile.originalFilename)
		}
	}
}
