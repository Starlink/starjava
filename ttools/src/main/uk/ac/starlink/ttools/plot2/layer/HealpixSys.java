package uk.ac.starlink.ttools.plot2.layer;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.ttools.plot2.geom.SkySys;

/**
 * Maps between plotting ({@link uk.ac.starlink.ttools.plot2.geom.SkySys})
 * and Healpix ({@link uk.ac.starlink.table.HealpixTableInfo.HpxCoordSys})
 * coordinate system identifiers.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2018
 */
public class HealpixSys {

    private static final Map<SkySys,HealpixTableInfo.HpxCoordSys> map1_;
    private static final Map<HealpixTableInfo.HpxCoordSys,SkySys> map2_;

    static {
        map1_ = new HashMap<SkySys,HealpixTableInfo.HpxCoordSys>();
        map2_ = new HashMap<HealpixTableInfo.HpxCoordSys,SkySys>();
        map1_.put( SkySys.EQUATORIAL, HealpixTableInfo.HpxCoordSys.CELESTIAL );
        map1_.put( SkySys.GALACTIC, HealpixTableInfo.HpxCoordSys.GALACTIC );
        map1_.put( SkySys.ECLIPTIC2000, HealpixTableInfo.HpxCoordSys.ECLIPTIC );
        for ( Map.Entry<SkySys,HealpixTableInfo.HpxCoordSys> entry :
              map1_.entrySet() ) {
            map2_.put( entry.getValue(), entry.getKey() );
        }
    }

    /**
     * Returns the plotting sky coordinate system corresponding to
     * a Healpix sky coordinate system.  Best efforts.
     *
     * @param   csys  healpix sky coordinate system identifier
     * @return  corresponding plotting sky system,
     *          or null if no correspondence is known
     */
    public static SkySys toGeom( HealpixTableInfo.HpxCoordSys csys ) {
        return map2_.get( csys );
    }


    /**
     * Returns the Healpix sky coordinate system corresponding to
     * a plotting sky coordinate system.  Best efforts.
     * 
     * @param  skySys  plotting sky coordinate system identifier
     * @return  corresponding healpix system,
     *          or null if no correspondence is known
     */
    public static HealpixTableInfo.HpxCoordSys fromGeom( SkySys skySys ) {
        return map1_.get( skySys );
    }
}
