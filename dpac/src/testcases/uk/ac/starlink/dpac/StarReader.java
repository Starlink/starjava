package uk.ac.starlink.dpac;

import gaia.cu9.tools.parallax.datamodel.StarVariables;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTable;

public class StarReader {

    private final int icSourceid_;
    private final int icRa_;
    private final int icDec_;
    private final int icL_;
    private final int icB_;
    private final int icParallax_;
    private final int icParallaxerror_;

    public StarReader( StarTable table ) {
        Map<String,Integer> colMap = new HashMap<String,Integer>();
        int ncol = table.getColumnCount();
        colMap.put( "l", Integer.valueOf( -1 ) );  // optional
        colMap.put( "b", Integer.valueOf( -1 ) );  // optional
        for ( int icol = 0; icol < ncol; icol++ ) {
            colMap.put( table.getColumnInfo( icol ).getName(),
                        Integer.valueOf( icol ) );
        }
        icSourceid_ = colMap.get( "source_id" ).intValue();
        icRa_ = colMap.get( "ra" ).intValue();
        icDec_ = colMap.get( "dec" ).intValue();
        icL_ = colMap.get( "l" ).intValue();
        icB_ = colMap.get( "b" ).intValue();
        icParallax_ = colMap.get( "parallax" ).intValue();
        icParallaxerror_ = colMap.get( "parallax_error" ).intValue();
    }

    public StarVariables getStarVariables( Object[] row ) {
        long sourceId = ((Long) row[ icSourceid_ ]).longValue();
        double ra = ((Double) row[ icRa_ ]).doubleValue();
        double dec = ((Double) row[ icDec_ ]).doubleValue();
        double l = icL_ >= 0 ? ((Double) row[ icL_ ]).doubleValue()
                             : Double.NaN;
        double b = icB_ >= 0 ? ((Double) row[ icB_ ]).doubleValue()
                             : Double.NaN;
        double parallax = ((Double) row[ icParallax_ ]).doubleValue();
        double parallaxError = ((Double) row[ icParallaxerror_ ]).doubleValue();
        // Note! comments in StarVariables source code (not doc comments)
        // say that alpha, delta are in Radians, lGal and bGal are in degrees.
        double alpha = Math.toRadians( ra );
        double delta = Math.toRadians( dec );
        return new StarVariables( sourceId, alpha, delta, l, b,
                                  parallax, parallaxError );
    }

    public int[] getStarColumnIndices() {
        return new int[] {
            icSourceid_,
            icRa_,
            icDec_,
            icParallax_,
            icParallaxerror_,
        };
    }
}
