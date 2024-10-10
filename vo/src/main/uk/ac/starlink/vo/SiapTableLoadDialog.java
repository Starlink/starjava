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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
 * Table load dialogue for retrieving the result of a SIAP query.
 * SIAP services are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Dec 2005
 * @see      <a href="http://www.ivoa.net/Documents/latest/SIA.html">SIA</a>
 */
public class SiapTableLoadDialog extends SkyDalTableLoadDialog {

    private final ContentCoding coding_;
    private DoubleValueField raField_;
    private DoubleValueField decField_;
    private DoubleValueField sizeField_;
    private JComboBox<Object> formatSelector_;
    private JComboBox<SiaVersion> versionSelector_;
    private static final ValueInfo SIZE_INFO =
        new DefaultValueInfo( "Angular Size", Double.class,
                              "Angular size of the search region"
                            + " in RA and Dec" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public SiapTableLoadDialog() {
        super( "Simple Image Access (SIA) Query", "SIA",
               "Get results of a Simple Image Access Protocol query",
               Capability.SIA, true, true );
        coding_ = ContentCoding.GZIP;
        setIcon( ResourceIcon.TLD_SIA );
    }

    protected Component createQueryComponent() {
        Component queryPanel = super.createQueryComponent();
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        sizeField_ = DoubleValueField.makeSizeDegreesField( SIZE_INFO );
        sizeField_.getEntryField().setText( "0" );
        skyEntry.addField( sizeField_ );

        /* Version selector. */
        versionSelector_ = new JComboBox<>( SiaVersion.values() );
        final RegistryPanel regPanel = getRegistryPanel();
        ListSelectionListener serviceListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegCapabilityInterface[] intfs =
                    regPanel.getSelectedCapabilities();
                if ( intfs.length == 1 ) {
                    SiaVersion version = SiaVersion.forInterface( intfs[ 0 ] );
                    if ( version != null ) {
                        versionSelector_.setSelectedItem( version );
                    }
                }
            }
        };
        regPanel.getResourceSelectionModel()
                .addListSelectionListener( serviceListener );
        regPanel.getCapabilitySelectionModel()
                .addListSelectionListener( serviceListener );
        JComponent versionLine = Box.createHorizontalBox();
        versionLine.add( Box.createHorizontalStrut( 10 ) );
        versionLine.add( new JLabel( "SIA Version: " ) );
        versionLine.add( new ShrinkWrapper( versionSelector_ ) );
        getServiceUrlBox().add( versionLine );

        /* Add a selector for image format. */
        JComponent formatLine = Box.createHorizontalBox();
        formatSelector_ =
            new JComboBox<Object>( SiaFormatOption.getStandardOptions() );
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
        final String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        final double ra = raField_.getValue();
        final double dec = decField_.getValue();
        final double size = sizeField_.getValue();
        final SiaVersion siaVersion = getSiaVersion();
        final SiaFormatOption format = getFormat();

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
                StarTable st =
                    siaVersion.executeQuery( serviceUrl, ra, dec, size, format,
                                             factory, coding_ );
                st.getParameters().addAll( metadata );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return summary;
            }
        };
    }

    /**
     * Returns the SIA version selected.
     *
     * @return  SIA version
     */
    public SiaVersion getSiaVersion() {
        return versionSelector_
              .getItemAt( versionSelector_.getSelectedIndex() );
    }

    /**
     * Returns the format option selected.
     *
     * @return SIA format
     */
    public SiaFormatOption getFormat() {
        return SiaFormatOption.fromObject( formatSelector_.getSelectedItem() );
    }

    /**
     * Returns the size in degrees selected.
     *
     * @return  requested size in degrees
     */
    public double getSizeDeg() {
        try {
            return sizeField_.getValue();
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }

    /**
     * Returns the format selector component.
     *
     * @return  format selector
     */
    public JComboBox<Object> getFormatSelector() {
        return formatSelector_;
    }

    /**
     * Returns the SIA version selector component.
     *
     * @return   version selector
     */
    public JComboBox<SiaVersion> getVersionSelector() {
        return versionSelector_;
    }
}
