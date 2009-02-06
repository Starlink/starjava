package net.ivoa.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;

/**
 * a container class that holds application configuration paramters read in 
 * from an XML file.  <p>
 *
 * A Configuration file is an XML file in which parameters are encoded either 
 * as attributes, simple elements containing only text values, or complex 
 * elements--an element containing one or more child elements.  Elements of the 
 * same name may be repeated anywhere in the file.  There are no 
 * requirements on the schema for the file except that there should be no
 * elements with mixed content (i.e., containing both elements and text as 
 * siblings). <p>
 * 
 * A Configuration is created by passing an XML file (either by name or as 
 * an input stream) which is parsed and the parameters loaded into memory.  
 * However, one can retrieve parses and loads into memory the parameters 
 * stored in an XML configuration file.  However, a Configuration object
 * can contain only a "block" of parameters, represented in the file as an 
 * XML element containing one or more child elements; such instances can be
 * retrieved via the getBlocks() method.  <p>
 * 
 * Because the configuration file is XML, the parameters are hierarchical.  
 * One can easily retrieve a parameter deep within the hierarchy via a 
 * hierarchical, slash-delimited path (much like a simple XPath without 
 * predicates).  See getParameters() for details.  
 */
public class Configuration {

    /**
     * the root DOM node of the configuration
     */
    protected Node root = null;

    /**
     * the name associated with this configuration
     */
    protected String name = null;

    /**
     * read the configuration in from an input stream
     * @param strm  the input stream to read configuration from
     * @param name  a name to associate with this configuration (can be null)
     * @throws IOException   if there is a problem while reading the file
     * @throws SAXException  if XML syntax errors are found
     */
    public Configuration(InputStream strm, String name) 
         throws IOException, SAXException 
    {
        load(strm, name);
    }
 
    /**
     * load a named configuration file.  This constructor will look for the 
     * file in the following locations, in order:
     * 
     * <ol>
     *   <li> If the file name is absolute, then it will load it from there.
     *        If that file does not exist, an exception is thrown.
     *   <li> It will look for the file in the class path. 
     *   <li> If the appClass argument is provided, the file will be looked 
     *          for relative to the directory of that class's package path.  
     * </ol>
     *
     * @param config   the XML-encoded config file name
     * @param appClass an optional object representing the object that will 
     *                   use this configuration.  Its class name may be used
     *                   locate the config file.  A null value will be ignored.
     * 
     * @throws FileNotFoundException  if the name configuration file can be
     *    found anywhere.
     * @throws IOException   if there is a problem while reading the file
     * @throws SAXException  if XML syntax errors are found
     */
    public Configuration(String config, Class appClass) 
         throws FileNotFoundException, IOException, SAXException
    {

        // check to see if the file is absolute
        File cfile = new File(config);
        if (cfile.isAbsolute()) { 
            load(new FileInputStream(cfile), null);
        }
        else {
            InputStream cstrm = 
                ClassLoader.getSystemResourceAsStream(config);

            if (cstrm == null && appClass != null) 
                cstrm = appClass.getResourceAsStream(config);

            if (cstrm == null) 
              throw new FileNotFoundException("Can't find configuration file: "
                                              + config);
            load(cstrm, null);
        }
    }

    /**
     * equivalent to Configuration(config, null)
     * @throws FileNotFoundException  if the name configuration file can be
     *    found anywhere.
     * @throws IOException   if there is a problem while reading the file
     * @throws SAXException  if XML syntax errors are found
     */
    public Configuration(String config) 
         throws FileNotFoundException, IOException, SAXException
    {
        this(config, null);
    }

    /**
     * create a Configuration from a DOM node.  This is used internally to 
     * create a Configuration object from a block of XML from the 
     * configuration file.
     * @param root  the DOM node that is to serve as the root of the 
     *                configuration.
     * @param name  a name to associate with this configuration; if null,
     *                a name will be generated from the name of the given
     *                node.
     */
    protected Configuration(Node root, String name) {
        this.root = root;
        this.name = name;
    }

