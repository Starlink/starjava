package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Graphical component which can edit in place a DescribedValue.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class ParameterEditor extends JComponent
                             implements ActionListener, FocusListener {

    private final DescribedValue dval_;
    private final JTextField field_;
    private final JComboBox conversionChooser_;
    private final ValueCodec codec0_;

    /**
     * Constructs a new editor.
     *
     * @param  dval  the DescribedValue that this object will edit
     */
    public ParameterEditor( DescribedValue dval ) {
        dval_ = dval;
        ValueInfo info = dval.getInfo();
        String descrip = info.getDescription();
        String units = info.getUnitString();

        /* Set up the text field for entering values. */
        field_ = new JTextField( 16 );
        if ( descrip != null && descrip.trim().length() > 0 ) {
            field_.setToolTipText( descrip );
        }
        field_.addActionListener( this );
        field_.addFocusListener( this );

        /* Set up a chooser for value conversion. */
        ValueCodec[] codecs = ValueCodec.getCodecs( info );
        if ( codecs.length > 1 ) {
            codec0_ = null;
            conversionChooser_ = new JComboBox( codecs );
            conversionChooser_.setSelectedIndex( 0 );
            conversionChooser_.addActionListener( this );
            conversionChooser_.setToolTipText( "Units for " + info.getName() );
        }
        else {
            conversionChooser_ = null;
            codec0_ = codecs[ 0 ];
        }
        field_.setText( getCodec().formatValue( dval_.getValue(), 16 ) );

        /* Lay out components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( field_ );
        if ( conversionChooser_ != null ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( conversionChooser_ );
        }
        else if ( units != null && units.trim().length() > 0 ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( new JLabel( " (" + units.trim() + ")" ) );
        }
    }

    /**
     * Returns the currently active value codec - this is what converts
     * between the representation of the value in the text field and the
     * actual value in the parameter.
     *
     * @return  current codec
     */
    private ValueCodec getCodec() {
        if ( codec0_ != null ) {
            return codec0_;
        }
        else {
            return (ValueCodec) conversionChooser_.getSelectedItem();
        }
    }

    /**
     * Effects the change to the described value.
     * This method is invoked when the GUI undergoes any change that
     * might signal the user has made a change that should be attended to.
     */
    private void updateValue() {
        ValueCodec codec = getCodec();
        String text = field_.getText();
        try {
            dval_.setValue( codec.unformatString( text ) );
        }
        catch ( RuntimeException e ) {
            field_.setText( codec.formatValue( dval_.getValue(), 16 ) );
            String msg = "Illegal value \"" + text + "\" for parameter " +
                         dval_.getInfo();
            ErrorDialog.showError( this, "Value Error", e, msg );
        }
    }

    public void actionPerformed( ActionEvent evt ) {
        updateValue();
    }

    public void focusGained( FocusEvent evt ) {
        updateValue();
    }

    public void focusLost( FocusEvent evt ) {
        updateValue();
    }
}
