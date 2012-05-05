package uk.ac.starlink.ttools.cone;

import cds.moc.HealpixImpl;
import cds.moc.HealpixMoc;
import java.io.IOException;
import java.io.InterruptedIOException;
import uk.ac.starlink.ttools.mode.MocMode;

/**
 * Footprint implementation giving the coverage defined by a sequence of
 * positional (cone search-like) queries.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2012
 */
public class ConeQueryFootprint extends MocFootprint {

    private ConeQueryRowSequence qseq_;
    private final double resolutionDeg_;

    /**
     * Constructor.
     *
     * @param   qseq  defines a sequence of cone searches
     * @param   resolutionDeg   approximate resolution of footprint in degrees
     */
    public ConeQueryFootprint( ConeQueryRowSequence qseq,
                               double resolutionDeg ) {
        super( PixtoolsHealpix.getInstance() );
        qseq_ = qseq;
        resolutionDeg_ = resolutionDeg;
    }

    @Override
    protected HealpixMoc createMoc() throws IOException {

        /* Initialise a MOC object with the right resolution. */
        Nsider nsider = new Nsider();
        int maxOrder = Math.min( nsider.calcOrder( resolutionDeg_ ),
                                 HealpixMoc.MAXORDER );
        HealpixMoc moc;
        try {
            moc = new HealpixMoc( maxOrder );
        }
        catch ( Exception e ) {
            throw (Error) new AssertionError( "Trouble creating HealpixMoc??" )
                         .initCause( e );
        }

        /* Add coverage for each item in the query sequence. */
        MocMode.setChecked( moc, false );
        try {
            HealpixImpl healpix = PixtoolsHealpix.getInstance();
            while ( qseq_.next() ) {
                if ( Thread.interrupted() ) {
                    throw new InterruptedIOException();
                }
                double ra = qseq_.getRa();
                double dec = qseq_.getDec();
                double radius = qseq_.getRadius();
                if ( ! Double.isNaN( ra ) &&
                     dec >= -90 && dec <= +90 &&
                     radius >= 0 ) {
                    int order = radius <= resolutionDeg_
                              ? maxOrder
                              : nsider.calcOrder( radius );
                    long[] pixels;
                    try {
                        pixels = healpix.queryDisc( order, ra, dec, radius );
                    }
                    catch ( Exception e ) {
                        throw (IOException)
                              new IOException( "HEALPix processing error" )
                             .initCause( e );
                    }
                    for ( int ip = 0; ip < pixels.length; ip++ ) {
                        try {
                            moc.add( order, pixels[ ip ] );
                        }
                        catch ( Exception e ) {
                            throw (IOException)
                                  new IOException( "HEALPix processing error" )
                                 .initCause( e );
                        }
                    }
                }
            }
            MocMode.setChecked( moc, true );
            return moc;
        }
        finally {
            try {
                qseq_.close();
            }
            catch ( IOException e ) {
            }
            qseq_ = null;
        }
    }

    /**
     * Utility class for HEALPix-related calculations required in the loop.
     */
    private static class Nsider {
        private final PixtoolsHealpix hpi = PixtoolsHealpix.getInstance();

        /**
         * Works out the appropriate HEALPix order for a given resolution.
         *
         * @param   sizeDeg   search size in degrees
         */
        synchronized int calcOrder( double sizeDeg ) {
            int nside = hpi.sizeToNside( sizeDeg );
            return PixtoolsHealpix.nsideToOrder( nside );
        }
    }

    public static void main( String[] args ) throws IOException {
        java.util.logging.Logger.getLogger( "uk.ac.starlink" )
            .setLevel( java.util.logging.Level.WARNING );
        double resDeg = 1.0;
        String tname = args[ 0 ];
        String raExpr = args[ 1 ];
        String decExpr = args[ 2 ];
        String srExpr = args[ 3 ];
        ConeQueryRowSequence qseq =
            new JELQuerySequenceFactory( raExpr, decExpr, srExpr )
           .createQuerySequence( new uk.ac.starlink.table.StarTableFactory()
                                .makeStarTable( tname ) );
        long start = System.currentTimeMillis();
        HealpixMoc moc = new ConeQueryFootprint( qseq, resDeg ).createMoc();
        long time = System.currentTimeMillis() - start;
        System.out.println( summariseMoc( moc ) );
        System.out.println( "time: " + time + " ms" );
    }
}
