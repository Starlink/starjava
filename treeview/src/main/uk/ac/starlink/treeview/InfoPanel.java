package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Component used for displaying information.  It contains a slot for
 * an icon, plus a number of aligned component pairs.  The intention
 * is that these form (label,value) pairs.
 */
public class InfoPanel extends JPanel {

    private JLabel iconLabel;
    private JComponent itemPanel;
    private GridBagLayout itemGrid;
    private int nrow;

    /**
     * Constructs a new InfoPanel.
     */
    public InfoPanel() {
  javax.swing.border.Border lineBorder = javax.swing.BorderFactory.createMatteBorder( 1, 1, 1, 1, java.awt.Color.black );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        JComponent iconPanel = Box.createVerticalBox();
        iconPanel.setBorder( gapBorder );
        iconLabel = new JLabel();
        iconLabel.setAlignmentX( 0.5f );
        iconPanel.add( iconLabel );
        iconPanel.add( Box.createHorizontalStrut( 32 ) );
        iconPanel.add( Box.createVerticalGlue() );
        add( iconPanel );

        itemGrid = new GridBagLayout();
        itemPanel = new JPanel( itemGrid );
        itemPanel.setBorder( gapBorder );
        add( itemPanel );
        add( Box.createHorizontalGlue() );
    }

    /**
     * Sets the icon for this panel.
     *
     * @param  icon  the icon to add (or <tt>null</tt>)
     */
    public void setIcon( Icon icon ) {
        iconLabel.setIcon( icon );
    }

    /**
     * Adds a new label, value pair to the panel.
     *
     * @param  label  component to serve as a label for this item
     * @param  value  component to serve as a value for this item
     */
    public void addItem( JComponent label, JComponent value ) {

        /* Set up constraints for this item. */
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridy = nrow++;
        cons.gridx = 0;
        cons.ipadx = 2;
        cons.ipady = 2;

        /* Add the label. */
        cons.anchor = GridBagConstraints.WEST;
        cons.weightx = 0.0;
        itemGrid.setConstraints( label, cons );
        itemPanel.add( label );
        cons.gridx++;

        /* Create a new label to contain the value. */
        cons.weightx = 1.0;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.anchor = GridBagConstraints.WEST;
        itemGrid.setConstraints( value, cons );
        itemPanel.add( value );
        cons.gridx++;
    }

    public Dimension getMaximumSize() {
        return new Dimension( Short.MAX_VALUE, getPreferredSize().height );
    }

}
