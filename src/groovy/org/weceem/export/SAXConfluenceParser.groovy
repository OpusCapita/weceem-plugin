package org.weceem.export

import org.xml.sax.*

class SAXConfluenceParser extends org.xml.sax.helpers.DefaultHandler {

    def pages = []
    def bodies = [:]
    def pageMode = false
    def propertyMode = false
    def bodyIdMode = false
    def captureBodyId = false
    def bodyMode = false
    def lastBodyId
    def lastBody = new StringBuffer()
    def captureBody = false
    def lastProperty

    void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName == 'object') pageMode = false
        if (bodyIdMode && qName == 'id') {
            captureBodyId = true
        }
        else if (bodyMode) {
            if (qName == 'id') {
                captureBodyId = true
            }
            if (qName == 'property' && 'body' == attributes.getValue('name')) {
                captureBody = true
            }
        }
        else if (pageMode) {
            if (qName == 'property') {
                propertyMode = true
                lastProperty = attributes.getValue('name')
            }
            if (qName == 'collection' && 'bodyContents' == attributes.getValue('name')) {
                bodyIdMode = true
                pageMode = false
            }
        }
        else {
            def className = attributes.getValue('class')
            if (qName == 'object' && 'Page' == className) {
                pages << [:]
                pageMode = true
            }
            else if (qName == 'object' && 'BodyContent' == className) {
                bodyMode = true
            }
        }
    }

    void endElement(String namespaceURI, String localName, String qName) {
        if (bodyMode && captureBody && 'property' == qName) {
            def entry = bodies.find {key, val -> val == lastBodyId}
            if (entry) {
                entry.value = lastBody.toString()
            }

            bodyMode = false
            captureBody = false
            lastBodyId = null
            lastBody = new StringBuffer()
        }
    }

    void characters(char[] ch,
            int start,
            int length) {
        if (propertyMode) {
            propertyMode = false
            if (lastProperty) {
                pages[-1]."$lastProperty" = String.copyValueOf(ch, start, length)
                lastProperty = null
            }
        }
        else if (bodyMode && captureBodyId) {
            lastBodyId = String.copyValueOf(ch, start, length)
            captureBodyId = false
        }
        else if (bodyMode && captureBody) {
            lastBody << String.copyValueOf(ch, start, length)
        }
        else if (bodyIdMode && captureBodyId) {
            def pageIndex = pages.size() - 1
            def id = String.copyValueOf(ch, start, length).trim()
            if (id) {
                bodies[pageIndex] = id
                bodyIdMode = false
                captureBodyId = false
                pageMode = true
            }
        }
    }
}
