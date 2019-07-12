package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.splat.util.ConstrainedList;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.splat.util.ConstrainedList.ConstraintType;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.votable.VOStarTable;

/** Class to handle SDSS FITS spectral data and  spectral lines data
 * 
 * @author Margarida Castro Neves
 *
 */
public class SDSSFITSSpecDataHandler {

	private DataSource datasrc;
	private String ylabel = null;
	private String xlabel= null;
	private String xunits = null;
	private String yunits=null;
	
	private FITSSpecDataImpl fitsImpl;
	private String specfile;
	
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.data.SDSSFITSSpecDataHandler" );

	
	/*
	 * Constructor
	 * fitsimpl - data already read from a fits file  
	 * specspec   - file name
	 */
	public SDSSFITSSpecDataHandler(FITSSpecDataImpl fitsimpl, String specspec) {

		this.fitsImpl = fitsimpl;
		this.specfile = specspec;

		// get useful information from hdu0
		String xvalue = fitsimpl.getProperty("WAT1_001");
		xlabel = findSDSSCoordValue(xvalue, "label");
		xunits = findSDSSCoordValue(xvalue, "units");

		try {
			datasrc = new FileDataSource( specspec );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* 
	 * getTableSpecDataImpl
	 * searches for the fits extension containing the spectrum
	 * returns a SDSSTableSpecDataImpl
	 */
	public SDSSTableSpecDataImpl getTableSpecDataImpl() throws IOException, SplatException {
		datasrc.setPosition("1");    // get table from HDU1 
		Header hdr1 = fitsImpl.hdurefs[1].getHeader();
		Header hdr0 = fitsImpl.hdurefs[0].getHeader();
		StarTable starTable = new FitsTableBuilder().makeStarTable( datasrc, true, StoragePolicy.getDefaultPolicy() );
		starTable.setName(fitsImpl.getShortName());		
		
		SDSSTableSpecDataImpl impl =  new SDSSTableSpecDataImpl(starTable, specfile, datasrc.getURL().toString(), hdr1, hdr0);
		//impl.setLineIDTable(getLineIDStarTable());
		impl.setLineIDImpl(getLineIDTableDataImpl());
		return impl;
	}
	
	/* 
	 * getTableSpecDataImpl
	 * searches for the fits extension containing the spectral lines and 
	 * returns a LineIDTableSpecDataImpl
	 */
	public LineIDTableSpecDataImpl getLineIDTableDataImpl() throws IOException, SplatException {
        StarTable linesTable = getLineIDStarTable();

        LineIDTableSpecDataImpl lineImpl = new LineIDTableSpecDataImpl(linesTable);
      
        lineImpl.astref.setLabel(1, xlabel);
		lineImpl.astref.setUnit(1, UnitUtilities.fixUpUnits( xunits ));
		return lineImpl;
	}

	/* 
	 * getLineIDStarTable
	 * searches for the fits extension containing the spectral lines and 
	 * returns a Star Table
	 */
	public StarTable getLineIDStarTable() throws IOException, SplatException {
        datasrc.setPosition("3");
        StarTable linesTable = new FitsTableBuilder().makeStarTable( datasrc, true, StoragePolicy.getDefaultPolicy() );
    	linesTable.setName("Lines from "+fitsImpl.getShortName());    
        ValueInfo xlabelInfo = new DefaultValueInfo( "xlabel", String.class, "label of x axis" );
        ValueInfo xunitInfo = new DefaultValueInfo( "xunitstring", String.class, "unit of  x axis" );
        linesTable.setParameter( new DescribedValue( xlabelInfo, xlabel ));                                                 
        linesTable.setParameter(new DescribedValue( xunitInfo, xunits));
		return linesTable;
	}

	public List<SpecDataImpl> getImpls() {
    	List<SpecDataImpl> specDataImpls = new ConstrainedList<SpecDataImpl>(ConstraintType.DENY_NULL_VALUES, LinkedList.class);

       	try {
    		TableSpecDataImpl  specTable = getTableSpecDataImpl();
    	    specDataImpls.add((TableSpecDataImpl) specTable);
    	} catch (IOException ioe) {
    		logger.info( "Failed to open SDSS FITS table "+ioe.getMessage() ) ;        		
    	} catch (SplatException se) {
    		logger.info( "Failed to open SDSS FITS table "+se.getMessage() ) ;   
		}  
/*    	
    	try {
    		LineIDTableSpecDataImpl lineImpl = getLineIDTableDataImpl();
      	  	specDataImpls.add(lineImpl);             
    	} catch (IOException ioe) {
    		logger.info( "Failed to open SDSS LINES table "+ioe.getMessage() ) ;        		
    	} catch (SplatException se) {
    		logger.info( "Failed to open SDSS LINES table "+se.getMessage() ) ; 
		}   
*/     	
    	return specDataImpls;

	}

	public static String findSDSSCoordValue(String value, String pattern) {
		int from = value.indexOf(pattern);
		from = value.indexOf("=", from);
		int to = value.indexOf(" ", from);
		if (to <0 )
			return value.substring(from+1);
		return value.substring(from+1, to);
	}

}
