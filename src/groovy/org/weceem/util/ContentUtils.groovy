package org.weceem.util

import java.text.BreakIterator

class ContentUtils {
    static summarize(String s, maxLen, ellipsis) { 
        if (!s) {
            return s;
        }
        maxLen -= ellipsis.size()
        if (s.size() < maxLen) {
            return s
        } else {
            def bi = BreakIterator.getWordInstance()
            bi.setText(s)
            int first_after = bi.following(maxLen)
            def result = new StringBuilder()
            result << s[0..first_after]
            if (first_after < s.size()) {
               result << ellipsis
            }
            return result
        }
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