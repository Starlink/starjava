package nom.tam.fits.test;

import nom.tam.image.*;
import nom.tam.util.*;
import nom.tam.fits.*;
import java.util.*;

public class BigImage {
    
    public static void main(String[] args) throws Exception {
	
	Fits f = new Fits();
	
	int[][] iimg = new int[1000][1000];
	long time0 = new Date().getTime();
	f.addHDU(Fits.makeHDU(iimg));
	System.out.println("Adding HDU:"+(new Date().getTime() - time0)/1000.);
	BufferedFile bf = new BufferedFile("image1.fits", "rw");
	f.write(bf);
	bf.flush();
	bf.close();
	bf = null;
	f = null;
	System.out.println("Write HDU:"+(new Date().getTime() - time0)/1000.);
    }
}
	
