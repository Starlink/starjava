package uk.ac.starlink.ttools.mode;

import cds.moc.HealpixMoc;
import cds.moc.HealpixImpl;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.JELQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.util.Destination;

/**
 * Turns a table into a Multi-Order Coverage map.
 *
 * @author   Mark Taylor
 * @since    8 Mar 2012
 */
public class MocMode implements ProcessingMode {

    private final IntegerParameter orderParam_;
    private final Parameter raParam_;
    private final Parameter decParam_;
    private final Parameter radiusParam_;
    private final ChoiceParameter<MocFormat> mocfmtParam_;
    private final OutputStreamParameter outParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    /**
     * Constructor.
     */
    public MocMode() {
        orderParam_ = new IntegerParameter( "order" );
        orderParam_.setPrompt( "MOC Healpix maximum order" );
        orderParam_.setMinimum( 1 );
        orderParam_.setMaximum( HealpixMoc.MAXORDER );
        orderParam_.setDescription( new String[] {
            "<p>Maximum HEALPix order for the MOC.",
            "This defines the maximum resolution of the output coverage map.",
            "The angular resolution corresponding to order <em>k</em>",
            "is approximately 180/sqrt(3.Pi)/2^<em>k</em>",
            "(3520*2^<em>-k</em> arcmin).",
            "</p>",
        } );
        orderParam_.setDefault( Integer.toString( 13 ) );

        raParam_ = new Parameter( "ra" );
        raParam_.setUsage( "<expr>" );
        raParam_.setPrompt( "Right Ascension expression in degrees" );
        raParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the right ascension in degrees",
            "for the position at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one,",
            "as described in <ref id='jel'/>.",
            "</p>",
        } );

        decParam_ = new Parameter( "dec" );
        decParam_.setUsage( "<expr>" );
        decParam_.setPrompt( "Declination expression in degrees" );
        decParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the declination in degrees",
            "for the position at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one,",
            "as described in <ref id='jel'/>.",
            "</p>",
        } );

        radiusParam_ = new DoubleParameter( "radius" );
        radiusParam_.setUsage( "<expr>" );
        radiusParam_.setPrompt( "Radius expression in degrees" );
        radiusParam_.setDescription( new String[] {
            "<p>Expression which evaluates to the radius in degrees",
            "of the cone at each row of the input table.",
            "The default is \"<code>0</code>\",",
            "which treats each position as a point rather than a cone,",
            "but a constant or an expression as described in",
            "<ref id='jel'/> may be used instead.",
            "</p>",
        } );
        radiusParam_.setDefault( "0" );

        mocfmtParam_ =
            new ChoiceParameter<MocFormat>( "mocfmt", MocFormat.getFormats() );
        mocfmtParam_.setPrompt( "Output format for MOC file" );
        mocfmtParam_.setDescription( new String[] {
            "<p>Determines the output format for the MOC file.",
            "</p>",
        } );
        mocfmtParam_.setDefaultOption( MocFormat.FITS );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPreferExplicit( true );
        outParam_.setPrompt( "Location of output MOC file" );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            orderParam_,
            raParam_,
            decParam_,
            radiusParam_,
            mocfmtParam_,
            outParam_,
        };
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Generates a Multi-Order Coverage map from the sky positions",
            "associated with the rows of the input table,",
            "and writes it out to a FITS or ASCII file.",
            "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        String raString = raParam_.stringValue( env );
        String decString = decParam_.stringValue( env );
        String radiusString = radiusParam_.stringValue( env );
        final QuerySequenceFactory qsFact =
            new JELQuerySequenceFactory( raString, decString, radiusString );
        final int order = orderParam_.intValue( env );
        final MocFormat mocfmt = mocfmtParam_.objectValue( env );
        final Destination dest = outParam_.destinationValue( env );
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                ConeQueryRowSequence qseq = qsFact.createQuerySequence( table );
                HealpixMoc moc;
                try {
                    moc = createMoc( order, qseq );
                }
                finally {
                    qseq.close();
                }
                if ( logger_.isLoggable( Level.INFO ) ) {
                    logger_.info( "MOC: size=" + moc.getSize() 
                                + ", coverage=" + moc.getCoverage() );
                }
                OutputStream out = dest.createStream();
                try {
                    mocfmt.writeMoc( moc, out );
                }
                finally {
                    out.close();
                }
            }
        };
    }

    /**
     * Builds and returns a MOC from a given sequence of cones.
     *
     * @param  order  MOC max order
     * @param  qseq  cone sequence
     */
    private static HealpixMoc createMoc( int order, ConeQueryRowSequence qseq )
            throws IOException {
        HealpixMoc moc;
        try {
            moc = new HealpixMoc( order );
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "Error creating MOC"
                                               + " (bad order " + order + "?)" )
                               .initCause( e );
        }
        logger_.info( "New MOC order=" + order
                    + ", resolution=" + (float) moc.getAngularRes() + "deg" );
        HealpixImpl healpix = PixtoolsHealpix.getInstance();
        setChecked( moc, false );
        while ( qseq.next() ) {
            double ra = qseq.getRa();
            double dec = qseq.getDec();
            double radius = qseq.getRadius();
            if ( ! Double.isNaN( ra ) &&
                 dec >= -90 && dec <= 90 &&
                 radius >= 0 ) {
                try {
                    if ( radius == 0 ) {
                        moc.add( order, healpix.ang2pix( order, ra, dec ) );
                    }
                    else {
                        long[] pixels =
                            healpix.queryDisc( order, ra, dec, radius );
                        int npix = pixels.length;
                        for ( int ipix = 0; ipix < npix; ipix++ ) {
                            moc.add( order, pixels[ ipix ] );
                        }
                    }
                }
                catch ( Exception e ) {
                    throw (IOException) new IOException( "HEALPix/MOC error" )
                                       .initCause( e );
                }
            }
        }
        setChecked( moc, true );
        return moc;
    }

    /**
     * Sets the continuous checking flag for the MOC object.
     * When continuous checking is on, parent pixels are supposed to get
     * weeded out as adds are done.  However, it's much slower.
     *
     * @param  moc  MOC to affect
     * @param  checked   true iff continuous checking should be performed
     */
    public static void setChecked( HealpixMoc moc, boolean checked )
            throws IOException {
        try {
            moc.setCheckConsistencyFlag( checked );
            if ( checked ) {
                moc.checkAndFix();
            }
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "MOC error" ).initCause( e );
        }
    }

    /**
     * Output strategy for a MOC.
     */
    private static class MocFormat {

        private final String name_;
        private final int outMode_;
        static final MocFormat FITS =
            new MocFormat( "fits", HealpixMoc.FITS );
        static final MocFormat ASCII =
            new MocFormat( "ascii", HealpixMoc.ASCII );

        /**
         * Constructor.
         *
         * @param   format name
         * @param   output mode key known to HealpixMoc class
         */
        private MocFormat( String name, int outMode ) {
            name_ = name;
            outMode_ = outMode;
        }

        /**
         * Outputs a given MOC to a given stream.
         *
         * @param  moc  MOC
         * @param  out  destination stream
         */
        public void writeMoc( HealpixMoc moc, OutputStream out )
                throws IOException {
            try {
                moc.write( out, outMode_ );
            }
            catch ( Exception e ) {
                throw (IOException) new IOException( "MOC write error" )
                                   .initCause( e );
            }
        }

        public String toString() {
            return name_;
        }

        /**
         * Returns all known instances.
         *
         * @return  instance array
         */
        public static MocFormat[] getFormats() {
            return new MocFormat[] { FITS, ASCII };
        }
    }
}
