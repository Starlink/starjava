// Testing:HdxFactory and NdxFactory.getNdxFromDom

import uk.ac.starlink.hdx.*;
import uk.ac.starlink.hdx.array.NDArray;

import java.net.URL;
import java.util.List;

import org.dom4j.*;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;

class t3 {
    public static void main (String argv[]) {
	if (argv.length < 1) {
	    System.err.println ("Too few arguments");
	    System.exit (1);
	}

	int exitstatus = 0;

	DocumentFactory df = new DocumentFactory();

	for (int i=0; i<argv.length; i++) {
	    try {
		URL url = new URL (new URL("file:."), argv[i]);
		System.out.println("===");
		System.out.println("File " + argv[i] + " (URL " + url + ")");

		SAXReader r = new SAXReader(df);
		r.setValidation(false);
		r.setIncludeExternalDTDDeclarations(false);

		Document dom4jdoc = r.read(url.openStream());
		DOMWriter dw = new DOMWriter();
		org.w3c.dom.Document w3cdoc = dw.write(dom4jdoc);

		Ndx ndx = NdxFactory.getNdx(w3cdoc.getDocumentElement());

		showArray("Data", ndx.getImage(), System.out);
		showArray("Variance", ndx.getVariance(), System.out);
		showArray("Quality", ndx.getQuality(), System.out);
	    } catch (java.net.MalformedURLException ex) {
		System.err.println("Malformed URL: " + ex);
		exitstatus = 1;
	    } catch (java.io.IOException ex) {
		System.err.println("IO exception: " + ex);
		exitstatus = 1;
	    } catch (org.dom4j.DocumentException ex) {
		System.err.println("dom4j document exception: " + ex);
		exitstatus = 1;
	    } catch (uk.ac.starlink.hdx.HdxException e) {
		System.err.println ("HDX error: " + e);
		exitstatus = 1;
	    }
	}

	System.exit (exitstatus);
    }

    private static void showArray(String name, NDArray a,
				  java.io.PrintStream o) {
	if (a == null)
	    o.println("  " + name + ": <null>");
	else
	    o.println("  " + name + " [" + a.getURL().toString()
		      + "]: " + a.toString());
    }
}
