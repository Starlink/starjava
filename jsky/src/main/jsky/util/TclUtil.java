/*
 * ESO Archive
 *
 * $Id: TclUtil.java,v 1.9 2002/08/20 09:57:58 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/04  Created
 */

package jsky.util;

import java.util.Vector;


/**
 * Utility class for dealing with Tcl style lists and evaluating Tcl style expressions.
 * <p>
 * Note that this class was previously based on Jacl/TclJava, but was rewritten to
 * improve performance. This version is more simple minded than the Tcl version.
 */
public class TclUtil {

    /**
     * Evaluate the given Tcl expression using the Jacl/TclJava classes
     * and return the result as a String.
     *
     * @param cmd a string containing the Tcl command to evaluate
     * @return the return value of the Tcl command
     *
     public static String eval(String cmd) {
     String result = null;
     try {
     interp.eval(cmd);
     result = interp.getResult().toString();
     } catch(TclException e) {
     throw new RuntimeException(e);
     }
     //System.out.println("XXX TclUtil.eval(" + cmd + ") = " + result);
     return result;
     }
     */


    /**
     * Set the given global Tcl variable to the given value.
     *
     * @param varName the variable name
     * @param value the variable value
     *
     public static void setVar(String varName, Object value)  {
     if (interp == null)
     interp = new Interp();

     try {
     interp.setVar(varName, TclString.newInstance(value.toString()), 0);
     }
     catch(TclException e) {
     throw new RuntimeException(e);
     }
     }
     */


    /**
     * Split a Tcl style list into an array of strings and
     * return the result.
     *
     * @param tclList a String in Tcl list format.
     * @return an array of Strings, one element for each Tcl list item
     */
    public static String[] splitList(String tclList) {
        if (tclList == null || tclList.length() == 0)
            return null;

        tclList = tclList.trim();
        Vector v = new Vector();
        char[] ar = tclList.toCharArray();
        int len = ar.length;
        int depth = 0;
        int i = 0;
        int start = 0;
        boolean ignoreWhitespace = false;
        boolean inQuote = false;

        while (i < len) {
            char c = ar[i];
            if (c == '"')
                inQuote = !inQuote;
            if (c == '{' || (c == '"' && inQuote)) {
                if (depth++ == 0) {
                    ignoreWhitespace = true;
                    start = i + 1;
                }
            }
            else if (c == '}' || (c == '"' && !inQuote)) {
                if (--depth == 0) {
                    ignoreWhitespace = true;
                    if (start == i) {
                        v.add("");
                    }
                    else {
                        v.add(new String(ar, start, i - start).trim());
                    }
                }
            }
            else if (depth == 0) {
                if (Character.isWhitespace(c)) {
                    if (!ignoreWhitespace) {
                        ignoreWhitespace = true;
                        if (start == i) {
                            v.add("");
                        }
                        else {
                            v.add(new String(ar, start, i - start).trim());
                        }
                    }
                }
                else {
                    if (ignoreWhitespace)
                        start = i;
                    ignoreWhitespace = false;
                }
            }
            i++;
        }

        // check last item
        if (!ignoreWhitespace) {
            if (start == i) {
                v.add("");
            }
            else {
                v.add(new String(ar, start, i - start).trim());
            }
        }

        int n = v.size();
        String[] result = new String[n];
        for (i = 0; i < n; i++)
            result[i] = (String) v.get(i);
        return result;
    }

    /**
     * Convert the given array of strings to a Tcl list formatted string
     * and return the result.
     *
     * @param ar an array of Strings, one element for each Tcl list item
     * @return a String in Tcl list format.
     */
    public static String makeList(String[] ar) {
        if (ar == null)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ar.length; i++) {
            sb.append('{');
            sb.append(ar[i]);
            sb.append('}');
            if (i + 1 < ar.length)
                sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Convert the given array of Objects to a Tcl list formatted string
     * and return the result.
     *
     * @param ar an array of Objects, one element for each Tcl list item
     * @return a String in Tcl list format.
     */
    public static String makeList(Object[] ar) {
        if (ar == null)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ar.length; i++) {
            sb.append('{');
            sb.append(ar[i].toString());
            sb.append('}');
            if (i + 1 < ar.length)
                sb.append(' ');
        }
        return sb.toString();
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        String s = "first {second item} \"third item\" last";
        System.out.println("Test list: " + s);
        String[] ar = TclUtil.splitList(s);
        System.out.println("length: " + ar.length);
        for (int i = 0; i < ar.length; i++)
            System.out.println("list[" + i + "] = " + ar[i]);


        // try creating a Tcl list
        System.out.println("makeList(ar) returns: " + TclUtil.makeList(ar));
    }
}
