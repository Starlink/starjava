package uk.ac.starlink.frog.data;

import java.io.FileOutputStream;
import java.io.PrintStream;

import nom.tam.fits.*;
import nom.tam.util.*;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.LutMap;
import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;

/**
 * FITSTimeSeriesImpl - implementation of TimeSeriesImpl to access FITS
 * files natively using nom.tam.fits. Initally based around Peter Drapers
 * FITSSpecDataImpl, but radically cut and modified to use FITS Binary
 * Tables and to get rid of ASTChannel support. AST FrameSets are no
 * longer saved to the FITS file as the time data is put directly into
 * the FITS table and AST LUT maps generated for the TimeSeries object
 * on load as in TXTTimeSeriesImpl. This makes alot more sense as its
 * going to be what the application will meet in the wild.
 *
 * Currently it assumes we're looking for the 1st FITS extension, rather
 * than the primary ImageHDU as SPLAT does. This is probably a bad thing.
 *
 * @author Alasdair Allan
 * @version $Id$
 * @see "The Bridge Design Pattern"
 */
public class FITSTimeSeriesImpl extends TimeSeriesImpl
    implements FITSHeaderSource
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();



    /**
     * Reference to Fits object.
     */
    protected Fits fitsref = null;

    /**
     * HDU references for the current Fits object.
     */
    protected BasicHDU[] hdurefs;

    /**
     * Current HDU number.
     */
    protected int hdunum = 1;

    /**
     * The state of the object. Cloned is seriesial and indicates that
     * all values are memory resident, rather than backed up by the
     * file.
     */
    protected boolean cloned = false;

    /**
     * Reference to cloned coordinates.
     */
    protected double[] coords = null;

    /**
     * Reference to cloned data values.
     */
    protected double[] data = null;

    /**
     * Reference to cloned data errors.
     */
    protected double[] errors = null;

    /**
     * Reference to cloned header cards.
     */
    protected Header clonedHeader = null;

    /**
     * Default symbolic name.
     */
    protected String shortName = "Time Series";

    /**
     * The FITS file name.
     */
    protected String fullName = "Time Series";

    /**
     * Reference to the AST frameset (also used when cloned).
     */
    protected FrameSet astref = null;

    /**
     * Maxiumum number of characters in a header string.
     */
    protected static int MAX_HEADER_VALUE = 68;

//
//  Implementation of TimeSeriesImpl methods.
//
    /**
     * Constructor - open a FITS file by file name.
     */
    public FITSTimeSeriesImpl( String fileName )
    {
        super( fileName );
        //hdunum = 0;
        openForRead( fileName );
        try {
          getDataCopy();
        } catch ( Exception e ) {
           // do nothing
           e.printStackTrace();
        }  
    }

    /**
     * Constructor, creating an object by cloning another.
     */
    public FITSTimeSeriesImpl( String fileName, TimeSeries source )
    {
        super( fileName );
        //hdunum = 0;
        fullName = fileName;
        makeMemoryClone( source );
    }

    /**
     * Return a copy of the series data values.
     */
    public double[] getData()
    {
        return data;
    }

    /**
     * Return a copy of the series data errors. TODO: need some
     * mechanism for getting errors from another extension or FITS file.
     */
    public double[] getDataErrors()
    {
        return errors;
    }

   /**
     * Return a copy of the series coord values.
     *
     * @return reference to the series coord values.
     */
    public double[] getTime()
    {
        return coords;
    }


    /**
     * Return the data array dimensionality.
     */
    public int[] getDims()
    {
        if ( cloned ) {
            int dummy[] = new int[1];
            dummy[0] = data.length;
            return dummy;
        } else {
            return getDataDims();
        }
    }

    /**
     * Return a symbolic name. This is value of the "OBJECT" card, if
     * present.
     */
    public String getShortName()
    {
        //  Note in order to be save this should not exceed
        //  MAX_HEADER_VALUE characters.
        return shortName;
    }

    /**
     * Return the file name.
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return reference to AST frameset describing coordinates of
     * FITS headers.
     */
    public FrameSet getAst()
    {
        return astref;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "FITS";
    }

    /**
     * Save series to the associated disk-file.
     */
    public void save() throws FrogException
    {
        saveToFile();
    }

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found.
     */
    public String getProperty( String key )
    {
        if ( getFitsHeaders() != null ) {
            String scard = getFitsHeaders().findKey( key );
            if ( scard != null ) {
                HeaderCard card = new HeaderCard( scard );
                if ( card != null ) {
                    return card.getValue();
                }
            }
        }
        return "";
    }

