/**
 * CocoPanel.java
 * Provides a Swing GUI to run COCO
 *
 * @author Roy Platon
 * @version 1.00 25 Oct 2002

 <h1>Java GUI for Coco - Conversion of Celestial Coordinates</h1>
 <p>
 This Java Application provides a GUI interface to the Coco Program,
 which can also be run as a Applet.
 This uses the Pal (Positional Astronomy Library).
 </p>
 **/

package uk.ac.starlink.coco;

import java.io.*;
import java.util.*;
import java.math.*;
import java.text.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/* The Starlink Positional Astronomy Library */
import uk.ac.starlink.pal.*;
/* This is used for the Wb Service Code, which is commented out */
// import uk.ac.starlink.util.*;

/**
 */
public class CocoPanel extends JPanel implements ActionListener {

/*  the Coco Class Library */
    private CoordinateConversion coco;

/*
 * Debug settings
 */
    private final boolean TRACE = false;
    private final boolean DEBUG = false;

/*
 * Constants
 */
    private final String RESET = "Reset";
    private final String CALCULATE = "Calculate";
    private final String CLOSE = "Exit";

/*
 * Maths Constants
 */
    private static final double D2R = Math.PI/180.0;
    private static final double R2D = 180.0/Math.PI;
    private static final double S2R = Math.PI/(12*3600.0);
    private static final double AS2R = Math.PI/(180*3600.0);
    private static final double PIBY2 = Math.PI/2.0;

/* Default values */
    private static char insys = '4';
    private static char inJB = 'B';
    private static double inepoch = 1950;
    private static char inJBeq = 'B';
    private static double inequinox = 1950;
    private static char outsys = '5';
    private static char outJB = 'J';
    private static double outepoch = 2000;
    private static char outJBeq = 'J';
    private static double outequinox = 2000;
    private static boolean hours = true;
    private static boolean degrees = ! hours;
    private static boolean lowprec = false;
    private static boolean mediumprec = true;
    private static boolean highprec = false;

/*
 * Default settings
 */
    private String task = null;
    private boolean applet = false;
    private boolean webservice = false;
    private String text = null;
    private String insystem = null;
    private String outsystem = null;
    private String inbase = null;
    private String outbase = null;
    private String coords = null;
    private String answer = null;

/* Decode table for the allowed Coordinate systems
 */
    private static String system_table[] = {
        "Equatorial, FK4",       "4", "B1950",
        "FK4, without E-Terms",  "B", "B1950",
        "Equatorial, FK5",       "5", "J2000",
        "Ecliptic",              "A", "",
        "Equatorial, geocentric","E", "",
        "Galactic",              "G", "B1950"
    };
    private final String label1 =
       "RA                  Dec             PM             PX      RV";
    private final String label2 =
       "h    m    s        d    '   \"      [s/y   \"/y     [\"       [km/s]]]";

    private static String system_in_def = system_table[0];
    private static String system_in = system_table[0];
    private static char code_in = system_table[1].charAt(0);
    private static String equinox_in = system_table[2];

    private static String system_out_def = system_table[6];
    private static String system_out = system_table[6];
    private static char code_out = system_table[7].charAt(0);
    private static String equinox_out = system_table[8];
    private static char units = 'H';
    private static char resolution = 'M';

    private final int XSIZE = 640;
    private final int YSIZE = 380;
    private final Color FOREGROUND = Color.blue;
    private final Color BACKGROUND = Color.lightGray;
/*
 * Text fields in dialogue box
 */
//    private Container Pane = getContentPane();
    private Container Pane;
    private GridBagConstraints default_constraint;

    private JComboBox inSystembox;
    private JComboBox outSystembox;

    private JTextField inEquinox;
    private JTextField outEquinox;
    private JTextField inputfield;
    private JTextField inEpoch;
    private JTextField outEpoch;
    private JTextField outputfield;

