package uk.ac.starlink.topcat.join;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.join.FindModeParameter;

/**
 * Panel for selecting matching mode for a pairwise crossmatch.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class PairModeSelector extends Box {

    private final JLabel label_;
    private final JComboBox comboBox_;

    /**
     * Constructor.
     */
    public PairModeSelector() {
        super( BoxLayout.X_AXIS );
        comboBox_ = new JComboBox( PairMode.values() );
        comboBox_.setRenderer( new DefaultListCellRenderer() {
            Formatter formatter_ = new Formatter();
            public Component getListCellRendererComponent( JList list,
                                                           Object value, int ix,
                                                           boolean isSel,
                                                           boolean hasFocus ) {
                Component c =
                    super.getListCellRendererComponent( list, value, ix, isSel,
                                                        hasFocus );
                String ttip = null;
                if ( value instanceof PairMode ) {
                    PairMode mode = (PairMode) value;
                    setText( mode.getSummary() );
                    String desc = FindModeParameter.getModeDescription( mode );
                    try {
                        ttip = "<html>"
                             + formatter_.formatXML( desc, 0 )
                                         .replaceAll( "\\n", "<br>" )
                             + "</html>";
                    }
                    catch ( SAXException e ) {
                        ttip = null;
                    }
                }
                setToolTipText( ttip );
                return c;
            }
        } );
        comboBox_.setSelectedItem( PairMode.BEST );
        label_ = new JLabel( "Match Selection: " );
        add( label_ );
        add( comboBox_ );
    }

    /**
     * Returns matching mode.
     *
     * @return  mode
     */
    public PairMode getMode() {
        return (PairMode) comboBox_.getSelectedItem();
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        label_.setEnabled( enabled );
        comboBox_.setEnabled( enabled );
    }
}
