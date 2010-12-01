package org.weceem.util

import java.text.BreakIterator

class ContentUtils {
    static summarize(String s, maxLen, ellipsis) { 
        if (!s || (s.size() <= maxLen)) {
            return s;
        }

        // Make sure output always big enough for ellipsis
        if (maxLen < ellipsis.size()+1) {
            throw new IllegalArgumentException("Cannot summarize to maxLen ${maxLen}, maxLen must be at least 1 longer than ellipsis length")
        }
        
        maxLen -= ellipsis.size()
        def bi = BreakIterator.getWordInstance()
        bi.setText(s)
        int breakpoint = bi.preceding(maxLen)
        println "Break point for $s is ${breakpoint}"
        def result = new StringBuilder()
        if (breakpoint > 0) {
            result << s[0..breakpoint-1]
        } else {
            result << s[0..maxLen-1]
        }
        result << ellipsis
        return result
    }

    /**
    * Remove markup from HTML but leave escaped entities, so result can
    * be output with encodeAsHTML() or not as the case may be
    */
    static htmlToText(s) {
        if (s) {
            s.replaceAll("\\<.*?>", '').decodeHTML()
        } else return s;
    }
}