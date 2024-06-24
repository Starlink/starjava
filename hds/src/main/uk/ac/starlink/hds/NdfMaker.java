package uk.ac.starlink.hds;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.UnitMap;
import uk.ac.starlink.ast.WinMap;
import uk.ac.starlink.hds.HDSException;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSReference;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.util.GenericNioBuffer;

/**
 * Creates NDFs on disk from Ndx objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NdfMaker {

    private HDSType hdstype;
    private NDShape window;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.hds" );
    private static final long[] SCALAR_DIMS = new long[ 0 ];

    /**
     * Constructs a default NdfMaker
     */
    public NdfMaker() {
    }

    /**
     * Set the type for the DATA and, if available, VARIANCE components
     * of created NDFs.  It is not necessary to do this, but since
     * a copy of the data has to be done in any case it may be more
     * efficient than creating a copy of the original data type and
     * subsequently mapping the NDF array data with a different type.
     * <p>
     * <b>Note</b>: not all HDS types may be used: only the following
     * are valid:
     * _BYTE, _WORD, _INTEGER, _REAL, _DOUBLE
     *
     * @param  hdstype the required data type for DATA and VARIANCE components.
     *                 If null, a type corresponding to that of the NDX
     *                 arrays is used.
     */
    public void setType( HDSType hdstype ) {
        this.hdstype = hdstype;
    }

    /**
     * Get the type with which the NDF DATA and VARIANCE arrays will be
     * created.
     *
     * @return  the HDS data type with which DATA and VARIANCE components
     *          will be created.  If null, a type corresponding to that
     *          of the NDX arrays is used.
     */
    public HDSType getType() {
        return hdstype;
    }

    /**
     * Set the shape of NDFs to be created, in the manner of an
     * NDF section.
     *
     * @param   window  the shape of NDFs to be written.  If null, the
     *                  shape of the NDX's image array will be used
     */
    public void setWindow( NDShape window ) {
        this.window = window;
    }

    /**
     * Get the shape of NDFs to be created, in the manner of an
     * NDF section.
     *
     * @return  the shape of NDFs to be written.  If null, the shape of
     *          the NDX's image array will be used
     */
    public NDShape getWindow() {
        return window;
    }

    /**
     * Creates an NDF in temporary filespace from an NDX.
     * The new NDF will be at the top level in a newly-created container
     * file; its location is given by the return value of this call.
     * It is the responsibility of the caller to delete the container
     * file when it is no longer needed.
     * <p>
     * The container file is written in one of the following directories
     * (listed in order of preference):
     * <ol>
     * <li>the <code>uk.ac.starlink.hds.scratch</code> system property
     * <li>the <code>java.io.tmpdir</code> system property
     * <li>the current directory
     * </ol>
     *
     * @param  ndx  the NDX to copy
     * @return  an HDSReference object giving the location of the new NDF
     *          structure based on ndx.
     * @throws  IOException  if there was some error in parsing the NDX
     *                       or creating the NDF
     */
    public HDSReference makeTempNDF( Ndx ndx ) throws IOException {
        File tmpfile = File.createTempFile( "ndf", ".sdf", getTmpDir() );
        tmpfile.delete();
        String container = tmpfile.getPath();
        container = container.replaceFirst( ".sdf$", "" );
        return makeNDF( ndx, container );
    }

    /**
     * Creates an NDF in a named container file from an NDX.
     *
     * @param  ndx        the NDX to copy
     * @param  container  the pathname of a container file to which the
     *                    NDF should be written.  Do not include the '.sdf'.
     *                    A new file container.sdf will be written and the
     *                    NDF will put into the top-level structure.
     * @return   an HDSReference giving the location of the new NDF structure
     * @throws  IOException  if there was some error in parsing the NDX
     *                       or creating the NDF
     */
    public HDSReference makeNDF( Ndx ndx, String container )
            throws IOException {
        String name = new File( container ).getName().toUpperCase();
        if ( name.length() > HDSObject.DAT__SZNAM ) {
            name = name.substring( 0, HDSObject.DAT__SZNAM );
        }
        HDSObject ndfob;
        HDSReference ndfref;
        try {
            ndfob = HDSObject.hdsNew( container, name, "NDF", SCALAR_DIMS );
            ndfref = new HDSReference( ndfob );
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        makeNDF( ndx, ndfob );
        return ndfref;
    }

    /**
     * Populates an NDF structure at a given location with data from an NDX.
     *
     * @param  ndx     the NDX to copy
     * @param  ndfob   an HDS structure into which the NDF's components
     *                 are to be written
     * @throws  IOException  if there was some error in parsing the NDX
     *                       or creating the NDF
     */
    public void makeNDF( Ndx ndx, HDSObject ndfob ) throws IOException {
        try {
            copyNdxToNdf( ndx, ndfob, true );
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Creates an NDF structure at a given location based on an NDX, 
     * but doesn't fill in the array components.
     *
     * @param  ndx     the NDX to copy
     * @param  ndfob   an HDS structure into which the NDF's components
     *                 are to be written
     * @throws  IOException  if there was some error in parsing the NDX
     *                       or creating the NDF
     */
    public void makeBlankNDF( Ndx ndx, HDSObject ndfob ) throws IOException {
        try {
            copyNdxToNdf( ndx, ndfob, false );
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Returns the directory to use for writing temporary HDS files.
     *
     * Note: update the makeTempNDF doc comment if the search policy is changed.
     *
     * @return   the temporary directory to use, or null if none is known
     */
    private File getTmpDir() {
        String dirname = null;
        if ( dirname == null ) {
            dirname = System.getProperty( "uk.ac.starlink.hds.scratch" );
        }
        if ( dirname == null ) {
            dirname = System.getProperty( "java.io.tmpdir" );
        }
        if ( dirname == null ) {
            dirname = ".";
        }
        return new File( dirname );
    }

    /**
     * Does the work of copying the data of an NDX into a new NDF structure
     * at a given location.  The supplied HDSObject <code>ndfob</code> is
     * annulled by this method when the write is complete.
     *
     * @param  ndx    the NDX to copy
     * @param  ndfob  the NDF object into which to copy the components
     * @param copyData  if true, the array component data is copied.
     *                  if false, the array components are created 
     *                  but the data is not copied across
     */
    private void copyNdxToNdf( Ndx ndx, HDSObject ndfob, boolean copyData )
            throws HDSException, IOException {

        /* Get the shape. */
        NDShape shape = ( window == null ) ? ndx.getImage().getShape()
                                           : window;

        /* Image component. */
        copyArray( ndx.getImage(), ndfob, "DATA_ARRAY", shape, hdstype,
                   copyData );

        /* Variance component. */
        if ( ndx.hasVariance() ) {
            copyArray( ndx.getVariance(), ndfob, "VARIANCE", shape, hdstype,
                       copyData );
        }

        /* Quality component. */
        if ( ndx.hasQuality() ) {
            ndfob.datNew( "QUALITY", "QUALITY", SCALAR_DIMS );
            HDSObject qobj = ndfob.datFind( "QUALITY" );
            int ibad = ndx.getBadBits();
            if ( ibad > 0 ) {
                qobj.datNew( "BADBITS", "_UBYTE", SCALAR_DIMS );
                qobj.datFind( "BADBITS" ).datPut0i( ibad );
            }
            NDArray qnda = ndx.getQuality();
            if ( qnda.getType() != Type.BYTE ) {
                logger.warning( "Truncating Quality component from " +
                                ( qnda.getType().getNumBytes() * 8 ) +
                                " to 8 bits" );
            }
            copyArray( qnda, qobj, "QUALITY", shape,
                       HDSType._UBYTE, copyData );
            HDSObject qcomp = qobj.datFind( "QUALITY" );
            qcomp.datNew( "BAD_PIXEL", "_LOGICAL", SCALAR_DIMS );
            qcomp.datFind( "BAD_PIXEL" ).datPut0l( false );
        }

        /* Title component. */
        if ( ndx.hasTitle() ) {
            String title = ndx.getTitle();
            ndfob.datNew( "TITLE", "_CHAR*" + title.length(), SCALAR_DIMS );
            ndfob.datFind( "TITLE" ).datPut0c( title );
        }

        /* Label component. */
        if ( ndx.hasLabel() ) {
            String label = ndx.getLabel();
            ndfob.datNew( "LABEL", "_CHAR*" + label.length(), SCALAR_DIMS );
            ndfob.datFind( "LABEL" ).datPut0c( label );
        }

        /* Units component. */
        if ( ndx.hasUnits() ) {
            String units = ndx.getUnits();
            ndfob.datNew( "UNITS", "_CHAR*" + units.length(), SCALAR_DIMS );
            ndfob.datFind( "UNITS" ).datPut0c( units );
        }

        /* WCS component. */
        if ( ndx.hasWCS() ) {
            FrameSet wcs = doctorForNDF( ndx.getAst(), shape );

            /* Construct a string filled with spaces. */
            final int nchars = 32;
            char[] blanks = new char[ nchars ];
            Arrays.fill( blanks, ' ' );
            final String blank = new String( blanks );

            /* Get a list of strings containing the channel output.
             * We use the same format as the NDF library - 32-character
             * strings in which the first character is ' ' for the first
             * string in a record, and '+' for continuation lines. */
            final List lines = new ArrayList();
            Channel lchan = new Channel() {
                protected void sink( String line ) {
                    line = line.trim();
                    int pos = 0;
                    int leng = line.length();
                    while ( pos < leng ) {
                        StringBuffer sbuf = new StringBuffer( blank );
                        sbuf.setCharAt( 0, pos == 0 ? ' ' : '+' );
                        for ( int i = 1;
                              pos < leng && i < nchars;
                              i++, pos++ ) {
                            sbuf.setCharAt( i, line.charAt( pos ) );
                        }
                        lines.add( sbuf.toString() );
                    }
                }
            };
            lchan.setComment( false );
            lchan.setFull( -1 );
            lchan.write( wcs );

            /* Now write the strings as an HDS array. */
            int nline = lines.size();
            ndfob.datNew( "WCS", "WCS", SCALAR_DIMS );
            HDSObject wcsholder = ndfob.datFind( "WCS" );
            wcsholder.datNew( "DATA", "_CHAR*" + nchars, new long[] { nline } );
            HDSObject wcsob = wcsholder.datFind( "DATA" );
            long[] pos = new long[] { 1 };
            for ( Iterator it = lines.iterator(); it.hasNext(); ) {
                wcsob.datCell( pos ).datPut0c( (String) it.next() );
                pos[ 0 ]++;
            }
        }

        /* Writing is done - annul the primary HDSObject to ensure that
         * data is flushed. */
        ndfob.datAnnul();
    }

    /**
     * Copies the data from an NDArray into a new ARY structure.
     *
     * @param  nda      the source NDArray
     * @param  parent   the HDS structure to contain the new ARY structure
     * @param  name     the name of the new ARY structure
     * @param  shape    the shape of the new ARY structure
     * @param  htype    the HDS type of the new ARY structure
     * @paraam copyData if true, the array component data is copied.
     *                  if false, the array components are created 
     *                  but the data is not copied across
     */
    private void copyArray( NDArray nda, HDSObject parent, String name,
                            NDShape shape, HDSType htype, boolean copyData )
            throws HDSException, IOException {

        /* Get the actual HDS type of data to write. */
        if ( htype == null ) {
            htype = this.hdstype;
        }
        if ( htype == null ) {
            htype = HDSType.fromJavaType( nda.getType() );
        }

        /* Create an array structure under the parent. */
        ArrayStructure ary = new ArrayStructure( parent, name, htype, shape );

        /* Copy the actual array data if required. */
        if ( copyData ) {
            long[] dims = shape.getDims();

            /* Prepare an NDArray suitable for direct copy to the HDS array. */
            Type jtype = htype.getJavaType();
            BadHandler handler = BadHandler
                                .getHandler( jtype, htype.getBadValue() );
            Requirements req = new Requirements( AccessMode.READ )
                              .setOrder( Order.COLUMN_MAJOR )
                              .setWindow( shape )
                              .setType( jtype )
                              .setBadHandler( handler );
            nda = NDArrays.toRequiredArray( nda, req );

            /* Map its data. */
            HDSObject data = ary.getData();
            Buffer mapped = data.datMapv( htype.getName(), "WRITE" );
            GenericNioBuffer genbuf = new GenericNioBuffer( mapped );

            /* Copy the data from the NDArray into the mapped HDS array. */
            ChunkStepper cit = new ChunkStepper( shape.getNumPixels() );
            Object buf = htype.getJavaType().newArray( cit.getSize() );
            ArrayAccess acc = nda.getAccess();
            try {
                while ( cit.hasNext() ) {
                    int size = cit.getSize();
                    acc.read( buf, 0, size );
                    genbuf.put( buf, 0, size );
                    cit.next();
                }
            }
            finally {
                acc.close();
                data.datUnmap();
            }
        }
    }

    /**
     * Turn the WCS frameset into a suitable form for an NDF.
     * The NDF library reconstructs a WCS FrameSet to some extent when
     * it reads it, but makes certain assumptions, notably that the
     * first three frames are in the GRID, PIXEL and AXIS domains.
     *
     * @param   origWcs  the undoctored WCS FrameSet
     * @param   shape    the shape of the NDX it comes from
     * @return  the doctored WCS FrameSet suitable for inserting into an NDF
     */
    private static FrameSet doctorForNDF( FrameSet origWcs, NDShape shape ) {

        /* If it looks about right, return it without further ado. */
        if ( origWcs.getNframe() >= 3 &&
             origWcs.getFrame( 1 ).getDomain().equals( "GRID" ) &&
             origWcs.getFrame( 2 ).getDomain().equals( "PIXEL" ) &&
             origWcs.getFrame( 3 ).getDomain().equals( "AXIS" ) ) {
            return origWcs;
        }

        /* If it looks right except that it's missing an AXIS Frame
         * (this is sometimes removed by early versions of NDFNdxImpl),
         * just add a new AXIS Frame which is a copy of the PIXEL one. */
        else if ( origWcs.getNframe() >=2 &&
             origWcs.getFrame( 1 ).getDomain().equals( "GRID" ) &&
             origWcs.getFrame( 2 ).getDomain().equals( "PIXEL" ) ) {

            /* Make a copy of the original FrameSet and remove all but the
             * first two Frames (GRID and PIXEL). */
            FrameSet wcs = (FrameSet) origWcs.copy();
            while ( wcs.getNframe() > 2 ) {
                wcs.removeFrame( wcs.getNframe() );
            }

            /* Add an AXIS Frame as the third one which is a copy of the
             * PIXEL Frame. */
            Frame axframe = (Frame) wcs.getFrame( 2 ).copy();
            axframe.setDomain( "AXIS" );
            int ndim = axframe.getNaxes();
            wcs.addFrame( 2, new UnitMap( ndim ), axframe );

            /* Add any remaining frames from the original frameset. */
            int extras = origWcs.getNframe() - 2;
            for ( int i = 0; i < extras; i++ ) {
                wcs.addFrame( FrameSet.AST__BASE,
                              origWcs.getMapping( FrameSet.AST__BASE, i + 3 )
                                     .simplify(),
                              origWcs.getFrame( i + 3 ) );
            }

            /* Set the current frame correctly. */
            int cur = origWcs.getCurrent();
            wcs.setCurrent( cur <= 2 ? cur : cur + 1 );

            /* Return the doctored frameset. */
            return wcs;
        }
        
        /* It needs fixing - create a new FrameSet with the same Base frame,
         * newly created PIXEL and AXIS frames, and the rest of the frames
         * from the original grafted on after. */
        else {
            Frame gridframe = (Frame) origWcs.getFrame( 1 ).copy();
            gridframe.setDomain( "GRID" );
            FrameSet wcs = new FrameSet( gridframe );

            /* Create a mapping between the GRID and PIXEL frames. */
            int ndim = shape.getNumDims();
            double[] ina = new double[ ndim ];
            double[] inb = new double[ ndim ];
            double[] outa = new double[ ndim ];
            double[] outb = new double[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                double trans = shape.getOrigin()[ i ] - 0.5;
                ina[ i ] = 0.0;
                inb[ i ] = 1.0;
                outa[ i ] = ina[ i ] + trans;
                outb[ i ] = inb[ i ] + trans;
            }
            Mapping pmap = new WinMap( ndim, ina, inb, outa, outb );

            /* Add the PIXEL and AXIS frames to the new frameset. */
            Frame pixframe = new Frame( ndim );
            pixframe.setDomain( "PIXEL" );
            wcs.addFrame( 1, pmap, pixframe );
            Frame axframe = new Frame( ndim );
            axframe.setDomain( "AXIS" );
            wcs.addFrame( 2, new UnitMap( ndim ), axframe );

            /* Add any remaining frames from the original frameset. */
            int extras = origWcs.getNframe() - 1;
            for ( int i = 0; i < extras; i++ ) {
                wcs.addFrame( FrameSet.AST__BASE,
                              origWcs.getMapping( FrameSet.AST__BASE, i + 2 )
                                     .simplify(),
                              origWcs.getFrame( i + 2 ) );
            }

            /* Set the current frame correctly. */
            int cur = origWcs.getCurrent();
            wcs.setCurrent( cur <= 1 ? cur : cur + 2 );

            /* Return the doctored frameset. */
            return wcs;
        }
    }

}
