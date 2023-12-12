package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import uk.ac.starlink.splat.vo.TapQueryWithDatalink;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.ContentCoding;

@SuppressWarnings("serial")
public class AutoFillCombo  extends JPanel implements ActionListener, DocumentListener{

	JTextField textf;
	JComboBox<String> matchesBox;

	Boolean elementChosen=false;
	Boolean makeQuery=false;
	Map <String,String> speciesInChiKey = null;

	public AutoFillCombo( String label, boolean querySpecies ) {
		
		 makeQuery= querySpecies;
		
		 GridBagConstraints gbc = new GridBagConstraints();
         gbc.anchor = GridBagConstraints.PAGE_START;
         gbc.fill = GridBagConstraints.HORIZONTAL; 
         this.setLayout(new GridBagLayout());
		
		JLabel lbl= new JLabel (label) ;
		
		textf = new JTextField( 25);
		textf.addActionListener(this);
		textf.getDocument().putProperty("owner", textf);
	    textf.getDocument().addDocumentListener( this );
	      
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(lbl, BorderLayout.WEST);
	    panel.add(textf,  BorderLayout.CENTER);
		matchesBox = new JComboBox<String>();
		matchesBox.setMaximumSize(this.getSize());
		matchesBox.addActionListener(this);
		//matchesBox.addItemListener(this);
		
		
		
		gbc.gridx=0;
		gbc.gridy=0;
		
		this.add (panel, gbc);
		gbc.gridy=1;
		this.add (matchesBox, gbc);
	
		
	}
	
	private void updateCombo(ArrayList<String> results ) {
		
		ComboBoxModel<String> results2;
		
		if (results.size()==0) {
			
			//matchesBox.removeAllItems();
			matchesBox.addItem("");
			matchesBox.setSelectedItem("");
		} else {
		    //results2 = new DefaultComboBoxModel(results.toArray()) ;
		   
		   // matchesBox.setModel( results2 ) ;	
		    matchesBox.setModel ( new DefaultComboBoxModel(results.toArray()));
			matchesBox.addItem("");	    
		    matchesBox.showPopup();
	    }
		
	}
	
	public String getElement(  ) {

		return textf.getText();
	}
	
	public String getInChiKey(  ) {
		if ( makeQuery)
			try {
				String spcs = textf.getText();
				if (! (spcs == null) && ! spcs.isEmpty())
					return speciesInChiKey.get( textf.getText()) ;
			} catch ( Exception e) {
				return "";
			}
		return "";
	}


	private void updateCombo(Map<String, String> results ) {
		
		speciesInChiKey = results;
		List<String>  resultList =  new ArrayList<>(results.keySet());
		
		if (resultList.size()==0) {
			
			//matchesBox.removeAllItems();
			matchesBox.addItem("");
			matchesBox.setSelectedItem("");
		} else {

			ComboBoxModel<String> results2 = new DefaultComboBoxModel(resultList.toArray()) ;
			matchesBox.setModel( results2 ) ;
			matchesBox.addItem("");
			matchesBox.showPopup();
		}
	}
    private void resetCombo()  {
		
        matchesBox.removeAllItems();
      //  matchesBox.addActionListener(this);
       // matchesBox.addItem("");
       // matchesBox.setSelectedItem("");
       // textf.setText("");
       // this.firePropertyChange("AutoFillCombo", true, false );

	}
	


	static class Elements {



		static final List<String> elementSymbol = Arrays.asList (new String[] {

				"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K",
				"Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb",
				"Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs",
				"Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta",
				"W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa",
				"U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt",
				"Ds", "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og"

		});

		static final List<String> elementName = Arrays.asList (new String[] {
				"Hydrogen", "Helium", "Lithium", "Beryllium", "Boron", "Carbon", "Nitrogen", "Oxygen", "Fluorine", "Neon",
				"Sodium", "Magnesium", "Aluminum", "Silicon", "Phosphorus", "Sulfur", "Chlorine", "Argon", "Potassium",
				"Calcium", "Scandium", "Titanium", "Vanadium", "Chromium", "Manganese", "Iron", "Cobalt", "Nickel",
				"Copper", "Zinc", "Gallium", "Germanium", "Arsenic", "Selenium", "Bromine", "Krypton", "Rubidium", "Strontium",
				"Yttrium", "Zirconium", "Niobium", "Molybdenum", "Technetium", "Ruthenium", "Rhodium", "Palladium", "Silver",
				"Cadmium", "Indium", "Tin", "Antimony", "Tellurium", "Iodine", "Xenon", "Cesium", "Barium", "Lanthanum",
				"Cerium", "Praseodymium", "Neodymium", "Promethium", "Samarium", "Europium", "Gadolinium", "Terbium", "Dysprosium",
				"Holmium", "Erbium", "Thulium", "Ytterbium", "Lutetium", "Hafnium", "Tantalum", "Tungsten", "Rhenium", "Osmium",
				"Iridium", "Platinum", "Gold", "Mercury", "Thallium", "Lead", "Bismuth", "Polonium", "Astatine", "Radon",
				"Francium", "Radium", "Actinium", "Thorium", "Protactinium", "Uranium", "Neptunium", "Plutonium", "Americium",
				"Curium", "Berkelium", "Californium", "Einsteinium", "Fermium", "Mendelevium", "Nobelium", "Lawrencium", 
				"Rutherfordium", "Dubnium", "Seaborgium", "Bohrium", "Hassium", "Meitnerium", "Darmstadtium", "Roentgenium",
				"Copernicium", "Nihonium", "Flerovium", "Moscovium", "Livermorium", "Tennessine", "Oganesson"
		});
		
	
		
