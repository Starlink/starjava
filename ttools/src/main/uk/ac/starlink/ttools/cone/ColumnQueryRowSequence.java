package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;

/**
 * ConeQueryRowSequence implementation which extracts values based on
 * supplied column indices.
 *
 * @author   Mark Taylor
 * @since    16 Oct 2007
 */
public class ColumnQueryRowSequence extends WrapperRowSequence
                                    implements ConeQueryRowSequence {

    private final int raCol_;
    private final int decCol_;
    private final int srCol_;

    /**
     * Constructor.
     *
     * @param  table  input table
     * @param  raCol  index of column giving right ascension in degrees
     * @param  decCol index of column giving declination in degrees
     * @param  srCol  index of column giving search radius in degrees
     */
    public ColumnQueryRowSequence( StarTable table, int raCol, int decCol,
                                   int srCol ) throws IOException {
        super( table.getRowSequence() );
        raCol_ = raCol;
        decCol_ = decCol;
        srCol_ = srCol;
    }

    public double getRa() throws IOException {
        return getDoubleCell( raCol_ );
    }

    public double getDec() throws IOException {
        return getDoubleCell( decCol_ );
    }

    public double getRadius() throws IOException {
        return getDoubleCell( srCol_ );
    }

    /**
     * Returns a double value from the given column at the current row.
     *
     * @param  colIndex  index of column to source data from
     * @return  numeric value at current row
     */
    private double getDoubleCell( int colIndex ) throws IOException {
        Object value = getCell( colIndex );
        return value instanceof Number
             ? ((Number) value).doubleValue()
             : Double.NaN;
    }

    /**
     * Utility method which constructs a ConeQueryRowSequence object using
     * column indices for RA and Dec but a constant value for the search
     * radius.
     *
     * @param  table  input table
     * @param  raCol  index of column giving right ascension in degrees
     * @param  decCol index of column giving declination in degrees
     * @param  sr     fixed search radius in degrees
     */
    public static ConeQueryRowSequence
           createFixedRadiusSequence( StarTable table, int raCol,
                                      int decCol, final double sr ) 
           throws IOException {
        return new ColumnQueryRowSequence( table, raCol, decCol, -1 ) {
            public double getRadius() {
                return sr;
            }
        };
    }
}
