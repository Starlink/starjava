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
 * Table load dialogue for retrieving the result of a SSAP query.
 * SSAP services are returned from a registry.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2009
 * @see      <http://www.ivoa.net/Documents/latest/SSA.html>
 */
public class SsapTableLoadDialog extends DalTableLoadDialog {

    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField sizeField_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Diameter", Double.class,
                              "Angular diameter of the search region" );

    /**
     * Constructor.
     */
    public SsapTableLoadDialog() {
        super( "SSAP Query",
               "Get results of a Simple Spectrum Access Protocol query",
               new KeywordServiceQueryFactory( Capability.SSA ), true );
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        sizeField_ = DoubleValueField.makeSizeDegreesField( SIZE_INFO );
        skyEntry.addField( sizeField_ );
    }

    public TableSupplier getTableSupplier() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        double ra = raField_.getValue();
        double dec = decField_.getValue();
        String sizeString = sizeField_.getEntryField().getText();
        double size = sizeString == null || sizeString.trim().length() == 0
                    ? Double.NaN
                    : sizeField_.getValue();
        final DalQuery query = new DalQuery( serviceUrl, ra, dec, size );
        query.addArgument( "REQUEST", "queryData" );
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            sizeField_.getDescribedValue(),
        } ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final String summary = getQuerySummary( serviceUrl, size );
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory factory,
                                       String format ) throws IOException {
                StarTable st = query.execute( factory );
                st.getParameters().addAll( metadata );
                return st;
            }
            public String getTableID() {
                return summary;
            }
        };
    }

    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        RegCapabilityInterface[] caps = super.getCapabilities( resource );
        List ssapcapList = new ArrayList();
        for ( int i = 0; i < caps.length; i++ ) {
            if ( Capability.SSA.isInstance( caps[ i ] ) ) {
                ssapcapList.add( caps[ i ] );
            }
        }
        return (RegCapabilityInterface[])
               ssapcapList.toArray( new RegCapabilityInterface[ 0 ] );
    }
}