    private JRadioButton hourButton, degreeButton;
    private JRadioButton highButton, mediumButton, lowButton;
    private ButtonGroup Alpha, Resolution;

/**
 * The constuctor to set up the executable
 * @param title The task name
 */
    CocoPanel( Container pane ) {
        task = "Applet";
        coco = new CoordinateConversion();
        Pane = pane;
        return;
    }

/**
 * Repaint the Window
 * @param gc The frame in which to draw
 */
    public void paintComponent ( Graphics gc ) {
    super.paintComponent( gc );
        gc.setColor( BACKGROUND );
        gc.fillRect( 0, 0, XSIZE, YSIZE );
     }

/**
 * Sets up the dialogue box and begins processing a command
 * @param task The task name used in the window title,
 *             if null runs as an Applet
 */
    public void setup( String task ) {

        trace( "Setup: " + task + " Date: " + getToday() );

        if ( task == null ) applet = true;
//        else {
//            Pane.setTitle( task );
//            Pane.setSize( xsize, ysize );
//        }
        Pane.setBackground( BACKGROUND );
        Pane.setForeground( FOREGROUND );

//  Set up the Panel
        initPanel( Pane );

        trace( "Make Visible");
        this.setVisible(true);

        return;

    }

    private void initPanel( Container pane ) {
        String SPACE = " ";
        JLabel BLANK = new JLabel(SPACE);

        trace( "initPanel: " + task );
        pane.setLayout( new GridBagLayout() );

// Standard Constraints for GridBag

        GridBagConstraints constraint = new GridBagConstraints();
        constraint.weightx = 50;
        constraint.weighty = 50;
        constraint.gridwidth = 1;
        constraint.gridheight = 1;
        constraint.ipady = 2;
        constraint.ipadx = 2;
        constraint.fill = GridBagConstraints.BOTH;
        constraint.anchor = GridBagConstraints.WEST;
        constraint.gridx = 0;
        constraint.gridy = 0;
//        default_constraint = (GridBagConstraints) constraint.clone();

// Add first row of Labels
        pane.add( new JLabel("INPUT"), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 1;
        pane.add( new JLabel("System:"), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 1;
        pane.add( new JLabel("Equinox:"), constraint );
        constraint.gridx += 1;
        pane.add( new JLabel("Epoch:    "), constraint );

// Add System Box
        inSystembox = new JComboBox();
        inSystembox.setEditable(false);
        inSystembox.setBackground( Pane.getBackground() );
        for ( int i = 0; i < system_table.length; i+=3) {
            inSystembox.addItem( system_table[i] );
        }
        inSystembox.setSelectedItem( system_in );
        inSystembox.addActionListener(this);
        constraint.gridy += 1;
        constraint.gridx = 1;
        constraint.fill = GridBagConstraints.HORIZONTAL;
        pane.add( inSystembox, constraint );

// Add Equinox Field
        constraint.gridx += 1;
        inEquinox = new JTextField( equinox_in );
        pane.add( inEquinox, constraint );

        constraint.gridx += 1;
        text = SPACE;
//        text = getToday();
        inEpoch = new JTextField( text );
        pane.add( inEpoch, constraint );

// Add Input Coordinate Labels
        constraint.gridy += 1;
        constraint.gridx = 0;
        constraint.gridwidth = 2;
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridx += 1;
        pane.add( new JLabel(label1), constraint );
        constraint.gridy += 2;
        constraint.gridx = 1;
        pane.add( new JLabel(label2), constraint );

//  Add Input Field
        constraint.gridy -= 1;
        constraint.gridx = 0;
        pane.add( new JLabel("Coordinate:  "), constraint );
        constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.gridx += 1;
        inputfield = new JTextField(SPACE);
        pane.add( inputfield, constraint );
        inputfield.addActionListener(this);

//  Add Hour/Degree Radio Button
        hourButton = new JRadioButton("Hour");
        degreeButton = new JRadioButton("Degree");
        hourButton.setBackground( Pane.getBackground() );
        degreeButton.setBackground( Pane.getBackground() );
        constraint.gridy -= 1;
        constraint.gridx +=2;
        constraint.gridwidth = 1;
        hourButton.setSelected( true );
        pane.add ( hourButton, constraint );
        hourButton.addActionListener(this);
        constraint.gridy += 2;
        pane.add ( degreeButton, constraint );
        degreeButton.addActionListener(this);
        Alpha = new ButtonGroup();
        Alpha.add( hourButton );
        Alpha.add( degreeButton );

//  Add Separator line
        constraint.gridy += 2;
        constraint.gridx = 1;
        constraint.gridwidth = constraint.REMAINDER;
        Pane.add ( new JSeparator(), constraint );

// Add Output Labels
        constraint.gridy += 2;
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.fill = GridBagConstraints.NONE;
        pane.add( new JLabel("OUTPUT"), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 2;
        pane.add( new JLabel("System:"), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 1;
        pane.add( new JLabel("Equinox:"), constraint );
        constraint.gridx += 1;
        pane.add( new JLabel("Epoch:           "), constraint );

// Add Output System Box
        outSystembox = new JComboBox();
        outSystembox.setEditable(false);
        outSystembox.setBackground( Pane.getBackground() );
        for ( int i = 0; i < system_table.length; i+=3 ) {
            outSystembox.addItem( system_table[i] );
        }
        outSystembox.setSelectedItem( system_out );
        outSystembox.addActionListener(this);
        constraint.gridy += 1;
        constraint.gridx = 1;
        constraint.fill = GridBagConstraints.HORIZONTAL;
        pane.add( outSystembox, constraint );

// Add Equinox Field
        constraint.gridx += 1;
        constraint.fill = GridBagConstraints.HORIZONTAL;
        outEquinox = new JTextField ( equinox_out );
        pane.add( outEquinox, constraint );

        constraint.gridx += 1;
        outEpoch = new JTextField(SPACE);
        pane.add( outEpoch, constraint );

// Add High/Medium/Low Radio Buttons
        ResolvePanel resolve = new ResolvePanel();
        resolve.setBackground( Pane.getBackground() );
        highButton = new JRadioButton("High");
        highButton.setBackground( Pane.getBackground() );
        resolve.add ( highButton );
        highButton.addActionListener(this);
        mediumButton = new JRadioButton("Medium");
        mediumButton.setBackground( Pane.getBackground() );
        resolve.add ( mediumButton );
        mediumButton.addActionListener(this);
        lowButton = new JRadioButton("Low");
        lowButton.setBackground( Pane.getBackground() );
        resolve.add ( lowButton );
        lowButton.addActionListener(this);
        mediumButton.setSelected( true );
        Resolution = new ButtonGroup();
        Resolution.add( highButton );
        Resolution.add( mediumButton );
        Resolution.add( lowButton );
        constraint.gridy += 1;
        constraint.gridx = 0;
        constraint.gridwidth = constraint.REMAINDER;
        pane.add( resolve, constraint );

// Add spacer
        constraint.gridy += 1;
        constraint.gridx = 0;
        pane.add( new JLabel(" "), constraint );
// Add Function Buttons
        ActionPanel actions = new ActionPanel();
        actions.setBackground( Pane.getBackground() );
        JButton button = new JButton( RESET );
        button.addActionListener(this);
        actions.add( button, BorderLayout.WEST );

        button = new JButton( CALCULATE );
        button.addActionListener(this);
        actions.add( button, BorderLayout.CENTER );

        if ( ! applet ) {
            button = new JButton( CLOSE );
            button.addActionListener(this);
            actions.add( button, BorderLayout.EAST );
        }

        constraint.gridy += 1;
        constraint.gridx = 0;
        constraint.gridwidth = constraint.REMAINDER;
        pane.add( actions, constraint );

// Add Webservice Option
        if ( ! applet ) {
            constraint.gridy += 1;
            constraint.gridx = 1;
            JCheckBox wserver = new JCheckBox( "Use Web Service" );
            wserver.addActionListener(this);
            wserver.setBackground( Pane.getBackground() );
            pane.add( wserver, constraint );
        }

//  Add Separator line
        constraint.gridy += 2;
        constraint.gridx = 1;
        constraint.gridwidth = constraint.REMAINDER;
        Pane.add ( new JSeparator(), constraint );

// Add Output Labels
        constraint.gridx = 0;
        constraint.gridwidth = 1;
        constraint.gridy += 1;
        constraint.fill = GridBagConstraints.NONE;
        pane.add( new JLabel("RESULT"), constraint );
        constraint.gridx += 1;
        constraint.gridwidth = 2;
        constraint.anchor = GridBagConstraints.WEST;
        constraint.fill = GridBagConstraints.NONE;
        constraint.gridx = 1;
        pane.add( new JLabel(label1), constraint );

        constraint.gridy += 2;
        constraint.gridx = 0;
//        constraint.gridwidth = 1;
        pane.add( new JLabel("Coordinate:  "), constraint );

// Add Output Field
        constraint.fill = GridBagConstraints.HORIZONTAL;
        constraint.gridx += 1;
//        constraint.gridwidth = 10;
        answer = SPACE;
        outputfield = new JTextField( answer );
        outputfield.setEditable(false);
        pane.add( outputfield, constraint );


        return;
    }

// Inner class for adding action buttons

    class ActionPanel extends JPanel {
       JPanel buttonPanel = new JPanel();
       JSeparator separator = new JSeparator();
       public ActionPanel () {
          buttonPanel.setLayout( new BorderLayout( 50, 5 ) );
          buttonPanel.setBackground( Pane.getBackground() );
          add( separator, "North" );
          add( buttonPanel, "Center");
       }
       public void add( JButton button, String pos ) {
           buttonPanel.add( button, pos );
       }
    }

//  Inner class for setting up resolution panel

    class ResolvePanel extends JPanel {
       JPanel buttonPanel = new JPanel();
       public ResolvePanel () {
          buttonPanel.setLayout( new FlowLayout( FlowLayout.LEFT, 60, 0 ) );
          buttonPanel.setBackground( Pane.getBackground() );
          add( buttonPanel );
       }
       public void add( JRadioButton button ) {
           buttonPanel.add( button );
       }
    }

/**
 * The main event processing.
 * This method defines the actions for Button Presses,
 * Combo Box selections and Radio Buttons
 * @param evt The event to process
 */
     public void actionPerformed ( ActionEvent evt ) {
        trace( "Action Event: " );
        printDebug( "Action Event: " + evt );
        Object source = evt.getSource();
        String arg = evt.getActionCommand();

        if ( source instanceof JComboBox ) {
            trace( "Combo Box Event: " + arg );
            JComboBox box = (JComboBox) source;
            String selection = (String)box.getSelectedItem();
            trace( selection + " selected" );
            if ( box == inSystembox ) {

// Input System Change
                insystem = selection;
                for ( int i = 0; i < system_table.length; i+=3) {
                   if ( selection.equals( system_table[i] ) ) {
                       code_in = system_table[i+1].charAt(0);
                       equinox_in = system_table[i+2];
                   }
                }
// Update Equinox default value
                inEquinox.setText ( equinox_in );
            } else if ( box == outSystembox ) {


// Output System Change
                outsystem = selection;
                for ( int i = 0; i < system_table.length; i+=3) {
                   if ( selection.equals( system_table[i] ) ) {
                       code_out = system_table[i+1].charAt(0);
                       equinox_out = system_table[i+2];
                   }
                }
// Update Equinox default value
                outEquinox.setText ( equinox_out );
            }
        } else

// Respond to Radio Buttons
        if ( source instanceof JRadioButton ) {
            if ( arg.equals("Hour") ) {
                trace( "Hours Selected");
                units = 'H';
            } else if ( arg.equals("Degree") ) {
                trace( "Degrees Selected");
                units = 'D';
            } else if ( arg.equals("High") ) {
                trace( "High Resolution");
                resolution = 'H';
            } else if ( arg.equals("Medium") ) {
                trace( "Medium Resolution");
                resolution = 'M';
            } else if ( arg.equals("Low") ) {
                trace( "Low Resolution");
                resolution = 'L';
            }
        } else

// Web Service Box
        if ( source instanceof JCheckBox ) {
            int index = 0;
            trace( "Check Box Event: " + arg );
            JCheckBox box = (JCheckBox) source;
            webservice = box.isSelected();

            if ( webservice ) trace( "web service selected" );
            else              trace( "web service not selected" );
            Pane.validate();
        }

// Action Buttons
        if ( source instanceof JButton ) {
            trace( "Button Event: " + arg );
            if ( arg.equals( CALCULATE ) ) {
// Calculate the Coordinates
                coords = inputfield.getText().trim();
                trace( "Coords: " + coords );
//             if ( webservice ) {
//                try {
//                    trace ( "Run Web Service here" );
//                    String data = equinox_in + " " + equinox_out + " "
//                                  + resolution + units + code_in  + code_out
//                                  + " " + coords;
//                    SOAPmessage msg = new SOAPmessage( "Coco" );
//                    trace ( "Convert " + data );
//                    answer = msg.get ( "convert", data );
//                    trace ( "Answer is " + answer );
//                }
//                catch ( Exception e ) {
//                    answer = "Exception raised: " + e;
//                }
//            } else {
                CoordinateConversion coco =
                          new CoordinateConversion( code_in, code_out );

                String text = inEquinox.getText().trim();
                if( text != null && text.length() > 0 ) equinox_in = text;

                text = outEquinox.getText().trim();
                if( text != null && text.length() > 0 ) equinox_out = text;

                String epoch_in = inEpoch.getText().trim();
                if( epoch_in == null || epoch_in.length() == 0 ||
                                        epoch_in.equals("") ) {
                  epoch_in = equinox_in;
                }

                String epoch_out = outEpoch.getText().trim();
                if( epoch_out == null || epoch_out.length() == 0 ||
                                        epoch_out.equals("") ) {
                  epoch_out = equinox_out;
                }

                if ( equinox_in != null ) coco.setInEquinox( equinox_in );
                if ( equinox_out != null ) coco.setOutEquinox( equinox_out );
                if ( epoch_in != null ) coco.setInEpoch( epoch_in );
                if ( epoch_out != null ) coco.setOutEpoch( epoch_out );

                if ( resolution != '\0' ) coco.setPrecision( resolution );
                if ( units != '\0' ) coco.setUnits( units );
                AngleDR r =  coco.validate( coords );
                if ( r != null ) {
                    answer = coco.convert( r );
                } else {
                    answer = "Invalid Coordinate: " + coords;
                }
//            }
                outputfield.setText( answer );
                outputfield.setForeground( Color.red );
                outputfield.setBackground( Color.white );
            } else if ( arg.equals( RESET ) ) {

// Reset Selected Fields
                trace( RESET );
                inSystembox.setSelectedItem( system_in_def );
                outSystembox.setSelectedItem( system_out_def );
                outputfield.setText( null );
                inEpoch.setText( null );
                outEpoch.setText( null );
                units = 'H';
                hourButton.setSelected( true );
                resolution = 'M';
                mediumButton.setSelected( true );
            } else if ( arg.equals( CLOSE ) ) {
// Exit
                 trace( task + " killed");
                 System.exit(0);
            }
        } else

// Change to Coordinate field
        if ( source instanceof JTextField ) {
            trace( "Text Field: " + arg );
            coords = inputfield.getText().trim();
            AngleDR r = coco.validate( coords );
            if ( r != null ) {
                answer = coco.convert( r );
            } else {
                answer = "Invalid Coordinate: " + r;
            }
            outputfield.setText( answer );
            outputfield.setForeground( Color.blue );
        }
        Pane.validate();
    }

// To trace program flow messages (for testing)
    private void trace( String message ) {
        if ( TRACE ) System.err.println( "[Coco] " + message );
        return;
    }

// To print debug messages (for testing)
    private void printDebug( String message ) {
        if ( DEBUG ) System.err.println( "[Coco Debug] " + message );
        return;
    }

// Get todays date
    private String getToday( ) {
        return getToday( true );
    }

    private String getToday( boolean decimal ) {
        DateFormat currentDateFormat =
                DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK);
        Date currentDate = new Date();
        Calendar today = Calendar.getInstance();
        int day = today.get(today.DATE);
        int month = today.get(today.MONTH) + 1;
        int year = today.get(today.YEAR);
        int days = today.get(today.DAY_OF_YEAR);

        if ( decimal ) return year + "." + days;
        else return currentDateFormat.format(currentDate);
    }
}


