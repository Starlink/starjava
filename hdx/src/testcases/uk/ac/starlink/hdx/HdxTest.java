package uk.ac.starlink.hdx;

import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.NodeDescendants;

import junit.framework.Assert;

import java.net.URI;
import java.net.URL;
import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Iterator;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.*;
import javax.xml.transform.TransformerException;

import uk.ac.starlink.util.SourceReader;


public class HdxTest
        extends TestCase {

    SourceReader srcrdr;
    HdxFactory fact;

    public HdxTest(String name) {
        super(name);

        srcrdr = new SourceReader();
        srcrdr.setIncludeDeclaration(false);
    }

    public void setUp() 
            throws HdxException, ParserConfigurationException {
        // Hmm, is there any reasonable way to pass system properties to tests?
        //System.err.println("HdxLogging=" + System.getProperty("HdxLogging"));
        // Set the hdx logger and its associated handlers to show all log messages
        if (false) {
            java.util.logging.Level preferredLevel = 
                    java.util.logging.Level.FINE;
            java.util.logging.Logger logger
                    = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");
            logger.setLevel(java.util.logging.Level.ALL);
            for (java.util.logging.Logger tl=logger;
                 tl!=null;
                 tl=tl.getParent()) {
                java.util.logging.Handler[] h = tl.getHandlers();
                for (int i=0; i<h.length; i++) {
                    System.err.println("Setting hdx log level to "
                                       + preferredLevel);
                    h[i].setLevel(preferredLevel);
                }
            }
        }

        fact = HdxFactory.getInstance();
    }
    
    public void testURIResolution()
            throws Exception {


        File cwdFile = new File(System.getProperty("user.dir"));
        File parentFile = cwdFile.getParentFile();

        String cwd = null;
        String parent = null;
        try {
            cwd = cwdFile.toURI().toURL().toString();
            parent = parentFile.toURI().toURL().toString();
        } catch (java.io.IOException ex) {
            fail("could not getCanonicalPath!");
        }
        assert cwd != null;
        assert parent != null;

        // basic cases
        assertEquals(cwd+"HdxTest.java",
                     resolveURI("HdxTest.java"));
        assertEquals(cwd+"HdxTest.java#frag",
                     resolveURI("HdxTest.java#frag"));
        assertEquals(parent+"oops/HdxTest.java",
                     resolveURI("../oops/HdxTest.java"));
        assertEquals("file:/etc/passwd", resolveURI("/etc/passwd"));
        File fileHdxTest = new File("HdxTest.java");

        String absfileHdxTest = null;
        try {
	    absfileHdxTest = 
		fileHdxTest.getAbsoluteFile().toURI().toURL().toString();
	}
	catch (java.io.IOException ex) {
	    fail("could not generate absolute file name");
	}

        assertEquals(cwd+"HdxTest.java",
                     resolveURI(absfileHdxTest));
        assertEquals(parent+"MasterFactory.java",
                     resolveURI("../MasterFactory.java"));

        // Also check that deprecated (because formally invalid) file:
        // URIs are special-cased according to the documentation
        assertEquals(cwd+"HdxTest.java",
                     resolveURI("file:HdxTest.java"));
        assertEquals(cwd+"HdxTest.java#frag",
                     resolveURI("file:HdxTest.java#frag"));
        assertEquals(cwd+"HdxTest.java",
                     resolveURI("FILE:HdxTest.java"));
        assertEquals("file:/etc/passwd", resolveURI("file:/etc/passwd"));
        assertEquals("file:/non-existent-path/HdxTest.java",
                     resolveURI("file:/non-existent-path/HdxTest.java"));
        assertEquals(parent+"MasterFactory.java",
                     resolveURI("file:../MasterFactory.java"));

        // Check non-file URLs aren't mangled
        assertEquals("http://www.starlink.ac.uk/index.html",
                     resolveURI("http://www.starlink.ac.uk/index.html"));
        assertEquals("http:/index.html",
                     resolveURI("http:/index.html"));

        // Now test fullyResolveURI
        String xmlin = "<t xml:base='http://x.org'><a/><b xml:base='http://y.org'/><c xml:base='/level1' ca='hello'/><d xml:base='/level1/'/><e xml:base='/level1/level2'/></t>";
        Element tdoc = stringToDom(xmlin);
        HdxFactory hdxFactory = HdxFactory.getInstance();
        //HdxFactory hdxFactory = HdxFactory.getInstance(new URI("http://x.org"));

        NodeList nl = tdoc.getChildNodes();

        Element kid = (Element)nl.item(0);
        assertEquals("a", kid.getNodeName());
        assertEquals("http://x.org/",
                     hdxFactory.getBaseURI(kid).toString());
        assertEquals("http://x.org/hello.xml",
                     hdxFactory.fullyResolveURI("hello.xml", kid).toString());
        assertEquals("http://www.starlink.ac.uk",
                     hdxFactory.fullyResolveURI("http://www.starlink.ac.uk",
                                                kid).toString());

        kid = (Element)nl.item(1);
        assertEquals("b", kid.getNodeName());
        assertEquals("http://y.org/", hdxFactory.getBaseURI(kid).toString());
        assertEquals("http://y.org/hello.xml",
                     hdxFactory.fullyResolveURI("hello.xml", kid).toString());

        kid = (Element)nl.item(2);
        assertEquals("c", kid.getNodeName());
        assertEquals("http://x.org/level1",
                     hdxFactory.getBaseURI(kid).toString());
        assertEquals("http://x.org/",
                     hdxFactory.getBaseURI
                     (kid.getAttributeNode("xml:base")).toString());
        assertEquals("http://x.org/level1",
                     hdxFactory.getBaseURI
                     (kid.getAttributeNode("ca")).toString());
        assertEquals("http://x.org/hello.xml",
                     hdxFactory.fullyResolveURI("hello.xml", kid).toString());

        kid = (Element)nl.item(3);
        assertEquals("d", kid.getNodeName());
        assertEquals("http://x.org/level1/",
                     hdxFactory.getBaseURI(kid).toString());
        assertEquals("http://x.org/",
                     hdxFactory.getBaseURI
                     (kid.getAttributeNode("xml:base")).toString());
        assertEquals("http://x.org/level1/hello.xml",
                     hdxFactory.fullyResolveURI("hello.xml", kid).toString());

        kid = (Element)nl.item(4);
        assertEquals("e", kid.getNodeName());
        assertEquals("http://x.org/level1/level2",
                     hdxFactory.getBaseURI(kid).toString());
        assertEquals("http://x.org/",
                     hdxFactory.getBaseURI
                     (kid.getAttributeNode("xml:base")).toString());
        assertEquals("http://x.org/level1/hello.xml",
                     hdxFactory.fullyResolveURI("hello.xml", kid).toString());

        String hdxxmlin = "<hdx><ndx><image uri='file.xml'/></ndx></hdx>";
        tdoc = stringToDom(hdxxmlin);
        HdxContainer hdx = hdxFactory.newHdxContainer(tdoc,
                                                      new URI("http://x.org"));
        Element de = hdx.getDOM(null);
        // This is not a `native' HdxNode
        assertTrue(!(de instanceof HdxNode));
        kid = (Element)de.getFirstChild().getFirstChild();
        assertEquals("image", kid.getNodeName());
        hdxFactory = HdxFactory.findFactory(de);
        assertEquals("http://x.org/file.xml",
                     hdxFactory
                     .fullyResolveURI(kid.getAttribute("uri"), kid)
                     .toString());
    }

    public void testHdxDocumentImportNode()
            throws Exception {
        String xmlin
      = "<doc id='top'><child id='kid'>Hello there<grandchild name='gc1'/><?pis just say no?><grandchild name='gc2' att='low'>kid</grandchild><!--nothing--></child>Ouch!?/.<child id='kid2'/></doc>";
        Element in = stringToDom(xmlin);
        
        Document hdx
                = HdxDOMImplementation
                .getInstance()
                .createDocument(null, "test", null);
        assertNotNull(hdx);
        assertDOMEquals("<doc id='top'/>", hdx.importNode(in, false));
        Node hdxnode = hdx.importNode(in, true);
        assertDOMSane(hdxnode);
        assertEquals(Node.ELEMENT_NODE, hdxnode.getNodeType());

        Element hdxel = (Element)hdxnode;
        assertTrue(hdxel.getOwnerDocument() != in.getOwnerDocument());
        assertSame(hdx, hdxel.getOwnerDocument());
        assertDOMEquals(xmlin, hdxel);

        // Find all elements
        NodeList nl = hdxel.getElementsByTagName("*");
        assertEquals(5, nl.getLength());
        assertEquals("doc", nl.item(0).getNodeName());
        assertEquals("child", nl.item(1).getNodeName());
        assertEquals("grandchild", nl.item(2).getNodeName());
        assertEquals("grandchild", nl.item(3).getNodeName());
        assertEquals("child", nl.item(4).getNodeName());
        
        nl = hdxel.getElementsByTagName("child");
        assertEquals(2, nl.getLength());
    }

    public void testTreeManipulations() {

        Document doc = HdxDOMImplementation
                .getInstance()
                .createDocument(null, "hdx", null);
        
        Element root = doc.createElement("hdx");
        doc.appendChild(root);

        Element ndx = doc.createElement("ndx");
        root.appendChild(ndx);
        
        Element image = doc.createElement("image");
        image.setAttribute("url", "file:something.sdf");
        ndx.appendChild(image);
        
        Element var = doc.createElement("variance");
        Attr att = doc.createAttribute("url");
        att.setValue("file:something.fits");
        var.setAttributeNode(att);
        var.setAttribute("uri", "fits://uist.jach.hawaii.edu/yesterday");
        ndx.appendChild(var);

        // Now check the DOM we've created
        Node t = root.getFirstChild();
        assertNotNull(t);
        assertEquals(Node.ELEMENT_NODE, t.getNodeType());
        assertEquals("ndx", t.getNodeName());
        assertNull(t.getNextSibling());

        t = t.getFirstChild();
        assertNotNull(t);
        assertEquals(Node.ELEMENT_NODE, t.getNodeType());
        assertEquals("image", t.getNodeName());
        assertEquals("file:something.sdf", ((Element)t).getAttribute("url"));

        t = t.getNextSibling();
        assertNotNull(t);
        assertEquals(Node.ELEMENT_NODE, t.getNodeType());
        assertEquals("variance", t.getNodeName());
        assertEquals("file:something.fits", ((Element)t).getAttribute("url"));
        assertEquals("fits://uist.jach.hawaii.edu/yesterday",
                     ((Element)t).getAttribute("uri"));

        t = t.getNextSibling();
        assertNull(t);
        
        Element[] etc = new Element[] {
            doc.createElement("etca"),
            doc.createElement("etcb"),
            doc.createElement("etcc"),
            doc.createElement("etcd"),
            doc.createElement("etce"),
            doc.createElement("etcf"),
            doc.createElement("etcg"),
            doc.createElement("etch"),
            doc.createElement("etci"),
        };
        assertEquals(etc[0], ndx.insertBefore(etc[0], image));
        assertEquals(etc[1], ndx.insertBefore(etc[1], var));
        assertEquals(etc[2], ndx.insertBefore(etc[2], null));
        assertNodeNameSequence
                (new String[] { "etca", "image", "etcb", "variance", "etcc" },
                 ndx.getFirstChild());
        
        assertEquals(var,    ndx.replaceChild(etc[3], var));
        assertEquals(image,  ndx.removeChild(image));
        assertEquals(etc[0], ndx.replaceChild(etc[4], etc[0]));
        assertEquals(etc[2], ndx.replaceChild(etc[5], etc[2]));
        assertNodeNameSequence
                (new String[] { "etce", "etcb", "etcd", "etcf" },
                 ndx.getFirstChild());

        DocumentFragment df = doc.createDocumentFragment();
        df.appendChild(etc[6]);
        df.appendChild(etc[7]);
        df.appendChild(etc[8]);
        assertEquals(etc[1], ndx.replaceChild(df, etc[1]));
        assertNodeNameSequence
                (new String[] { "etce", "etcg", "etch", "etci",
                                        "etcd", "etcf" },
                 ndx.getFirstChild());
    }

    public void testText() 
            throws Exception {
        String xmlin = "<top><t1>HelloThere</t1><t2>onetwo<t3>low</t3>ten</t2><t4/></top>";
        Document hdxdoc = HdxDOMImplementation.getInstance()
                .createDocument(null, "test", null);
        Node top = hdxdoc.importNode(stringToDom(xmlin), true);

        class NodeVisitor implements NodeDescendants.Visitor {
            List l;
            NodeVisitor() {
                l = new java.util.ArrayList();
            }
            public void reset() {
                l.clear();
            }
            public List get() {
                return l;
            }
            public List getValues() {
                for (int i=0; i<l.size(); i++)
                    l.set(i,((Node)l.get(i)).getNodeValue());
                return l;
            }
            public Object visitNode(Node n) {
                l.add(n);
                return null;
            }
        }
        NodeVisitor nv = new NodeVisitor();
        NodeDescendants nodeset = new NodeDescendants
                (top,
                 NodeDescendants.SHOW_TEXT|NodeDescendants.SHOW_CDATA_SECTION);
        nodeset.visitTree(nv);

        // Extract the text nodes from the tree
        List l = nv.getValues();
        assertEquals(4, l.size());
        assertEquals("HelloThere", (String)l.get(0));
        assertEquals("onetwo", (String)l.get(1));
        assertEquals("low", (String)l.get(2));
        assertEquals("ten", (String)l.get(3));

        nodeset.reset(NodeDescendants.SHOW_ELEMENT);
        nv.reset();
        nodeset.visitTree(nv);
        l = nv.get();
        assertEquals(5, l.size());

        // splitText() contents of <t1>
        Node n = (Node)l.get(1);
        ((Text)n.getFirstChild()).splitText(5);

        // Add new text nodes
        n = (Node)l.get(2);
        Node n3 = (Node)l.get(3);
        n.insertBefore(hdxdoc.createTextNode("three"), n3);
        n.insertBefore(hdxdoc.createTextNode("four"), n3.getNextSibling());
        top.insertBefore(hdxdoc.createTextNode(""), n);

        // append/insert/delete/replace
        n3.appendChild(hdxdoc.createTextNode("lower"));
        Text tn = (Text)n3.getLastChild();
        tn.appendData("lowest"); // now lowerlowest
        tn.insertData(5, "wick"); // lowerwicklowest
        tn.deleteData(3, 2);     // lowwicklowest
        tn.replaceData(3, 7, "kerry"); // lowkerryest
        assertEquals("kerry", tn.substringData(3, 5));
        assertEquals("low", tn.substringData(0, 3));

        // assorted new blank nodes
        n = (Node)l.get(4);
        top.insertBefore(hdxdoc.createTextNode(""), n);
        top.appendChild(hdxdoc.createTextNode(""));
        n.appendChild(hdxdoc.createTextNode(""));

        // confirm we've got what we think we've got
        nodeset.reset
                (NodeDescendants.SHOW_TEXT|NodeDescendants.SHOW_CDATA_SECTION);
        nv.reset();
        nodeset.visitTree(nv);
        l = nv.getValues();
        assertEquals(12, l.size());
        assertEquals("Hello",	(String)l.get(0));
        assertEquals("There",	(String)l.get(1));
        assertEquals("",	(String)l.get(2));
        assertEquals("onetwo",	(String)l.get(3));
        assertEquals("three",	(String)l.get(4));
        assertEquals("low",	(String)l.get(5));
        assertEquals("lowkerryest",	(String)l.get(6));
        assertEquals("four",	(String)l.get(7));
        assertEquals("ten",	(String)l.get(8));
        assertEquals("",	(String)l.get(9));
        assertEquals("",	(String)l.get(10));
        assertEquals("",	(String)l.get(11));

        // Test normalize()
        top.normalize();
        nodeset.reset
                (NodeDescendants.SHOW_TEXT|NodeDescendants.SHOW_CDATA_SECTION);
        nv.reset();
        nodeset.visitTree(nv);
        l = nv.getValues();
        assertEquals(4, l.size());
        assertEquals("HelloThere",	(String)l.get(0));
        assertEquals("onetwothree",	(String)l.get(1));
        assertEquals("lowlowkerryest",	(String)l.get(2));
        assertEquals("fourten",		(String)l.get(3));
    }

    public void testBackingElements()
            throws Exception {

        if (XmlParserFeatureset.getInstance().xalanHasEndEndDocumentBug())
            return;

        String xmlstring = "<ndx><title>Title</title><image uri='file:test1.fits' url='file:nothing.fits'/></ndx>";
   
        HdxFactory docfact = HdxFactory.getInstance();
        Document stringdoc = (Document)srcrdr.getDOM
                (new StreamSource(new StringReader(xmlstring)));
        Element origDoc = stringdoc.getDocumentElement();
        HdxContainer hdxContainer = docfact.newHdxContainer(origDoc);
        assertNotNull(hdxContainer);
        Element hdx = hdxContainer.getDOM(null);
        assertNotNull(hdx);
        assertTrue(HdxResourceType.HDX.isValid(hdx));

        // Modify image attributes and check
        NodeList nl = hdx.getElementsByTagName("image");
        assertEquals(1, nl.getLength());
        Element hdximage = (Element)nl.item(0);
        hdximage.setAttribute("uri", "WALLOP"); // current: is shadowed
        assertEquals("WALLOP", hdximage.getAttribute("uri"));

        ((HdxElement)hdximage).setShadowAttributes(false);
        hdximage.setAttribute("ping", "KERPOW"); // new: not shadowed
        assertEquals("KERPOW", hdximage.getAttribute("ping"));

        ((HdxElement)hdximage).setShadowAttributes(true);
        hdximage.setAttribute("pong", "SPLAT"); // new: is shadowed
        assertEquals("SPLAT", hdximage.getAttribute("pong"));

        // Check changes have been made
        assertDOMEquals
                ("<hdx><ndx><title value='Title'/><image uri='WALLOP' url='file:nothing.fits' ping='KERPOW' pong='SPLAT'/></ndx></hdx>",
                 hdx);
        // Check original DOM doesn't have ping, but does have pong
        assertDOMEquals("<ndx><title>Title</title><image uri='WALLOP' url='file:nothing.fits' pong='SPLAT'/></ndx>", origDoc);
        
        // Check getAttributes does work on the values
        NamedNodeMap nm = hdximage.getAttributes();
        Attr uriatt = (Attr)nm.getNamedItem("uri");
        assertNotNull(uriatt);
        uriatt.setValue("OUCH"); // set value on Attr, propagated to shadow
        assertTrue(hdximage.hasAttribute("uri")); // still
        assertEquals("OUCH", hdximage.getAttribute("uri"));
        Attr pingatt = (Attr)nm.getNamedItem("ping");
        assertNotNull(pingatt);
        pingatt.setValue("THWACK");

        // Check again
        assertDOMEquals
                ("<hdx><ndx><title value='Title'/><image uri='OUCH' url='file:nothing.fits' ping='THWACK' pong='SPLAT'/></ndx></hdx>",
                 hdx);
        assertDOMEquals
                ("<ndx><title>Title</title><image uri='OUCH' url='file:nothing.fits' pong='SPLAT'/></ndx>",
                 origDoc);
    }

    public void testBackingElementNamespaced()
            throws Exception {
        // Same as testBackingElement(), but with elements and
        // attributes all in the non-default namespace.
        String xmlstring = "<doc xmlns:h='http://www.starlink.ac.uk/HDX' h:hdxname='ndx'><sub h:hdxname='image' h:uri='file:test1.fits' uri='nothing'/><h:image h:uri='file:test2.fits'/></doc>";

        HdxFactory docfact = HdxFactory.getInstance();
        Document stringdoc = (Document)srcrdr.getDOM
                (new StreamSource(new StringReader(xmlstring)));
        Element origDoc = stringdoc.getDocumentElement();
        HdxContainer hdxContainer = docfact.newHdxContainer(origDoc);
        assertNotNull(hdxContainer);
        Element hdx = hdxContainer.getDOM(null);
        assertNotNull(hdx);
        assertTrue(HdxResourceType.HDX.isValid(hdx));

        // Modify image attributes and check
        NodeList nl = hdx.getElementsByTagName("image");
        assertEquals(2, nl.getLength());
        Element firstImage = (Element)nl.item(0); // for below
        for (int i=0; i<2; i++) {
            Element hdximage = (Element)nl.item(i);
            hdximage.setAttribute("uri", "WALLOP"); // _is_ shadowed
            assertEquals("WALLOP", hdximage.getAttribute("uri"));

            ((HdxElement)hdximage).setShadowAttributes(false);
            hdximage.setAttribute("ping", "KERPOW"); // new -- not shadowed
            assertEquals("KERPOW", hdximage.getAttribute("ping"));

            ((HdxElement)hdximage).setShadowAttributes(true);
            hdximage.setAttribute("pong", "SPLAT");
            assertEquals("SPLAT", hdximage.getAttribute("pong"));
        }
        
        // Check changes have been made
        assertDOMEquals
                ("<hdx><ndx><image uri='WALLOP' ping='KERPOW' pong='SPLAT'/><image uri='WALLOP' ping='KERPOW' pong='SPLAT'/></ndx></hdx>",
                 hdx);
        // Check original DOM doesn't have ping, but does have pong
        assertDOMEquals("<doc xmlns:h='http://www.starlink.ac.uk/HDX' h:hdxname='ndx'><sub h:hdxname='image' h:uri='WALLOP' uri='nothing' h:pong='SPLAT'/><h:image h:uri='WALLOP' h:pong='SPLAT'/></doc>",
                        origDoc);
        
        // Check getAttributes does work on the values
        NamedNodeMap nm = firstImage.getAttributes();
        Attr uriatt = (Attr)nm.getNamedItem("uri");
        assertNotNull(uriatt);
        uriatt.setValue("OUCH");
        assertTrue(firstImage.hasAttribute("uri")); // still
        assertEquals("OUCH", firstImage.getAttribute("uri"));
        Attr pingatt = (Attr)nm.getNamedItem("ping");
        assertNotNull(pingatt);
        pingatt.setValue("THWACK");

        // Check again
        assertDOMEquals
                ("<hdx><ndx><image uri='OUCH' ping='THWACK' pong='SPLAT'/><image uri='WALLOP' ping='KERPOW' pong='SPLAT'/></ndx></hdx>",
                 hdx);
        // Check original DOM doesn't have ping, but does have pong
        assertDOMEquals("<doc xmlns:h='http://www.starlink.ac.uk/HDX' h:hdxname='ndx'><sub h:hdxname='image' h:uri='OUCH' uri='nothing' h:pong='SPLAT'/><h:image h:uri='WALLOP' h:pong='SPLAT'/></doc>",
                        origDoc);
    }

    public void testNamespaceParsing()
            throws Exception {
        String[] xmlFiles = new String[] {
            "no-ns.xml", "redefining.xml", "embedded.xml" 
        };
        Class c = this.getClass();
        
        for (int i=0; i<xmlFiles.length; i++) {
            URL url = c.getResource(xmlFiles[i]);
            HdxContainer hdx = fact.newHdxContainer(url);
            assertDOMEquals(c.getResource(xmlFiles[i]+"-correct"),
                            hdx.getDOM(null));
        }
    }    

    // testToSource is essentially the same as testNoNS, except that
    // it goes via the HdxContainer.getSource() method
    public void testToSource()
            throws Exception {
        // Tests the Source obtained from HdxContainer
        Class c = this.getClass();
        URL url = c.getResource("no-ns.xml");
        assertNotNull(url);
        HdxContainer hdx = fact.newHdxContainer(url);
        assertNotNull(hdx);
        Source source = hdx.getSource(null);
        assertNotNull(source);
        DOMResult dr = new DOMResult();
        TransformerFactory
                .newInstance()
                .newTransformer()
                .transform(source, dr);
        Node resultNode = dr.getNode();
        assertEquals(Node.DOCUMENT_NODE, resultNode.getNodeType());
        assertDOMEquals(c.getResource("no-ns.xml-correct"),
                        ((Document)resultNode).getDocumentElement());
    }

    public void testHashCode()
            throws Exception {
        HdxNode.HashCode hc1 = new HdxNode.HashCode(1);
        HdxNode.HashCode hc1bis = new HdxNode.HashCode().add(1);
        assertEquals(hc1.value(), hc1bis.value());
        HdxNode.HashCode hc2 = new HdxNode.HashCode(2);
        assertTrue(hc1.value() != hc2.value());
        hc1.add(2);
        hc1bis.add(2);
        assertEquals(hc1.value(), hc1bis.value()); // deterministic
        hc1.add(3);             // now equiv .add(1).add(2).add(3)
        hc2.add(1).add(3);      // now equiv .add(2).add(1).add(3)
        assertTrue(hc1.value() != hc2.value()); // order-dependent
        // The following test values are ones that cropped up during
        // testing, surprisingly giving repeated hashCodes.  This is
        // redundant with the previous test, but is done with somewhat
        // more `random' numbers than 1, 2, 3.
        HdxNode.HashCode hc3 = new HdxNode.HashCode()
                .add(-1969829113)
                .add(1038930352)
                .add(-1318889626);
        HdxNode.HashCode hc4 = new HdxNode.HashCode()
                .add(-1969829113)
                .add(-1318889626)
                .add(1038930352);
        assertTrue(hc3.value() != hc4.value());
    }

    // Test Node.cloneNode.  This test, and testObjectOverrides below,
    // should test a broader range of types, particularly including
    // DocumentFragments.
    public void testCloneNode()
            throws Exception {
        String xmlin = "<top a='av1'><child b='bv1'><grandchild/></child><child b='bv2' c2='cv2'>test text</child></top>";
        Document hdxdoc = HdxDOMImplementation.getInstance()
                .createDocument(null, "test", null);
        Node innode = hdxdoc.importNode(stringToDom(xmlin), true);
        assertTrue(innode instanceof HdxNode);
        assertEquals(Node.ELEMENT_NODE, innode.getNodeType());

        // Sanity checks
        assertTrue(innode instanceof HdxElement);
        Element in = (Element)innode;
        assertEquals("top", in.getNodeName());

        // Make sure the following assertions start off true!
        assertDOMEquals(xmlin, in);
        Node kid = in.getFirstChild();
        Node grandkid = kid.getFirstChild();
        assertEquals("child", kid.getNodeName());
        assertEquals("grandchild", grandkid.getNodeName());
        assertSame(in, kid.getParentNode());
        assertSame(kid, grandkid.getParentNode());

        // cloneNode(false) -- shallow copy
        Node shallowcopy = in.cloneNode(false);
        assertDOMSane(shallowcopy);
        assertDOMEquals(xmlin, in); // we haven't messed up the original
        assertNull(shallowcopy.getParentNode());
        assertTrue(shallowcopy instanceof HdxNode);
        assertDOMEquals("<top a='av1'/>", shallowcopy);

        // cloneNode(true) -- deep copy
        Node deepcopy = in.cloneNode(true);
        assertDOMSane(deepcopy);
        assertDOMEquals(xmlin, in); // we haven't messed up the original
        assertTrue(deepcopy instanceof HdxNode);
        assertNull(deepcopy.getParentNode());
        assertEquals(Node.ELEMENT_NODE, deepcopy.getNodeType());
        assertDOMEquals(in, deepcopy);

        // Parent-child relationships still OK? (these tests are
        // probably redundant after the assertDOMSane(deepcopy)
        // assertion above.
        Node kid2 = deepcopy.getFirstChild();
        Node grandkid2 = kid2.getFirstChild();
        assertEquals("child", kid2.getNodeName());
        assertEquals("grandchild", grandkid2.getNodeName());
        assertSame(deepcopy, kid2.getParentNode());
        assertSame(kid2, grandkid2.getParentNode());

        // Explicitly verify that modifying the original does not affect the clone
        in.appendChild(in.getOwnerDocument().createElement("changeling"));
        messUpNode(in);
        assertDOMEquals("<top a='av1x'><child b='bv1x'><grandchild/></child><child b='bv2x' c2='cv2x'>test textx</child><changeling/></top>", in); // ie, we did change it
        assertDOMEquals(xmlin, deepcopy); // initial value, still
        assertDOMEquals("<top a='av1'/>", shallowcopy); // ...still
    }

    // Check the behaviour of clone(), and the equivalence relations
    // for equals() and hashCode().  See Object.clone()
    public void testObjectOverrides() 
            throws Exception {
        String xmlin = "<top a='av1'><child b='bv1'/><child b='bv2' c2='cv2'>test text</child></top>";
        Document hdxdoc = HdxDOMImplementation.getInstance()
                .createDocument(null, "test", null);
        Node inn = hdxdoc.importNode(stringToDom(xmlin), true);
        assertTrue(inn instanceof HdxNode);
        assertTrue(inn instanceof HdxElement);
        HdxNode in = (HdxNode)inn;
        HdxNode inclone = (HdxNode)in.clone();
        //assertTrue(inclone instanceof HdxNode);
        Node clone2 = (Node)inclone.clone();

        // Clone has same parent
        assertSame(in.getParentNode(), inclone.getParentNode());

        assertDOMSane(inclone);

        assertTrue(in != inclone);
        assertTrue(inclone.getClass() == in.getClass());

        assertTrue(in.equals(in)); // reflexive
        assertTrue(inclone.equals(in));
        assertTrue(in.equals(inclone)); // symmetric
        assertTrue(in.equals(inclone)); // consistent
        assertTrue(! in.equals(null)); // inequivalence with null
        assertTrue(inclone.equals(clone2));
        assertTrue(in.equals(clone2)); // transitive

        int inhash1 = in.hashCode();
        assertEquals(inhash1, in.hashCode()); // consistent
        assertEquals(inhash1, inclone.hashCode()); // respects equals()
        NodeList nl = ((Element)in).getElementsByTagName("child");
        assertEquals(2, nl.getLength());
        Element child = (Element)nl.item(1);
        child.setAttribute("b", "bv3"); // change one attribute
        // It's not a requirement that hashcodes are different for
        // non-equal() trees, so this test could fail by statistical
        // chance.  But if it does fail it almost certainly means that
        // hashCode() is producing the same hash for all trees.
        assertTrue(inhash1 != in.hashCode());

        // Test same elements, different order (should be different)
        inhash1 = clone2.hashCode();
        Node kid1 = clone2.getFirstChild();
        clone2.removeChild(kid1);
        clone2.appendChild(kid1);
        assertDOMEquals("<top a='av1'><child b='bv2' c2='cv2'>test text</child><child b='bv1'/></top>", clone2); // has changed
        assertTrue(inhash1 != clone2.hashCode());

        // Test same attributes, different order (should be the same)
        Element e1 = hdxdoc.createElement("t");
        e1.setAttribute("a", "1");
        e1.setAttribute("b", "2");
        Element e2 = hdxdoc.createElement("t");
        e2.setAttribute("b", "2");
        e2.setAttribute("a", "1");
        assertTrue(e1.hashCode() == e2.hashCode());

        // Explicitly verify that modifying the original does not affect the clone
        messUpNode(in);
        assertDOMEquals("<top a='av1x'><child b='bv1x'/><child b='bv3x' c2='cv2x'>test textx</child></top>", in); // ie, we did change it
        assertDOMEquals(xmlin, inclone); // initial value
    }

    /* ******************** PROTECTED HELPER METHODS ******************** */
    /**
     * Checks a DOM for sanity.  Mostly checks parent-child-sibling
     * relationships.
     *
     * <p>According to the DOM2 standard, Attr nodes <em>may</em> have
     * <code>Text</code> or <code>EntityReference</code> children.
     * This Hdx implementation, however, does not support
     * <code>EntityReference</code> children, and creates only a
     * single <code>Text</code> child below its <code>Attr</code>
     * nodes, and this is what this method checks.
     * That is, this method may incorrectly assert that a Node
     * obtained from a non-Hdx DOM is invalid on this account, when it
     * is not in fact.
     *
     * @param n Node to be thrashed
     * @throws AssertionError if any problem is found
     */
    static void assertDOMSane(Node n) {
        if (n.hasAttributes()) {
            assertEquals(Node.ELEMENT_NODE, n.getNodeType());
            Element e = (Element)n;
            NamedNodeMap nm = e.getAttributes();
            for (int i=0; i<nm.getLength(); i++) {
                Attr a = (Attr)nm.item(i);
                assertSame(a, e.getAttributeNode(a.getName()));
                assertEquals(e.getAttribute(a.getName()), a.getValue());
                assertNull(a.getParentNode());
                assertNull(a.getNextSibling());
                assertNull(a.getPreviousSibling());
                Node tkid = a.getFirstChild();
                assertEquals(Node.TEXT_NODE, tkid.getNodeType());
                assertEquals(a.getValue(), tkid.getNodeValue());
                assertSame(tkid, a.getLastChild());
                assertSame(e, a.getOwnerElement());
            }
        }
        NodeList nl = n.getChildNodes();
        if (nl.getLength() == 0) {
            assertTrue(! n.hasChildNodes());
            assertNull(n.getFirstChild());
            assertNull(n.getLastChild());
        } else {
            assertTrue(n.hasChildNodes());
            Node kid = n.getFirstChild();
            Node lastKid = null;
            for (int nn = 0; nn<nl.getLength(); nn++) {
                assertSame(kid, nl.item(nn));
                if (nn == 0) {
                    // first one
                    assertNull(kid.getPreviousSibling());
                } else {
                    assertSame(lastKid.getNextSibling(), kid);
                    assertSame(lastKid, kid.getPreviousSibling());
                }

                assertSame(n, kid.getParentNode());
                assertDOMSane(kid);

                lastKid = kid;
                kid = kid.getNextSibling();
                if (nn == nl.getLength()-1) // last one
                    assertNull(kid);
            }
            assertNull(kid);
        }
    }
    
    /* ******************** PRIVATE HELPER METHODS ******************** */
    
    private String domToString(Node d) 
            throws TransformerException {
        java.io.OutputStream os = new java.io.ByteArrayOutputStream();
        srcrdr.writeSource(new DOMSource(d), os);
        return os.toString();
    }

    private Element stringToDom(String s) 
            throws TransformerException {
        return srcrdr.getElement(new StreamSource(new StringReader(s)));
    }

    private String resolveURI(String uriString) {
        try {
            URI uri = new URI(uriString);
            URL url = HdxFactory.getInstance().fullyResolveURI(uri,null); 
           //URL url = HdxFactory.getInstance().fullyResolveURI(uri);
            return url.toString();
        } catch (HdxException ex) {
            return "(HdxException: " + ex + ")";
        } catch (java.net.URISyntaxException ex) {
            return "(URI exception: " + ex + ")";
        }
    }

    // Messes up the tree, by adding an x to everything it can
    private static void messUpNode(Node n) {
        NamedNodeMap nm = n.getAttributes();
        if (nm != null)
            for (int i=0; i<nm.getLength(); i++) {
                Attr a = (Attr)nm.item(i);
                a.setValue(a.getValue()+"x");
            }
        String v = n.getNodeValue();
        if (v != null)
            n.setNodeValue(v+"x");
        for (Node kid=n.getFirstChild(); kid!=null; kid=kid.getNextSibling())
            messUpNode(kid);
    }
    
    // Asserts that the chain of getNextSibling nodes starting with n
    // is the same length as the array of strings s, and that the
    // getNodeName() of each of the nodes is the same as the
    // corresponding element of s.  Throws an assertion error if
    // either of these is false.
    private void assertNodeNameSequence(String[] s, Node n) {
        java.util.Iterator si = java.util.Arrays.asList(s).iterator();
        while (true) {
            if (n == null || !si.hasNext()) {
                // end of the list
                assertTrue (n == null && !si.hasNext());
                return;
            }
            assertEquals((String)si.next(), n.getNodeName());
            n = n.getNextSibling();
        }
    }

    // Not used at present
//     // Given a node n, asserts that this is an Element node, that its
//     // name is `name', and that the list of attributes on it is
//     // precisely that listed in the `name=value ...' list `attlist'
//     private void assertElementEquals(String name, String attlist, Node n) {
//         assertNotNull(n);
//         assertEquals(Node.ELEMENT_NODE, n.getNodeType());
//         assertEquals(name, n.getNodeName());
        
//         NamedNodeMap atts = n.getAttributes();
//         int attsUnchecked = atts.getLength();

//         if (attlist == null) {
//             assertEquals(0, attsUnchecked);
//         } else {
//             java.util.StringTokenizer st
//                     = new java.util.StringTokenizer(attlist, "= ");
//             while (st.hasMoreTokens()) {
//                 String attname = st.nextToken();
//                 if (!st.hasMoreTokens())
//                     throw new IllegalArgumentException
//                             ("unbalanced attlist to assertElementEquals");
//                 String attvalue = st.nextToken();
//                 assertEquals(attvalue,
//                              atts.getNamedItem(attname).getNodeValue());
//                 attsUnchecked--;
//             }
//             assertEquals(0, attsUnchecked);
//         }
//         return;
//     }
}
