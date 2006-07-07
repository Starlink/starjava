package nom.tam.fits.test;

import nom.tam.fits.*;
import nom.tam.util.*;
import nom.tam.image.ImageTiler;
import java.io.*;
import java.util.StringTokenizer;

/** This class tests the ImageTiler.  It
 *  first creates a FITS file and then reads
 *  it back and allows the user to select
 *  tiles.  The values of the corner and center
 *  pixels for the selected tile are displayed.
 *  Both file and memory tiles are checked.
 */
public class TilerTester {
    
    public static void main(String[] args) throws Exception {
	
	float[][] data = new float[300][300];
	
	for (int i=0; i<300; i += 1) {
	    for (int j=0; j<300; j += 1) {
		data[i][j] = 1000*i+j;
	    }
	}
	
	Fits f = new Fits();
	
	BufferedFile bf = new BufferedFile("tiler1.fits", "rw");
	f.addHDU(Fits.makeHDU(data));
	
	f.write(bf);
	bf.close();
	
	f = new Fits("tiler1.fits");
	
	ImageHDU h = (ImageHDU) f.readHDU();
	
	ImageTiler t = h.getTiler();
	
	StreamTokenizer toker = new StreamTokenizer(
				 new InputStreamReader(System.in));
	toker.parseNumbers();
	while (true) {
	    System.out.print("File: Enter cx,cy,nx,ny (or END to terminate)");
	    int type = toker.nextToken();
	    if (type == StreamTokenizer.TT_EOL ||
		type == StreamTokenizer.TT_WORD) {
		break;
	    }
	    
	    int cx = (int) toker.nval;
	    
	    toker.nextToken();
	    int cy = (int) toker.nval;
	    
	    toker.nextToken();
	    int nx = (int) toker.nval;
	    toker.nextToken();
	    int ny = (int) toker.nval;
	    
	    int[] cor = new int[]{cx, cy};
	    int[] siz = new int[]{nx, ny};
	    
	    float[] tile = new float[nx*ny];
	    t.getTile(tile, cor, siz);
	    System.out.println("Corners/center:"+tile[0] + " "+ 
			                  tile[nx-1]+ " "+
			                  tile[nx*ny-nx]+" "+
			                  tile[nx*ny-1]+" "+
			                  tile[nx*(ny/2) + nx/2]);
	    
	}
	
	// Force the image to be read.
	h.info();
	while (true) {
	    System.out.print("Mem:  Enter cx,cy,nx,ny (or END to terminate)");
	    int type = toker.nextToken();
	    if (type == StreamTokenizer.TT_EOL ||
		type == StreamTokenizer.TT_WORD) {
		break;
	    }
	    
	    int cx = (int) toker.nval;
	    
	    toker.nextToken();
	    int cy = (int) toker.nval;
	    
	    toker.nextToken();
	    int nx = (int) toker.nval;
	    toker.nextToken();
	    int ny = (int) toker.nval;
	    
	    int[] cor = new int[]{cx, cy};
	    int[] siz = new int[]{nx, ny};
	    
	    float[] tile = new float[nx*ny];
	    t.getTile(tile, cor, siz);
	    System.out.println("Corners/center:"+tile[0] + " "+ 
			                  tile[nx-1]+ " "+
			                  tile[nx*ny-nx]+" "+
			                  tile[nx*ny-1]+" "+
			                  tile[nx*(ny/2) + nx/2]);
	    
	}
	
    }
}
