package uk.ac.starlink.vo;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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

    private final Action resolveAction_;
    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField srField_;

    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public ConeSearchDialog() {
        super( "Cone Search",
               "Obtain source catalogues using cone search web services",
               "serviceType like 'CONE'" );

        /* Add name resolution field. */
        Box resolveBox = Box.createHorizontalBox();
        JTextField resolveField = new JTextField( 20 );
        resolveAction_ = new ResolveAction( resolveField );
        resolveBox.add( new JLabel( "Object Name: " ) );
        resolveBox.add( resolveField );
        resolveBox.add( Box.createHorizontalStrut( 5 ) );
        resolveBox.add( new JButton( resolveAction_ ) );
        resolveBox.add( Box.createHorizontalGlue() );
        getControlBox().add( resolveBox );
        getControlBox().add( Box.createVerticalStrut( 5 ) );

        /* Add fields for entering query position and location. */
        Action okAction = getOkAction();
        raField_ = DoubleValueField.makeRADegreesField();
        raField_.getEntryField().addActionListener( okAction );
        raField_.setDescription( "Right Ascension of cone centre (J2000)" );

        decField_ = DoubleValueField.makeDecDegreesField();
        decField_.getEntryField().addActionListener( okAction );
        decField_.setDescription( "Declination of cone centre (J2000)" );

        srField_ = DoubleValueField.makeRadiusDegreesField();
        srField_.getEntryField().addActionListener( okAction );
        srField_.setDescription( "Radius of cone search" );

        /* Install these fields in the control panel. */
        ValueFieldPanel qPanel = new ValueFieldPanel();
        qPanel.addField( raField_, new JLabel( "(J2000)" ) );
        qPanel.addField( decField_, new JLabel( "(J2000)" ) );
        qPanel.addField( srField_ );
        getControlBox().add( qPanel );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        raField_.setEnabled( enabled );
        decField_.setEnabled( enabled );
        srField_.setEnabled( enabled );
    }

    protected TableSupplier getTableSupplier() {
        SimpleResource[] resources = getRegistryPanel().getSelectedResources();
        if ( resources.length != 1 ) {
            throw new IllegalStateException( "No cone search service " +
                                             "selected" );
        }
        final ConeSearch coner = new ConeSearch( resources[ 0 ] );
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

    private void setResolvedObject( ResolverInfo info ) {
        setDegrees( raField_, info.getRaDegrees() );
        setDegrees( decField_, info.getDecDegrees() );
    }

    private void setDegrees( DoubleValueField field, double value ) {
        JComboBox convSel = field.getConverterSelector();
        for ( int i = 0; i < convSel.getItemCount(); i++ ) {
            Object item = convSel.getItemAt( i );
            if ( "degrees".equals( item.toString() ) ) {
                convSel.setSelectedItem( item );
                field.getEntryField().setText( Double.toString( value ) );
                return;
            }
        }
        logger_.warning( "Oops - no degrees option" );
    }

    private class ResolveAction extends AbstractAction {
        final JTextField resolveField_;

        ResolveAction( JTextField field ) {
            super( "Resolve" );
            resolveField_ = field;
            resolveField_.addActionListener( this );
        }

        public void actionPerformed( ActionEvent evt ) {
            final String name = resolveField_.getText();
            if ( name == null || name.trim().length() == 0 ) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            setEnabled( false );
            new Thread( "Name Resolver: " + name ) {
                public void run() {
                    ResolverInfo info = null;
                    ResolverException error = null;
                    try {
                        info = ResolverInfo.resolve( name );
                    }
                    catch ( ResolverException e ) {
                        error = e;
                    }
                    final ResolverInfo info1 = info;
                    final ResolverException error1 = error;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( info1 != null ) {
                                setResolvedObject( info1 );
                            }
                            else {
                                JOptionPane
                               .showMessageDialog( ConeSearchDialog.this,
                                                   error1.getMessage(),
                                                   "Name Resolution Error",
                                                   JOptionPane.ERROR_MESSAGE );
                            }
                            setEnabled( true );
                        }
                    } );
                }
            }.start();
        }

        public void setEnabled( boolean isEnabled ) {
            super.setEnabled( isEnabled );
            resolveField_.setEnabled( isEnabled );
        }
    }
}
