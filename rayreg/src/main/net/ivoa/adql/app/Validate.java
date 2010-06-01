/*
 * Adapted from the Apache Xerces sample sax.Counter by Ray Plante for 
 * the National Virtual Observatory.  Thus, the following License applies:
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package net.ivoa.adql.app;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.ParserFactory;

/**
 * A sample SAX2 counter. This sample program illustrates how to
 * register a SAX2 ContentHandler and receive the callbacks in
 * order to print information about the document. The output of
 * this program shows the time and count of elements, attributes,
 * ignorable whitespaces, and characters appearing in the document.
 * <p>
 * This class is useful as a "poor-man's" performance tester to
 * compare the speed and accuracy of various SAX parsers. However,
 * it is important to note that the first parse time of a parser
 * will include both VM class load time and parser initialization
 * that would not be present in subsequent parses with the same
 * file.
 * <p>
 * <strong>Note:</strong> The results produced by this program
 * should never be accepted as true performance measurements.
 *
 * @author Andy Clark, IBM
 *
 * @version $Id: Validate.java,v 1.1 2006/07/17 16:44:37 rplante Exp $
 */
public class Validate extends DefaultHandler {

    //
    // Constants
    //

    // feature ids

    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = 
        "http://xml.org/sax/features/namespaces";

    /** Namespace prefixes feature id 
     *  (http://xml.org/sax/features/namespace-prefixes). */
    protected static final String NAMESPACE_PREFIXES_FEATURE_ID = 
        "http://xml.org/sax/features/namespace-prefixes";

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = 
        "http://xml.org/sax/features/validation";

    /** Schema validation feature id 
     *  (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = 
        "http://apache.org/xml/features/validation/schema";

    /** Schema full checking feature id 
     *  (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = 
        "http://apache.org/xml/features/validation/schema-full-checking";

    /** Dynamic validation feature id 
     *  (http://apache.org/xml/features/validation/dynamic). */
    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = 
        "http://apache.org/xml/features/validation/dynamic";

    /** External schemaLocation property
     *  (http://apache.org/xml/properties/schema/external-schemaLocation) */
    protected static final String EXTERNAL_SCHEMA_LOCATION = 
        "http://apache.org/xml/properties/schema/external-schemaLocation";

    // default settings

    /** Default parser name. */
    protected static final String DEFAULT_PARSER_NAME = 
        "org.apache.xerces.parsers.SAXParser";

    /** Default repetition (1). */
    protected static final int DEFAULT_REPETITION = 1;

    /** Default namespaces support (true). */
    protected static final boolean DEFAULT_NAMESPACES = true;

    /** Default namespace prefixes (false). */
    protected static final boolean DEFAULT_NAMESPACE_PREFIXES = false;

    /** Default validation support (true). */
    protected static final boolean DEFAULT_VALIDATION = true;

    /** Default Schema validation support (true). */
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = true;

    /** Default Schema full checking support (true). */
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = true;

    /** Default dynamic validation support (false). */
    protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;

    /** Default memory usage report (false). */
    protected static final boolean DEFAULT_MEMORY_USAGE = false;

    /** Default "tagginess" report (false). */
    protected static final boolean DEFAULT_TAGGINESS = false;

    //
    // Data
    //

    /** Number of elements. */
    protected long fElements;

    /** Number of attributes. */
    protected long fAttributes;

    /** Number of characters. */
    protected long fCharacters;

    /** Number of ignorable whitespace characters. */
    protected long fIgnorableWhitespace;

    /** Number of characters of tags. */
    protected long fTagCharacters;

    /** Number of other content characters for the "tagginess" calculation. */
    protected long fOtherCharacters;

    /** a list of colon-separated directories to search for schemas in.  **/
    protected LinkedList schemaPath = null;

    /** the map of namespaces to locations */
    protected Properties schemaLocations = null;

    //
    // Constructors
    //

