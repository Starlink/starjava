package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;

/**
 * Table load dialogue which allows cone searches.  Cone search services
 * are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ConeSearchDialog extends BasicTableLoadDialog {

    private final RegistryPanel regPanel_;
    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField srField_;
    private Boolean available_;

    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public ConeSearchDialog() {
        super( "Cone Search",
               "Obtain source catalogues using cone search web services" );
        final Action okAction = getOkAction();
        setLayout( new BorderLayout() );

        /* Construct and configure a panel which knows how to query the
         * registry and display the result. */
        regPanel_ = new RegistryPanel();
        regPanel_.getResourceSelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel_.getResourceSelectionModel()
                 .addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                okAction.setEnabled( regPanel_.getSelectedResources()
                                              .length == 1 );
            }
        } );
        regPanel_.getQueryPanel().setPresetQueries( new String[] {
            "serviceType like 'CONE'",
        } );
        add( regPanel_, BorderLayout.CENTER );
 
        /* Add controls for performing the actual cone search. */
        JComponent controlBox = Box.createVerticalBox();
        add( controlBox, BorderLayout.SOUTH );

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

        ControlPanel qPanel = new ControlPanel();
        qPanel.addField( raField_, new JLabel( "(J2000)" ) );
        qPanel.addField( decField_, new JLabel( "(J2000)" ) );
        qPanel.addField( srField_ );
        controlBox.add( qPanel );

        /* Cosmetics. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        regPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                "Available Cone Search Services" ) );
        controlBox.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder( lineBorder, gapBorder ),
                "Cone Search Parameters" ) );
        setBorder( gapBorder );
        setPreferredSize( new Dimension( 600, 450 ) );
    }

    public boolean isAvailable() {
        if ( available_ == null ) {
            try {
                Class c = this.getClass();
                c.forName( "net.ivoa.www.xml.VORegistry.v0_3.Registry" );
                c.forName( "org.us_vo.www.Registry" );
                available_ = Boolean.TRUE;
            }
            catch ( Throwable th ) {
                logger_.info( "WSDL classes unavailable" + " (" + th + ")" );
                available_ = Boolean.FALSE;
            }
        }
        return available_.booleanValue();
    }

    /**
     * Returns the RA value currently filled in.
     *
     * @return   J2000 right ascension in degrees
     */
    public double getRA() {
        return raField_.getValue();
    }

    /**
     * Returns the Dec value currently filled in.
     *
     * @return   J2000 declination in degrees
     */
    public double getDec() {
        return decField_.getValue();
    }

    /**
     * Returns the search radius value currently filled in.
     *
     * @return   search radius in degrees
     */
    public double getSR() {
        return srField_.getValue();
    }

    /**
     * Returns the verbosity flag currently filled in.
     *
     * @return  verbosity flag
     */
    public int getVerb() {
        return 0;
    }

    protected TableSupplier getTableSupplier() {
        SimpleResource[] resources = regPanel_.getSelectedResources();
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
        final double ra = getRA();
        final double dec = getDec();
        final double sr = getSR();
        final int verb = getVerb();
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

    protected JDialog createDialog( Component parent ) {

        /* Embellish the dialogue with a menu allowing selection of which
         * columns are visible in the displayed registry table. */
        JDialog dia = super.createDialog( parent );
        if ( dia.getJMenuBar() == null ) {
            dia.setJMenuBar( new JMenuBar() );
        }
        dia.getJMenuBar()
           .add( regPanel_.makeColumnVisibilityMenu( "Display" ) );
        return dia;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        regPanel_.setEnabled( enabled );
        raField_.setEnabled( enabled );
        decField_.setEnabled( enabled );
        srField_.setEnabled( enabled );
    }


    /**
     * Helper class to deal with GridBagLayout.
     */
    private static class ControlPanel extends JPanel {
        GridBagLayout layer = new GridBagLayout();
        Insets ins = new Insets( 2, 2, 2, 2 );
        int gx = 0;
        GridBagConstraints[] gbcs = new GridBagConstraints[] {
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
                fill = BOTH;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
                fill = HORIZONTAL;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0;
                weightx = 1.0;
            }},
        };

        ControlPanel() {
            setLayout( layer );
        }

        void addField( DoubleValueField vf, JComponent trailer ) {
            JComponent[] comps = new JComponent[] {
                vf.getLabel(),
                vf.getEntryField(),
                vf.getConverterSelector(),
                trailer,
                new JPanel(),
            };
            for ( int i = 0; i < comps.length; i++ ) {
                if ( comps[ i ] != null ) {
                    layer.setConstraints( comps[ i ], gbcs[ i ] );
                    add( comps[ i ] );
                    gbcs[ i ].gridy++;
                }
            }
        }

        void addField( DoubleValueField vf ) {
            addField( vf, null );
        }
    }

}
