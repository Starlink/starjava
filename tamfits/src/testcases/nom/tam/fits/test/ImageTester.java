package nom.tam.fits.test;

import nom.tam.image.*;
import nom.tam.util.*;
import nom.tam.fits.*;

/** Test the ImageHDU, ImageData and ImageTiler classes.
 *    - multiple HDU's in a single file
 *    - deferred input of HDUs
 *    - creating and reading arrays of all permitted types.
 *    - Tiles of 1, 2 and 3 dimensions
 *        - from a file
 *        - from internal data
 *    - Multiple tiles extracted from an image.
 */
public class ImageTester {
    
    public static void main(String[] args) throws Exception {
	
	Fits f = new Fits();
	
	byte[][] bimg = new byte[40][40];
	for (int i=10; i<30; i += 1) {
	    for (int j=10; j<30; j += 1) {
		bimg[i][j] = (byte)(i+j);
	    }
	}
	
	short[][] simg = (short[][]) ArrayFuncs.convertArray(bimg, short.class);
	int[][] iimg = (int[][]) ArrayFuncs.convertArray(bimg, int.class);
	long[][] limg = (long[][]) ArrayFuncs.convertArray(bimg, long.class);
	float[][] fimg = (float[][]) ArrayFuncs.convertArray(bimg, float.class);
	double[][] dimg = (double[][]) ArrayFuncs.convertArray(bimg, double.class);
        int[][][] img3 = new int[10][20][30];
	for (int i=0; i<10; i += 1) {
	    for (int j=0; j<20; j += 1) {
		for (int k=0; k<30; k += 1) {
		    img3[i][j][k] = i+j+k;
		}
	    }
	}
	
	double[] img1 = (double[]) ArrayFuncs.flatten(dimg);
	
	System.out.println("*** Creating internal FITS data ****");
	
	// Make HDUs of various types.
	f.addHDU(Fits.makeHDU(bimg));
	f.addHDU(Fits.makeHDU(simg));
	f.addHDU(Fits.makeHDU(iimg));
	f.addHDU(Fits.makeHDU(limg));
	f.addHDU(Fits.makeHDU(fimg));
	f.addHDU(Fits.makeHDU(dimg));
	f.addHDU(Fits.makeHDU(img3));
	f.addHDU(Fits.makeHDU(img1));
	
	System.out.println("*** Writing FITS data to image1.fits ****");
	
	// Write a FITS file.
	
	BufferedFile bf = new BufferedFile("image1.fits", "rw");
	f.write(bf);
	bf.flush();
	bf.close();
	bf = null;
	
	
	f = null;
	
	bf = new BufferedFile("image1.fits");
	
	// Read a FITS file
	f = new Fits("image1.fits");
	
	System.out.println("*** Reading FITS file ****");
	
	BasicHDU[] hdus = f.read();
	
	ImageHDU h = (ImageHDU) hdus[1];
	
	// Check out image tiling from files.
	
	System.out.println("*** Testing tilers reading directly from file ***");
	ImageTiler t = h.getTiler();
	short[] stile= (short[]) t.getTile(new int[]{10,10}, new int[]{2,2});
	System.out.println("2-d Tile length [4]:"+stile.length);
	System.out.println("2-d Tile values [20,21,21,22]:"+stile[0]+","+
			   stile[1]+","+stile[2]+","+stile[3]);
	stile=  (short[])t.getTile(new int[]{20,20}, new int[]{2,2});
	System.out.println("2-d Values are [40,41,41,42]:"+stile[0]+","+
			   stile[1]+","+stile[2]+","+stile[3]);
	
	short[] xtile = new short[4];
	t.getTile(xtile, new int[]{20,20}, new int[]{2,2});
	System.out.println("Specify output tile array[40,41,41,42]:"+xtile[0]+","+
			   xtile[1]+","+xtile[2]+","+xtile[3]);
	
	// Check a 3-d image.
	h = (ImageHDU) hdus[6];
	t = h.getTiler();
	int[] itile = (int[]) t.getTile(new int[]{3,3,3}, new int[]{2,2,2});
	System.out.println("3-d Tile length [8]:"+itile.length);
	System.out.println("3-d Tile values [9,10,10,11,10,11,11,12]:"+itile[0]+","+
			   itile[1]+","+itile[2]+","+itile[3]+","+itile[4]+","+
			   itile[5]+","+itile[6]+","+itile[7]);
	
	// How about a 1-d image.
	h = (ImageHDU) hdus[7];
	t = h.getTiler();
	double[] dtile = (double[])t.getTile(new int[]{410}, new int[]{3});
	System.out.println("1-d Tile length [3]:"+dtile.length);
	System.out.println("1-d Tile values [20,21,22]:"+dtile[0]+","+dtile[1]+","+dtile[2]);
	
	System.out.println("\n\n****Displaying information about each HDU.****");
	System.out.println("This will also bring images into memory.\n");
	
	// Display info about each HDU -- and read images into memory
	for (int i=0; i<hdus.length; i += 1) {
	    if (i == 0) {
		System.out.println("Primary array:\n");
	    } else {
		System.out.println("\n\nExtension "+i+":");
	    }
	    hdus[i].info();
	}
	
	// Check from memory tiling!
	
	System.out.println("\n\n****Check in-memory tiling.****");
	t = ((ImageHDU)hdus[1]).getTiler();
	stile=  (short[])t.getTile(new int[]{20,20}, new int[]{2,2});
	System.out.println("2-d Tile values [40,41,41,42]:"+stile[0]+","+
			   stile[1]+","+stile[2]+","+stile[3]);
	stile=  (short[])t.getTile(new int[]{25,25}, new int[]{2,2});
	System.out.println("2-d Tile values [50,51,51,52]:"+stile[0]+","+
			   stile[1]+","+stile[2]+","+stile[3]);
	
	
	h = (ImageHDU) hdus[6];
	t = h.getTiler();
	itile = (int[]) t.getTile(new int[]{3,3,3}, new int[]{2,2,2});
	System.out.println("3-d Tile length [8]:"+itile.length);
	System.out.println("3-d Tile values [9,10,10,11,10,11,11,12]:"+itile[0]+","+
			   itile[1]+","+itile[2]+","+itile[3]+","+itile[4]+","+
			   itile[5]+","+itile[6]+","+itile[7]);
	
	
	h = (ImageHDU) hdus[7];
	t = h.getTiler();
	dtile = (double[])t.getTile(new int[]{410}, new int[]{3});
	System.out.println("1-d Tile length [3]:"+dtile.length);
	System.out.println("1-d Tile values [20,21,22]:"+dtile[0]+","+dtile[1]+","+dtile[2]);

    }
    
    
}
