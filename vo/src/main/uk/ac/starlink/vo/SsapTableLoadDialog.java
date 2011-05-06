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
 * Table load dialogue for retrieving the result of a SSAP query.
 * SSAP services are returned from a registry.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2009
 * @see      <http://www.ivoa.net/Documents/latest/SSA.html>
 */
public class SsapTableLoadDialog extends SkyDalTableLoadDialog {

    private DoubleValueField raField_;
    private DoubleValueField decField_;
    private DoubleValueField sizeField_;
    private JComboBox formatSelector_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Diameter", Double.class,
                              "Angular diameter of the search region" );

    /**
     * Constructor.
     */
    public SsapTableLoadDialog() {
        super( "Simple Spectral Access (SSA) Query", "SSA",
               "Get results of a Simple Spectrum Access Protocol query",
               Capability.SSA, true, true );
        setIconUrl( SsapTableLoadDialog.class.getResource( "ssa.gif" ) );
    }

    protected Component createQueryComponent() {
        Component queryPanel = super.createQueryComponent();
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        sizeField_ = DoubleValueField.makeSizeDegreesField( SIZE_INFO );
        skyEntry.addField( sizeField_ );

        /* Add a selector for spectrum format. */
        JComponent formatLine = Box.createHorizontalBox();
        formatSelector_ = new JComboBox( getFormatOptions() );
        formatSelector_.setEditable( true );
        formatSelector_.setSelectedIndex( 0 );
        formatLine.add( new JLabel( "Spectrum Format: " ) );
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
        String sizeString = sizeField_.getEntryField().getText();
        double size = sizeString == null || sizeString.trim().length() == 0
                    ? Double.NaN
                    : sizeField_.getValue();
        final DalQuery query = new DalQuery( serviceUrl, "SSA", ra, dec, size );
        query.addArgument( "REQUEST", "queryData" );
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
     * These are taken from the SSA standard; they are not exhaustive, but
     * represent some of the more useful options.  The user is able to
     * enter custom items as an alternative.
     * The first element in the returned list is a reasonable default.
     *
     * @return   format option strings
     */
    public static String[] getFormatOptions() {
        return new String[] {
            "",
            "all",
            "compliant",
            "native",
            "graphic",
            "votable",
            "fits",
            "xml",
        };
    }
}
