package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Window for defining up a mutually exclusive group of subsets
 * based on the values of a given table expression.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2015
 */
public class ClassifyWindow extends AuxWindow {

    private final TopcatModel tcModel_;
    private final JComboBox<ColumnData> colSelector_;
    private final Action startAct_;
    private final Action stopAct_;
    private final Action subsetsAct_;
    private final JProgressBar progBar_;
    private final SpinnerNumberModel ncatModel_;
    private final JLabel countLabel_;
    private final JTextField prefixField_;
    private final ClassifyReportPanel reportPanel_;
    private Classification classification_;
    private volatile ClassifyWorker activeWorker_;

    /**
     * Constructor.
     *
     * @param  tcModel   topcat model
     * @param  parent   parent component
     */
    @SuppressWarnings("this-escape")
    public ClassifyWindow( TopcatModel tcModel, Component parent ) {
        super( "Column Classification", parent );
        tcModel_ = tcModel;

        /* UI components for acquiring user input. */
        colSelector_ = new ColumnDataComboBox();
        colSelector_.setModel( new ColumnDataComboBoxModel( tcModel,
                                                            Object.class,
                                                            true ) );
        colSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateState();
                if ( startAct_.isEnabled() ) {
                    startAct_.actionPerformed( null );
                }
            }
        } );
        ncatModel_ = new SpinnerNumberModel( 4, 1, Integer.MAX_VALUE, 1 );
        ncatModel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                reportClassification();
            }
        } );
        countLabel_ = new JLabel( "" );
        prefixField_ = new JTextField( 12 );
        prefixField_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                reportClassification();
            }
        } );

        /* Actions for starting and stopping the classification. */
        startAct_ = new BasicAction( "Classify", null,
                                     "Classify rows by selected value" ) {
            public void actionPerformed( ActionEvent evt ) {
                startClassification();
            }
        };
        stopAct_ = new BasicAction( "Stop", null,
                                    "Interrupt running classification" ) {
            public void actionPerformed( ActionEvent evt ) {
                setActiveWorker( null );
            }
        };
        subsetsAct_ = new BasicAction( "Add Subsets", null,
                                              "Add new subsets based on "
                                            + "current selections to table" ) {
            public void actionPerformed( ActionEvent evt ) {
                for ( RowSubset rset : reportPanel_.createSubsets() ) {
                    tcModel_.addSubset( rset );
                }
                dispose();
            }
        };
        Action closeAct = new BasicAction( "Cancel", null,
                                            "Close window"
                                          + " with no further action" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };

        /* Release the classification result on window disposal,
         * since it may have a large memory footprint.
         * This presupposes that the window cannot be re-opened once closed.
         * (Is it necessary?) */
        addWindowListener( new WindowAdapter() {
            public void windowClosed( WindowEvent evt ) {
                classification_ = null;
            }
        } );

        /* Construct and place components. */
        reportPanel_ = new ClassifyReportPanel();

        JComponent queryBox = Box.createVerticalBox();
        queryBox.setBorder( makeTitledBorder( "Query" ) );
        queryBox.add( new LineBox( "Classification value", colSelector_,
                                   true ) );
        queryBox.add( Box.createVerticalStrut( 5 ) );
        JComponent goLine = Box.createHorizontalBox();
        goLine.add( Box.createHorizontalGlue() );
        goLine.add( new JButton( startAct_ ) );
        goLine.add( Box.createHorizontalStrut( 10 ) );
        goLine.add( new JButton( stopAct_ ) );
        goLine.add( Box.createHorizontalStrut( 5 ) );
        queryBox.add( goLine );
        queryBox.add( Box.createVerticalStrut( 5 ) );
        getMainArea().add( queryBox, BorderLayout.NORTH );

        JComponent resultBox = new JPanel( new BorderLayout() );
        resultBox.setBorder( makeTitledBorder( "Results" ) );
        JComponent rcBox = Box.createVerticalBox();
        JComponent ncatLine = Box.createHorizontalBox();
        ncatLine.add( new JLabel( "Number of categories: " ) );
        JSpinner ncatSpinner = new JSpinner( ncatModel_ ) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension( 80, super.getMinimumSize().height );
            }
            @Override
            public Dimension getMaximumSize() {
                return new Dimension( Integer.MAX_VALUE,
                                      super.getPreferredSize().height );
            }
        };
        ncatLine.add( ncatSpinner );
        ncatLine.add( countLabel_ );
        ncatLine.add( Box.createHorizontalGlue() );
        rcBox.add( ncatLine );
        rcBox.add( Box.createVerticalStrut( 5 ) );
        rcBox.add( new LineBox( "Subset name prefix", prefixField_, true ) );
        resultBox.add( rcBox, BorderLayout.NORTH );
        JScrollPane reportScroller = new JScrollPane( reportPanel_ );
        reportScroller.setPreferredSize( new Dimension( 300, 200 ) );
        resultBox.add( reportScroller, BorderLayout.CENTER );
        getMainArea().add( resultBox, BorderLayout.CENTER );

        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( subsetsAct_ ) );
        controlLine.add( Box.createHorizontalStrut( 10 ) );
        controlLine.add( new JButton( closeAct ) );
        controlLine.add( Box.createHorizontalGlue() );
        getControlPanel().add( controlLine );

        progBar_ = placeProgressBar();
        addHelp( "ClassifyWindow" );
        updateState();
    }

    /**
     * Invoked when the classification has been completed.
     *
     * @param  classification   result of the classification process
     */
    private void setClassification( Classification classification ) {
        classification_ = classification;
        reportClassification();
    }

    /**
     * Updates the state of this component when the user inputs or
     * classification results have changed.
     */
    private void reportClassification() {
        final String countText;
        if ( classification_ == null ) {
            reportPanel_.setData( null, null );
            countText = "";
        }
        else {
            Object ncval = ncatModel_.getValue();
            if ( ncval instanceof Number ) {
                reportPanel_.setMaxCount( ((Number) ncval).intValue() );
            }
            reportPanel_.setPrefix( prefixField_.getText() );
            ColumnData cdata = classification_.cdata_;
            Classifier<?> classifier = classification_.classifier_;
            reportPanel_.setData( cdata, classifier );
            countText = "  / " + classifier.getValueCount() + " ";
        }
        countLabel_.setText( countText );
        countLabel_.revalidate();
        countLabel_.repaint();
        updateState();
    }

    /**
     * Begins the asynchronous classification process in accordance with
     * the current state of this component.
     */
    private void startClassification() {
        ColumnData cdata = getClassifyData();
        if ( cdata != null ) {
            prefixField_.setText( sanitiseText( cdata.toString(), true, 6 )
                                + "_" );
            ClassifyWorker worker = new ClassifyWorker( cdata );
            setActiveWorker( worker );
            new Thread( worker, "Classifier" ).start();
        }
        else {
            String msg = "No classification column given";
            JOptionPane
                .showMessageDialog( this, "No classification column given",
                                    "No data", JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Marks a given worker object as currently working on behalf of
     * this panel.  If null is given, no worker is active.
     * Any existing active thread is interrupted.
     *
     * @param   worker  worker object
     */
    private void setActiveWorker( ClassifyWorker worker ) {
        activeWorker_ = worker;
        updateState();
    }

    /**
     * Updates components based on current state.
     * In particular enabledness is set appropriately.
     */
    private void updateState() {
        boolean isActive = activeWorker_ != null;
        boolean hasData = getClassifyData() != null;
        startAct_.setEnabled( ! isActive && hasData );
        stopAct_.setEnabled( isActive );
        colSelector_.setEnabled( ! isActive );
        boolean hasSubsets = reportPanel_.getSubsetCount() > 0;
        subsetsAct_.setEnabled( hasSubsets );
        if ( ! isActive && progBar_ != null ) {
            progBar_.setValue( 0 );
            progBar_.setMinimum( 0 );
            progBar_.setMaximum( 1 );
        }
    }

    /**
     * Returns the column data object according to which the classification
     * will be done.
     *
     * @return  column data, or null
     */
    private ColumnData getClassifyData() {
        Object cdata = colSelector_.getSelectedItem();
        return cdata instanceof ColumnData ? (ColumnData) cdata : null;
    }

    /**
     * Obtain a suitable string from a text for use as (part of) a short
     * label, such as a subset name.
     *
     * @param   txt  basic text
     * @param   isStart  true iff this is the start of an identifier
     * @param   maxlen  maximum length of result
     * @return   innocuous label string
     */
    public static String sanitiseText( String txt, boolean isStart,
                                       int maxlen ) {
        String regex = ( isStart ? "[a-zA-Z]" : "[a-zA-Z0-9]" )
                     + "[a-zA-Z0-9]{0," + ( maxlen - 1 ) + "}";
        Matcher matcher = Pattern.compile( regex ).matcher( txt );
        if ( matcher.find() ) {
            return matcher.group();
        }
        else {
            return txt.length() < maxlen ? txt : txt.substring( 0, maxlen );
        }
    }

    /**
     * Aggregates the column data and corresponding classification results.
     */
    private static class Classification {
        final Classifier<Object> classifier_;
        final ColumnData cdata_;

        /**
         * Constructor.
         *
         * @param  classifier  object containing classification results
         * @param  cdata  data object to which the results apply
         */
        Classification( Classifier<Object> classifier, ColumnData cdata ) {
            classifier_ = classifier;
            cdata_ = cdata;
        }
    }

    /**
     * Runnable that will execute a classification action.
     */
    private class ClassifyWorker implements Runnable {

        private final ColumnData cdata_;
        private final long nrow_;
        private final Scheduler scheduler_;
 
        /**
         * Constructor.
         *
         * @param  cdata  column data to classify
         */
        ClassifyWorker( ColumnData cdata ) {
            cdata_ = cdata;
            nrow_ = tcModel_.getDataModel().getRowCount();
            scheduler_ = new Scheduler( ClassifyWindow.this.getBodyPanel() ) {
                public boolean isActive() {
                    return isActiveWorker();
                }
            };
        }

        /**
         * Indicates whether this worker is currently active on behalf
         * of its parent window.  If not, its results are unwanted and it
         * must not affect the GUI.
         *  
         * @return   true iff this worker is currently active
         */
        private boolean isActiveWorker() {
            return ClassifyWindow.this.activeWorker_ == this;
        }

        /**
         * Performs the classification, making asynchronous updates to the UI.
         */
        public void run() {
            scheduler_.schedule( new Runnable() {
                public void run() {
                    setClassification( null );
                    progBar_.setMinimum( 0 );
                    progBar_.setMaximum( (int) nrow_ );
                    progBar_.setValue( 0 );
                }
            } );
            long step =
                Math.max( 1000, nrow_ / ClassifyWindow.this.getWidth() );
            try {
                final Classifier<Object> classifier = new Classifier<Object>();
                for ( long ir = 0; ir < nrow_; ir++ ) {
                    classifier.submit( cdata_.readValue( ir ) );
                    if ( ir % step == 0 ) {
                        if ( isActiveWorker() ) {
                            final int ir0 = (int) ir;
                            scheduler_.schedule( new Runnable() {
                                public void run() {
                                    progBar_.setValue( ir0 );
                                }
                            } );
                        }
                        else {
                            return;
                        }
                    }
                }
                scheduler_.schedule( new Runnable() {
                    public void run() {
                        setClassification( new Classification( classifier,
                                                               cdata_ ) );
                    }
                } );
            }
            catch ( OutOfMemoryError e ) {
                ErrorDialog.showError( ClassifyWindow.this.getBodyPanel(),
                                       "Out of memory", e );
            }
            catch ( IOException e ) {
                ErrorDialog.showError( ClassifyWindow.this.getBodyPanel(),
                                       "Read error", e );
            }
            finally {
                scheduler_.schedule( new Runnable() {
                    public void run() {
                        setActiveWorker( null );
                    }
                } );
            }
        }
    }
}