    /** Default constructor. */
    public Validate() { 

        // set the default schema location mapping
        Properties defsloc = new Properties();
        String dsf = System.getProperty("adql.defaultSchemaFiles");
        if (dsf != null) {
            String ns = null;
            String loc = null;
            StringTokenizer tok = new StringTokenizer(dsf);

            while (tok.hasMoreTokens()) {
                ns = tok.nextToken();
                if (! tok.hasMoreTokens()) break;
                loc = tok.nextToken();

                defsloc.setProperty(ns, loc);
            }
        }
        schemaLocations = new Properties(defsloc);

        // set the schema search path
        setSchemaPath(System.getProperty("adql.schemaPath"));

        // set the default schema directory
        // 
        // The schemaDir functionality is deprecated; for backward 
        // compatibility, it will, if set, be placed at the front 
        // of the schema path.
        prependSchemaPath(System.getProperty("adql.schemaDir"));

    } // <init>()

    //
    // Public methods
    //

    /**
     * set the local directory to search for standard schema documents that 
     * define ADQL.
     * @param dir   the directory to search.  If null, no local directory will
     *              be searched, in which case, the inpute XML document must 
     *              provide a xsi:schemaLocation attribute.
     * @throws FileNotFoundException if the directory does not exist
     * @deprecated
     */
    public void setSchemaDir(String dir) throws FileNotFoundException {
        File sdir = new File(dir);
        if (! sdir.exists())
            throw new FileNotFoundException("Directory not found: " + dir);
        if (! sdir.isDirectory()) 
            throw new FileNotFoundException("Not a directory: " + dir);

        prependSchemaPath(dir);
    }

    /**
     * return the local directory that will be searched for standard schema 
     * documents that define ADQL.
     * @return String  the name of the directory or null if no default is 
     *                    been set.  
     * @deprecated
     */
    public String getSchemaDir() { 
        return ((String) ((schemaPath.size() > 0) ? schemaPath.getFirst() 
                                                  : null));
    }

    /**
     * prepend a directory to the schema search path.  No check is done 
     * to determine if the directory exists until it is actually used, at 
     * which time, if it doesn't exist, it will be silently ignored.  
     * @directory   a directory to add to the path
     */
    public void prependSchemaPath(String directory) {
        if (directory != null) schemaPath.addFirst(directory);
    }

    /**
     * append a directory to the schema search path.  No check is done 
     * to determine if the directory exists until it is actually used, at 
     * which time, if it doesn't exist, it will be silently ignored.  
     * @directory   a directory to add to the path
     */
    public void appendSchemaPath(String directory) {
        if (directory != null) schemaPath.addLast(directory);
    }

    /**
     * set the schema search path, replacing any previously set path.
     * This set of directories will be searched, in order, to find schema
     * files whose location is given as a relative file path.  No check is 
     * done to determine if the directories exists until they are actually 
     * used, at which time, if one doesn't exist, it will be silently ignored.
     * @param path    a colon-separated list of directories.  If null,
     *                 the search path is set to be empty.
     */
    public void setSchemaPath(String path) {
        schemaPath = new LinkedList();
        if (path == null) return;

        StringTokenizer st = new StringTokenizer(path,":");
        while (st.hasMoreTokens()) {
            schemaPath.addLast(st.nextToken().trim());
        }
    }

    /**
     * add a namespace-schema location mapping.  If the location is a relative
     * file path, then the schema search path will be searched for its 
     * actual location.  
     * @param nsuri     the namespace URI
     * @param location  either a URL or a file path pointing the XML schema
     *                    document defining that namespace.
     */
    public void addSchemaLocation(String nsuri, String location) {
        schemaLocations.setProperty(nsuri, location);
    }

