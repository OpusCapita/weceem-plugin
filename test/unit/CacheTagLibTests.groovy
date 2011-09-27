import groovy.mock.interceptor.MockFor

import org.weceem.controllers.WcmContentController
import org.weceem.tags.*
import org.weceem.content.RenderEngine

class CacheTagLibTests extends grails.test.GrailsUnitTestCase {

    void setUp() {
        super.setUp()
    }

    void testCacheTag() {
        mockTagLib(CacheTagLib)
        def taglib = new CacheTagLib()
        taglib.request[RenderEngine.REQUEST_ATTRIBUTE_PAGE] = [URI:'test']
        taglib.wcmCacheService = [
            getOrPutValue: { cacheName, key, valueCallable -> 
                assertEquals 'contentCache', cacheName
                assertEquals 'test0', key
                def v = valueCallable().toString()
                assertEquals 'HELLO', v
                return v
            }
        ]

        taglib.cache([:]) { ->
            "HELLO"
        }
        
        assertEquals "HELLO", taglib.out.toString()
    }

}