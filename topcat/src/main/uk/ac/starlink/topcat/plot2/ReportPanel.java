package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Displays plot reports.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2014
 */
public class ReportPanel extends JPanel {

    private final JComboBox subsetSelector_;
    private final Map<ReportKey,JTextField> fieldMap_;
    private final JPanel reportHolder_;
    private Map<RowSubset,ReportMap> reports_;

    /**
     * Constructor.
     */
    public ReportPanel() {
        super( new BorderLayout() );
        subsetSelector_ = new JComboBox();
        subsetSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateDisplay();
            }
        } );
        add( new LineBox( "Subset", new ShrinkWrapper( subsetSelector_ ),
                          true ),
             BorderLayout.NORTH );
        reportHolder_ = new JPanel( new BorderLayout() );
        add( reportHolder_, BorderLayout.CENTER );
        fieldMap_ = new LinkedHashMap<ReportKey,JTextField>();
        reports_ = new HashMap<RowSubset,ReportMap>();
    }

    /**
     * Updates the reports that can be displayed by this component.
     * They are supplied as a map keyed by RowSubset.
     *
     * @param  reports   map of subsets to plot reports
     */
    public void submitReports( Map<RowSubset,ReportMap> reports ) {
        reports_ = reports;
        RowSubset[] subsets = reports.keySet().toArray( new RowSubset[ 0 ] );
        subsetSelector_.setModel( new DefaultComboBoxModel( subsets ) );
        if ( subsetSelector_.getSelectedIndex() < 0 ) {
            subsetSelector_.setSelectedIndex( subsets.length - 1 );
        }
        updateDisplay();
    }

    /**
     * Updates this component's appearance to match its current state.
     */
    private void updateDisplay() {
        displayReport( reports_.get( subsetSelector_.getSelectedItem() ) );
    }

    /** 
     * Displays the content of a particular report map.
     *
     * @param  report  plot report to display
     */
    private void displayReport( ReportMap report ) {

        /* Assemble a list of key-string pairs to display. */
        Map<ReportKey,String> txtMap = new LinkedHashMap<ReportKey,String>();
        if ( report != null ) {
            for ( ReportKey key : report.keySet() ) {
                if ( key.isGeneralInterest() ) {
                    Object val = report.get( key );
                    txtMap.put( key, val == null ? null : val.toString() );
                }
            }
        }

        /* Ensure that the display component has slots for each of the
         * items we are going to display.  If not, throw it out and
         * position a new one that does. */
        if ( ! fieldMap_.keySet().containsAll( txtMap.keySet() ) ) {
            fieldMap_.clear();
            LabelledComponentStack stack = new LabelledComponentStack();
            for ( ReportKey key : txtMap.keySet() ) {
                JTextField field = new JTextField();
                field.setEditable( false );
                stack.addLine( key.getMeta().getLongName(), field );
                fieldMap_.put( key, field );
            }
            reportHolder_.removeAll();
            reportHolder_.add( stack );
            reportHolder_.revalidate();
        }

        /* Display the report items in the display component. */
        for ( ReportKey key : fieldMap_.keySet() ) {
            fieldMap_.get( key ).setText( txtMap.get( key ) );
        }
    }
}
