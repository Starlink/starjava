/*
 * Copyright (C) 2002-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-MAY-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Source;

import uk.ac.starlink.ast.FrameSet;
import nom.tam.fits.Header;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.hds.NDFNdxHandler;
import uk.ac.starlink.hds.NdfMaker;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.ndx.XMLNdxHandler;
import uk.ac.starlink.util.URLUtils;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;

/**
 * NDXSpecDataImpl - implementation of SpecDataImpl to access a spectrum
 *                   stored in an NDX.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see "The Bridge Design Pattern"
 */
public class NDXSpecDataImpl
    extends AbstractSpecDataImpl
{

    //// Implementation of abstract methods.

    /**
     * Constructor - open an HDX description by name. The name is
     * understood by HDX, we don't need to know too much, but this
     * should only contain a single NDX, that is a spectrum.
     */
    public NDXSpecDataImpl( String hdxName )
        throws SplatException
    {
        super( hdxName );
        open( hdxName );
    }

    /**
     * Constructor - use an NDX described in a DOM.
     */
    public NDXSpecDataImpl( Element ndxElement )
        throws SplatException
    {
        super( "Wired NDX" );
        open( ndxElement );
    }

    /**
     * Constructor - use a URL containing a NDX.
     */
    public NDXSpecDataImpl( URL url  )
        throws SplatException
    {
        super( "Network NDX" );
        open( url );
    }

    /**
     * Constructor - use a Source containing a NDX.
     */
    public NDXSpecDataImpl( Source source  )
        throws SplatException
    {
        super( "Sourced NDX" );
        open( source );
    }

    /**
     * Constructor - an NDX.
     */
    public NDXSpecDataImpl( Ndx ndx  )
        throws SplatException
    {
        super( "Native NDX" );
        open( ndx );
    }

    /**
     * Constructor - an NDX with a given short and full names.
     */
    public NDXSpecDataImpl( Ndx ndx, String shortName, String fullName  )
        throws SplatException
    {
        super( "Native NDX" );
        open( ndx );
        this.shortName = shortName;
        this.fullName = fullName;
    }

    /**
     * Constructor. Initialise this spectrum by cloning the content of
     * another spectrum (usual starting point for saving).
     */
    public NDXSpecDataImpl( String hdxName, SpecData source )
        throws SplatException
    {
        super( hdxName );
        makeMemoryClone( source );
        this.fullName = hdxName;
        this.shortName = hdxName;
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
     * Return the NDX shape.
     */
    public int[] getDims()
    {
        if ( dims == null ) {
            try {
                long[] ldims =  ndx.getImage().getShape().getDims();
                dims = new int[ldims.length];
                for ( int i = 0; i < dims.length; i++ ) {
                    dims[i] = (int) ldims[i];
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                dims = new int[0];
                dims[0] = 0;
            }
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
        if ( astref == null ) {
            try {
                astref = Ndxs.getAst( ndx );
                astref.setUnit
                    ( 1, UnitUtilities.fixUpUnits( astref.getUnit( 1 ) ) );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return astref;
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
    public void save()
        throws SplatException
    {
        saveToFile();
    }

    /**
     * Return a keyed value from the NDF character components or FITS headers.
     * Returns "" if not found.
     */
    public String getProperty( String key )
    {
        //  NDX offers title, label and units for to describe the data.
        if ( key.equalsIgnoreCase( "title" ) && ndx.hasTitle() ) {
            return ndx.getTitle();
        }
        return super.getProperty( key );
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
     * The AST FrameSet, cached from NDX access or set by clone.
     */
    private FrameSet astref = null;

    /**
     * True when object is cloned, but not yet saved.
     */
    private boolean cloned = false;

    /**
     * FITS headers of cloned object.
     */
    private Header clonedHeader = null;

    /**
     * Cached dimensions of the NDX.
     */
    private int[] dims = null;

    /**
     * Open an HDX description and locate the NDX.
     *
     * @param hdxName HDX description of the NDX (could be a simple
     *                XML file or my include an XPath component, this
     *                should be dealt with by HDX).
     */
    protected void open( String hdxName )
        throws SplatException
    {
        try {
            // Check for a local file first. Windows doesn't like
            // using a "file:." for these.
            URL url = null;
            File infile = new File( hdxName );
            if ( infile.exists() ) {
                url = infile.toURL();
            }
            else {
                url = new URL( new URL( "file:." ), hdxName );
            }
            open( url );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }
    }

    /**
     * Open an HDX description and locate the NDX.
     */
    protected void open( URL url )
        throws SplatException
    {
        try {
            NdxIO ndxIO = new NdxIO();
            ndx = ndxIO.makeNdx( url, AccessMode.READ );
        }
        catch ( Exception e ) {
            throw new SplatException( e );
        }
        if ( ndx == null ) {
            throw new SplatException( "Document contains no NDXs" );
        }
        setNames( url.toString(), false );

        //  Read in the data.
        readData();
    }

    /**
     * Open an NDX stored in a Source.
     */
    protected void open( Source source )
        throws SplatException
    {
        try {
            // Get a HDX container with the NDX inside.
            ndx = XMLNdxHandler.getInstance().makeNdx( source,
                                                       AccessMode.READ );
        }
        catch (Exception e) {
            throw new SplatException( e );
        }
        if ( ndx == null ) {
            throw new SplatException( "Document contains no NDXs" );
        }
        setNames( "Sourced NDX", true );

        //  Read in the data.
        readData();
    }

    /**
     * Open an NDX stored in an Element.
     *
     * @param ndxElement the Element.
     */
    protected void open( Element ndxElement )
        throws SplatException
    {
        DOMSource ndxSource = new DOMSource( ndxElement );
        open( ndxSource );
        setNames( "Wired NDX", true );
    }

    /**
     * Use an existing NDX.
     *
     * @param ndx the NDX.
     */
    protected void open( Ndx ndx )
        throws SplatException
    {
        this.ndx = ndx;
        setNames( "NDX", true );
        readData();
    }

    //  Match names to title of the ndx, or generate a unique title from
    //  a prefix.
    protected void setNames( String defaultPrefix, boolean unique )
    {
        String title = ndx.hasTitle() ? ndx.getTitle() : "";
        if ( title == null || title.equals( "" ) ) {
            if ( unique ) {
                title = defaultPrefix + " (" + (counter++) + ")";
            }
            else {
                title = defaultPrefix;
            }
        }
        fullName = title;
        shortName = title;
    }

    /**
     * Read in the NDX data components. These are copied to local
     * double precision array.
     */
    protected void readData()
        throws SplatException
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
            data = null;
            throw new SplatException( e );
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

            //  Set the data units and label.
            if ( ndx.hasLabel() ) {
                setDataLabel( ndx.getLabel() );
            }
            if ( ndx.hasUnits() ) {
                setDataUnits( ndx.getUnits() );
            }
        }
        catch ( IOException e ) {
            data = errors = null;
            throw new SplatException( e );
        }
        return;
    }

    /**
     * Finalise object. Free any resources associated with member
     * variables.
     */
    protected void finalize()
        throws Throwable
    {
        if ( imAccess != null ) {
            imAccess.close();
        }
        if ( errAccess != null ) {
            errAccess.close();
        }
        super.finalize();
    }

    /**
     * Set up this object as a clone of another spectrum. The resultant state
     * is a memory-only resident one, until the save method is called, when
     * the actual file is created and the contents are written.
     */
    protected void makeMemoryClone( SpecData source )
    {
        cloned = true;
        shortName = source.getShortName();
        data = source.getYData();
        errors = source.getYDataErrors();
        astref = source.getFrameSet();
        dataLabel = source.getDataLabel();
        dataUnits = source.getDataUnits();
        dims = new int[1];
        dims[0] = data.length;

        //  If the source spectrum provides access to a set of FITS
        //  headers then we should preserve them.
        if ( source.getSpecDataImpl().isFITSHeaderSource() ) {
            clonedHeader =
                ((FITSHeaderSource) source.getSpecDataImpl()).getFitsHeaders();
        }
    }

    /**
     * Create a new NDX as a HDS representation using the current
     * configuration to populate it. Will only succeed for clone spectra.
     */
    protected void saveToFile()
        throws SplatException
    {
        //  Parse the name to extract the container file name.
        PathParser namer = new PathParser( fullName );
        String container = namer.diskfile();
        if ( container.endsWith( ".sdf" ) ) {
            container = container.substring(0, container.length()-4);
        }
        String path = namer.path();
        if ( namer.format().equals( "XML" ) ) {
            path = ".xml";
            container = container.substring(0, container.length()-4);
        }
        try {
            //  Create an NDX from the existing state.
            Number bad = new Double( SpecData.BAD );
            long[] dims = new long[1];
            dims[0] = (long) data.length;
            OrderedNDShape shape = new OrderedNDShape( dims, null );
            ArrayArrayImpl aai = new ArrayArrayImpl( data, shape, bad );

            NDArray ndArray = new BridgeNDArray( aai, null );
            MutableNdx tmpNdx = new DefaultMutableNdx( ndArray);
            tmpNdx.setWCS( astref );
            if ( errors != null ) {
                double[] vars = new double[errors.length];
                for ( int i = 0; i < errors.length; i++ ) {
                    vars[i] = errors[i] * errors[i];
                }
                aai = new ArrayArrayImpl( vars, shape, bad );
                ndArray = new BridgeNDArray( aai, null );
                tmpNdx.setVariance( ndArray );
            }
            tmpNdx.setTitle( shortName );
            tmpNdx.setLabel( dataLabel );
            tmpNdx.setUnits( dataUnits );

            //  Create the new HDS file.
            HDSReference href = new NdfMaker().makeNDF( tmpNdx, container );
            HDSObject hobj = href.getObject( "WRITE" );
            NDFNdxHandler handler = NDFNdxHandler.getInstance();

            //  Full URL for HDS component is
            //  file://localhost/container.sdf#hds_path.
            URL url = null;
            if ( path == null || "".equals( path ) ) {
                url = new URL( "file://localhost/" + container + ".sdf" );
            }
            else {
                url = new URL( "file://localhost/" + container + ".sdf#" +
                               path.substring( 1 ) );
            }
            Ndx newNdx = handler.makeNdx( hobj, url, AccessMode.WRITE );

            //  No longer a memory clone, backing file is created.
            cloned = false;
            data = null;
            errors = null;
            clonedHeader = null;
            dims = null;

            //  So open properly.
            open( newNdx );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new SplatException( "Failed saving NDX to an NDF", e );
        }
    }
}

