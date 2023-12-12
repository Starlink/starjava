package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.splat.iface.images.ImageHolder;


public class LineZoomOptionsFrame extends JFrame implements ActionListener, DocumentListener {
	
	private JPanel zPanel;
	private JRadioButton probButton;
	private JRadioButton tempButton;
	private int temperature=-1;
	private boolean ok=false;
	private JButton submitButton;
	
	public static int PROB = 0;
	public static int TEMP = 1;
	
	private int zoomOption = 0;
	
	private JLabel okLabel;
	private ImageIcon okImage;
	private ImageIcon notOkImage; 
	private JLabel infoLabel;
	
	 private static Logger logger =
		        Logger.getLogger( "uk.ac.starlink.splat.iface.SplatBrowser" );
	
	public LineZoomOptionsFrame() throws HeadlessException {
		initUI();
	}
	private void initUI() {
		  {
		        this.setSize(400, 200);
		        okImage = new ImageIcon( ImageHolder.class.getResource( "OK.gif" ) );
		        notOkImage = new ImageIcon( ImageHolder.class.getResource( "notOK.gif" ) );
		        //
				

		          // zoom sorting parameter buttons
		          
		          probButton = new JRadioButton("EinsteinA probability (default)");
		          tempButton = new JRadioButton("Temperature Function");
		          probButton.addActionListener(this);
		          tempButton.addActionListener(this);
		          ButtonGroup zoomGroup = new ButtonGroup();
		          zoomGroup.add(probButton);
		          zoomGroup.add(tempButton);
		          probButton.setSelected(true); // default
		         
		          
		          // Panel with zoom sorting parameters
		          
		          JPanel inputPanel = new JPanel();
		          inputPanel.setBorder(BorderFactory.createTitledBorder("zoom sorting Parameters"));
		         
		          inputPanel.add(probButton, BorderLayout.PAGE_START);    
		          inputPanel.add(tempButton, BorderLayout.PAGE_END);
		        
		          // temperature input 
		          
		          JPanel tempPanel = new JPanel();
		          tempPanel.add( new JLabel("Temperature:"));
		          JTextField tempField = new JTextField(8);
				  tempField.addActionListener(this);
				  tempField.getDocument().putProperty("owner", tempField); //set the owner
				  tempField.getDocument().addDocumentListener(this);
				  tempField.setText("");

		        
		          tempPanel.add(tempField);
		          
		          // submit and feedback panel
		          JPanel submitPanel = new JPanel();
		          submitButton = new JButton("Set parameters");
		          submitButton.addActionListener(this);
		          submitButton.setName("setButton");		        		         
		          okLabel = new JLabel();
		          submitPanel.add(submitButton, BorderLayout.PAGE_START);
		          submitPanel.add(okLabel, BorderLayout.LINE_END);
		          
		          // information line Panel
		          infoLabel = new JLabel("set parameters to make changes");

		          // add all to main panel
		      
		        zPanel = (JPanel) this.getContentPane();
		       //zPanel.setPreferredSize(new Dimension(400,300));
		       
		        zPanel.setBorder(BorderFactory.createTitledBorder("Line zoom options"));		        		        
		        zPanel.setLayout(new GridBagLayout());
		         
		        GridBagConstraints gbc = new GridBagConstraints();
		        gbc.anchor = GridBagConstraints.NORTHWEST;
		        gbc.fill = GridBagConstraints.HORIZONTAL;
		        gbc.insets = new Insets(5,0,0,0);
		        gbc.gridwidth=1;	            
	            gbc.weighty=0;
	            gbc.weightx=0;
	            
	            gbc.gridx=0;
	            gbc.gridy=0;
	            	                  
		        zPanel.add(inputPanel, gbc);
		          
		        gbc.gridy=1;
		        zPanel.add(tempPanel, gbc);
		          
		       
		        gbc.gridy=2;
		        zPanel.add(submitPanel, gbc);
		          
		        gbc.gridy=3;
		        zPanel.add(infoLabel, gbc);
		            
		       
		           
		    }
		
		
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		
		Object owner = e.getDocument().getProperty("owner");
	    JTextField param=(JTextField) owner;
	    String tempText=param.getText();
	    ok=true;
	    try{
            temperature = Integer.parseInt(tempText);
        }
        catch (NumberFormatException ex){
        	ok = false;
        	temperature = -1;
          // param.setText(tempText());
        	// TODO do some warning stuff
        }
	        
	}
	
	@Override
	public void removeUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void changedUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void actionPerformed(ActionEvent e) {
        Component source = (Component) e.getSource();   
        
        if (source == submitButton) {
        	if ( temperature <0 && zoomOption == TEMP) {
        	   okLabel.setIcon(notOkImage);
        	   infoLabel.setText("temperature must me set");
        	}
        	else {
        		 okLabel.setIcon(okImage);        		
        		 infoLabel.setText("set parameters to make changes");        		 
        	}       	   
        	this.firePropertyChange("zoomOptions", (float) 0, (float) 1);
        	
        }
        if (source == probButton || source == tempButton ) {
        	if ( probButton.isSelected() ) {
        		zoomOption = PROB;      
        		
        		
        	} else if (tempButton.isSelected() ) {
        		zoomOption = TEMP;    		
        	}
        	okLabel.setIcon(null);
        	
        }
       
        
	}
	
	public int getTemperature() {
		if (ok)
			return temperature;
		else return -1;
	}
	
	public int getZoomOption() {
		
		return zoomOption;
	}
	public boolean getStatusOK() {
		
		return (ok || (zoomOption == PROB));
	}


}
