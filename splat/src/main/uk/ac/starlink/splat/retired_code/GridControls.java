package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.ast.AstGrid;

/**
 * GridControls creates a "page" of widgets that are a view of an
 * AstGrid object. They provide the ability to configure all the
 * properties of the AstGrid object.
 *
 * @since $Date$
 * @since 13-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see AstGrid, PlotConfigFrame.
 */
public class GridControls extends JPanel implements ChangeListener
{
    /**
     * AstGrid model for current state.
     */
    protected AstGrid astGrid = null;

    /**
     * Whether the grid is to be shown.
     */
    protected JCheckBox show = new JCheckBox();

    /**
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Line properties controls.
     */
    protected LineControls lineControls = null;

    /**
     * Whether to inhibit change events from LineControls from recyling.
     */
    protected boolean inhibitLineChangeListener = false;

    /**
     * Create an instance.
     */
    public GridControls( AstGrid astGrid )
    {
        initUI();
        setAstGrid( astGrid );
    }

    /**
     * Reset controls to defaults.
     */
    public void reset()
    {
        astGrid.setDefaults();
        lineControls.reset();
        updateFromAstGrid();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridBagLayout() );

        //  Whether grid is shown or not.
        show.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchShow();
                }
            });

        //  Add labels for all fields.
        addLabel( "Show:", 0 );
        addLabel( "Thickness:", 1 );
        addLabel( "Style:", 2 );
        addLabel( "Colour:", 3 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Show grid.
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.BOTH;
        add( show, gbc );

        //  Line controls.
        row = addLineControls( row );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        show.setToolTipText( "Display grid lines in plot" );
    }

    /**
     * Set the AstGrid object (only after UI is initiliased).
     */
    public void setAstGrid( AstGrid astGrid )
    {
        this.astGrid = astGrid;
        astGrid.addChangeListener( this );
        updateFromAstGrid();
    }

    /**
     * Update interface to reflect values of AstGrid object.
     */
    protected void updateFromAstGrid()
    {
        astGrid.removeChangeListener( this );

        show.setSelected( astGrid.getShown() );

        inhibitLineChangeListener = true;
        lineControls.setThick( (int) astGrid.getWidth() );
        lineControls.setColour( astGrid.getColour() );
        lineControls.setStyle( (int) astGrid.getStyle() );
        inhibitLineChangeListener = false;

        astGrid.addChangeListener( this );

    }

    /**
     * Get copy of reference to current AstGrid.
     */
    public AstGrid getAstGrid()
    {
        return astGrid;
    }

    /**
     * Add a new UI description label. This is added to the front of
     * the given row.
     */
    private void addLabel( String text, int row )
    {
        JLabel label = new JLabel( text );
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = labelInsets;
        add( label, gbc );
    }

    /**
     * Match whether to display the grid.
     */
    protected void matchShow()
    {
        astGrid.setShown( show.isSelected() );
    }

    /**
     * Add line property controls.
     */
    private int addLineControls( int row )
    {
        lineControls = new LineControls( this, row, 1 );

        //  Respond to changed of line properties.
        lineControls.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchLine();
                }
            });
        return row + 3;
    }

    /**
     * Match line properties.
     */
    protected void matchLine()
    {
        if ( ! inhibitLineChangeListener ) {

            //  Update AstGrid object to match properties. Take care
            //  as modifying this fires a ChangeEvent that attempts to
            //  synchronize the lineControls (which are actually
            //  storing the state for now).
            astGrid.removeChangeListener( this );

            astGrid.setWidth( lineControls.getThick() );
            astGrid.setStyle( lineControls.getStyle() );
            astGrid.setColour( lineControls.getColour() );

            astGrid.addChangeListener( this );
            updateFromAstGrid();
        }
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the AstGrid object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromAstGrid();
    }
}
