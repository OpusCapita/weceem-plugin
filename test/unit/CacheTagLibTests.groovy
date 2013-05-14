import groovy.mock.interceptor.MockFor

import org.weceem.controllers.WcmContentController
import org.weceem.tags.*
import org.weceem.content.RenderEngine

@TestFor(CacheTagLib)
class CacheTagLibTests {

    void testCacheTag() {
        tagLib.request[RenderEngine.REQUEST_ATTRIBUTE_PAGE] = [URI:'test']
        tagLib.wcmCacheService = [
            getOrPutValue: { cacheName, key, valueCallable -> 
                assertEquals 'contentCache', cacheName
                assertEquals 'test0', key
                def v = valueCallable().toString()
                assertEquals 'HELLO', v
                return v
            }
        ]

        def output = applyTemplate("<wcm:cache>HELLO</wcm:cache>")

        assertEquals "HELLO", output.toString()
    }

}