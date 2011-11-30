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

package org.weceem.content

import org.grails.taggable.*

/**
 * WcmContent class describes the content information.
 *
 * @author Stephan Albers
 * @author July Karpey
 * @author Sergei Shushkevich
 * @author Marc Palmer
 */
// @todo this SHOULD be abstract, but will it work?
class WcmContent implements Comparable, Taggable {
    
    static VALID_ALIAS_URI_CHARS = 'A-Za-z0-9_\\-\\.'
    static INVALID_ALIAS_URI_CHARS_PATTERN = "[^"+VALID_ALIAS_URI_CHARS+"]"
    static VALID_ALIAS_URI_PATTERN = "["+VALID_ALIAS_URI_CHARS+"]+"
    static MAX_CONTENT_SIZE = 500000
    
    transient def wcmSecurityService
    transient def proxyHandler
    
    // we only index title and space
    static searchable = {
         alias WcmContent.name.replaceAll("\\.", '_')
         
         only = ['title', 'status', 'absoluteURI']
         
         absoluteURI excludeFromAll: true
         space component: true 
         status component: [prefix:'status_']
    }
    
    static icon = [plugin: "weceem", dir: "_weceem/images/weceem/content-icons", file: "html-file-32.png"]
    
    // that is also the subject for forums
    String title

    // alias of the title aka "page slug"
    // by default - first 30 "escaped" chars of title
    String aliasURI

    Integer orderIndex
    SortedSet children

    String language
    
    String createdBy
    Date createdOn
    String changedBy
    Date changedOn
    
    Date publishFrom
    Date publishUntil
    
    WcmStatus status
    
    String description // Description/abstract of the content
    String identifier  // A unique ID

    // Dublin Core stuff
    String metaCreator     // The name of the originator of the content
    String metaPublisher   // Entity responsible for making content available
    String metaSource      // Entity that was original source of the content
    String metaCopyright   // Copyright for the content (aka Rights)
    
    Integer validFor = VALID_FOR_HOUR  // cache maxAge

    String contentDependencies

    static belongsTo = [space: WcmSpace, parent: WcmContent]
    static transients = [ 
        'lastModified', 
        'titleForHTML', 
        'titleForMenu', 
        'versioningProperties', 
        'contentAsText', 
        'contentAsHTML', 
        'mimeType', 
        'wcmSecurityService',
        'proxyHandler',
        'absoluteURI',
        'lineage'
    ]

    static hasMany = [children: WcmContent]
    static hasOne = [parent: WcmContent]

    static final VALID_FOR_DAY = 60*60*24
    static final VALID_FOR_HOUR = 60*60
    static final VALID_FOR_5MIN = 60*5
    
    static constraints = {
        title(size:1..100, nullable: false, blank: false)
        aliasURI(nullable: false, blank: false, unique: ['space', 'parent'], maxSize: 50)
        space(nullable: false)
        status(nullable: false)
        orderIndex(nullable: true)
        validFor(nullable: true, inList:[null, 0, 60, VALID_FOR_5MIN, VALID_FOR_HOUR, VALID_FOR_DAY, 60*60*24*7, 60*60*24*30, 60*60*24*300])
        parent(nullable: true, lazy:true)
        createdBy(nullable: true)
        createdOn(nullable: true)
        changedBy(nullable: true)
        changedOn(nullable: true)
        publishFrom(nullable: true)
        publishUntil(nullable: true, validator: { value, obj -> 
            // Allow it to be null or greater than publishFrom
            if (value != null) {
                if (value.time <= (obj.publishFrom ? obj.publishFrom.time : value.time-1)) {
                    return "content.publish.until.must.be.in.future"
                }
            }
            return null
        })
        language(nullable: true, size:0..3)

        contentDependencies(maxSize:500, nullable: true)

        metaCreator nullable: true, blank: true, size:0..80   
        metaPublisher nullable: true, blank: true, size:0..80   
        description nullable: true, blank: true, size:0..500
        identifier nullable: true, blank: true, size:0..80
        metaSource nullable: true, blank: true, size:0..80
        metaCopyright nullable: true, blank: true, size:0..200
    }

    static mapping = {
        cache usage: 'read-write', include: 'non-lazy'
        createdOn index:'content_createdOn_Idx'
        changedOn index:'content_changedOn_Idx'
        title index:'content_contentName_Idx'
        aliasURI index:'content_aliasURI_Idx'
        space index:'content_space_Idx', lazy: false // we never want proxies for this
        children cascade:"all", lazy: true
        status lazy:false
    }

    static editors = {
        space()
        title()
        status()
        aliasURI(group:'extra')
        language(group:'extra', editor:'LanguageList')

        orderIndex hidden:true
        createdBy editor:'ModifiedBy', group:'extra'
        createdOn hidden: true
        changedBy editor:'ModifiedBy', group:'extra'
        changedOn hidden: true
        publishFrom group:'extra'
        publishUntil group:'extra'
        tags editor:'Tags', group:'extra'
        parent hidden:true
        children hidden:true

        validFor group:'advanced', editor:'CacheMaxAge'   // cache maxAge
        contentDependencies group:'advanced'

        metaCreator group:'meta'    
        metaPublisher group:'meta'   
        description group:'extra', editor:'LongString'
        identifier group:'extra'
        metaSource group:'meta'      
        metaCopyright group:'meta'
    }
    
    int compareTo(Object o) {
        if (this.is(o)) return 0
        
        // NOTE: the orderIndex == a.orderIndex returning -1 is REQUIRED with Grails 1.1.1 to workaround
        // issues where orderIndex for children is not unique - returning 0 stops a node being returned in the set!
        if (!o || (o.orderIndex == null) || (o.orderIndex == orderIndex)) return -1
        this.orderIndex <=> o.orderIndex
    }

