// Testing:transformation using javax.xml.transform

import java.io.*;
import java.net.MalformedURLException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;


class t2 {
    public static void main (String[] argv) {
	if (argv.length < 1) {
	    System.err.println ("Usage: t2 xml-file...");
	    System.exit (1);
	}

	String xsltfilename = "../support/normalize-hdx.xslt";

	Transformer transformer;
	Document olddoc;
	Document newdoc;

	int exitstatus = 0;

	try {

	    transformer = newStyler(new StreamSource(new File(xsltfilename)));

	    for (int i=0; i<argv.length; i++) {
		System.out.println("--- " + argv[i]);
		System.err.println("--- " + argv[i]);
		olddoc = parseWithSAX(new File(argv[i]));
		newdoc = transform (olddoc, transformer);
		serializeToXML(newdoc, System.out);
		System.out.println();
	    }

	} catch (MalformedURLException e) {
	    System.err.println("Malformed URL: " + e);
	    exitstatus = 1;
	} catch (javax.xml.transform.TransformerException e) {
	    System.err.println("Transformer exception: " + e);
	    exitstatus = 1;
	} catch (org.dom4j.DocumentException e) {
	    System.err.println("org.dom4j.DocumentException: " + e);
	    exitstatus = 1;	// failure
	} catch (java.io.IOException e) {
	    System.err.println("java.io.IOException: " + e);
	    exitstatus = 1;	// failure
	}

	System.exit (exitstatus);
    }

    public static Document parseWithSAX(File f)
	throws DocumentException, MalformedURLException {
	SAXReader xmlReader = new SAXReader();
	return xmlReader.read(f);
    }

    public static void serializeToXML(Document doc, OutputStream out)
	throws IOException {
	XMLWriter writer = new XMLWriter(out);
	writer.write(doc);
	writer.flush();
    }

    public static Transformer newStyler(Source stylesheet)
	throws javax.xml.transform.TransformerConfigurationException {
	TransformerFactory factory = TransformerFactory.newInstance();
	return factory.newTransformer(stylesheet);
    }

    public static Document transform (Document doc, Transformer t)
	throws javax.xml.transform.TransformerException  {
	DocumentSource source = new DocumentSource(doc);
	DocumentResult result = new DocumentResult();
	t.transform(source, result);
	return result.getDocument();
    }
}
