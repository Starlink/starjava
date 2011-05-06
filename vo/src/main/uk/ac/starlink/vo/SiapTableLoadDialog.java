package uk.ac.starlink.vo;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Table load dialogue for retrieving the result of a SIAP query.
 * SIAP services are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Dec 2005
 * @see      <http://www.ivoa.net/Documents/latest/SIA.html>
 */
public class SiapTableLoadDialog extends SkyDalTableLoadDialog {

    private DoubleValueField raField_;
    private DoubleValueField decField_;
    private DoubleValueField sizeField_;
    private JComboBox formatSelector_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Angular Size", Double.class,
                              "Angular size of the search region"
                            + " in RA and Dec" );

    /**
     * Constructor.
     */
    public SiapTableLoadDialog() {
        super( "Simple Image Access (SIA) Query", "SIA",
               "Get results of a Simple Image Access Protocol query",
               Capability.SIA, true, true );
        setIconUrl( SiapTableLoadDialog.class.getResource( "sia.gif" ) );
    }

    protected Component createQueryComponent() {
        Component queryPanel = super.createQueryComponent();
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        sizeField_ = DoubleValueField.makeSizeDegreesField( SIZE_INFO );
        sizeField_.getEntryField().setText( "0" );
        skyEntry.addField( sizeField_ );

        /* Add a selector for image format. */
        JComponent formatLine = Box.createHorizontalBox();
        formatSelector_ = new JComboBox( getFormatOptions() );
        formatSelector_.setEditable( true );
        formatSelector_.setSelectedIndex( 0 );
        formatLine.add( new JLabel( "Image Format: " ) );
        formatLine.add( new ShrinkWrapper( formatSelector_ ) );
        formatLine.add( Box.createHorizontalGlue() );
        getControlBox().add( Box.createVerticalStrut( 5 ) );
        getControlBox().add( formatLine );
        return queryPanel;
    }

    public TableLoader createTableLoader() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        double ra = raField_.getValue();
        double dec = decField_.getValue();
        double size = sizeField_.getValue();
        final DalQuery query = new DalQuery( serviceUrl, "SIA", ra, dec, size );
        Object format = formatSelector_.getSelectedItem();
        if ( format != null && format.toString().trim().length() > 0 ) {
            query.addArgument( "FORMAT", format.toString() );
        }
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            sizeField_.getDescribedValue(),
        } ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final String summary = getQuerySummary( serviceUrl, size );
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory factory )
                    throws IOException {
                StarTable st = query.execute( factory );
                st.getParameters().addAll( metadata );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return summary;
            }
        };
    }

    /**
     * Returns the list of standard options provided by the Format selector.
     * These are taken from the SIA standard; they are not exhaustive, but
     * represent some of the more useful options.  The user is able to
     * enter custom items as an alternative.
     * The first element in the returned list is a reasonable default.
     *
     * @return  format option strings
     */
    public static String[] getFormatOptions() {
        return new String[] {
            "image/fits",
            "GRAPHIC",
            "ALL",
            "",
        };
    }
}
