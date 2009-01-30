package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Table load dialogue for retrieving the result of a SIAP query.
 * SIAP services are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Dec 2005
 */
public class SiapTableLoadDialog extends RegistryServiceTableLoadDialog {

    private final SkyPositionEntry skyEntry_;

    /**
     * Constructor.
     */
    public SiapTableLoadDialog() {
        super( "SIAP Query",
               "Get results of a Simple Image Access Protocol query",
               new KeywordServiceQueryFactory( RegCapabilityInterface
                                              .SIA_STDID ),
               false );
        skyEntry_ = new SkyPositionEntry( "J2000" );
        skyEntry_.addActionListener( getOkAction() );
        getControlBox().add( skyEntry_ );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        skyEntry_.setEnabled( enabled );
    }

    public TableSupplier getTableSupplier() {
        RegResource[] resources =
            getRegistryPanel().getSelectedResources();
        RegCapabilityInterface[] capabilities =
            getRegistryPanel().getSelectedCapabilities();
        if ( resources.length != 1 || capabilities.length < 1 ) {
            throw new IllegalStateException( "No SIAP service selected" );
        }
        RegResource resource = resources[ 0 ];
        RegCapabilityInterface capability = capabilities[ 0 ];
        double ra = skyEntry_.getRaDegreesField().getValue();
        double dec = skyEntry_.getDecDegreesField().getValue();
        double size = skyEntry_.getRadiusDegreesField().getValue();
        final SiapQuery query =
            new SiapQuery( resource, capability, ra, dec, size, size );
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            skyEntry_.getRaDegreesField().getDescribedValue(),
            skyEntry_.getDecDegreesField().getDescribedValue(),
            skyEntry_.getRadiusDegreesField().getDescribedValue(),
        } ) );
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory factory,
                                       String format ) throws IOException {
                StarTable st = query.execute( factory );
                st.getParameters().addAll( metadata );
                return st;
            }
            public String getTableID() {
                return query.toString();
            }
        };
    }

    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        RegCapabilityInterface[] caps = super.getCapabilities( resource );
        List siapcapList = new ArrayList();
        for ( int i = 0; i < caps.length; i++ ) {
            if ( RegCapabilityInterface.SIA_STDID
                .equals( caps[ i ].getStandardId() ) ) {
                siapcapList.add( caps[ i ] );
            }
        }
        return (RegCapabilityInterface[])
               siapcapList.toArray( new RegCapabilityInterface[ 0 ] );
    }

}