		private static ArrayList<String> getMatches(String pref) {
			
	
			ArrayList <String> results = new ArrayList<String>();
			String prefl = pref.toLowerCase().trim();
			
			
			for (int i=0;i< elementSymbol.size(); i++) {
				String name = elementName.get(i);
				String symbol = elementSymbol.get(i);
				String result = symbol + "-" + name;
				 if (name.toLowerCase().startsWith(prefl)) {
					 results.add(result);
				 }  
				 if (symbol.toLowerCase().startsWith(prefl) && !results.contains(result)) {
					 results.add(result);
				 }
				
			}
			return  results;
			
		} // getMatches


	} // Elements
	
	static class SpeciesQuery {
		
		private static String SPECIESDB_URL = "http://dc.zah.uni-heidelberg.de/tap";
		private static String SPECIES_TABLE = "species.main";
	
		
		private static StarTable querySpecies( String pref ) {
			
		 
			
		   	TapQueryWithDatalink tq;
	    	StarTable startable;
	    	
	    	String query = "SELECT DISTINCT name, inchikey FROM "+SPECIES_TABLE+" WHERE name ILIKE '%"+pref+"%' OR FORMULA ILIKE '%"+pref+"'" ;
	       
	    	try {
				tq =  new TapQueryWithDatalink( new URL(SPECIESDB_URL), query,  null );
			} catch (MalformedURLException e) {
			
				e.printStackTrace();
				return null;
			}
	    	StarTableFactory tfact = new StarTableFactory();
	    	try {
				startable = tq.executeSync( tfact.getStoragePolicy(), ContentCoding.NONE );
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
	    	return startable;

		
		}
		
		private static Map<String,String>  getMatches(String pref) {
			
		//	ArrayList <String> results = new ArrayList<String>();
			Map <String, String> results = new HashMap<String,String>();
			
			StarTable st = querySpecies(pref);
			String prefl = pref.toLowerCase();
			
			
			for (int i = 0; i < st.getRowCount(); i++) {  // Loop through the rows						
		        // name, inchi, inchikey
				try {
					results.put( (String) st.getCell(i,0),(String) st.getCell(i,1));
				
				} catch (IOException e) {
					
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return  results;
			
		} // getMatches
		
		
	} // SpeciesQuery

	@Override
	public void insertUpdate(DocumentEvent e) {
		updateAction(e);
		
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
	//	if (elementChosen)
	//		updateAction(e);
	//	if (textf.getText().isEmpty() )
	//		resetCombo();
	//	else 
			updateAction(e);
		
	}

	@Override
	public void changedUpdate(DocumentEvent e) {		
		updateAction(e);

	}
  
	protected void updateAction(DocumentEvent e) {
		  ArrayList<String> result = new ArrayList<String>();
		  Map<String, String> resultMap;
		  
				  
		  
		  if (elementChosen==true) {
			  elementChosen = false;
			  return;
		  }
		  Object owner = e.getDocument().getProperty("owner");
		  if(owner != null && owner.getClass() == JTextField.class) {
			  String text = textf.getText();

			  if ( text != null && ! text.isEmpty() ) {
				  if ( makeQuery ) {
					  try {
						  resultMap = SpeciesQuery.getMatches(text);
						 
					  }
					  catch (Exception e1) {

						  return;
					  }
					 
					  updateCombo(resultMap);

				  } else {
					  try {
						  result = Elements.getMatches(text);
					  }
					  catch (Exception e1) {

						  return;
					  }
					  
					  updateCombo(result);
					 
				  }

				
			  }
			  else
				  matchesBox.removeAllItems();
	      
	      }
		  
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == matchesBox) {
			try {
				
				String chosen = (String) matchesBox.getSelectedItem();
				if (chosen == null)
					chosen="";
				if (!chosen.isEmpty())
				   elementChosen=true;
				textf.setText( chosen);
				
				
			} catch (Exception ex) {}
		}
		this.firePropertyChange("AutoFillCombo", true, false );


	}

/*	@Override
	public void itemStateChanged(ItemEvent e) {
		int state = e.getStateChange();
		System.out.println((state == ItemEvent.SELECTED) ? "Selected" : "Deselected");
		System.out.println("Item: " + e.getItem());
		ItemSelectable is = e.getItemSelectable();
		Object [] selected =   is.getSelectedObjects();
		if (selected.length > 0) {
			String sel = selected[0].toString();
			System.out.println(", Selected: " + sel);
			textf.setText(sel);
		}	        
	}
*/

}

