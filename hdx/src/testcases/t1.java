// Testing:HdxContainerFactory and HdxContainerFactory.getNdxList

import uk.ac.starlink.hdx.*;
import uk.ac.starlink.hdx.array.NDArray;

import java.net.URL;
import java.util.List;

class t1 {
    public static void main (String argv[]) {
	if (argv.length < 1) {
	    System.err.println ("Too few arguments");
	    System.exit (1);
	}

	HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
	HdxContainer hdx;
	int exitstatus = 0;

	for (int i=0; i<argv.length; i++) {
	    try {
		URL url = new URL (new URL("file:."), argv[i]);
		hdx = hdxf.readHdx(url);
		List ndxlist = hdx.getNdxList();

		System.out.println("===");
		System.out.println("File " + argv[i] + " (URL " + url + ")");
		for (int ndxno=0; ndxno<ndxlist.size(); ndxno++) {
		    System.out.println("--- NDX " + ndxno);
		    Ndx ndx = (Ndx) ndxlist.get(ndxno);

		    showArray("Data", ndx.getImage(), System.out);
		    showArray("Variance", ndx.getVariance(), System.out);
		    showArray("Quality", ndx.getQuality(), System.out);
		}
	    } catch (java.net.MalformedURLException e) {
		System.err.println ("Malformed URL: " + e);
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
