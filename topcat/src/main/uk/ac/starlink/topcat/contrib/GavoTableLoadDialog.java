
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;
import uk.ac.starlink.table.gui.LabelledComponentStack;

public class GavoTableLoadDialog extends BasicTableLoadDialog {

    private List queries = new Vector();
    private CSVTableParser csvParser = new CSVTableParser();
    public class GAVOTableSupplier implements TableSupplier{
     final StarTable table;
        GAVOTableSupplier(StarTable table)
        {
            this.table = table;
        }
        
        public StarTable getTable(StarTableFactory arg0, String arg1)
                throws IOException {
            // TODO Auto-generated method stub
            return this.table;
        }
        public String getTableID() {
            // TODO Auto-generated method stub
            return table.getName();
        }
    }
    
    private final JComboBox urlField_;
    private final JTextField userField_;
    private final JPasswordField passField_;
    private final JEditorPane sqlField_;

    /**
     * Constructor.  A public no-arg constructor is required by STIL's
     * pluggable load dialogue mechanism.
     */
    public GavoTableLoadDialog() {
        super( "GAVO query", "Queries GAVO database" );

        /* Set up fields for user interaction. */
        String[] availableURLs = new String[]{
            "http://www.g-vo.org/MyMillennium?action=doQuery&SQL=",
            "http://www.g-vo.org/Millennium?action=doQuery&SQL="
        }; // TODO may invent nicer configuration
        urlField_ = new JComboBox(availableURLs);
        urlField_.setEditable(true);
        userField_ = new JTextField();
        passField_ = new JPasswordField();
        sqlField_ = new JEditorPane();
        sqlField_.setEditable( true );

        /* Arrange them in a GUI container. */
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "Query URL", urlField_ );
        stack.addLine( "User", userField_ );
        stack.addLine( "Password", passField_ );
//        stack.addLine( "Is numeric", isNumericCB_ );
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

        JComponent sqlHolder = new JPanel( new BorderLayout() );
        Box labelBox = Box.createVerticalBox();
        labelBox.add( new JLabel( "SQL Query: " ) );
        labelBox.add( Box.createVerticalGlue() );
        sqlHolder.add( labelBox, BorderLayout.WEST );
        sqlHolder.add( new JScrollPane( sqlField_ ), BorderLayout.CENTER );
        sqlHolder.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );

        /* Place the container in the body of this component for display. */
        setLayout( new BorderLayout() );
        add( stack, BorderLayout.NORTH );
        add( sqlHolder, BorderLayout.CENTER );
        setPreferredSize( new Dimension( 400, 300 ) );
    }


    /**
     * Indicates whether this dialogue is available.  Should return false
     * if you happen to know somehow that that this service is unavailable.
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Interrogates the internal state of this component and returns a
     * TableSupplier object.
     */
    protected TableSupplier getTableSupplier() {

        /* Get state. */
        final String url = (String)urlField_.getSelectedItem();
        final String user = userField_.getText();
        final String pass = new String( passField_.getPassword() );
        final String sql = sqlField_.getText();
        queries.add(sql);

        /* Validate state, throwing an IllegalArgumentException if we can
         * tell straight away that this isn't going to result in a readable
         * table. */
        if ( url == null || url.trim().length() == 0 ||
             user == null || user.trim().length() == 0 ||
             pass == null || pass.trim().length() == 0 ||
             sql == null || sql.trim().length() == 0 ) {
            throw new IllegalArgumentException(
                "All fields must be filled in" );
        }

        /* Construct an object which can read the data.  DataSource is 
         * basically a way of wrapping a stream so that it can be re-read.
         * CSV files need to be read in two passes (once to determine the
         * type of data in each column, and once to read the actual data).
         * If the data was returned in a format such as FITS or VOTable 
         * there would be more efficent ways of doing it. */
        // GL: replaced original code with a straightforward parse of the stream by
        // the custom parser tailored ot the streams coming form the
        // GAVO web databases
        
        try {
                String sqlEncoding = URLEncoder.encode(sql, "UTF-8");
                String userPassword = user + ":" + pass;
                String encoding = new sun.misc.BASE64Encoder().encode (userPassword.getBytes());
                String urlString = url+sqlEncoding;
                URLConnection uc = new URL( urlString ).openConnection();
                uc.setRequestProperty ("Authorization", "Basic " + encoding);
                InputStream stream = uc.getInputStream();
                StarTable starTable;
                System.out.println("Content type = "+uc.getContentType());
                starTable = csvParser.parse(stream);
                starTable.setName("Query "+queries.size());
                return new GAVOTableSupplier(starTable);
        }
        catch(IllegalArgumentException iae)
        {
            throw iae;
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Configures the components of this panel to match the enabledness of
     * this panel itself.
     *
     * @param  enabled  enabled status
     */
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        urlField_.setEnabled( enabled );
        userField_.setEnabled( enabled );
        passField_.setEnabled( enabled );
        sqlField_.setEnabled( enabled );
    }
}
