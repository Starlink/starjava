package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.StorageType;

/**
 * GUI component for obtaining data position coordinates.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public abstract class PositionCoordPanel extends CoordPanel {

    /**
     * Constructor.
     *
     * @param  coords  coordinate definitions for which values are required
     * @param  autoPopulate  if true, some attempt will be made to
     *                       fill in the fields with non-blank values
     *                       when a table is selected
     */
    protected PositionCoordPanel( Coord[] coords, boolean autoPopulate ) {
        super( coords, autoPopulate );
    }

    /**
     * Returns the position geometry that defines the mapping of input
     * to data coordinates.
     *
     * @return  data geom
     */
    public abstract DataGeom getDataGeom();

    /**
     * Returns a list of coordinates which is like multiple copies of a
     * supplied group.  The returned coords have metadata which
     * distinguish them from each other, currently an integer appended
     * to their name.  The returned coords are not totally respectable,
     * but their metadata is OK.
     *
     * @param   coords  basic coordinates
     * @param  ncopy   number of copies to make
     * @return   array of ncopy copies of coords
     */
    public static Coord[] multiplyCoords( Coord[] coords, int ncopy ) {
        List<Coord> coordList = new ArrayList<Coord>();
        for ( int ig = 0; ig < ncopy; ig++ ) {
            for ( int ic = 0; ic < coords.length; ic++ ) {
                Coord coord = coords[ ic ];
                coordList.add( ncopy == 1 ? coord : relabel( coord, ig ) );
            }
        }
        return coordList.toArray( new Coord[ 0 ] );
    }

    /**
     * Returns a Coord like a given one but with modified metadata.
     *
     * <p>The returned Coord is not of the right subclass, hence does not
     * have the appropriate type-specific read*Coord method.
     * However that doesn't matter, because we're just using the results
     * from this call to represent coordinate metadata, not for reading data.
     *
     * @param  baseCoord  coord on which to base the copy
     * @param  iPoint  point index, used to label the coordinate
     * @return   new coord like the input one
     */
    private static Coord relabel( final Coord baseCoord, int iPoint ) {
        String iptxt = Integer.toString( iPoint + 1 );
        final ValueInfo[] infos = baseCoord.getUserInfos().clone();
        int nuc = infos.length;
        for ( int iuc = 0; iuc < nuc; iuc++ ) {
            DefaultValueInfo info = new DefaultValueInfo( infos[ iuc ] );
            info.setName( info.getName() + iptxt );
            info.setDescription( info.getDescription()
                               + " for point " + iptxt );
            infos[ iuc ] = info;
        }
        return new Coord() {
            public ValueInfo[] getUserInfos() {
                return infos;
            }
            public boolean isRequired() {
                return true;
            }
            public StorageType getStorageType() {
                return baseCoord.getStorageType();
            }
            public List<Class<? extends DomainMapper>> getUserDomains() {
                return baseCoord.getUserDomains();
            }
            public Object userToStorage( Object[] userCoords,
                                         DomainMapper[] userMappers ) {
                return baseCoord.userToStorage( userCoords, userMappers );
            }
        };
    }
}
