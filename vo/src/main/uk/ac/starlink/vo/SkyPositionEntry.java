package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Component for acquiring from the user a sky position and radius.
 * Object name resolution is provided.
 *
 * @author   Mark Taylor
 * @since    28 Jan 2008
 */
public class SkyPositionEntry extends JPanel {

    private final Action resolveAction_;
    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final DoubleValueField srField_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   coordSysLabel  label such as "J2000" identifying the coordinate
     *                         system for additional info to the user;
     *                         may be null
     */
    public SkyPositionEntry( String coordSysLabel ) {
        super( new BorderLayout() );
        Box box = Box.createVerticalBox();
        add( box, BorderLayout.CENTER );

        /* Add name resolution field. */
        Box resolveBox = Box.createHorizontalBox();
        JTextField resolveField = new JTextField( 20 );
        resolveAction_ = new ResolveAction( resolveField );
        resolveBox.add( new JLabel( "Object Name: " ) );
        resolveBox.add( resolveField );
        resolveBox.add( Box.createHorizontalStrut( 5 ) );
        resolveBox.add( new JButton( resolveAction_ ) );
        resolveBox.add( Box.createHorizontalGlue() );
        box.add( resolveBox );
        box.add( Box.createVerticalStrut( 5 ) );

        /* Add fields for entering query position and radius. */
        raField_ = DoubleValueField.makeRADegreesField();
        decField_ = DoubleValueField.makeDecDegreesField();
        srField_ = DoubleValueField.makeRadiusDegreesField();

        /* Place these fields. */
        String sysLabel =
            ( coordSysLabel == null || coordSysLabel.trim().length() == 0 )
                ? null
                : "(" + coordSysLabel + ")";
        ValueFieldPanel qPanel = new ValueFieldPanel();
        qPanel.addField( raField_,
                         sysLabel == null ? null : new JLabel( sysLabel ) );
        qPanel.addField( decField_,
                         sysLabel == null ? null : new JLabel( sysLabel ) );
        qPanel.addField( srField_ );
        box.add( qPanel );
    }

    /**
     * Returns the field containing right ascension in degrees.
     *
     * @return   RA field
     */
    public DoubleValueField getRaDegreesField() {
        return raField_;
    }

    /**
     * Returns the field containing declination in degrees.
     *
     * @return   dec field
     */
    public DoubleValueField getDecDegreesField() {
        return decField_;
    }

    /**
     * Returns the field containing search radius in degrees.
     *
     * @return   radius field
     */
    public DoubleValueField getRadiusDegreesField() {
        return srField_;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        raField_.setEnabled( enabled );
        decField_.setEnabled( enabled );
        srField_.setEnabled( enabled );
    }

    /**
     * Adds an action listener to the entry fields of this component.
     *
     * @param   listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        raField_.getEntryField().addActionListener( listener );
        decField_.getEntryField().addActionListener( listener );
        srField_.getEntryField().addActionListener( listener );
    }

    /**
     * Removes an action listener from the entry fields of this component.
     *
     * @param  listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        raField_.getEntryField().removeActionListener( listener );
        decField_.getEntryField().removeActionListener( listener );
        srField_.getEntryField().removeActionListener( listener );
    }

    /**
     * Sets this component to contain the position of a given resolved object.
     *
     * @param  info   resolved object information
     */
    private void setResolvedObject( ResolverInfo info ) {
        setDegrees( raField_, info.getRaDegrees() );
        setDegrees( decField_, info.getDecDegrees() );
    }

    /**
     * Sets a value field to contain a given value in degrees.
     *
     * @param   field  field to set
     * @param   degValue  value in degrees
     */
    private void setDegrees( DoubleValueField field, double degValue ) {
        JComboBox convSel = field.getConverterSelector();
        for ( int i = 0; i < convSel.getItemCount(); i++ ) {
            Object item = convSel.getItemAt( i );
            if ( "degrees".equals( item.toString() ) ) {
                convSel.setSelectedItem( item );
                field.getEntryField().setText( Double.toString( degValue ) );
                return;
            }
        }
        logger_.warning( "Oops - no degrees option" );
    }

    /**
     * Action to perform when the Resolve button is hit.
     * It takes the currently entered object name, resolves it, and
     * sets the RA/Dec fields accordingly.
     */
    private class ResolveAction extends AbstractAction {
        final JTextField resolveField_;

        /**
         * Constructor.
         *
         * @param   field   object name entry field
         */
        ResolveAction( JTextField field ) {
            super( "Resolve" );
            resolveField_ = field;
            resolveField_.addActionListener( this );
        }

        public void actionPerformed( ActionEvent evt ) {
            final String name = resolveField_.getText();
            if ( name != null && name.trim().length() == 0 ) {
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
                                ErrorDialog.showError( SkyPositionEntry.this,
                                                       "Name Resolution Error",
                                                       error1 );
                            }
                            setEnabled( true );
                        }
                    } );
                }
            }.start();
        }

        public void setEnabled( boolean enabled ) {
            super.setEnabled( enabled );
            resolveField_.setEnabled( enabled );
        }
    }
}
