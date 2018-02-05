package uk.ac.starlink.splat.vo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.splat.iface.IntervalField;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.ValuesElement;

public class DataLinkQueryFrame extends JFrame implements ActionListener, DocumentListener {

    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.vo.DataLinkQueryFrame" );

    private HashMap< String, DataLinkServices > servers; // one entry for each datalink or soda  supporting server
    /** The list of all dataLink parameters read from the servers as a hash map */
    /** List of all possible parameters **/
     private String currentServer = null; // HashMap for accessURL and idSourcem key=server!!
    
    Boolean isActive;
    // Panel for components and options from dataLink parameters
    private JPanel dataLinkPanel;
    private JPanel paramPanel;
   // private JScrollPane  dataLinkScroller; 
    private JButton submitButton;
    private JButton clearButton;
    private JButton displayButton = null;
    
    private ArrayList<Component> queryComponents;
    private JComboBox optbox;

    private JLabel okLabel;

    private ImageIcon okImage;

    private ImageIcon notOkImage; 
   
    
    public DataLinkQueryFrame() {
        isActive = false;
        servers = new HashMap< String, DataLinkServices >();    
        queryComponents = new ArrayList<Component>();
        
        initUI();        
    }
    
    public DataLinkQueryFrame( JButton display ) {
    	displayButton= display;
    	displayButton.setEnabled(false);
    	
        isActive = false;
        servers = new HashMap< String, DataLinkServices >();    
        queryComponents = new ArrayList<Component>();
        
        initUI();        
    }
   
    
    public void addServer( String server, DataLinkServices dlparams ) {
      //  DataLinkParams dlp = servers.get(server);
      //  if (dlp == null) {          
            servers.put(server, dlparams);           
      //  }
    }
    
    public void addServer( String server, DataLinkServiceResource dlr ) {
         DataLinkServices dlp = servers.get(server);
         if (dlp == null) {      
        	 dlp = new DataLinkServices(dlr);
         }
         
         servers.put(server, dlp);           
     
      }
    
    protected void reset() // is it still needed? 
    {
 
        servers.clear();
 //       dataLinkParam.clear();
        queryComponents.clear();

    }
    
    protected void initUI()
    {
        this.setSize(500, 230);
        okImage = new ImageIcon( ImageHolder.class.getResource( "OK.gif" ) );
        notOkImage = new ImageIcon( ImageHolder.class.getResource( "notOK.gif" ) );

             dataLinkPanel = (JPanel) this.getContentPane();
          //  dataLinkPanel.setPreferredSize(new Dimension(480,180));
            dataLinkPanel.setLayout(new GridBagLayout());
         
            GridBagConstraints gbc = new GridBagConstraints();
          
            
            dataLinkPanel.setAlignmentY(CENTER_ALIGNMENT);
            dataLinkPanel.setBorder(BorderFactory.createTitledBorder("Parameters for Server-Generated data processing"));
            Border empty = BorderFactory.createEmptyBorder(); 
   
            
            paramPanel = new JPanel();
            paramPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
         //   paramPanel.setMinimumSize(new Dimension(350,250));
            paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
         
            paramPanel.setAlignmentY(CENTER_ALIGNMENT);
               
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setBorder(empty);
           
            clearButton = new JButton("Clear parameters");
            clearButton.addActionListener(this);
            clearButton.setName("clearButton");
           // clearButton.setMargin(new Insets(2,10,2,10));  
            buttonsPanel.add(clearButton);
            
            submitButton = new JButton("Set parameters");
            submitButton.addActionListener(this);
            submitButton.setName("setButton");
           // clearButton.setMargin(new Insets(2,10,2,10));  
            buttonsPanel.add(submitButton);
            
            if (displayButton != null)
            	buttonsPanel.add(displayButton);
            
            okLabel = new JLabel();
            buttonsPanel.add(okLabel);
            
            
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx=0;
            gbc.gridy=0;
            gbc.weighty=0;
            gbc.weightx=0;
            gbc.fill=GridBagConstraints.HORIZONTAL;
            dataLinkPanel.add(paramPanel, gbc); //(paramPanel);
            
           
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridx=0;
            gbc.gridy=1;
            gbc.weighty=1;
            gbc.weightx=1;
            gbc.fill=GridBagConstraints.NONE;
        
            dataLinkPanel.add(buttonsPanel, gbc);
           
    }

