package uk.ac.starlink.ttools.mode;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.moc.SMoc;
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
    public static final MocFormat FITS_FORMAT;

    /** MocFormat implementation that writes JSON files. */
    public static final MocFormat JSON_FORMAT;

    /** MocFormat implementation that writes MOC 2.0 ASCII output. */
    public static MocFormat ASCII_FORMAT;

    /** Available output formats. */
    private static MocFormat[] FORMATS = new MocFormat[] {
        FITS_FORMAT = new CdsMocFormat( "fits" ) {
            protected void doWrite( SMoc moc, OutputStream out )
                    throws Exception {
                moc.writeFITS( out );
            }
        },
        JSON_FORMAT = new CdsMocFormat( "json" ) {
            protected void doWrite( SMoc moc, OutputStream out )
                    throws Exception {
                moc.writeJSON( out );
            }
        },
        ASCII_FORMAT = new CdsMocFormat( "ascii" ) {
            protected void doWrite( SMoc moc, OutputStream out )
                    throws Exception {
                moc.writeASCII( out );
                out.write( '\n' );
            }
        },
    };

    /**
     * Constructor.
     */
    public MocMode() {
        orderParam_ = new IntegerParameter( "order" );
        orderParam_.setPrompt( "MOC Healpix maximum order" );
        orderParam_.setUsage( "0.." + SMoc.MAXORD_S );
        orderParam_.setMinimum( 0 );
        orderParam_.setMaximum( SMoc.MAXORD_S );
        int orderDflt = 13;
        orderParam_.setIntDefault( orderDflt );
        int dfltResArcsec = 
            (int) Math.round( 3520 * Math.pow( 2, -orderDflt ) * 60 );
        orderParam_.setDescription( new String[] {
            "<p>Maximum HEALPix order for the MOC.",
            "This defines the maximum resolution of the output coverage map.",
            "The angular resolution corresponding to order <em>k</em>",
            "is approximately 180/sqrt(3.Pi)/2^<em>k</em> degrees",
            "(3520*2^<em>-k</em> arcmin).",
            "Permitted values are 0.." + SMoc.MAXORD_S + " inclusive.",
            "The default value is " + orderDflt + ", which corresponds to",
            "about " + dfltResArcsec + " arcsec.",
            "</p>",
        } );

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

        mocfmtParam_ = new ChoiceParameter<MocFormat>( "mocfmt",
                                                       MocFormat.class,
                                                       FORMATS );
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
            "<p>Generates and outputs a Multi-Order Coverage map from",
            "the sky positions associated with the rows of the input table.",
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
                SMoc moc;
                try {
                    moc = createMoc( order, qseq );
                }
                finally {
                    qseq.close();
                }
                if ( logger_.isLoggable( Level.INFO ) ) {
                    logger_.info( "MOC: size=" + moc.getNbCoding() 
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
    private static SMoc createMoc( int order, ConeQueryRowSequence qseq )
            throws IOException {
        SMoc moc;
        try {
            moc = new SMoc( order );
        }
        catch ( Exception e ) {
            throw (IOException) new IOException( "Error creating MOC"
                                               + " (bad order " + order + "?)" )
                               .initCause( e );
        }
        logger_.info( "New MOC order=" + order
                    + ", resolution=" + (float) moc.getAngularRes() + "deg" );
        HealpixNested hpx = Healpix.getNested( order );
        moc.bufferOn();
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
        moc.bufferOff();
        return moc;
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
        protected abstract void doWrite( SMoc moc, OutputStream out )
                throws Exception;

        public void writeMoc( SMoc moc, OutputStream out )
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
