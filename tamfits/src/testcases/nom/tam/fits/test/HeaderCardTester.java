package nom.tam.fits.test;

import nom.tam.fits.HeaderCard;

public class HeaderCardTester {
    
    public static void main(String[] args) {
	
	HeaderCard p;
	
	try {
	    if (args.length == 3) {
	    
	        p = new HeaderCard(args[0], args[1], args[2]);
	    
	    } else {
	        p = new HeaderCard(args[0]);
	    }
	} catch (Exception e) {
	    System.out.println("Got exception: "+e);
	    return;
	}
	    
	System.out.println("Key is:     "+p.getKey());
	System.out.println("Value is:   "+p.getValue());
	System.out.println("Comment is: "+p.getComment());
	System.out.println("Is this a key/value pair:"+p.isKeyValuePair());
	System.out.println("Is this a string:"+p.isStringValue());
	System.out.println("The card is:\n"+
	  "0         1         2         3         4         5         6         7        X\n"+
	  p.toString());
    }
}
	    