    /**
     * add namespace-schema location mappings.  The format of the input string
     * is the same as for the value of an xsi:schemaLocation attribute:  it is
     * a list of namespace identifier and location pairs, 
     * e.g., "http://www.ivoa.net/xml/ADQL/v1.0 localdir/ADQL-v1.0.xsd ..."
     * The locations can be URLs or pathnames on a local filesystem.
     * If there is an odd number of names in the list (i.e. ends a namespace 
     * without a location), the last name is silently ignored.  
     * @param nsuri     the namespace URI
     * @param location  either a URL or a file path pointing the XML schema
     *                    document defining that namespace.
     */
    public void addSchemaLocation(String schemaLocation) {
        String ns = null, loc = null;
        StringTokenizer tok = new StringTokenizer(schemaLocation);

        while (tok.hasMoreTokens()) {
            ns = tok.nextToken();
            if (! tok.hasMoreTokens()) break;
            loc = tok.nextToken();
            addSchemaLocation(ns, loc);
        }
    }

    /**
     * return a schema location list in a form needed by the XML Parser.
     * This list will include the user provided mappings as well as the 
     * default schemas found in the schema path.
     */
    public String getSchemaLocation() {
        String ns = null;
        String loc = null;
        File dir = null;
        StringBuffer out = new StringBuffer();

        ArrayList schemaDirs = new ArrayList(schemaPath.size());
        Iterator iter = schemaPath.iterator();
        while (iter.hasNext()) {
            dir = new File((String) iter.next());
            if (dir.exists() && dir.isDirectory()) 
                schemaDirs.add(dir.getAbsoluteFile());
        }

        for(Enumeration e = schemaLocations.propertyNames(); 
            e.hasMoreElements();)
        {
            ns = (String) e.nextElement();
            loc = schemaLocations.getProperty(ns);

            if (loc.indexOf(':') < 0 && ! (new File(loc)).isAbsolute()) 
                loc = findFile(schemaDirs, loc);

            out.append(ns).append(' ').append(loc);
            if (e.hasMoreElements()) out.append(' ');
        }

        return out.toString();
    }

    private String findFile(ArrayList dirs, String loc) {
        String out = loc;
        File f = null;
        Iterator iter = dirs.iterator();
        while (f == null && iter.hasNext()) {
            f = new File((File) iter.next(), loc);
            if (! f.exists()) f = null;
        }
        if (f != null) loc = f.getAbsolutePath();
        return loc;
    }
    

    /** Prints the results. */
    public void printResults(PrintWriter out, String uri, long time,
                             long memory, boolean tagginess,
                             int repetition) {

        // filename.xml: 631 ms (4 elems, 0 attrs, 78 spaces, 0 chars)
        out.print(uri);
        out.print(": ");
        if (repetition == 1) {
            out.print(time);
        }
        else {
            out.print(time);
            out.print('/');
            out.print(repetition);
            out.print('=');
            out.print(time/repetition);
        }
        out.print(" ms");
        if (memory != Long.MIN_VALUE) {
            out.print(", ");
            out.print(memory);
            out.print(" bytes");
        }
        out.print(" (");
        out.print(fElements);
        out.print(" elems, ");
        out.print(fAttributes);
        out.print(" attrs, ");
        out.print(fIgnorableWhitespace);
        out.print(" spaces, ");
        out.print(fCharacters);
        out.print(" chars)");
        if (tagginess) {
            out.print(' ');
            long totalCharacters = fTagCharacters + fOtherCharacters
                                 + fCharacters + fIgnorableWhitespace;
            long tagValue = fTagCharacters * 100 / totalCharacters;
            out.print(tagValue);
            out.print("% tagginess");
        }
        out.println();
        out.flush();

    } // printResults(PrintWriter,String,long)

    //
    // ContentHandler methods
    //

    /** Start document. */
    public void startDocument() throws SAXException {

        fElements            = 0;
        fAttributes          = 0;
        fCharacters          = 0;
        fIgnorableWhitespace = 0;
        fTagCharacters       = 0;

    } // startDocument()

    /** Start element. */
    public void startElement(String uri, String local, String raw,
                             Attributes attrs) throws SAXException {

        fElements++;
        fTagCharacters++; // open angle bracket
        fTagCharacters += raw.length();
        if (attrs != null) {
            int attrCount = attrs.getLength();
            fAttributes += attrCount;
            for (int i = 0; i < attrCount; i++) {
                fTagCharacters++; // space
                fTagCharacters += attrs.getQName(i).length();
                fTagCharacters++; // '='
                fTagCharacters++; // open quote
                fOtherCharacters += attrs.getValue(i).length();
                fTagCharacters++; // close quote
            }
        }
        fTagCharacters++; // close angle bracket

    } // startElement(String,String,StringAttributes)

