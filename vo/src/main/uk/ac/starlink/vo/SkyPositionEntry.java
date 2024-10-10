package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
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
    private final JTextField resolveField_;
    private final DoubleValueField raField_;
    private final DoubleValueField decField_;
    private final ValueFieldPanel qPanel_;
    private final List<DoubleValueField> fieldList_;
    private final JComponent[] enablables_;
    private final List<ActionListener> actListeners_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   coordSysLabel  label such as "J2000" identifying the coordinate
     *                         system for additional info to the user;
     *                         may be null
     */
    @SuppressWarnings("this-escape")
    public SkyPositionEntry( String coordSysLabel ) {
        super( new BorderLayout() );
        Box box = Box.createVerticalBox();
        add( box, BorderLayout.CENTER );
        actListeners_ = new ArrayList<ActionListener>();
        List<JComponent> enList = new ArrayList<JComponent>();

        /* Add name resolution field. */
        Box resolveBox = Box.createHorizontalBox();
        resolveField_ = new JTextField( 20 );
        resolveAction_ = new ResolveAction();
        JLabel resolveLabel = new JLabel( "Object Name: " );
        enList.add( resolveLabel );
        resolveBox.add( resolveLabel );
        resolveBox.add( resolveField_ );
        resolveBox.add( Box.createHorizontalStrut( 5 ) );
        resolveBox.add( new JButton( resolveAction_ ) );
        resolveBox.add( Box.createHorizontalGlue() );
        add( resolveBox, BorderLayout.NORTH );

        /* Add fields for entering query position and radius. */
        raField_ = DoubleValueField.makeRADegreesField();
        decField_ = DoubleValueField.makeDecDegreesField();

        /* Place these fields. */
        final JLabel raSysLabel;
        final JLabel decSysLabel;
        if ( coordSysLabel == null || coordSysLabel.trim().length() == 0 ) {
            raSysLabel = null;
            decSysLabel = null;
        }
        else {
            String sysLabel = "(" + coordSysLabel + ")";
            raSysLabel = new JLabel( sysLabel );
            decSysLabel = new JLabel( sysLabel );
            enList.add( raSysLabel );
            enList.add( decSysLabel );
        }
        qPanel_ = new ValueFieldPanel();
        qPanel_.addField( raField_, raSysLabel );
        qPanel_.addField( decField_, decSysLabel );
        box.add( qPanel_ );

        /* Set up list of fields. */
        fieldList_ = new ArrayList<DoubleValueField>();
        fieldList_.add( raField_ );
        fieldList_.add( decField_ );
        enablables_ = enList.toArray( new JComponent[ 0 ] );
    }

    /**
     * Adds a field to the list of ones controlled by this component.
     * The added field is placed in a tidy way, and its enable/disablement
     * etc is handled alongside the basic positional fields.
     *
     * @param   field   field to add
     */
    public void addField( DoubleValueField field ) {
        qPanel_.addField( field );
        fieldList_.add( field );
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
     * Returns the field in which an astronomical object whose position is
     * be resolved can be entered.
     *
     * @return  object resolver field
     */
    public JTextField getResolveField() {
        return resolveField_;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        resolveField_.setEnabled( enabled );
        resolveAction_.setEnabled( enabled );
        for ( DoubleValueField field : fieldList_ ) {
            field.setEnabled( enabled );
        }
        for ( int i = 0; i < enablables_.length; i++ ) {
            enablables_[ i ].setEnabled( enabled );
        }
    }

    /**
     * Adds an action listener to the entry fields of this component.
     *
     * @param   listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        for ( DoubleValueField field : fieldList_ ) {
            field.getEntryField().addActionListener( listener );
            field.getConverterSelector().addActionListener( listener );
        }
        actListeners_.add( listener );
    }

    /**
     * Removes an action listener from the entry fields of this component.
     *
     * @param  listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        for ( DoubleValueField field : fieldList_ ) {
            field.getEntryField().removeActionListener( listener );
            field.getConverterSelector().removeActionListener( listener );
        }
        actListeners_.remove( listener );
    }

    /**
     * Adds a caret listener to the entry fields of this component.
     *
     * @param  listener  listener to add
     */
    public void addCaretListener( CaretListener listener ) {
        for ( DoubleValueField field : fieldList_ ) {
            field.getEntryField().addCaretListener( listener );
        }
    }

    /**
     * Removes a caret listener from the entry fields of this component.
     *
     * @param  listener  listener to remove
     */
    public void removeCaretListener( CaretListener listener ) {
        for ( DoubleValueField field : fieldList_ ) {
            field.getEntryField().removeCaretListener( listener );
        }
    }

    /**
     * Sets this component to contain the position of a given resolved object.
     *
     * @param  info   resolved object information
     */
    private void setResolvedObject( ResolverInfo info ) {
        setDegrees( raField_, info.getRaDegrees() );
        setDegrees( decField_, info.getDecDegrees() );
        ActionEvent evt = new ActionEvent( this, 1, "Resolved" );
        for ( ActionListener l : actListeners_ ) {
            l.actionPerformed( evt );
        }
    }

    /**
     * Sets the current sky position in degrees.  This can be called by
     * external code, and resets the resolver field to empty.
     *
     * @param   raDegrees   right ascension in degreees
     * @param   decDegrees  declination in degrees
     * @param   clearResolver  if true, clear the contents of the
     *           resolved object field
     */
    public void setPosition( double raDegrees, double decDegrees,
                             boolean clearResolver ) {
        setDegrees( raField_, raDegrees );
        setDegrees( decField_, decDegrees );
        if ( clearResolver ) {
            resolveField_.setText( null );
        }
    }

    /**
     * Sets a value field to contain a given value in degrees.
     *
     * @param   field  field to set
     * @param   degValue  value in degrees
     */
    private void setDegrees( DoubleValueField field, double degValue ) {
        JComboBox<ValueConverter> convSel = field.getConverterSelector();
        for ( int i = 0; i < convSel.getItemCount(); i++ ) {
            ValueConverter item = convSel.getItemAt( i );
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

        /**
         * Constructor.
         *
         * @param   field   object name entry field
         */
        ResolveAction() {
            super( "Resolve" );
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
