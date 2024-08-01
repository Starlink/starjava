package uk.ac.starlink.ttools.scheme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.ValueInfo;

/**
 * StarTable implementation based on an Attractor.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2020
 */
public class AttractorStarTable extends AbstractStarTable {

    private final AttractorFamily.Attractor att_;
    private final long nrow_;
    private final int ndim_;
    private final ColumnInfo[] colInfos_;
    public static final ValueInfo ATTRACTOR_INFO =
        new DefaultValueInfo( "Attractor", String.class );
    public static final ValueInfo FILL_INFO =
        new DefaultValueInfo( "FillFactor", Double.class,
                              "Proportion of space filled" );

    /**
     * Constructor.
     *
     * @param   att  attractor
     * @param   nrow   row count
     */
    @SuppressWarnings("this-escape")
    public AttractorStarTable( AttractorFamily.Attractor att, long nrow ) {
        att_ = att;
        nrow_ = nrow;
        ndim_ = att.getFamily().getDimCount();
        colInfos_ = new ColumnInfo[ ndim_ ];
        for ( int id = 0; id < ndim_; id++ ) {
            char nchr = ndim_ <= 3 ? "xyz".charAt( id )
                                   : (char) ( 'a' + id );
            colInfos_[ id ] = new ColumnInfo( Character.toString( nchr ),
                                              Double.class,
                                              "Value of dimension #" + id );
            colInfos_[ id ].setUCD( "pos.cartesian." + nchr );
        }
        setParameter( new DescribedValue( ATTRACTOR_INFO, att.toString() ) );
    }

    public int getColumnCount() {
        return ndim_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public RowSequence getRowSequence() {
        Stream<double[]> stream = att_.pointStream().skip( 100 );
        if ( nrow_ >= 0 ) {
            stream = stream.limit( nrow_ );
        }
        Iterator<double[]> it = stream.iterator();
        return new RowSequence() {
            double[] point_;
            public boolean next() {
                if ( it.hasNext() ) {
                    point_ = it.next();
                    return true;
                }
                else {
                    return false;
                }
            }
            public Double getCell( int ic ) {
                if ( point_ != null ) {
                    return Double.valueOf( point_[ ic ] );
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object[] getRow() {
                if ( point_ != null ) {
                    Object[] row = new Object[ ndim_ ];
                    for ( int id = 0; id < ndim_; id++ ) {
                        row[ id ] = Double.valueOf( point_[ id ] );
                    }
                    return row;
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public void close() {
            }
        };
    }

    /**
     * Utility/example method that can generate example tables corresponding
     * to interesting (strange) attractors.
     * There is informative output to stdout,
     * and the tables are written to the current directory.
     *
     * @param  family   attractor family
     * @param  nrow    numer of rows in each output table
     * @param  nfile   number of output table files to write
     */
    public static void writeFiles( AttractorFamily family,
                                   int nrow, int nfile )
            throws IOException {
        Random rnd = new Random( 4429772 );
        StarTableOutput sto = new StarTableOutput();
        for ( int ig = 0; ig < nfile; ) {
            AttractorFamily.Attractor attractor = family.createAttractor( rnd );
            double frac = AttractorFamily.getSpaceFraction( attractor, 100 );
            if ( frac > family.getFillThreshold() ) {
                String loc = family.getName() + ++ig + ".fits";
                System.out.println( "\n" + loc
                                  + "\t" + ig + "/" + nfile
                                  + "\t" + frac
                                  + "\t" + attractor );
                StarTable table = new AttractorStarTable( attractor, nrow );
                table.setParameter( new DescribedValue( FILL_INFO,
                                                        Double
                                                       .valueOf( frac ) ) );
                sto.writeStarTable( table, loc, "fits" );
            }
            else {
                System.out.print( "." );
            }
        }
    }

    /**
     * Writes some example attractor tables to stdout.
     * Use -h for usage.
     */
    public static void main( String[] args ) throws IOException {
        String usage = AttractorStarTable.class.getName() + " [nrow] [nfile]";
        int nrow = 10_000_000;
        int nfile = 8;
        try {
            nrow = (int) Double.parseDouble( args[ 0 ] );
            if ( args.length > 1 ) {
                nfile = Integer.parseInt( args[ 1 ] );
            }
        }
        catch ( RuntimeException e ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        Random rnd = new Random( 456623 );
        for ( AttractorFamily family :
              new AttractorFamily[] { AttractorFamily.CLIFFORD,
                                      AttractorFamily.RAMPE } ) {
            writeFiles( family, nrow, nfile );
        }
    }
}
