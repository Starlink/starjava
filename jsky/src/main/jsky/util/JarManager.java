/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: JarManager.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import jsky.util.gui.DialogUtil;


/**
 * Jar file manager: keep jar files up to date with copies on a given remote server.
 */
public final class JarManager {


    public void JarManager() {
    }

    /**
     * Print a description of the jar files in the class path to the
     * given stream.  The format is a properties file where the key is
     * the base name of the jar file and the value is the size in
     * bytes followed by a space followed by the checksum.
     *
     * @param out write the jar file information to this stream
     */
    public void show(OutputStream out) throws IOException {
        String classPath = System.getProperty("java.class.path");
        String sep = System.getProperty("path.separator");
        StringTokenizer st = new StringTokenizer(classPath, sep);
        Properties props = new Properties();
        while (st.hasMoreTokens()) {
            String fileName = st.nextToken();
            if (!fileName.endsWith(".jar"))
                continue;
            File file = new File(fileName);
            long size = file.length();
            long sum = _getChecksum(file);
            props.setProperty(file.getName(), "" + size + " " + sum);
        }
        props.store(out, "JarManager v1.0");
    }

    private long _getChecksum(File file) throws IOException {
        byte[] buf = new byte[1024 * 8];
        FileInputStream in = null;
        Checksum cs = new CRC32();
        try {
            in = new FileInputStream(file);
            while (in.read(buf) != -1)
                cs.update(buf, 0, buf.length);
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
        return cs.getValue();
    }


    /**
     * Print a description of the jar files in the class path to the
     * given file.  The format is a properties file where the key is
     * the base name of the jar file and the value is the size in
     * bytes followed by a space followed by the checksum.
     *
     * @param fileName the name of the properties file in which to store the jar file descriptions
     */
    public void show(String fileName) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            show(out);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException ex) {
                    DialogUtil.error(ex);
                }
            }
        }
    }


    /**
     * Update the jar files in the class path by comparing the current
     * jar file versions with ones described under
     * <em>baseURL/filename</em>. If any jar files need to be
     * downloaded, the user will be asked for confirmation before
     * downloading and then asked to restart the application when the
     * download is complete (using a dialog window).
     *
     * @param baseURL the base URL for downloading jar files and the given fileName
     * @param fileName the base name of the properties file describing the jar files on the server
     */
    public void update(String baseURL, String fileName) {
        // to be done...
    }


    /**
     * main: usage: java
     */
    public static void main(String[] args) {
        try {
            new JarManager().show(System.out);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
        System.exit(0);
    }
}