    public void addToUI(String shortname, DataLinkServices dlp ) {
    	
    	  addToUI(shortname, dlp.getSodaService(), null);  
    	
    }
 
    public void addToUI(String shortname, DataLinkServiceResource service, JButton display ) {
                 
           paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.Y_AXIS));
           Border empty = BorderFactory.createEmptyBorder(); 
           paramPanel.setBorder(empty);
           paramPanel.setAlignmentY(CENTER_ALIGNMENT);
           
        
       
               JPanel servicePanel = initServicePanel(service);               

               if ( servicePanel.getComponentCount() > 0 ) { // !!!!! for the moment, add only parameters for visible query components!
                  paramPanel.add(servicePanel);
               }
               currentServer = shortname;
               
           
           
    } // addToUI
    
	private JPanel initServicePanel(DataLinkServiceResource service) {


		ParamElement[] sodaParams = service.getQueryParams();

		JPanel servicePanel = new JPanel();

		servicePanel.setBorder(BorderFactory.createLineBorder(Color.gray));
		servicePanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		int i=0;  
		while ( i < sodaParams.length ) {

			c.gridx=0;
			c.gridy=i;
			c.gridwidth=1;
			c.weightx=0.5;
			String paramName = sodaParams[i].getName();
			JLabel paramLabel=new JLabel(paramName+" : ");
			servicePanel.add(paramLabel, c);

			String description = sodaParams[i].getDescription();                  
			String value = sodaParams[i].getValue();
			String unit = sodaParams[i].getUnit();
			String xtype = sodaParams[i].getXtype();
			long[] arraysize = sodaParams[i].getArraysize();

			String[] paramValue=service.getQueryParamValue(paramName);

			ValuesElement values = (ValuesElement) sodaParams[i].getChildByName("VALUES");
			String [] options = null;

			if (values != null )
				options = values.getOptions();


			if ( options != null && options.length > 0 ) {
				JComboBox optbox = new JComboBox(options);
				c.gridx=1;
				c.gridwidth=2;
				c.weightx=0.0;
				if (paramName.equals("FORMAT")) { // choose best format for SPLAT as default

					if (value != null && ! paramValue[0].isEmpty())
						value=paramValue[0];
					else {
						for (i=0;i<options.length; i++) {
							if ( options[i].contains("application/x-votable")) {
								value = options[i];
							} else if ( value == null && options[i].contains("application/fits")) {
								value = options[i];
							}
						}
						service.setDefaultFormat(value);
					}
				} 
				else  {
					optbox.addItem("");
					if (value != null && ! paramValue[0].isEmpty())
						value=paramValue[0];
					else 
						value="";
				}
				optbox.setSelectedItem(value);
				optbox.setMaximumSize( optbox.getPreferredSize() );

				//   optbox.setName("gd:"+shortname+":"+paramName);
				optbox.setName(paramName);
				optbox.addActionListener(this);
				if (description.length() > 0)
					optbox.setToolTipText(description);

				servicePanel.add(optbox, c);
				queryComponents.add(optbox);

			} else if ( values != null ) {

				c.gridx=1;
				c.gridwidth=1;                 
				c.weightx=0.0;
				if ( xtype != null && xtype.equalsIgnoreCase("interval") && arraysize != null && arraysize[0]!=1) {
					c.weightx=1.0;
					IntervalField interval = new IntervalField();                           
					interval.setName(paramName);
					if (paramValue.length == 2) {
						interval.setInterval(paramValue[0], paramValue[1]);
					}
					servicePanel.add(interval, c);
					queryComponents.add(interval); 

				}
				else {

					JTextField paramField = new JTextField(8);

					if (description != null && description.length() > 0) {
						paramField.setToolTipText(description);
					}                           
					paramField.setName(paramName);
					paramField.addActionListener(this);
					paramField.getDocument().putProperty("owner", paramField); //set the owner
					paramField.getDocument().addDocumentListener(this);
					paramField.setText(paramValue[0]);
					Dimension size=new Dimension(paramField.getPreferredSize().width, paramField.getPreferredSize().height+5);
					paramField.setMinimumSize(size);
					paramField.setMaximumSize(size);
					servicePanel.add(paramField, c);
					queryComponents.add(paramField);
				}

				String min=null, max=null;
				VOElement constraint = values.getChildByName("MIN"); 
				try {
				    min=numberformat( constraint.getAttribute("value") );
				} catch (Exception e) {
					// do nothing
				}

				constraint = values.getChildByName("MAX"); 
				try {
					max=numberformat(constraint.getAttribute("value")); 
				} catch (Exception e) {
					// do nothing
				}

				String info="";
				if (min != null || max != null) 
					info = String.format("["+min+".."+max+"]");
				else if (min != null)
					info = min;
				else if (max != null)
					info = max;
				if ( unit != null && !unit.isEmpty())
					info += "   "+unit;

				JLabel infoLabel = new JLabel(info);
				c.weightx=1.0;
				c.gridx=2;
				servicePanel.add(infoLabel, c);

			} // else      
			i++;
		} //while
		return servicePanel;
	}

    
    private String numberformat(String s) {
        
        
        DecimalFormat df = new DecimalFormat("#.####E0");
        Double d = Double.parseDouble(s);
       
        if (d % 1.0 != 0) 
                return df.format(d);        
        else 
            return s;
    }
    
    public String  getIdSource(String server) {
         return  servers.get(server).getIdSource();
    }
    public String  getServiceID(String server) {
        return  servers.get(server).getServiceId();
   }
    public String  getDataLinkLink(String server) {
        return  servers.get(server).getDataLinkLink();
   }
    public String  getSodaFieldRefID(String server) {
        return  servers.get(server).getSodaFieldRefID();
   }

    public void actionPerformed(ActionEvent e) {
       
        //
        // dynamically generated dataLink parameters
        //
        
        Component source = (Component) e.getSource();   
        
        okLabel.setIcon(null);
        if (source.equals(clearButton)) {
           
            //DataLinkServices serverparam = servers.get(currentServer);


            for (int i = 0; i < queryComponents.size(); i++) 
            {      
                Component c = queryComponents.get(i);
                if (c instanceof JComboBox) {
                    JComboBox cb = (JComboBox) c;              
                    cb.setSelectedItem("");
                }
                if (c instanceof JTextField) {
                    JTextField tf = (JTextField) c;
                    tf.setText("");
                }
                if (c instanceof IntervalField) { // interval
                    IntervalField intp = (IntervalField) c;
                    intp.clear();
                }
            }
            okLabel.setIcon(okImage);
           
            setQueryParams();
            if ( displayButton != null)
            	displayButton.setEnabled(true);
            
            
        } else if (source.equals(submitButton)) {
            
            boolean ok = true;
            
            for (int i = 0; i < queryComponents.size(); i++) 
            {      
                Component c = queryComponents.get(i);
                if (c instanceof JComboBox) {
                    JComboBox cb = (JComboBox) c;
                    String name = cb.getName();
                   
                    // if ( cb.getSelectedItem().toString().length() > 0)
                  
                }
                if (c instanceof JTextField) {
                    JTextField tf = (JTextField) c;
                    String name = tf.getName();
                    // to do: consistency checking, add values/ranges
               

                }
                if (c instanceof IntervalField) { // interval
                    IntervalField intp = (IntervalField) c;
                    String name = intp.getName();
                    // to do: consistency checking, add values/ranges
               }
                
            } // for    

            if (ok) {
                okLabel.setIcon(okImage);
                if ( displayButton != null)
                	displayButton.setEnabled(true);
                setQueryParams();
            }
           
           
            else {
                okLabel.setIcon(notOkImage);
                if ( displayButton != null)
                	displayButton.setEnabled(false);
            }
            
        } 
        dataLinkPanel.updateUI();
    }
    
    
    private void setQueryParams() {
        DataLinkServices dlp = servers.get(currentServer);
        for (int i = 0; i < queryComponents.size(); i++) 
        {      
            Component c = queryComponents.get(i);
            String name = c.getName();
            if (c instanceof JComboBox) {
                JComboBox cb = (JComboBox) c;              
                dlp.setQueryParam(name, (String) cb.getSelectedItem());
            }
            if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                dlp.setQueryParam(name, (String) tf.getText());
            }
            if (c instanceof IntervalField) { // interval
                IntervalField intp = (IntervalField) c;
                dlp.setQueryParam(name, intp.getLower(), intp.getUpper());
            }
        }
        
        
    }

    public String setServer(String server) {//, HashMap<String,String> params) {
        
        paramPanel.removeAll();
        okLabel.setIcon(null);
        if (servers.get(server) == null)
            return null;
        currentServer=server;
        queryComponents.clear();
        addToUI(server, servers.get(server));

        paramPanel.updateUI();
        dataLinkPanel.revalidate();
        dataLinkPanel.repaint();
        return ("OK");
        
    }
    

    
    public DataLinkServices getServerParams(String currentServer) {
        
        if (servers == null )
            return null;
        return servers.get(currentServer);
    }

    public void changedUpdate(DocumentEvent arg0) {
     // Plain text components don't fire these events.
        okLabel.setIcon(null);
        okLabel.updateUI();
    }

    public void insertUpdate(DocumentEvent de) {
        
        okLabel.setIcon(null);
        okLabel.updateUI();
        dataLinkPanel.updateUI();
        
        
        //get the owner of this document
      Object owner = de.getDocument().getProperty("owner");
        JTextField param=(JTextField) owner;
        
    }

    public void removeUpdate(DocumentEvent arg0) {
        // TODO Auto-generated method stub
        okLabel.setIcon(null);
        okLabel.updateUI();
        dataLinkPanel.updateUI();
 
    }

    /** get the list of datalink supporting services **/
    public String[] getServers() {
               
        Set<String>  s = servers.keySet();
        return (String[]) s.toArray(new String[s.size()]);
    }

 /*   
    class IntervalField extends JPanel {
        JTextField _lower;
        JTextField _upper;
        
        IntervalField () {
            _lower=new JTextField(8);
            _upper=new JTextField(8);
          

            this.add(_lower);
            this.add(new JLabel("/"));
            this.add(_upper);
        }
        
      

        public void setInterval(String interval) {
            if (interval == null || interval.isEmpty()) {
                _upper.setText("");
                _lower.setText("");
            }
            String[] values = interval.split("/", 1);
            _lower.setText(values[0]);
            if (values.length>1)
                _upper.setText(values[1]);
            else
                _upper.setText("");
        }
        
        public void setInterval(String min, String max) {
           
                _upper.setText(max);
                _lower.setText(min);
        
        }

        protected String getLower() {
            return _lower.getText();
        }
        
        protected String getUpper() {
            return _upper.getText();
        }
        
        private String getText() {
            String value="";
            String   txt=_lower.getText();
            if ( txt == null || txt.isEmpty())           
//                txt="-Inf";
                txt="";
            value=txt+"/";
            txt=_upper.getText();
            if ( txt == null || txt.isEmpty())           
//                txt="+Inf";
                txt="";
            value+=txt;  
            
            return value;   
        }
        void clear() {
            _lower.setText("");
            _upper.setText("");
        }
        
    }
*/

	
    
}
