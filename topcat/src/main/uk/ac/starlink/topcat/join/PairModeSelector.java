package uk.ac.starlink.topcat.join;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
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
    private final JComboBox<PairMode> comboBox_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public PairModeSelector() {
        super( BoxLayout.X_AXIS );
        comboBox_ = new JComboBox<PairMode>( PairMode.values() );
        final DefaultListCellRenderer baseRenderer =
            new DefaultListCellRenderer();
        comboBox_.setRenderer( new ListCellRenderer<PairMode>() {
            Formatter formatter_ = new Formatter();
            public Component getListCellRendererComponent(
                    JList<? extends PairMode> list, PairMode mode, int ix,
                    boolean isSel, boolean hasFocus ) {
                Component c =
                    baseRenderer
                   .getListCellRendererComponent( list, mode, ix, isSel,
                                                  hasFocus );
                String ttip = null;
                if ( c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setText( mode.getSummary() );
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
                    label.setToolTipText( ttip );
                }
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
        return comboBox_.getItemAt( comboBox_.getSelectedIndex() );
    }

    /**
     * Returns the mode selector component.
     *
     * @return  mode selector combo box
     */
    public JComboBox<PairMode> getComboBox() {
        return comboBox_;
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        label_.setEnabled( enabled );
        comboBox_.setEnabled( enabled );
    }
}
