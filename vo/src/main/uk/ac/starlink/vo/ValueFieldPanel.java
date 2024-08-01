package uk.ac.starlink.vo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Utility class for presenting an aligned stack of value fields.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Jan 2005
 */
public class ValueFieldPanel extends JPanel {

    private final GridBagLayout layer_;
    private final GridBagConstraints[] gbcs_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public ValueFieldPanel() {
        layer_ = new GridBagLayout();
        setLayout( layer_ );
        final Insets ins = new Insets( 2, 2, 2, 2 );
        gbcs_ = new GridBagConstraints[] {
            new GridBagConstraints() {{
                gridx = 0; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = 1; gridy = 0; anchor = WEST; insets = ins;
                weightx = 1.0; fill = BOTH;
            }},
            new GridBagConstraints() {{
                gridx = 2; gridy = 0; anchor = WEST; insets = ins;
                fill = HORIZONTAL;
            }},
            new GridBagConstraints() {{
                gridx = 3; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = 4; gridy = 0;
                weightx = 1.0;
            }},
        };
    }

    /**
     * Adds a field to this panel.
     *
     * @param   vf  field to add
     */
    void addField( DoubleValueField vf ) {
        addField( vf, null );
    }

    /**
     * Adds a field to this panel with optional trailing component.
     *
     * @param   vf  field to add
     * @param   trailer  additional component placed after the field
     */
    public void addField( DoubleValueField vf, JComponent trailer ) {
        JComponent[] comps = new JComponent[] {
            vf.getLabel(),
            vf.getEntryField(),
            vf.getConverterSelector(),
            trailer,
            new JPanel(),
        };
        for ( int i = 0; i < comps.length; i++ ) {
            if ( comps[ i ] != null ) {
                layer_.setConstraints( comps[ i ], gbcs_[ i ] );
                add( comps[ i ] );
                gbcs_[ i ].gridy++;
            }
        }
    }

}
