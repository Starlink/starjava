package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.us_vo.www.SimpleResource;
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

    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField sizeField_;

    /**
     * Constructor.
     */
    public SiapTableLoadDialog() {
        super( "SIAP Query",
               "Get results of a Simple Image Access Protocol query" );
        getRegistryPanel().getQueryPanel().setPresetQueries( new String[] {
            "serviceType like 'SIAP'",
        } );
        Action okAction = getOkAction();

        /* Add fields for entering SIAP query parameters. */
        raField_ = DoubleValueField.makeRADegreesField();
        raField_.getEntryField().addActionListener( okAction );
        raField_.setDescription( "Right Ascension of " +
                                 "region of interest centre" );

        decField_ = DoubleValueField.makeDecDegreesField();
        decField_.getEntryField().addActionListener( okAction );
        decField_.setDescription( "Declination of region of interest centre" );

        sizeField_ = DoubleValueField.makeRadiusDegreesField();
        sizeField_.getEntryField().addActionListener( okAction );
        sizeField_.getValueInfo()
                  .setName( "Size" );
        sizeField_.setDescription( "Size along RA,Dec axes of desired image" );

        /* Install these fields in the control panel. */
        ValueFieldPanel qPanel = new ValueFieldPanel();
        qPanel.addField( raField_ );
        qPanel.addField( decField_ );
        qPanel.addField( sizeField_ );
        getControlBox().add( qPanel );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        raField_.setEnabled( enabled );
        decField_.setEnabled( enabled );
        sizeField_.setEnabled( enabled );
    }

    public TableSupplier getTableSupplier() {
        SimpleResource[] resources = getRegistryPanel().getSelectedResources();
        if ( resources.length != 1 ) {
            throw new IllegalStateException( "No SIAP service selected" );
        }
        double ra = raField_.getValue();
        double dec = decField_.getValue();
        double size = sizeField_.getValue();
        final SiapQuery query =
            new SiapQuery( resources[ 0 ], ra, dec, size, size );
        final List metadata = Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            sizeField_.getDescribedValue(),
        } );
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

}
