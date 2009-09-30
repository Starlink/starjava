package uk.ac.starlink.topcat.join;

import java.awt.Component;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.ServiceConeSearcher;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeSearch;

/**
 * DalMultiWindow subclass for Cone Search services.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2009
 */
public class ConeMultiWindow extends DalMultiWindow {

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public ConeMultiWindow( Component parent ) {
        super( parent, new ConeMultiService() );
        addHelp( "ConeMultiWindow" );
    }

    /**
     * DalMultiService implementation for Cone Search service type.
     */
    private static class ConeMultiService implements DalMultiService {

        public String getName() {
            return "Cone Search";
        }

        public String getLabel() {
            return "cone";
        }

        public Capability getCapability() {
            return Capability.CONE;
        }

        public ValueInfo getSizeInfo() {
            DefaultValueInfo info =
                new DefaultValueInfo( "Search Radius", Number.class,
                                      "Maximum distance from target position" );
            info.setUnitString( "radians" );
            info.setUCD( "pos.angDistance" );
            return info;
        }

        public void setSizeDefault( ColumnSelector sizeSelector ) {
            sizeSelector.setStringValue( "1.0" );
        }

        public ConeSearcher createSearcher( String url,
                                            StarTableFactory tfact ) {
            return new ServiceConeSearcher( new ConeSearch( url ), 0, false,
                                            tfact );
        }
    }
}
