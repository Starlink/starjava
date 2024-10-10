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
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Table load dialogue for retrieving the result of a SSAP query.
 * SSAP services are returned from a registry.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2009
 * @see      <a href="http://www.ivoa.net/Documents/latest/SSA.html">SSA</a>
 */
public class SsapTableLoadDialog extends SkyDalTableLoadDialog {

    private final ContentCoding coding_;
    private DoubleValueField raField_;
    private DoubleValueField decField_;
    private DoubleValueField sizeField_;
    private JComboBox<String> formatSelector_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Diameter", Double.class,
                              "Angular diameter of the search region" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public SsapTableLoadDialog() {
        super( "Simple Spectral Access (SSA) Query", "SSA",
               "Get results of a Simple Spectrum Access Protocol query",
               Capability.SSA, true, true );
        coding_ = ContentCoding.GZIP;
        setIcon( ResourceIcon.TLD_SSA );
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
        formatSelector_ = new JComboBox<String>( getFormatOptions() );
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
        String raString = raField_.getEntryField().getText();
        String decString = decField_.getEntryField().getText();
        String sizeString = sizeField_.getEntryField().getText();
        double ra;
        double dec;
        if ( ( raString == null || raString.trim().length() == 0 ) &&
             ( decString == null || decString.trim().length() == 0 ) ) {
            ra = Double.NaN;
            dec = Double.NaN;
        }
        else {
            ra = raField_.getValue();
            dec = decField_.getValue();
        }
        double size = sizeString == null || sizeString.trim().length() == 0
                    ? Double.NaN
                    : sizeField_.getValue();
        final DalQuery query =
            new DalQuery( serviceUrl, "SSA", ra, dec, size, coding_ );
        query.addArgument( "REQUEST", "queryData" );
        String format = getFormat();
        if ( format != null && format.toString().trim().length() > 0 ) {
            query.addArgument( "FORMAT", format.toString() );
        }
        final List<DescribedValue> metadata = new ArrayList<DescribedValue>();
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
     * Returns the requested search diameter.
     *
     * @return  search diameter
     */
    public double getSearchDiameter() {
        try {
            return sizeField_.getValue();
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the selected format variant.
     *
     * @return   format string
     */
    public String getFormat() {
        Object format = formatSelector_.getSelectedItem();
        return format == null ? null : format.toString();
    }

    /**
     * Returns the format selector component.
     *
     * @return  format selector
     */
    public JComboBox<String> getFormatSelector() {
        return formatSelector_;
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