//
//  Implementation of FITSHeaderSource.
//
    public Header getFitsHeaders()
    {
        if ( clonedHeader == null ) {
            if ( hdurefs != null ) {
                if ( hdurefs[hdunum] != null ) {
                    return hdurefs[hdunum].getHeader();
                }
            }
            return null;
        }
        else {
            return clonedHeader;
        }
    }

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize() throws Throwable
    {
        fitsref = null;
        hdurefs = null;
        shortName = null;
        fullName = null;
        super.finalize();
    }

    /**
     * Open a named FITS file for reading.
     *
     * @param fileName The name of the FITS file, including any HDU
     *                 reference (format file.fits[0]).
     */
    protected void openForRead( String fileName )
    {
        //  Parse the name to extract the HDU reference.
        InputNameParser namer = new InputNameParser( fileName );
        
        // If we have a raw fits filename, with no extensions specified
        // assume we actually want the 1st extension rather than the
        // primary ImageHDU. This is a poorly implemented kludge that
        // needs fixing.
        hdunum = namer.fitshdunum();
        if ( hdunum == 0 ) {
           hdunum = 1;
        }
           
        boolean ok = true;
        try {
            fitsref = new Fits( namer.fullname() );
        }
        catch (Exception e ) {
            e.printStackTrace();
            fitsref = null;
            ok = false;
        }
        if ( ok ) {
            try {
                hdurefs = fitsref.read();
            }
            catch (Exception e ) {
                e.printStackTrace();
                hdurefs = null;
                ok = false;
            }
            if ( ok ) {
            
                shortName = fileName;
                fullName = fileName;
            }
        }
    }

    /**
     * Create a new FITS file and populate it.
     */
    protected void saveToFile()
    {
        //  Parse the name to extract the HDU reference and container
        //  file name. Extension number is ignored for now.
        InputNameParser namer = new InputNameParser( fullName );
        //hdunum = namer.fitshdunum();
        String container = namer.fullname();

        boolean ok = true;
        try {
        
            // Create a FITS Reference and assocaited objects
            // ----------------------------------------------
        
            // Create a null FITS object
            fitsref = new Fits();
            
            // Primary Image and Header
             
            ImageData primaryData = new ImageData();
            Header primaryHeader = new Header( primaryData );
            primaryHeader.removeCard("NAXIS1");
            primaryHeader.removeCard("NAXIS");
            primaryHeader.setNaxes(0);
          
            debugManager.print("\nPrimary Header\n--------------\n");
            Cursor piter = primaryHeader.iterator();
            while ( piter.hasNext() ) {
               debugManager.print( ((HeaderCard)piter.next()).toString() );
            }             
          
            // Primary HDU
            
            ImageHDU primary = new ImageHDU( primaryHeader, primaryData );
            //primary.setPrimaryHDU( true );
            
            fitsref.addHDU(primary);
            
            debugManager.print( "\nPrimary HDU\n-----------"); 
            primary.info();   
          
            
            // BINARY TABLE
            BinaryTable binTab = new BinaryTable();
            binTab.addColumn( coords );
            binTab.addColumn( data );
            if( errors != null ) {
               binTab.addColumn( errors );
            }
            
            // ASSOCIATED HEADER
            Header header = new Header( binTab );
            debugManager.print( "\nBinary Header\n-------------\n" 
                          + header.toString());
          
            Cursor iter = header.iterator();
            while ( iter.hasNext() ) {
               debugManager.print( ((HeaderCard)iter.next()).toString() );
            }

            // HDU
            BinaryTableHDU hdu = new BinaryTableHDU( header, binTab );
            hdu.setColumnName(0, "Time", "Time of data point" );
            hdu.setColumnName(1, "Data", "Data value" );
            
            if ( errors != null ) {
               hdu.setColumnName(2, "Error", "Error in Data" );
            }
            
            debugManager.print( "\nHDU\n---");
            debugManager.print( "hdu.isHeader() = " + hdu.isHeader() + "\n");
            hdu.info();
           
            // Add existing header cards
            Cursor hiter = getStandardIterator( primaryHeader );

            if ( clonedHeader != null ) {
                HeaderCard card;
                String key;

                Cursor citer = getStandardIterator( clonedHeader );
                while ( citer.hasNext() ) {
                    card = (HeaderCard) citer.next();
                    key = card.getKey();
                    if ( key.equals( "COMMENT" ) || key.equals( "HISTORY" ) ) {
                        hiter.add( card );
                    }
                    else if ( ! header.containsKey( key ) ) {
                        hiter.add( key, card );
                    }
                }
            }
                  
            // add the HDU to the FITS reference
            fitsref.addHDU( hdu );
          
            //  Write the HDU (data and header) to the file.
            FileOutputStream fo = new FileOutputStream( container );
            BufferedDataOutputStream os = new BufferedDataOutputStream( fo );
            fitsref.write( os );
        }
        catch (Exception e ) {
            e.printStackTrace();
            fitsref = null;
            ok = false;
        }

        //  No longer a memory clone, backing file is created.
        cloned = false;
        data = null;
        coords = null;
        errors = null;
        clonedHeader = null;
    }

    /**
     * Get a copy of the FITS series data in double precision.
     */
    protected void getDataCopy() throws FitsException
    {
        double[] series = null;
        if ( hdunum < hdurefs.length ) {

            // Grab the raw data from the table
            Object rawindex = null;            
            Object rawcoords = null;
            Object rawdata = null;
            Object rawerrors = null;
            try {
                if( ((TableHDU) hdurefs[hdunum]).getNCols() >= 4 ) {
                   rawindex = ((TableHDU) hdurefs[hdunum]).getColumn(0);
                   rawcoords = ((TableHDU) hdurefs[hdunum]).getColumn(1);
                   rawdata = ((TableHDU) hdurefs[hdunum]).getColumn(2);
                   rawerrors = ((TableHDU) hdurefs[hdunum]).getColumn(3);
                   
                } else if ( ((TableHDU) hdurefs[hdunum]).getNCols() >= 3 ) {
                   rawcoords = ((TableHDU) hdurefs[hdunum]).getColumn(0);
                   rawdata = ((TableHDU) hdurefs[hdunum]).getColumn(1);
                   rawerrors = ((TableHDU) hdurefs[hdunum]).getColumn(2);
                   
                } else {
                   rawcoords = ((TableHDU) hdurefs[hdunum]).getColumn(0);
                   rawdata = ((TableHDU) hdurefs[hdunum]).getColumn(1); 
                }
            } catch ( Exception e ) {
                // Do nothing
                return;
            }
            
            //  Get the data format.
            int bitpix = 0;
            bitpix = hdurefs[hdunum].getBitPix();

            //  Get the BSCALE and BZERO values.
            double bscale = hdurefs[hdunum].getBScale();
            double bzero = hdurefs[hdunum].getBZero();

            //  Get the BLANK value, note if none present.
            int blank = 0;
            boolean haveblank = true;
            
            try {
              blank = hdurefs[hdunum].getBlankValue();
            } catch (FitsException e ) {
              haveblank = false;
            }
              
            coords = (double[]) rawcoords;
            data = (double[]) rawdata;
            if( ((TableHDU) hdurefs[hdunum]).getNCols() >= 3 ) {
               errors = (double[]) rawerrors;
            }   
            
            // muck with data values, not sure this is actually needed?
            for( int i=0; i < data.length; i++ ) {
               if( haveblank ) {
                   if ( data[i] == blank ) {
                      data[i] =  TimeSeries.BAD;
                   } else {
                      data[i] = data[i] * bscale + bzero;
                   }
               } else
                   data[i] = data[i] * bscale + bzero;
               }               
            } 
            
            // create AST mapping
            createAst();      

    }


    /**
     * Create an AST frameset that relates the series coordinates to
     * data value positions.
     */
    protected void createAst()
    {
        debugManager.print("                createAst()");

        //  Create two simple frames, one for the indices of the data
        //  counts and one for the coordinates (timestamp).
        Frame baseframe = new Frame( 1 );
        baseframe.set( "Label(1)=Data Counts" );
        Frame currentframe = new Frame( 1 );
        currentframe.set( "Label(1)=Time" );

        //  Create an AST lutmap that relates the index of the data
        //  counts to the coordinates.

        LutMap lutmap = new LutMap( coords, 1.0, 1.0 );

        //  Now create a frameset and add all these to it.
        astref = new FrameSet( baseframe );
        astref.addFrame( 1, lutmap, currentframe );
    }
    
    /**
     * Return an array of with length the number of dimensions of
     * the original data and with each element set to the size of that
     * dimension.
     */
    protected int[] getDataDims()
    {
        int[] dims = new int[1];
        if ( hdunum < hdurefs.length ) {
           dims[0] = ((BinaryTableHDU)hdurefs[hdunum]).getNRows();
           return dims;
        }
        dims[0] = 0;
        return dims;
    }

    /**
     * Set up this object as a clone of another series. The
     * resultant state is a memory-only resident one, until the save
     * method is called, when the actual file is created and the
     * contents are written.
     */
    protected void makeMemoryClone( TimeSeries source )
    {
        cloned = true;
        shortName = source.getShortName();
        data = source.getYData();
        coords = source.getXData();
        errors = source.getYDataErrors();
        astref = source.getAst().getRef();

        //  If the source series provides access to a set of FITS
        //  headers then we should preserve them.
        if ( source.getTimeSeriesImpl() instanceof FITSHeaderSource ) {
            clonedHeader = 
             ((FITSHeaderSource) source.getTimeSeriesImpl()).getFitsHeaders();
        }
    }

    /**
     * Get a Cursor iterator that is positioned after the standard
     * FITS keywords. If none are found then the iterator is
     * positioned at the end.
     *
     * @param header Header that you want an iterator for.
     *
     * @return a Cursor iterator positioned at the end of the standard
     *         headers.
     */
    public static Cursor getStandardIterator( Header header )
    {
        Cursor iter = header.iterator();
        
        //  Standard headers complete (for images) after the last
        //  NAXIS<n> card.
        int naxes = header.getIntValue( "NAXIS" );
        iter.setKey( "NAXIS" + naxes );
        if ( iter.hasNext() ) {
            iter.next();
        }
        return iter;
    }

    /** 
     * Class utility: Print the header to a given stream.
     * 
     * @param ps the stream to which the card images are dumped.
     */
    public static void dumpHeader( Header header, PrintStream ps ) 
    {
        Cursor iter = header.iterator();
        ps.println( "Header dump begins" );
        ps.println( "==================" );
        while ( iter.hasNext() ) {
            ps.println( (HeaderCard)iter.next() );
        }
        ps.println( "==================" );
    }
}
