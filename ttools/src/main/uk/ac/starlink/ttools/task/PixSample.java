package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.filter.CalculatorTable;
import uk.ac.starlink.ttools.filter.JELColumnTable;

public class PixSample {
// extends MapperTask {
//  public PixSample() {
//      super( "Samples from a HEALPix pixel data file", new ChoiceMode(),
//             true, new PixSampleMapper(), new PixSampleTablesInput() );
//  }

    /**
     * Creates a table containing pixel samples corresponding to the rows
     * of a base table in accordance with supplied parameters.
     *
     * @param   base  base table
     * @param   pixSample  characterises pixel sampling
     * @param   coordReader  turns input coordinate pairs into
     *                       lon/lat coords in the HEALPix coordinate system
     * @param   lonExpr  JEL expression for first input coordinate
     * @param   latExpr  JEL expression for second input coordinate
     * @param   radExpr  JEL expression for averaging radius
     * @return   table containing sampled columns
     */
    public static StarTable
            createSampleTable( StarTable base, final PixSampler pixSampler,
                               final PixSampler.StatMode statMode,
                               final CoordReader coordReader,
                               String lonExpr, String latExpr, String radExpr )
            throws IOException {

        /* Put together a table containing just the input lon, lat, radius. */
        StarTable calcInputTable =
            new JELColumnTable( base,
                                new String[] { lonExpr, latExpr, radExpr },
                                null );

        /* Feed it to a calculator table that turns those inputs into the
         * required pixel samples. */
        return new CalculatorTable( calcInputTable,
                                    pixSampler.getValueInfos( statMode ) ) {
            protected Object[] calculate( Object[] inRow ) throws IOException {
                double[] coords =
                    coordReader.getCoords( getDouble( inRow[ 0 ] ),
                                           getDouble( inRow[ 1 ] ) );
                double lon = coords[ 0 ];
                double lat = coords[ 1 ];
                double radius = getDouble( inRow[ 2 ] );
                return pixSampler.sampleValues( lon, lat, radius, statMode );
            }
        };
    }

    /**
     * Returns a coordinate reader which converts between a given input
     * and output coordinate system.
     * If no conversion is required, use <code>null</code> for in/out systems.
     *
     * @param   inSys  input sky coordinate system
     * @param  outSsy  output sky coordinate system
     * @return  coordinate reader that converts
     */
    public static CoordReader createCoordReader( final SkySystem inSys,
                                                 final SkySystem outSys ) {
        if ( inSys == null && outSys == null ) {
            return new CoordReader() {
                public double[] getCoords( double lonDeg, double latDeg ) {
                    return new double[] { lonDeg, latDeg };
                }
            };
        }
        else if ( inSys != null && outSys != null ) {
           final double epoch = 2000.0;
            return new CoordReader() {
                public double[] getCoords( double lonDegIn, double latDegIn ) {
                    double lonRadIn= lonDegIn / 180. * Math.PI;
                    double latRadIn = latDegIn / 180. * Math.PI;
                    double[] fk5Rad = inSys.toFK5( lonRadIn, latRadIn, epoch );
                    double[] radOut = outSys.fromFK5( fk5Rad[ 0 ], fk5Rad[ 1 ],
                                                      epoch );
                    double lonRadOut = radOut[ 0 ];
                    double latRadOut = radOut[ 1 ];
                    double lonDegOut = lonRadOut * 180. / Math.PI;
                    double latDegOut = latRadOut * 180. / Math.PI;
                    return new double[] { lonDegOut, latDegOut };
                }
            };
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Interface to turn input coordinate values into coordinate values
     * suitable for pixel sampling.
     *
     * @param   lonDeg  first input coordinate
     * @param   latDeg  second input coordinate
     * @return   (lon,lat) array of coordinates giving sampling position
     */
    public interface CoordReader {
        abstract double[] getCoords( double lonDeg, double latDeg );
    }
}
