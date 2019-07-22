package uk.ac.starlink.splat.data;

import org.mortbay.html.FrameSet;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.table.StarTable;

public class SDSSTableSpecDataImpl extends TableSpecDataImpl {
	
	private MathMap sdssMap = null;
	private StarTable lineIDTable = null;
	private LineIDTableSpecDataImpl lineIDSpecDataImpl = null;
	
/*
 * Constructor, just like TableSpecDataImpl.
 * The last argument is the initial fits header, where information about units and wavelength scale are stored.
 */
	public SDSSTableSpecDataImpl(StarTable starTable, String shortName, String fullName, Header fitsHeaders, Header fitsHeaders0)
			throws SplatException {
		super(starTable, shortName, fullName, fitsHeaders);
		
		if (FITSSpecDataImpl.isSDSSFITSHeader(fitsHeaders0)) {
			// get useful information from hdu0
			HeaderCard xcard= fitsHeaders0.findCard("WAT1_001");
    		String xlabel = SDSSFITSSpecDataHandler.findSDSSCoordValue(xcard.getValue(), "label");
    		String xunits = SDSSFITSSpecDataHandler.findSDSSCoordValue(xcard.getValue(), "units");
    		HeaderCard ycard= fitsHeaders0.findCard("BUNIT");
    		String yunits=ycard.getValue();
    		
    		int modelColumn=-1;
    		// set units and coordinates
    		for (int i=0;i<this.columnInfos.length; i++) {
    			String name = this.columnNames[i];
    			if ("flux".equalsIgnoreCase(name) || "model".equalsIgnoreCase(name))
    				this.columnInfos[i].setUnitString(yunits);
    			else if ("loglam".equalsIgnoreCase(name) ) {
    				this.columnInfos[i].setUnitString(xunits);
    				this.columnInfos[i].setDescription(xlabel);
    			}
    			if ("model".equalsIgnoreCase(name))
    				modelColumn=i;	
    		}    	
    		
    		// use "model" as primary data column
    		if (modelColumn >=0)
    			setDataColumnName(this.columnNames[modelColumn]);
    		
    		// Map coords to SDSS log10     		
    		sdssMap  = mapSDSSCoords(fitsHeaders0);    		
    		createAst(); // remake ast to set units, labels and translate wavelengths     		
		}		
	}

	/*
	 * Create a map to translate the wavelenth axis
	 */
	private MathMap mapSDSSCoords(Header header) {
		HeaderCard c0 = header.findCard( "COEFF0" );
		HeaderCard c1 = header.findCard( "COEFF1" );
		if ( c0 != null && c1 != null ) {
			String c0s = c0.getValue();
			String c1s = c1.getValue();

			//  Formulae are w = 10**(c0+c1*i)
			//               i = (log(w)-c0)/c1
			String fwd[] = {
					"w = 10**(" + c0s + " + ( i * " + c1s + " ) )" };
			String inv[] = {
					"i = ( log10( w ) - " + c0s + ")/" + c1s };
			
			MathMap map =  new MathMap( 1, 1, fwd, inv );
    	
			return map;
		}
		return null;
		
	}
		
	 protected void createAst()
	    {
	        super.createAst();      
	        //  rescale the wavelength axis
	        if (coordColumn >=0) {
	        	String label = columnNames[coordColumn];
	            if (sdssMap != null && label.equalsIgnoreCase("loglam")) {
	            	astref.removeFrame(1);
	    			astref.addFrame( 1, sdssMap, new SpecFrame() );
	            }
	        }	       
	    }

	public StarTable getLineIDTable() {
		return lineIDSpecDataImpl.getStarTable();
	}
	
	public LineIDSpecDataImpl getLineIDImpl() {
		return lineIDSpecDataImpl;
	}

/*	public void setLineIDTable(StarTable table) {
		lineIDTable = table;
	}*/
	
	public void  setLineIDImpl(LineIDTableSpecDataImpl impl) {
		lineIDSpecDataImpl = impl;
	}
    
}

