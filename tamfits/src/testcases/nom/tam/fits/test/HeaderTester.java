package nom.tam.fits.test;

import nom.tam.fits.*;
import nom.tam.util.*;

public class HeaderTester {
    
    /** Check out header manipulation.
     */
    public static void main(String[] args) throws Exception {
        float[][] img = new float[300][300];
    
        Fits f = new Fits();
        
        ImageHDU hdu = (ImageHDU) Fits.makeHDU(img);
        BufferedFile bf = new BufferedFile("ht1.fits", "rw");
        f.addHDU(hdu);
        f.write(bf);
        bf.close();
        
        f = new Fits("ht1.fits");
        hdu        = (ImageHDU) f.getHDU(0);
        Header hdr = hdu.getHeader();
        
        Cursor c = hdr.iterator();
        
        c.setKey("XXX");
        c.add("CTYPE1", new HeaderCard("CTYPE1","GLON-CAR", "Galactic Longitude"));
        c.add("CTYPE2", new HeaderCard("CTYPE2","GLAT-CAR", "Galactic Latitude"));
        c.setKey("CTYPE1");  // Move before CTYPE1
        c.add("CRVAL1", new HeaderCard("CRVAL1", 0., "Longitude at reference"));
        c.setKey("CTYPE2"); // Move before CTYPE2
        c.add("CRVAL2", new HeaderCard("CRVAL2", -90., "Latitude at reference"));
        c.setKey("CTYPE1");  // Just practicing moving around!!
        c.add("CRPIX1", new HeaderCard("CRPIX1", 150.0, "Reference Pixel X"));
        c.setKey("CTYPE2");
        c.add("CRPIX2", new HeaderCard("CRPIX2", 0., "Reference pixel Y"));
	c.add("INV2", new HeaderCard("INV2", true, "Invertible axis"));
	c.add("SYM2", new HeaderCard("SYM2", "YZ SYMMETRIC", "Symmetries..."));
	    
        
        c.setKey("CTYPE2");
        System.out.println("Looking at:"+c.next());
        c.add(new HeaderCard("COMMENT", null, "This should come after CTYPE2"));
        c.add(new HeaderCard("COMMENT", null, "This should come second after CTYPE2"));
        
        hdr.findCard("CRPIX1");
        hdr.addValue("INTVAL1", 1,"An integer value");
        hdr.addValue("LOG1", true, "A true value");
        hdr.addValue("LOGB1", false, "A false value");
        hdr.addValue("FLT1", 1.34, "A float value");
        hdr.addValue("FLT2", -1.234567890e-134, "A very long float");
        hdr.insertComment("Comment after flt2");
        
        System.out.println("Is the header rewritable?"+hdr.rewriteable());
	hdr.rewrite();
	
	System.out.println("Try with no BITPIX");
	c = hdr.iterator();
	c.next();
	c.next();
	c.remove();
	try {
	    hdr.rewrite();
	    System.out.println("Rewrite succeeded...OOOPS!");
	} catch (Exception e) {
	    System.out.println("Rewrite failed [Good!]:"+e);
	}
	c.add("BITPIX", new HeaderCard("BITPIX", 8, ""));
	
	f = new Fits("ht1.fits");
	hdr = f.getHDU(0).getHeader();
	
	System.out.println("Checking:");
	System.out.println("   CRVAL2 [-90]:"+hdr.getFloatValue("CRVAL2"));
	System.out.println("   INV2 [true] :"+hdr.getBooleanValue("INV2"));
	System.out.println("   NAXIS [2]   :"+hdr.getIntValue("NAXIS"));
	System.out.println("   FLT2 [-1.23...]:"+hdr.getDoubleValue("FLT2"));
	
	c = hdr.iterator();
	while (c.hasNext()) {
	    HeaderCard card = (HeaderCard) c.next();
	    String key = card.getKey();
	    String val = card.getValue();
	    String comment = card.getComment();
	    System.out.println("   "+key+" || "+val+" || "+comment);
	}
	
	while (hdr.rewriteable()) {
	    System.out.println("Rewriteable with "+hdr.getNumberOfCards()+" cards.");
	    c.add(new HeaderCard("DUMMY", null, null));
	}
    }
}
    
