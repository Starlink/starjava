package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table load dialogue which allows cone searches.  Cone search services
 * are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ConeSearchDialog extends DalTableLoadDialog {

    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField srField_;
    private static final ValueInfo SR_INFO =
        new DefaultValueInfo( "Radius", Double.class, "Search Radius" );

    /**
     * Constructor.
     */
    public ConeSearchDialog() {
        super( "Cone Search",
               "Obtain source catalogues using cone search web services",
               new KeywordServiceQueryFactory( Capability.CONE ), true, false );
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        srField_ = DoubleValueField.makeSizeDegreesField( SR_INFO );
        skyEntry.addField( srField_ );
    }

    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        return selectConeSearches( super.getCapabilities( resource ) );
    }

    protected TableSupplier getTableSupplier() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        final ConeSearch coner = new ConeSearch( serviceUrl );
        final double ra = raField_.getValue();
        final double dec = decField_.getValue();
        final double sr = srField_.getValue();
        final int verb = 0;
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            srField_.getDescribedValue(),
        } ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final String summary = getQuerySummary( serviceUrl, sr );
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory factory,
                                       String format ) throws IOException {
                StarTable st = coner.performSearch( ra, dec, sr, verb,
                                                    factory );
                st.getParameters().addAll( metadata );
                return st;
            }
            public String getTableID() {
                return summary;
            }
        };
    }

    /**
     * Selects only the capability objects from a list of them which 
     * represent cone search services.
     *
     * @param   caps  input list
     * @return  all the capabilities from <code>caps</code> which
     *          are cone search services
     */
    public static RegCapabilityInterface[]
                  selectConeSearches( RegCapabilityInterface[] caps ) {
        List cscapList = new ArrayList();
        for ( int i = 0; i < caps.length; i++ ) {
            if ( Capability.CONE.isInstance( caps[ i ] ) ) {
                cscapList.add( caps[ i ] );
            }
        }
        return (RegCapabilityInterface[])
               cscapList.toArray( new RegCapabilityInterface[ 0 ] );
    }
}
