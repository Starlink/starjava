package uk.ac.starlink.ttools.mode;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.moc.HealpixMoc;
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
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.cone.ConeQueryRowSequence;
import uk.ac.starlink.ttools.cone.JELQuerySequenceFactory;
import uk.ac.starlink.ttools.cone.MocFormat;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.task.SkyCoordParameter;
import uk.ac.starlink.util.Destination;

/**
 * Turns a table into a Multi-Order Coverage map.
 *
 * @author   Mark Taylor
 * @since    8 Mar 2012
 */
public class MocMode implements ProcessingMode {

    private final IntegerParameter orderParam_;
    private final StringParameter raParam_;
    private final StringParameter decParam_;
    private final StringParameter radiusParam_;
    private final ChoiceParameter<MocFormat> mocfmtParam_;
    private final OutputStreamParameter outParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    /** MocFormat implementation that writes MOC 1.0-compliant FITS files. */
    public static final MocFormat FITS_FORMAT = new CdsMocFormat( "fits" ) {
        protected void doWrite( HealpixMoc moc, OutputStream out )
                throws Exception {
            moc.writeFits( out );
        }
    };

    /** MocFormat implementation that writes JSON files. */
    public static final MocFormat JSON_FORMAT = new CdsMocFormat( "json" ) {
        protected void doWrite( HealpixMoc moc, OutputStream out )
                throws Exception {
            moc.writeJSON( out );
        }
    };

    /**
     * Constructor.
     */
    public MocMode() {
        orderParam_ = new IntegerParameter( "order" );
        orderParam_.setPrompt( "MOC Healpix maximum order" );
        orderParam_.setMinimum( 0 );
        orderParam_.setMaximum( HealpixMoc.MAXORDER );
        orderParam_.setDescription( new String[] {
            "<p>Maximum HEALPix order for the MOC.",
            "This defines the maximum resolution of the output coverage map.",
            "The angular resolution corresponding to order <em>k</em>",
            "is approximately 180/sqrt(3.Pi)/2^<em>k</em>",
            "(3520*2^<em>-k</em> arcmin).",
            "</p>",
        } );
        orderParam_.setIntDefault( 13 );

        String system = null;
        String inDescrip = "the input table";
        raParam_ =
            SkyCoordParameter.createRaParameter( "ra", system, inDescrip );
        decParam_ =
            SkyCoordParameter.createDecParameter( "dec", system, inDescrip );

        radiusParam_ = new StringParameter( "radius" );
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
        radiusParam_.setStringDefault( "0" );

        mocfmtParam_ =
            new ChoiceParameter<MocFormat>( "mocfmt", MocFormat.class,
                                            new MocFormat[] {
                                                FITS_FORMAT, JSON_FORMAT,
                                            } );
        mocfmtParam_.setPrompt( "Output format for MOC file" );
        mocfmtParam_.setDescription( new String[] {
            "<p>Determines the output format for the MOC file.",
            "</p>",
        } );
        mocfmtParam_.setDefaultOption( FITS_FORMAT );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPreferExplicit( true );
        outParam_.setPrompt( "Location of output MOC file" );
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[] {
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
            "and writes it out to a FITS or JSON file.",
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
        final Destination dest = outParam_.objectValue( env );
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
        HealpixNested hpx = Healpix.getNested( order );
        setChecked( moc, false );
        while ( qseq.next() ) {
            double raRad = Math.toRadians( qseq.getRa() );
            double decRad = Math.toRadians( qseq.getDec() );
            double radiusRad = Math.toRadians( qseq.getRadius() );
            if ( ! Double.isNaN( raRad ) &&
                 decRad >= -0.5 * Math.PI && decRad <= 0.5 * Math.PI &&
                 radiusRad >= 0 ) {
                HealpixNestedBMOC bmoc =
                    hpx.newConeComputerApprox( radiusRad )
                       .overlappingCells( raRad, decRad );
                for ( HealpixNestedBMOC.CurrentValueAccessor vac : bmoc ) {
                    try {
                        moc.add( vac.getDepth(), vac.getHash() );
                    }
                    catch ( Exception e ) {
                        throw (IOException)
                              new IOException( "HEALPix/MOC error" )
                             .initCause( e );
                    }
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
     * Partial MocFormat implementation.
     */
    private static abstract class CdsMocFormat implements MocFormat {

        private final String name_;

        /**
         * Constructor.
         *
         * @param   format name
         */
        CdsMocFormat( String name ) {
            name_ = name;
        }

        /**
         * Does the write.
         * This method throws Exception, which is what the corresponding
         * CDS MOC library write methods do.
         *
         * @param  moc  MOC
         * @param  out  destination stream
         */
        protected abstract void doWrite( HealpixMoc moc, OutputStream out )
                throws Exception;

        public void writeMoc( HealpixMoc moc, OutputStream out )
                throws IOException {
            try {
                doWrite( moc, out );
            }
            catch ( Exception e ) {
                throw (IOException) new IOException( "MOC write error" )
                                   .initCause( e );
            }
        }

        @Override
        public String toString() {
            return name_;
        }
    }
}
