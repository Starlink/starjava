// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMSource;

import uk.ac.starlink.ast.FrameSet;
////import uk.ac.starlink.hdx.HdxContainer;
////import uk.ac.starlink.hdx.HdxContainerFactory;
////import uk.ac.starlink.hdx.Ndx;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.ndx.XMLNdxHandler;

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
        try {
            return Ndxs.getAst( ndx );
        }
        catch (Exception e) {
            e.printStackTrace();
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
     * Reference to NDX Image component Access object.
     */
    protected ArrayAccess imAccess = null;

    /**
     * Reference to NDX Error component Access object.
     */
    protected ArrayAccess errAccess = null;

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
        ////List ndxs = null;
        ////HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        ////HdxContainer hdx;
        try {
            NdxIO ndxIO = new NdxIO();
            URL url = new URL( new URL( "file:." ), hdxName );
            ndx = ndxIO.makeNdx( url, AccessMode.READ );
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndx == null ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }
        fullName = hdxName;
        shortName = hdxName;

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
        ////List ndxs = null;
        ////HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        ////HdxContainer hdx;
        DOMSource ndxSource = new DOMSource( ndxElement );
        try {
            // Get a HDX container with the NDX inside.
            ndx = XMLNdxHandler.getInstance().makeNdx( ndxSource,
                                                       AccessMode.READ );
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndx == null ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }

        String title = ndx.hasTitle() ? ndx.getTitle() : "";
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
        // Use a Requirements object to make sure that data is all the
        // same shape. We also require that the data be returned in
        // double precision and using our BAD data value.
        Requirements req = new Requirements();
        req.setType( Type.DOUBLE );
        BadHandler badHandler = 
            BadHandler.getHandler( Type.DOUBLE, new Double(SpecData.BAD) );
        req.setBadHandler( badHandler );
        OrderedNDShape oshape = ndx.getImage().getShape();
        req.setShape( oshape );

        try {
            NDArray imNda = Ndxs.getMaskedImage( ndx, req );
            imAccess = imNda.getAccess();
            if ( ndx.hasVariance() ) {
                NDArray errNda = Ndxs.getMaskedErrors( ndx, req );
                errAccess = errNda.getAccess();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            data = null;
            return;
        }

        //  No spectra longer than (int)?
        int size = (int) oshape.getNumPixels();

        // Get the data and possibly errors.
        try {
            if ( imAccess.isMapped() ) {
                data = (double []) imAccess.getMapped();
            }
            else {
                data = new double[ size ];
                imAccess.read( data, 0, size );
                imAccess.close();
            }

            errors = null;
            if ( errAccess != null ) {
                if ( errAccess.isMapped() ) {
                    errors = (double[]) errAccess.getMapped();
                }
                else {
                    errors = new double[ size ];
                    errAccess.read( errors, 0, size );
                    errAccess.close();
                }
            }
        }
        catch ( IOException ie ) {
            ie.printStackTrace();
            data = errors = null;
            return;
        }

        return;
    }

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize() throws Throwable
    {
        if ( imAccess != null ) {
            imAccess.close();
        }
        if ( errAccess != null ) {
            errAccess.close();
        }
        super.finalize();
    }
}
