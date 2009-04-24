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
public class ConeSearchDialog extends RegistryServiceTableLoadDialog {

    private final SkyPositionEntry skyEntry_;
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
               new KeywordServiceQueryFactory( RegCapabilityInterface
                                              .CONE_STDID ),
               true );
        skyEntry_ = new SkyPositionEntry( "J2000" );
        raField_ = skyEntry_.getRaDegreesField();
        decField_ = skyEntry_.getDecDegreesField();
        srField_ = DoubleValueField.makeSizeDegreesField( SR_INFO );
        skyEntry_.addField( srField_ );
        skyEntry_.addActionListener( getOkAction() );
        getControlBox().add( skyEntry_ );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        skyEntry_.setEnabled( enabled );
    }

    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        return selectConeSearches( super.getCapabilities( resource ) );
    }

    protected TableSupplier getTableSupplier() {
        RegResource[] resources =
            getRegistryPanel().getSelectedResources();
        RegCapabilityInterface[] capabilities =
            getRegistryPanel().getSelectedCapabilities();
        if ( resources.length != 1 || capabilities.length != 1 ) {
            throw new IllegalStateException( "No cone search service " +
                                             "selected" );
        }
        RegResource resource = resources[ 0 ]; 
        RegCapabilityInterface capability = capabilities[ 0 ];
        final ConeSearch coner = new ConeSearch( resource, capability );
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
        metadata.addAll( Arrays.asList( ConeSearch
                                       .getMetadata( resource, capability ) ) );
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory factory,
                                       String format ) throws IOException {
                StarTable st = coner.performSearch( ra, dec, sr, verb,
                                                    factory );
                st.getParameters().addAll( metadata );
                return st;
            }
            public String getTableID() {
                return coner.toString();
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
            if ( RegCapabilityInterface.CONE_STDID
                .equals( caps[ i ].getStandardId() ) ) {
                cscapList.add( caps[ i ] );
            }
        }
        return (RegCapabilityInterface[])
               cscapList.toArray( new RegCapabilityInterface[ 0 ] );
    }
}
