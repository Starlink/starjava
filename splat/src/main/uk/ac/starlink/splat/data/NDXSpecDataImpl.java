// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.hdx.HdxContainer;
import uk.ac.starlink.hdx.HdxContainerFactory;
import uk.ac.starlink.hdx.Ndx;
import uk.ac.starlink.hdx.array.BadHandler;
import uk.ac.starlink.hdx.array.ChunkIterator;
import uk.ac.starlink.hdx.array.Function;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NDArrayFactory;
import uk.ac.starlink.hdx.array.Requirements;
import uk.ac.starlink.hdx.array.Type;
import uk.ac.starlink.splat.util.SplatException;

/**
 * NDXSpecDataImpl - implementation of SpecDataImpl to access a spectrum
 *                   stored in an NDX.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 28-MAY-2002
 * @see "The Bridge Design Pattern"
 */
public class NDXSpecDataImpl extends SpecDataImpl
{

    //// Implementation of abstract methods.

    /**
     * Constructor - open an HDX description by name. The name is
     * understood by HDX, we don't need to know too much, but this
     * should only contain a single NDX, that is a spectrum.
     */
    public NDXSpecDataImpl( String hdxName )
    {
        super( hdxName );
        open( hdxName );
    }

    /**
     * Constructor - use an NDX described in a DOM.
     */
    public NDXSpecDataImpl( Element ndxElement )
    {
        super( "Wired NDX" );
        open( ndxElement );
    }

    /**
     * Constructor. Initialise this spectrum by cloning the content of
     * another spectrum (usual starting point for saving).
     */
    public NDXSpecDataImpl( String hdxName, SpecData source )
    {
        super( hdxName );
        throw new RuntimeException
            ( "Duplication of NDX spectra not implemented" );
    }

    /**
     * Return a copy of the spectrum data values.
     */
    public double[] getData()
    {
        return data;
    }

    /**
     * Return a copy of the spectrum data errors.
     */
    public double[] getDataErrors()
    {
        return errors;
    }

    /**
     * Return the NDX shape, always the number of pixels as the
     * underlying data is vectorized. If this fails the size is
     * returned as 0.
     */
    public int[] getDims()
    {
        int[] dims = new int[1];
        try {
            long npix = ndx.getImage().getShape().getNumPixels();
            dims[0] = (int) npix;
        }
        catch (Exception e) {
            e.printStackTrace();
            dims[0] = 0;
        }
        return dims;
    }

    /**
     * Return a symbolic name.
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * Return the full name of the NDX.
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * Return reference to NDX AST frameset.
     */
    public FrameSet getAst()
    {
        if ( ndx.hasWCS() ) {
            try {
                return ndx.getWCS();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Return the data format.
     */
    public String getDataFormat()
    {
        return "NDX";
    }

    /**
     * Save the spectrum to disk-file.
     */
    public void save() throws SplatException
    {
        throw new RuntimeException( "Saving NDX spectra is not implemented" );
    }

//
//  Implementation specific methods and variables.
//
    /**
     * Reference to NDX object.
     */
    protected Ndx ndx = null;

    /**
     * Original HDX specification that pointed to the NDX.
     */
    protected String fullName;

    /**
     * Short name for the NDX (if any, defaults to long name).
     */
    protected String shortName;

    /**
     * Reference to data values.
     */
    protected double[] data = null;

    /**
     * Reference to data errors.
     */
    protected double[] errors = null;

    /**
     * Static variable for creating unique names.
     */
    private static int counter = 0;

    /**
     * Open an HDX description and locate the NDX.
     *
     * @param hdxName HDX description of the NDX (could be a simple
     *                XML file or my include an XPath component, this
     *                should be dealt with by HDX).
     */
    protected void open( String hdxName )
    {
        // Read the complete document that we've been given.
        List ndxs = null;
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx;
        try {
            URL url = new URL( new URL( "file:." ), hdxName );
            hdx = hdxf.readHdx( url );
            ndxs = hdx.getNdxList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndxs != null && ndxs.size() == 0 ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }
        fullName = hdxName;
        shortName = hdxName;

        //  This is always the NDX we require.
        ndx = (Ndx) ndxs.get( 0 );

        //  Read in the data.
        readData();
    }

    /**
     * Open an NDX stored in an Element.
     *
     * @param ndxElement the Element.
     */
    protected void open( Element ndxElement )
    {
        List ndxs = null;
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx;
        try {
            // Get a HDX container with the NDX inside.
            hdx = hdxf.readHdx( ndxElement );
            ndxs = hdx.getNdxList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndxs != null && ndxs.size() == 0 ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }

        //  This is always the NDX we require.
        ndx = (Ndx) ndxs.get( 0 );
        String title = ndx.getTitle();
        if ( title == null || title.equals( "" ) ) {
            title = "Wired NDX (" + (counter++) + ")";
        }
        fullName = title;
        shortName = title;

        //  Read in the data.
        readData();
    }

    /**
     * Read in the NDX data components. These are copied to local
     * double precision array.
     */
    protected void readData()
    {
        NDArray image = null;
        try {
            image = ndx.getImage();
        }
        catch (Exception e) {
            e.printStackTrace();
            data = null;
            return;
        }
        data = unwrapNDArray( image, null );

        //  Read any variance. Note that these are transformed into
        //  errors.
        if ( ndx.hasVariance() ) {
            NDArray variance = null;
            try {
                variance = ndx.getVariance();
                if ( variance != null ) {
                    Function sqrtFunc = new Function() {
                            public double forward( double x ) {
                                return Math.sqrt( x );
                            }
                            public double inverse( double y ) {
                                return ( y * y );
                            }
                        };
                    errors = unwrapNDArray( variance, sqrtFunc );
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                errors = null;
            }
        }
        else {
            errors = null;
        }
        return;
    }

    /**
     * "Unwrap" an NDArray by copying it into a double precision
     * array, converting the type as needed and applying a function to
     * the values (e.g. to convert from variance to errors).
     */
    protected double[] unwrapNDArray( NDArray nda, Function func )
    {
        int size = 0;
        try {
            size = (int) nda.getShape().getNumPixels();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        double result[] = null;

        //  Wrap the NDArray so that it will be converted to double
        //  precision use our BAD value and have the function applied.
        if ( func != null ) {
            nda = NDArrayFactory.toFunctionNDArray( nda, func, Type.DOUBLE );
        }
        Requirements req = new Requirements( Requirements.Mode.READ );
        req.setType( Type.DOUBLE );
        req.setBadHandler( BadHandler.getHandler( Type.DOUBLE,
                                                  new Double(SpecData.BAD) ) );
        try {
            nda = NDArrayFactory.toRequiredNDArray( nda, req );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }

        // Array is mapped - read pixels directly.
        if ( nda.isMapped() ) {
            result = (double[]) nda.getMapped();
        }
        else {
            result = new double[size];
            try {
                // Array is not mapped - work through it in chunks.
                ChunkIterator cIt = new ChunkIterator( size );
                int chunk = cIt.getSize();
                int count = 0;
                while ( cIt.hasNext() ) {
                    int thisSize = cIt.getSize();
                    nda.read( result, count, thisSize );
                    cIt.next();
                    count += thisSize;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
