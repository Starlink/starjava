package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import javax.swing.JLabel;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Table load dialogue which allows cone searches.  Cone search services
 * are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ConeSearchDialog extends RegistryServiceTableLoadDialog {

    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField srField_;

    /**
     * Constructor.
     */
    public ConeSearchDialog() {
        super( "Cone Search",
               "Obtain source catalogues using cone search web services" );
        getRegistryPanel().getQueryPanel().setPresetQueries( new String[] {
            "serviceType like 'CONE'",
        } );
        Action okAction = getOkAction();

        /* Add fields for entering query parameters. */
        raField_ = DoubleValueField.makeRADegreesField();
        raField_.getEntryField().addActionListener( okAction );
        raField_.getValueInfo()
                .setDescription( "Right Ascension of cone centre (J2000)" );

        decField_ = DoubleValueField.makeDecDegreesField();
        decField_.getEntryField().addActionListener( okAction );
        decField_.getValueInfo()
                 .setDescription( "Declination of cone centre (J2000)" );

        srField_ = DoubleValueField.makeRadiusDegreesField();
        srField_.getEntryField().addActionListener( okAction );
        srField_.getValueInfo()
                .setDescription( "Radius of cone search" );

        ValueFieldPanel qPanel = new ValueFieldPanel();
        qPanel.addField( raField_, new JLabel( "(J2000)" ) );
        qPanel.addField( decField_, new JLabel( "(J2000)" ) );
        qPanel.addField( srField_ );
        getControlBox().add( qPanel );
    }

    protected TableSupplier getTableSupplier() {
        SimpleResource[] resources = getRegistryPanel().getSelectedResources();
        if ( resources.length != 1 ) {
            throw new IllegalStateException( "No cone search service " +
                                             "selected" );
        }
        final ConeSearch coner;
        try {
            coner = new ConeSearch( resources[ 0 ] );
        }
        catch ( IllegalArgumentException e ) {
            throw new IllegalStateException( e.getMessage() );
        }
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
        metadata.addAll( Arrays.asList( coner.getMetadata() ) );
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

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        raField_.setEnabled( enabled );
        decField_.setEnabled( enabled );
        srField_.setEnabled( enabled );
    }
}
