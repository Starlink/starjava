/*
 * ESO Archive
 *
 * $Id: StringUtil.java,v 1.5 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/02  Created
 */

package jsky.util;

import java.net.*;
import java.io.*;


/**
 * Contains static String utility methods.
 */
public class StringUtil {

    /**
     * Replace all occurances of the given target string with the given
     * replacement string in the source string.
     *
     * @param source source string to be searched
     * @param target the target string to replace
     * @param replacement the value to replace the target string with
     * @return a new string with the target string replaced with the replacement
     *         string.
     */
    public static String replace(String source, String target, String replacement) {
        // Note: the implementation could be more efficient...
        StringBuffer sbuf = new StringBuffer(source);
        int n = source.length();
        int offset = 0;
        for (int i = 0; i < n; i++) {
            if (source.startsWith(target, i)) {
                int tl = target.length(), rl = replacement.length();
                sbuf.replace(i + offset, i + offset + tl, replacement);
                offset += (rl - tl);
                i += tl - 1;
            }
        }
        return sbuf.toString();
    }


    /**
     * Split the string s at the given separator char, if found, and return
     * an array containing the two resulting strings, or null if the separator
     * char was not found.
     */
    public static String[] split(String s, int sep) {
        int i = s.indexOf(sep);
        if (i > 0) {
            String[] ar;
            ar = new String[2];
            ar[0] = s.substring(0, i);
            ar[1] = s.substring(i + 1);
            return ar;
        }
        return null;
    }

    /**
     * Return true if the two strings are equal (like String.equals(), but
     * allowing null values for both operands).
     */
    public static boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    /**
     * Checks whether a string matches a given wildcard pattern.
     * Only does ? and * (or '%'), and multiple patterns separated by |.
     * (Taken from http://www.acme.com/java/software/Acme.Utils.html).
     */
    public static boolean match(String pattern, String string) {
        for (int p = 0; ; ++p) {
            for (int s = 0; ; ++p, ++s) {
                boolean sEnd = (s >= string.length());
                boolean pEnd = (p >= pattern.length() ||
                        pattern.charAt(p) == '|');
                if (sEnd && pEnd)
                    return true;
                if (sEnd || pEnd)
                    break;
                if (pattern.charAt(p) == '?')
                    continue;
                if (pattern.charAt(p) == '*' || pattern.charAt(p) == '%') {
                    int i;
                    ++p;
                    for (i = string.length(); i >= s; --i)
                        if (match(pattern.substring(p), string.substring(i)))  /* not quite right */
                            return true;
                    break;
                }
                if (pattern.charAt(p) != string.charAt(s))
                    break;
            }
            p = pattern.indexOf('|', p);
            if (p == -1)
                return false;
        }
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        System.out.println("");
        System.out.println("test StringUtil.replace");
        String source = "replace $this with $that and $this with $that";
        String s = StringUtil.replace(source, "$this", "X");
        System.out.println("s (1) = " + s);
        s = StringUtil.replace(s, "$that", "Y");
        System.out.println("s (2) = " + s);

        System.out.println("");
        System.out.println("test StringUtil.split");
        String[] ar = StringUtil.split("test|passed", '|');
        if (ar == null) {
            System.out.println("test failed");
        }
        else {
            System.out.println(ar[0] + " " + ar[1]);
        }
    }
}