    boolean contentShouldAcceptChildren() { true }

    boolean contentShouldAcceptChild(WcmContent newChild) { true }

    String getMimeType() { "text/plain" }
    
    /**
     * Can be overriden by content types to customize the short title used for rendering menu items etc
     */
    public String getTitleForMenu() { title }

    /**
     * Can be overriden by content types to customize the long title used for rendering HTML SEO-friendly page titles
     */
    public String getTitleForHTML() { title }

    /**
     * Must be overriden by content types that can represent their content as text.
     * Used for search results and versioning
     */
    public String getContentAsText() { "" }

    /**
     * Should be overriden by content types that can represent their content as HTML.
     * Used for wcm:content tag (content rendering)
     */
    public String getContentAsHTML() { contentAsText ? contentAsText.encodeAsHTML() : '' }

    /** 
     * Descendents must override and call super and add their own properties
     */
    public Map getVersioningProperties() { 
        def t = this
        [
            title:t.title,
            aliasURI:t.aliasURI,
            orderIndex:t.orderIndex,
            space:t.space?.name, // Hmm what makes sense here if moved?
            parent:t.parent?.ident(), // Hmm what makes sense here if moved?
            createdBy:t.createdBy,
            createdOn:t.createdOn,
            changedBy:t.changedBy,
            changedOn:t.changedOn,
            metaCreator:metaCreator,    
            metaPublisher:metaPublisher,
            description:description,
            identifier:identifier,
            metaSource:metaSource,
            metaCopyright:metaCopyright
        ] 
    }

    public void createAliasURI(parent) {
        if (title) {
            def uri = (title.size() < 30) ? title : title.substring(0, 30)
            aliasURI = uri.replaceAll(INVALID_ALIAS_URI_CHARS_PATTERN, '-')
        }
    }

    public String getAbsoluteURI() {
        def uri = new StringBuilder()
        uri << aliasURI
        def c = this
        def visitedNodes = [this.ident()]
        while (c.parent != null) {
            c = c.parent
            uri.insert(0,'/')
            uri.insert(0,c.aliasURI)
            if (visitedNodes.contains(c.ident())) {
                def msg = "Cannot calculate absoluteURI of content with id ${ident()} and "+
                    "aliasURI $aliasURI because there is a loop in the ancestry to ${c.ident()} (${c.aliasURI})"
                log.error(msg)
                throw new IllegalArgumentException(msg)
            } else {
                visitedNodes << c.ident() 
            }
        }
        return uri.toString()
    }
    
    public List getLineage() {
        def result = []
        def c = this
        while (c.parent != null) {
            c = c.parent
            result << c
        }
        return result
    }
    
    def beforeInsert = {
        def by
        WcmContent.withNewSession {
            by = wcmSecurityService?.userName
        }
        if (by == null) by = "system"
        //assert by != null
        
        if (createdBy == null) createdBy = by
        if (createdOn == null) createdOn = new Date()
    }   

    def beforeUpdate = {
        // @todo check, its possible should be using 'delegate' here not normal property access
        
        changedOn = new Date()
        
        def by
        WcmContent.withNewSession {
            by = wcmSecurityService?.userName
        }
        
        changedBy = by
        if (changedBy == null) {
            changedBy = "system"
        }
    }

    /**
     * Return list of content dependency URIs comma-delimited, that must always apply to this
     * node based on its current state
     */
    String getHardDependencies() {
        ''
    }

    String calculateFingerprint() {
        "${ident()}:${version}".encodeAsSHA256()
    }
    
    /**
     * Save a new content revision object with the current state of this content node
     */
    void saveRevision(def latestTitle, def latestSpaceName) {
        def self = this 
        
        def t = this
        
        if (log.debugEnabled) {
            log.debug "Building revision info for ${this}"
        }
        
        def lastRevision
        // Don't force this session to flush just so we can query!
        WcmContentVersion.withNewSession {
            def criteria = WcmContentVersion.createCriteria()
            lastRevision = criteria.get {
                eq('objectKey', ident())
                projections {
                    max('revision')
                }
            }
        }
        
        // Ask the content instance what we should serialize in a version
        def verProps = getVersioningProperties()
        
        def output = new StringBuilder()
        output << "<revision>"
        verProps.each { vp ->
            def propName = escapeToXML(vp.key)
            def propValue = escapeToXML(vp.value?.toString())
            if (propValue) {
                output << "<property name=\"${propName}\">${propValue}</property>"
            }
        }
        // Write out the human-readable content summary always
        output << "<content>${escapeToXML(getContentAsText())}</content>"
        output << "</revision>"

        def xml = output.toString()
        def cv = new WcmContentVersion(objectKey: ident(),
                revision: (lastRevision ? lastRevision + 1 : 1),
                objectClassName: proxyHandler.unwrapIfProxy(self).class.name,
                objectContent: xml,
                contentTitle: latestTitle,
                spaceName: latestSpaceName,
                createdBy: wcmSecurityService?.userName,
                createdOn: new Date())

        cv.updateRevisions()
        if (log.debugEnabled) {
            log.debug "Saving revision info for ${this}"
        }
        if (!cv.save()) {
            log.error "In createVersion of ${this}, save failed: ${cv.errors}"
            assert false
        }
    }
    
    def escapeToXML(s) {
        // MUST escape & first
        s = s?.replaceAll('&', '&amp;')
        s = s?.replaceAll('<', '&lt;')
        s = s?.replaceAll('>', '&gt;')
        return s
    }
    
    Date getLastModified() {
        changedOn ?: createdOn
    }
}
