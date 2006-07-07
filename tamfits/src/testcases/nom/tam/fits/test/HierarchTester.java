package nom.tam.fits.test;

import nom.tam.fits.*;
import nom.tam.util.*;

public class HierarchTester {
    
    public static void main(String[] args) throws 
       Exception {
	
	FitsFactory.setUseHierarch(true);
	Fits f = new Fits(args[0]);
	
	BasicHDU h = f.readHDU();
	
	Header hdr = h.getHeader();
	Cursor c   = hdr.iterator();
	
	while (c.hasNext()) {
	    HeaderCard hc = (HeaderCard) c.next();
	    System.out.print("Key= "+hc.getKey()+"  ");
	    System.out.print("Value ="+hc.getValue()+"  ");
	    System.out.println("Comment ="+hc.getComment());
	}
	c.setKey("END");
	String key = "HIERARCH.TEST1.TEST2.INT";
	c.add(key, new HeaderCard(key, 1234, "An integer"));
	key = "HIERARCH.TEST1.TEST2.DOUBLE";
	c.add(key, new HeaderCard(key, 1234.56, "A double"));
	key = "HIERARCH.TEST1.TEST2.BOOLEAN";
	c.add(key, new HeaderCard(key, true, "A boolean"));
	key = "HIERARCH.TEST1.TEST2.STRING";
	c.add(key, new HeaderCard(key, "A STRING....", "A string"));
	
        BufferedFile bf = new BufferedFile("ht2.fits", "rw");
	f.write(bf);
	bf.flush();
	   
	bf.close();
	String base = "HIERARCH TEST1 TEST2 TEST3 ";
	String x = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
	   
	int i = 0;
	for (i=0; i<70; i += 1) {
	    String base1 = base + x.substring(0,i) + "=123 / ABC";
	    HeaderCard card = new HeaderCard(base1);
	    System.out.println("At "+i+":"+card.toString());
	}
	
	String[] tests = {
	    "HIERARCH TEST1='abcdef'/squeezed",
	    "HIERARCH TEST2=123/squeezed2",
	    "HIERARCH TEST3 = '  ''abcdef''  ' / enclosed quotes ",
	    "HIERARCH TEST4 =''''/single enclosed quote",
	    "HIERARCH TEST5=      123/abcdef",
	    "HIERARCH TEST6 =123 / No prob..",
	    "HIERARCH TEST7 = / No Value..",
	    "HIERARCH TEST8 =/No Value..",
	};
	for (i=0; i<tests.length; i += 1) {
	    HeaderCard card = new HeaderCard(tests[i]);
	    System.out.println(tests[i] + " ->\n    "+card.toString());
	}
		
		
    }
}