    /**
     * read in the configuration in from the stream.
     * @param strm  the input stream to read configuration from
     * @param name  a name to associate with this configuration; if null,
     *                a name will be generated from the name of the root 
     *                element read from the stream.
     * @throws IOException   if there is a problem while reading the file
     * @throws SAXException  if XML syntax errors are found
     * @throws Error         if there's a problem with the XML parser 
     *                         configuration
     */
    protected void load(InputStream strm, String name) 
         throws IOException, SAXException 
    {
        try {
            DocumentBuilderFactory factory = 
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(strm);
            root = doc.getDocumentElement();

            // concatonate split text values 
            root.normalize();
        }
        catch (ParserConfigurationException e) {
            throw new Error("XML Parser Configuration problem: " + 
                            e.getMessage());
        }

        // set the config name
        if (name == null) name = "/" + root.getNodeName();
        this.name = name;
    }

    /**
     * return the root node of the Configuration
     */
    protected Node getRoot() { return root; }

    /**
     * return the name of this Configuration.  This is typically the path to 
     * XML node in the input that contains the data held by this Configuration;
     * however, this can be overridden by setName();
     */
    public String getName() { return name; }

    /**
     * override the current default name of this Configuration.
     */
    public void setName(String name) { this.name = name; }

    /**
     * return all configuration blocks that match a given path name as a List
     * @param path   the path to the desired XML node.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    protected List findBlocks(String path) throws IllegalArgumentException {
        String name;
        Node node;
        int i;

        MatchedBlocks matched = new MatchedBlocks(this);
        StringTokenizer tok = new StringTokenizer(path, "/");
        while (tok.hasMoreTokens()) {
            boolean attrOnly = false;
            name = tok.nextToken();
            if (name.startsWith("@")) {
                if (tok.hasMoreTokens())
                    throw new IllegalArgumentException("Illegal path: " + path);
                attrOnly = true;
                name = name.substring(1);
            }

            int len = matched.size();
            for(i=0; i < len; i++) {
                Configuration candidate = matched.pop();
                boolean findElements = true;
                if (! tok.hasMoreTokens()) {
                    // at the end of the path; check for matching attributes
                    node = 
                        ((Element) candidate.getRoot()).getAttributeNode(name);

                    if (node != null) 
                        matched.appendNode(node, candidate.getName());
                    if (attrOnly) findElements = false;
                }

                if (findElements) {
                    for(node = candidate.getRoot().getFirstChild(); 
                        node != null; 
                        node = node.getNextSibling())
                    {
                        if (node.getNodeType() == Node.ELEMENT_NODE && 
                            node.getNodeName().equals(name))
                        {
                            matched.appendNode(node, candidate.getName());
                        }
                    }
                }
            }
        }

        return matched;
    }

    /**
     * return all configuration blocks that match a given path name.  
     * @param path   the path to the desired XML node.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public Configuration[] getBlocks(String path) 
         throws IllegalArgumentException 
    {
        List matched = findBlocks(path);
        Configuration[] out = new Configuration[matched.size()];
        ListIterator iter = matched.listIterator();
        for(int i=0; i < out.length && iter.hasNext(); i++) {
            out[i] = (Configuration) iter.next();
        }
        return out;
    }

    class MatchedBlocks extends LinkedList {
        public MatchedBlocks(Configuration first) {
            addLast(first);
        }

        public void appendNode(Node node, String base) { 
            addLast(new Configuration(node, base + "/" + node.getNodeName()));
        }

        public Configuration pop() { return (Configuration) removeFirst(); }
    }

    /**
     * return values of all parameters with a given name
     * @param path   the path to the desired parameter.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public String[] getParameters(String path) {
        List matched = findBlocks(path);

        LinkedList values = new LinkedList();
        ListIterator iter = matched.listIterator();
        while(iter.hasNext()) {
            Node node = ((Configuration) iter.next()).getRoot();
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                values.addLast(node.getNodeValue());
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE) {
                for (node = node.getFirstChild();
                     node != null && node.getNodeType() != Node.TEXT_NODE;
                     node = node.getNextSibling()) 
                { 
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        node = null;
                        break;
                    }
                }
                if (node != null) 
                    values.addLast(node.getNodeValue().trim());
            }
        }

        String[] out = new String[values.size()];
        iter = values.listIterator();
        for(int i=0; i < out.length; i++) {
            out[i] = (String) iter.next();
        }

        return out;
    }

    /**
     * return the first value matching the given parameter name
     * @param path   the path to the desired parameter.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public String getParameter(String path) {
        String[] out = getParameters(path);
        return ((out != null && out.length > 0) ? out[0] : null);
    }
}