    /** Characters. */
    public void characters(char ch[], int start, int length)
        throws SAXException {

        fCharacters += length;

    } // characters(char[],int,int);

    /** Ignorable whitespace. */
    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {

        fIgnorableWhitespace += length;

    } // ignorableWhitespace(char[],int,int);

    /** Processing instruction. */
    public void processingInstruction(String target, String data)
        throws SAXException {
        fTagCharacters += 2; // "<?"
        fTagCharacters += target.length();
        if (data != null && data.length() > 0) {
            fTagCharacters++; // space
            fOtherCharacters += data.length();
        }
        fTagCharacters += 2; // "?>"
    } // processingInstruction(String,String)

    //
    // ErrorHandler methods
    //

    /** Warning. */
    public void warning(SAXParseException ex) throws SAXException {
        printError("Warning", ex);
    } // warning(SAXParseException)

    /** Error. */
    public void error(SAXParseException ex) throws SAXException {
        printError("Error", ex);
    } // error(SAXParseException)

    /** Fatal error. */
    public void fatalError(SAXParseException ex) throws SAXException {
        printError("Fatal Error", ex);
        //throw ex;
    } // fatalError(SAXParseException)

    //
    // Protected methods
    //

    /** Prints the error message. */
    protected void printError(String type, SAXParseException ex) {

        System.err.print("[");
        System.err.print(type);
        System.err.print("] ");
        if (ex== null) {
            System.out.println("!!!");
        }
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            System.err.print(systemId);
        }
        System.err.print(':');
        System.err.print(ex.getLineNumber());
        System.err.print(':');
        System.err.print(ex.getColumnNumber());
        System.err.print(": ");
        System.err.print(ex.getMessage());
        System.err.println();
        System.err.flush();

    } // printError(String,SAXParseException)

    //
    // MAIN
    //

    /** Main program entry point. */
    public static void main(String argv[]) {

        // is there anything to do?
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }

        // variables
        Validate validater = new Validate();
        PrintWriter out = new PrintWriter(System.out);
        XMLReader parser = null;
        int repetition = DEFAULT_REPETITION;
        boolean namespaces = DEFAULT_NAMESPACES;
        boolean namespacePrefixes = DEFAULT_NAMESPACE_PREFIXES;
        boolean validation = DEFAULT_VALIDATION;
        boolean schemaValidation = DEFAULT_SCHEMA_VALIDATION;
        boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
        boolean dynamicValidation = DEFAULT_DYNAMIC_VALIDATION;
        boolean memoryUsage = DEFAULT_MEMORY_USAGE;
        boolean tagginess = DEFAULT_TAGGINESS;

        // process arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if (option.equals("p")) {
                    // get parser name
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -p option.");
                        continue;
                    }
                    String parserName = argv[i];

                    // create parser
                    try {
                        parser = XMLReaderFactory.createXMLReader(parserName);
                    }
                    catch (Exception e) {
                        try {
                            Parser sax1Parser = ParserFactory.makeParser(parserName);
                            parser = new ParserAdapter(sax1Parser);
                            System.err.println("warning: Features and properties not supported on SAX1 parsers.");
                        }
                        catch (Exception ex) {
                            parser = null;
                            System.err.println("error: Unable to instantiate parser ("+parserName+")");
                        }
                    }
                    continue;
                }
                if (option.equals("x")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -x option.");
                        continue;
                    }
                    String number = argv[i];
                    try {
                        int value = Integer.parseInt(number);
                        if (value < 1) {
                            System.err.println("error: Repetition must be at least 1.");
                            continue;
                        }
                        repetition = value;
                    }
                    catch (NumberFormatException e) {
                        System.err.println("error: invalid number ("+number+").");
                    }
                    continue;
                }
                if (option.equals("l")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -l " +
                                           "option.");
                        continue;
                    }
                    try {
                        validater.setSchemaDir(argv[i]);
                    }
                    catch (FileNotFoundException ex) {
                        System.err.println("error: no such directory with " +
                                           "read permission: " + argv[i]);
                    }
                }
                if (option.equals("sl")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -sl " +
                                           "option.");
                        continue;
                    }
                    validater.addSchemaLocation(argv[i]);
                }
                if (option.equalsIgnoreCase("n")) {
                    namespaces = option.equals("n");
                    continue;
                }
                if (option.equalsIgnoreCase("np")) {
                    namespacePrefixes = option.equals("np");
                    continue;
                }
                if (option.equalsIgnoreCase("v")) {
                    validation = option.equals("v");
                    continue;
                }
                if (option.equalsIgnoreCase("s")) {
                    schemaValidation = option.equals("s");
                    continue;
                }
                if (option.equalsIgnoreCase("f")) {
                    schemaFullChecking = option.equals("f");
                    continue;
                }
                if (option.equalsIgnoreCase("dv")) {
                    dynamicValidation = option.equals("dv");
                    continue;
                }
                if (option.equalsIgnoreCase("m")) {
                    memoryUsage = option.equals("m");
                    continue;
                }
                if (option.equalsIgnoreCase("t")) {
                    tagginess = option.equals("t");
                    continue;
                }
                if (option.equals("-rem")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -# option.");
                        continue;
                    }
                    System.out.print("# ");
                    System.out.println(argv[i]);
                    continue;
                }
                if (option.equals("h")) {
                    printUsage();
                    continue;
                }
                System.err.println("error: unknown option ("+option+").");
                continue;
            }

            // use default parser?
            if (parser == null) {

                // create parser
                try {
                    parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
                }
                catch (Exception e) {
                    System.err.println("error: Unable to instantiate parser ("+DEFAULT_PARSER_NAME+")");
                    continue;
                }
            }

            // set parser features
            try {
                parser.setFeature(NAMESPACES_FEATURE_ID, namespaces);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
            }
            try {
                parser.setFeature(NAMESPACE_PREFIXES_FEATURE_ID, namespacePrefixes);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACE_PREFIXES_FEATURE_ID+")");
            }
            try {
                parser.setFeature(VALIDATION_FEATURE_ID, validation);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, schemaValidation);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");

            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");

            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
            }
            try {
                parser.setFeature(DYNAMIC_VALIDATION_FEATURE_ID, dynamicValidation);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not support feature ("+DYNAMIC_VALIDATION_FEATURE_ID+")");

            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+DYNAMIC_VALIDATION_FEATURE_ID+")");
            }

            // Set the location of the schemas
            try {
                parser.setProperty(EXTERNAL_SCHEMA_LOCATION, 
                                   validater.getSchemaLocation());
            }
            catch (SAXNotSupportedException e) {
              System.err.println("warning: Parser does not support property (" +
                                 EXTERNAL_SCHEMA_LOCATION + ")");
            }
            catch (SAXNotRecognizedException e) {
              System.err.println("warning: Parser does not recognize property ("
                                 + EXTERNAL_SCHEMA_LOCATION + ")");
            }

            // parse file
            parser.setContentHandler(validater);
            parser.setErrorHandler(validater);
            try {
                long timeBefore = System.currentTimeMillis();
                long memoryBefore = Runtime.getRuntime().freeMemory();
                for (int j = 0; j < repetition; j++) {
                    parser.parse(arg);
                }
                long memoryAfter = Runtime.getRuntime().freeMemory();
                long timeAfter = System.currentTimeMillis();

                long time = timeAfter - timeBefore;
                long memory = memoryUsage
                            ? memoryBefore - memoryAfter : Long.MIN_VALUE;
                validater.printResults(out, arg, time, memory, tagginess,
                                     repetition);
            }
            catch (SAXParseException e) {
                // ignore
            }
            catch (Exception e) {
                System.err.println("error: Parse error occurred - "+e.getMessage());
                Exception se = e;
                if (e instanceof SAXException) {
                    se = ((SAXException)e).getException();
                }
                if (se != null)
                  se.printStackTrace(System.err);
                else
                  e.printStackTrace(System.err);

            }
        }

    } // main(String[])

    //
    // Private static methods
    //

    /** Prints the usage. */
    private static void printUsage() {

        System.err.println("usage: java net.ivoa.adql.apps.Validater " + 
                           "(options) uri ...");
        System.err.println();

        System.err.println("options:");
        System.err.println("  -p name     Select parser by name.");
        System.err.println("  -x number   Select number of repetitions.");
        System.err.println("  -n  | -N    Turn on/off namespace processing" +
                                         "(default: on).");
        System.err.println("  -np | -NP   Turn on/off namespace prefixes" +
                                         "(default: false).");
        System.err.println("              NOTE: Requires use of -n.");
        System.err.println("  -v  | -V    Turn on/off validation." +
                                         "(default: on).");
        System.err.println("  -s  | -S    Turn on/off Schema validation " +
                                         "support (default: on).");
        System.err.println("              NOTE: Not supported by all parsers.");
        System.err.println("  -f  | -F    Turn on/off Schema full checking" +
                                         "(default: on).");
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.");
        System.err.println("  -dv | -DV   Turn on/off dynamic validation.");
        System.err.println("              NOTE: Requires use of -v and not supported by all parsers.");
        System.err.println("  -m  | -M    Turn on/off memory usage report" +
                                         "(default: on).");
        System.err.println("  -t  | -T    Turn on/off \"tagginess\" report." +
                                         "(default: on).");
        System.err.println("  --rem text  Output user defined comment before next parse.");
        System.err.println("  -h          This help screen.");

        System.err.println();
        System.err.println("defaults:");
        System.err.println("  Parser:     "+DEFAULT_PARSER_NAME);
        System.err.println("  Repetition: "+DEFAULT_REPETITION);
        System.err.print("  Namespaces: ");
        System.err.println(DEFAULT_NAMESPACES ? "on" : "off");
        System.err.print("  Prefixes:   ");
        System.err.println(DEFAULT_NAMESPACE_PREFIXES ? "on" : "off");
        System.err.print("  Validation: ");
        System.err.println(DEFAULT_VALIDATION ? "on" : "off");
        System.err.print("  Schema:     ");
        System.err.println(DEFAULT_SCHEMA_VALIDATION ? "on" : "off");
        System.err.print("  Schema full checking:     ");
        System.err.println(DEFAULT_SCHEMA_FULL_CHECKING ? "on" : "off");
        System.err.print("  Dynamic:    ");
        System.err.println(DEFAULT_DYNAMIC_VALIDATION ? "on" : "off");
        System.err.print("  Memory:     ");
        System.err.println(DEFAULT_MEMORY_USAGE ? "on" : "off");
        System.err.print("  Tagginess:  ");
        System.err.println(DEFAULT_TAGGINESS ? "on" : "off");

        System.err.println();
        System.err.println("notes:");
        System.err.println("  The speed and memory results from this program should NOT be used as the");
        System.err.println("  basis of parser performance comparison! Real analytical methods should be");
        System.err.println("  used. For better results, perform multiple document parses within the same");
        System.err.println("  virtual machine to remove class loading from parse time and memory usage.");
        System.err.println();
        System.err.println("  The \"tagginess\" measurement gives a rough estimate of the percentage of");
        System.err.println("  markup versus content in the XML document. The percent tagginess of a ");
        System.err.println("  document is equal to the minimum amount of tag characters required for ");
        System.err.println("  elements, attributes, and processing instructions divided by the total");
        System.err.println("  amount of characters (characters, ignorable whitespace, and tag characters)");
        System.err.println("  in the document.");
        System.err.println();
        System.err.println("  Not all features are supported by different parsers.");

    } // printUsage()

} // class Validater
