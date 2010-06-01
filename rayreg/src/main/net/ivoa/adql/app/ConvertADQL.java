/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005, 2006
 */
package net.ivoa.adql.app;

import net.ivoa.adql.convert.X2STransformer;
import net.ivoa.adql.convert.S2XTransformer;
import net.ivoa.adql.convert.XSLx2s;
import net.ivoa.adql.convert.DOMParser2XML;

import net.ivoa.util.Configuration;

import ncsa.horizon.util.CmdLine;

import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;

public class ConvertADQL {

    public final static String DEFAULT_VERSION = "v1.0";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("ConvertADQL: convert between ADQL/x (xml) " + 
                               "and ADQL/s (SQL)");
            printUsage(new PrintWriter(new OutputStreamWriter(System.out)));
            System.exit(0);
        }

        CmdLine cl = new CmdLine("Xx:Ss:o:t:c:v:", 
                                 (CmdLine.RELAX|CmdLine.USRWARN));

        // parse the command line
        try {
            cl.setCmdLine(args); 
        } catch (CmdLine.UnrecognizedOptionException ex) { }

        // check for option sanity
        if ((cl.isSet('x') || cl.isSet('X')) &&
            (cl.isSet('s') || cl.isSet('S'))   ) 
        {
            System.err.print("ConvertADQL: can't specify both XML (-x|-X) ");
            System.err.println("and string (-s|-S) input!\n");
            printUsage(null);
            System.exit(1);
        }
        if (! (cl.isSet('x') || cl.isSet('X') ||
              (cl.isSet('s') || cl.isSet('S'))  ))
        {
            System.err.print("ConvertADQL: no input type specified ");
            System.err.println("(-x|-X|-s|-S)\n");
            printUsage(null);
            System.exit(1);
        }

        String version = DEFAULT_VERSION;
        if (cl.isSet('v')) version = cl.getValue('v');

        try {
            // get the configuration
            Configuration config = null;
            if (cl.isSet('c')) {
                File cfile = new File(cl.getValue('c'));
                config = new Configuration(cfile.getAbsolutePath(), null);
            }
            if (config == null) {
                config = new Configuration("conf/ConvertADQL.xml", 
                                           (new ConvertADQL()).getClass());
            }
            if (config == null) {
                System.err.print("ConvertADQL: Can't find configuration ");
                System.err.println("file");
                System.exit(2);
            }

            // set output stream
            PrintWriter out = null;
            if (cl.isSet('o')) {
                File ofile = new File(cl.getValue('o'));
                if (ofile.exists() && ! ofile.canWrite()) {
                    System.err.println("ConvertADQL: " + ofile + 
                                       ": permission denied");
                    System.exit(3);
                }
                out = new PrintWriter(new FileWriter(ofile));
            }
            if (out == null) 
                out = new PrintWriter(new OutputStreamWriter(System.out));

            // converting ADQL/x to ADQL/s
            if (cl.isSet('x') || cl.isSet('X')) {
                Reader in = null;

                if (cl.isSet('x')) {
                    in = new FileReader(cl.getValue('x'));
                } else {
                    in = new InputStreamReader(System.in);
                }

                X2STransformer cvt = 
                    createX2S(cl.getValue('t'), version, config);
                String adqls = cvt.transform(new StreamSource(in));
                out.println(adqls);
                out.flush();
            }

            // converting ADQL/s to ADQL/x
            else if (cl.isSet('s') || cl.isSet('S')) {
                String adqls = null;
                if (cl.isSet('s')) {

                    // get input string from a file
                    adqls = slurp(new FileReader(cl.getValue('s')));

                } else if (cl.getNumArgs() > 0) {

                    // get input string from command line
                    StringBuffer sb = new StringBuffer();
                    for(Enumeration e = cl.arguments(); e.hasMoreElements();) {
                        sb.append(e.nextElement());
                        if (e.hasMoreElements()) sb.append(' ');
                    }
                    adqls = sb.toString();

                }
                else {

                    // get input string from standard input
                    adqls = slurp(new InputStreamReader(System.in));

                }

                S2XTransformer cvt = 
                    createS2X(cl.getValue('t'), version, config);
                cvt.transform(adqls, new StreamResult(out));
                out.flush();
            }
        }
        catch (FileNotFoundException e) {
            System.err.println("ConvertADQL: " + e.getMessage() + 
                           ": file not found");
            System.exit(5);
        }
        catch (SAXException e) {
            System.err.println("ConvertADQL: error reading configuration " + 
                               "file: " + e.getMessage());
            System.exit(6);
        }
        catch (IOException e) {
            System.err.println("ConvertADQL: I/O error: " + e.getMessage());
            System.exit(7);
        }
        catch (TransformerException e) {
            System.err.println("ConvertADQL: conversion failed (ADQL syntax " +
                               "error?): " + e.getMessage());
            System.exit(8);
        }
        catch (IllegalArgumentException e) {
            System.err.println("ConvertADQL: problem engaging transformer: " +
                               e.getMessage());
            System.exit(8);
        }
    }

    /**
     * read in an entire file and pack it into a string
     */
    static String slurp(Reader input) throws IOException {
        BufferedReader rdr = new BufferedReader(input);
        StringBuffer sb = new StringBuffer();
        String line = rdr.readLine();
        while (line != null) {
            sb.append(line);
            line = rdr.readLine();
        }
        return sb.toString();
    }

    static Properties typeToClass = new Properties();
    {
       typeToClass.put("XSLx2s", "net.ivoa.adql.convert.XSLx2s");
       typeToClass.put("DOMParser2XML", "net.ivoa.adql.convert.DOMParser2XML");
    }

    /**
     * create and configure a X2STransformer
     */
    static X2STransformer createX2S(String name, String version,
                                    Configuration config) 
         throws IllegalArgumentException, TransformerException
    {
        if (name == null) name = config.getParameter("defaultX2S");
        if (name == null) 
            throw new TransformerException("Configuration error: " +
                                           "no defaultX2S defined");
        String clname = typeToClass.getProperty(name);
        if (clname == null) clname = name;
        if (clname == null) 
            throw new IllegalArgumentException("No default X2STransformer " +
                                               "class specified");

        Class x2sclass = null;
        try {
            x2sclass = Class.forName(clname);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(clname + ": class not found");
        }
        if (! X2STransformer.class.isAssignableFrom(x2sclass)) {
            throw new IllegalArgumentException(clname + 
                                               ": not an X2STransformer class");
        }

        X2STransformer out = null;
        try {
            out = (X2STransformer) Class.forName(clname).newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(clname + ": not instantiable (" +
                                               e.getMessage() + ")");
        }

        int dot = name.indexOf(".");
        while (dot >= 0) {
            name = name.substring(dot+1);
            dot = name.indexOf(".");
        }

        Configuration blck = findConfigSection(config, name, version);
        if (blck != null) {
            out.init(blck);
        }
        else {
            if (version.equals(config.getParameter("version"))) 
                out.init(config);
            else 
                throw new 
                    IllegalArgumentException("Unable to find configuration " + 
                                             "data for version " + version);
        }

        return out;
    }

    private static Configuration findConfigSection(Configuration conf, 
                                                   String name, String version)
    {
        Configuration[] blks = conf.getBlocks(name);
        int i = blks.length;
        for (i=0; i < blks.length; i++) {
            if (version.equals(blks[i].getParameter("version"))) break;
        }

        return ((i < blks.length) ? blks[i] : null);
    }

    /**
     * create and configure a S2XTransformer
     */
    static S2XTransformer createS2X(String name, String version, 
                                    Configuration config) 
         throws IllegalArgumentException, TransformerException
    {
        if (name == null) name = config.getParameter("defaultS2X");
        if (name == null) 
            throw new TransformerException("Configuration error: " +
                                           "no defaultS2X defined");
        String clname = typeToClass.getProperty(name);
        if (clname == null) clname = name;
        if (clname == null) 
            throw new IllegalArgumentException("No default S2XTransformer " +
                                               "class specified");

        Class s2xclass = null;
        try {
            s2xclass = Class.forName(clname);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(clname + ": class not found");
        }
        if (! S2XTransformer.class.isAssignableFrom(s2xclass)) {
            throw new IllegalArgumentException(clname + 
                                               ": not an S2XTransformer class");
        }

        S2XTransformer out = null;
        try {
            out = (S2XTransformer) Class.forName(clname).newInstance();
        }
        catch (Exception e) {
            throw new IllegalArgumentException(clname + ": not instantiable (" +
                                               e.getMessage() + ")");
        }

        int dot = name.indexOf(".");
        while (dot >= 0) {
            name = name.substring(dot+1);
            dot = name.indexOf(".");
        }

        Configuration blck = findConfigSection(config, name, version);
        if (blck != null) {
            out.init(blck);
        }
        else {
            if (version.equals(config.getParameter("version"))) 
                out.init(config);
            else 
                throw new 
                    IllegalArgumentException("Unable to find configuration " + 
                                             "data for version " + version);
        }

        return out;
    }

    /**
     * print the usage message to an output stream
     * @param out   the writer to write to.  If null, print to System.err
     */
    public static void printUsage(PrintWriter out) {
      if (out == null) 
          out = new PrintWriter(new OutputStreamWriter(System.err));

      out.print("ConvertADQL -X|-S|-x xmlfile|-s sqlfile [-o outfile] ");
      out.println("[-t transformer]");
      out.println("              [-c config] [sql...]");
      out.println("Options:");
      out.println("  -X              read and convert XML from standard input");
      out.println("  -x xmlfile      read and convert XML from xmlfile");
      out.print("  -S              read and convert SQL from command line or ");
      out.println("standard input");
      out.println("  -s sqlfile      read and convert SQL from sqlfile");
      out.print("  -o outfile      write results to output file; if not ");
      out.println("given, write to\n                     standard out");
      out.println("  -t transformer  use named transformer (e.g. XSLx2s)");
      out.println("  -c config       load customized config file");
      out.println("Arguments:");
      out.print("  sql             ADQL/s string to convert with -S; if not ");
      out.println("given, read from\n                      standard in");   
      out.flush();
    }

}

