package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table load dialogue for retrieving the result of a SIAP query.
 * SIAP services are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Dec 2005
 * @see      <http://www.ivoa.net/Documents/latest/SIA.html>
 */
public class SiapTableLoadDialog extends RegistryServiceTableLoadDialog {

    private final SkyPositionEntry skyEntry_;
    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField sizeField_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Angular Size", Double.class,
                              "Angular size of the search region"
                            + " in RA and Dec" );

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
        raField_ = skyEntry_.getRaDegreesField();
        decField_ = skyEntry_.getDecDegreesField();
        sizeField_ = DoubleValueField.makeSizeDegreesField( SIZE_INFO );
        skyEntry_.addField( sizeField_ );
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
        double ra = raField_.getValue();
        double dec = decField_.getValue();
        double size = sizeField_.getValue();
        final DalQuery query =
            new DalQuery( resource, capability, ra, dec, size );
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            sizeField_.getDescribedValue(),
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
