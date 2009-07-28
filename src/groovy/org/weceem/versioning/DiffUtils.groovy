package org.weceem.versioning

import org.apache.commons.lang.StringEscapeUtils

/**
 * Diff utils to compare two strings.
 *
 * Code was originally written in JavaScript by John Resig (http://ejohn.org/files/jsdiff.js)
 *
 * @author Sergei Shushkevich
 */
class DiffUtils {

    static def diffString(o, n) {
        o = StringEscapeUtils.escapeHtml(o).replaceFirst(/\s+$/, '')
        n = StringEscapeUtils.escapeHtml(n).replaceFirst(/\s+$/, '')
        def out = diff((o == '') ? [] : o.split().toList(), (n == '') ? [] : n.split().toList())
        def str = ''
        def oSpace = []
        (o =~ /\s+/).each { oSpace << it }
        oSpace << '\n'
        def nSpace = []
        (n =~ /\s+/).each { nSpace << it }
        nSpace << '\n'
        if (out.n.size() == 0) {
            for (i in 0..<out.o.size()) {
                str <<= "<span class=\"deleted\">${out.o[i]}${oSpace[i]}</span>"
            }
        } else {
            if (out.n[0] instanceof String) {
                for (def i = 0; i < out.o.size() && (out.o[i] instanceof String); i++) {
                    str <<= "<span class=\"deleted\">${out.o[i]}${oSpace[i]}</span>"
                }
            }
            for (i in 0..<out.n.size()) {
                if (out.n[i] instanceof String) {
                    str <<= "<span class=\"added\">${out.n[i]}${nSpace[i]}</span>"
                } else {
                    def pre = ''
                    for (def j = out.n[i].row + 1; j < out.o.size() && out.o[j] instanceof String; j++) {
                        pre <<= "<span class=\"deleted\">${out.o[j]}${oSpace[j]}</span>"
                    }
                    str <<= " ${out.n[i].text}${nSpace[i]}${pre}"
                }
            }
        }
        return str.toString().replaceAll(/\n/, '<br/>')
    }

    private static def diff(o, n) {
        def ns = [:]
        def os = [:]
        for (i in 0..<n.size()) {
            if (ns[n[i]] == null) {
                ns[n[i]] = ['rows': [], 'o': null]
            }
            ns[n[i]].rows << i
        }
        for (i in 0..<o.size()) {
            if (os[o[i]] == null) {
                os[o[i]] = ['rows': [], 'n': null]
            }
            os[o[i]].rows << i
        }
        ns.each {key, value ->
            if (value.rows.size() == 1 && os[key] != null && os[key].rows.size() == 1) {
                n[value.rows[0]] = ['text': n[value.rows[0]], 'row': os[key].rows[0]]
                o[os[key].rows[0]] = ['text': o[os[key].rows[0]], 'row': value.rows[0]]
            }
        }
        for (i in 0..<(n.size() - 1)) {
            if ((n[i] instanceof Map) && (n[i + 1] instanceof String) && (n[i].row + 1) < o.size()
                    && (o[n[i].row + 1] instanceof String) && n[i + 1] == o[n[i].row + 1]) {
                n[i + 1] = ['text': n[i + 1], 'row': n[i].row + 1]
                o[n[i].row + 1] = ['text': o[n[i].row + 1], 'row': i + 1]
            }
        }
        for (i in (n.size() - 1)..<0) {
            if ((n[i] instanceof Map) && (n[i - 1] instanceof String) && n[i].row > 0
                    && (o[n[i].row - 1] instanceof String) && n[i - 1] == o[n[i].row - 1]) {
                n[i - 1] = ['text': n[i - 1], 'row': n[i].row - 1]
                o[n[i].row - 1] = ['text': o[n[i].row - 1], 'row': i - 1]
            }
        }
        return ['o': o, 'n': n]
    }
}
