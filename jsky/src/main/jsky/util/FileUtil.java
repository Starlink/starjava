/*
 * ESO Archive
 *
 * $Id: FileUtil.java,v 1.4 2002/08/04 21:48:51 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/24  Created
 */

package jsky.util;

import java.net.*;
import java.io.*;


/**
 * Contains static utility methods for dealing with files and URLs.
 */
public class FileUtil {

    /**
     * Given a URL context (for resolving relative path names) and
     * a string, which may be either a file or a URL string, return a
     * new URL made from the string.
     *
     * @param context the base URL, used to resolve relative path names (may be null)
     * @param fileOrUrlStr a file name or URL string (may be relative)
     */
    public static URL makeURL(URL context, String fileOrUrlStr) {
        URL url = null;
        try {
            if (context != null
                    || fileOrUrlStr.startsWith("http:")
                    || fileOrUrlStr.startsWith("file:")
                    || fileOrUrlStr.startsWith("jar:")
                    || fileOrUrlStr.startsWith("ftp:")) {
                if (context != null)
                    url = new URL(context, fileOrUrlStr);
                else
                    url = new URL(fileOrUrlStr);
            }
            else {
                File file = new File(fileOrUrlStr);
                url = file.getAbsoluteFile().toURL();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return url;
    }


    /**
     * This method returns an InputStream for the given URL, and also wraps it
     * in a BufferedInputStream, if necessary.
     *
     * @param url the URL to read
     */
    public static InputStream makeURLStream(URL url) {
        try {
            InputStream stream = url.openStream();
            if (!(stream instanceof BufferedInputStream))
                stream = new BufferedInputStream(stream);
            return stream;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Given a URL context (for resolving relative path names) and
     * a string, which may be either a file or a URL string, return a
     * new InputStream by creating the URL and opening it for reading.
     *
     * @param context the base URL, used to resolve relative path names
     * @param fileOrUrlStr a file name or URL string (may be relative)
     */
    public static InputStream makeURLStream(URL context, String fileOrUrlStr) {
        return FileUtil.makeURLStream(FileUtil.makeURL(context, fileOrUrlStr));
    }


    /**
     * Copy the given input stream to the given output stream.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[8 * 1024];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    
    /** Return the contents of the URL as a String */
    public static String getURL(URL url) throws IOException {
	InputStream in = makeURLStream(url);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    copy(in, out);
	    return out.toString();
	}
	finally {
	    in.close();
	}
    }


    /** 
     * Return the suffix of a file. This is everything after the first
     * period, so can be confused by the some names with many periods
     *  not related to the suffix, for instance my.file.fits.gz.
     */
    public static String getSuffix( String filename )
    {
        int index = filename.indexOf( '.' );
        if ( index != -1 ) {
            return filename.substring( index );
        }
        return "";
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        System.out.println("test FileUtil.makeURL");
        try {
            URL url = FileUtil.makeURL(new URL("http://testing/"), "/test/passed");
            if (url.toString().equals("http://testing/test/passed"))
                System.out.println("test passed");
            else
                System.out.println("test failed: " + url);
        }
        catch (Exception e) {
            System.out.println("test failed: " + e.getMessage());
        }
    }
}


