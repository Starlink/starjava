package nom.tam.fits.test;

import nom.tam.util.*;
import nom.tam.fits.*;

/** Test random groups formats in FITS data.
 *    Write and read random groups data
 */

public class RandomGroupsTester {
    
    
  public static void main(String[] args) throws Exception {
    float[][] fa = new float[20][20];
    float[]   pa = new float[3];
    
    BufferedFile bf = new BufferedFile("rg1.fits", "rw");
    
    Object[][] data = new Object[1][2];
    data[0][0] = pa;
    data[0][1] = fa;
    
    System.out.println("***** Write header ******");
    BasicHDU hdu = Fits.makeHDU(data);
    Header   hdr = hdu.getHeader();
    // Change the number of groups
    hdr.addValue("GCOUNT", 20, "Number of groups");
    hdr.write(bf);
    
    System.out.println("***** Write data group by group ******");
    for (int i=0; i<20; i += 1) {
	
	for (int j=0; j<pa.length; j += 1) {
	    pa[j] = i+j; 
	}
	for (int j=0; j<fa.length; j += 1) {
	    fa[j][j] = i*j;
	}
	// Write a group
        bf.writeArray(data);
    }
    byte[] padding = new byte[FitsUtil.padding(20*ArrayFuncs.computeSize(data))];
    System.out.println("****** Write padding ******");
    bf.write(padding);
    
    bf.flush();
    bf.close();
    
    System.out.println("****** Read data back in ******");
    Fits f = new Fits("rg1.fits");
    BasicHDU[] hdus = f.read();
    
    data = (Object[][]) hdus[0].getKernel();
    System.out.println("**** Check parameter and data info *****");
    for (int i=0; i<data.length; i += 1) {
	
	pa = (float[]) data[i][0];
	fa = (float[][]) data[i][1];
	System.out.println("Sizes[3,400]: "+pa.length+" "+ArrayFuncs.nElements(fa));
	
	System.out.println("Params[n,n+1,n+2]:"+pa[0]+","+pa[1]+","+pa[2]);
	System.out.println("Center elements[9n,0,0,10n]:"+fa[9][9]+","+fa[9][10]+
			                   ","+fa[10][0]+","+fa[10][10]);
    }
      
    f = new Fits();
    
    
    System.out.println("**** Create HDU from kernel *****");
    // Generate a FITS HDU from the kernel.
    f.addHDU(Fits.makeHDU(data));
    bf = new BufferedFile("rg2.fits", "rw");
    System.out.println("**** Write new file *****");
    f.write(bf);
    
    bf.flush();
    bf.close();
      
    System.out.println("**** Read and check *****");
    f = new Fits("rg2.fits");
    data = (Object[][]) f.read()[0].getKernel();
    for (int i=0; i<data.length; i += 1) {
	
	pa = (float[]) data[i][0];
	fa = (float[][]) data[i][1];
	System.out.println("Sizes[3,400]: "+pa.length+" "+ArrayFuncs.nElements(fa));
	
	System.out.println("Params[n,n+1,n+2]:"+pa[0]+","+pa[1]+","+pa[2]);
	System.out.println("Center elements[9n,0,0,10n]:"+fa[9][9]+","+fa[9][10]+
			                   ","+fa[10][0]+","+fa[10][10]);
    }
    
  }
}
	
    
	
	
