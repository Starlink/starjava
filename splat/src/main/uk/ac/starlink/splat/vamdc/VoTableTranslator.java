package uk.ac.starlink.splat.vamdc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import gavo.spectral.ssldm.PhysicalQuantity;
import gavo.spectral.ssldm.SpectralLine;

public class VoTableTranslator {
    ArrayList<String> tableLines;
    String[] columns = { "title", "wavelength", "initial energy", "final energy", "einsteinA", "initial level", "final level", "air wavelength", "oscillator strength","weighted oscillator strength"}; 
    ArrayList<SpectralLine> data;
    String query;
    
    public VoTableTranslator() {
        tableLines = new ArrayList<String>();
        
    }
    
    public VoTableTranslator(ArrayList<SpectralLine> data, String query) {
        this.data=data;
        this.query=query;
        tableLines = new ArrayList<String>();
        
    }
    
    public ArrayList<String> makeTable( ArrayList<SpectralLine> data, String query) {
        this.data=data;
        this.query=query;
        return makeTable();
       
    }
    
    public ArrayList<String> makeTable() {
        openTable();
        writeInfos(query);
        writeToTable(data);
        closeVoTable();
        return tableLines;
    }
    
    private void openTable() {
        tableLines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        tableLines.add("<VOTABLE xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        tableLines.add("xsi:noNamespaceSchemaLocation=\"xmlns:http://www.ivoa.net/xml/VOTable/VOTable-1.1.xsd\"");
        tableLines.add("xmlns:ssldm =\"http://www.ivoa.net/xml/SimpleSpectrumLineDM/SimpleSpectrumLineDMv1.0.xsd\" version=\"1.0\">");
        tableLines.add("<RESOURCE type=\"results\">");
    }
    public void writeInfos(String vamdcrequest){
        tableLines.add("<DESCRIPTION>GAVO VAMDC to SLAP translator</DESCRIPTION>");
        tableLines.add("<INFO name=\"QUERY_STATUS\" value=\"OK\"/>");
        tableLines.add("<INFO name=\"SERVICE_PROTOCOL\" value=\"1.0\">SLAP</INFO>");
       // tableLines.add("<INFO name=\"VAMDCREQUEST\" value=\""+vamdcrequest+"\"/>");        
    }
    public void writeToTable(ArrayList<SpectralLine> data){
        
        SpectralLine d=data.get(0);
        tableLines.add("<TABLE>");
        //tableLines.add("<FIELD ucd=\"meta.id;\" name=\"XXXXXX\" datatype=\"char\" arraysize=\"*\"/>");
        tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:Line.title\" datatype=\"char\" arraysize=\"*\"/>");        
        tableLines.add("<FIELD ucd=\"em.wl\" name=\"wavelength\" utype=\"ssldm:Line.wavelength.value\" datatype=\"double\" "+makeUnit(d.getWavelength())+"/>");        
        tableLines.add("<FIELD ucd=\"phys.energy;phys.atmol.initial;phys.atmol.level\" name=\"initial energy\" utype=\"ssldm:Line.initialLevel.energy.value\" datatype=\"double\" "+makeUnit(d.getInitialLevel().getEnergy())+"/>");        
        tableLines.add("<FIELD ucd=\"phys.energy;phys.atmol.final;phys.atmol.level\" name=\"final energy\" utype=\"ssldm:Line.finalLevel.energy.value\" datatype=\"double\" "+makeUnit(d.getFinalLevel().getEnergy())+"/>");      
        tableLines.add("<FIELD ucd=\"phys.atmol.transProb\" name=\"einsteinA\" utype=\"ssldm:Line.einsteinA.value\" datatype=\"double\" "+makeUnit(d.getEinsteinA())+"/>");          
        //tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:Line.title\" datatype=\""makeUnit(d.)"/>");  
    //    if PhysicalEntity - check if it's char or double !!!!!!!!!!!!!!!!!
    

        tableLines.add("<FIELD ucd=\"phys.atmol.final;phys.atmol.level\" name=\"initial\" utype=\"ssldm:line.initialLevel.name\" datatype=\"char\" arraysize=\"*\"/>");        
        tableLines.add("<FIELD ucd=\"phys.atmol.final;phys.atmol.level\" name=\"final\" utype=\"ssldm:line.finalalLevel.name\" datatype=\"char\" arraysize=\"*\"/>");        
    
        tableLines.add("<FIELD ucd=\"em.wl\" name=\"air wavelength\" utype=\"ssldm:line.airWavelength.value\" datatype=\"double\" "+makeUnit(d.getAirWavelength())+"/>");        
       
        tableLines.add("<FIELD ucd=\"phys.atmol.oscStrength\" name=\"oscillator strength\" utype=\"ssldm:line.oscillatorStrength\" datatype=\"double\""+makeUnit(d.getOscillatorStrength())+"/>");        
       
        tableLines.add("<FIELD ucd=\"phys.atmol.wOscStrength\" name=\"weighted oscillator strength\" utype=\"ssldm:line.weightedOscillatorStrength\" datatype=\"double\" "+makeUnit(d.getWeightedOscillatorStrength())+"/>");        

        //  tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:line.title\" datatype=\"char\" arraysize=\"*\"/>");        
      //  tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:line.title\" datatype=\"char\" arraysize=\"*\"/>");        
      //  tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:line.title\" datatype=\"char\" arraysize=\"*\"/>");        
      //  tableLines.add("<FIELD ucd=\"em.line\" name=\"title\" utype=\"ssldm:line.title\" datatype=\"char\" arraysize=\"*\"/>");        
          writeTableData(data);
          tableLines.add("</TABLE>");
    }
    
    private String makeUnit(PhysicalQuantity pq) {
        try {
            String u= pq.getUnit().getExpression();
            if (u!=null)
                return "unit=\""+u+"\"";
        }catch(Exception e) {
        }
        return "";
        
    }
    public void writeTableData(ArrayList<SpectralLine> data){
        
        tableLines.add("<DATA><TABLEDATA>");
      
        for (int i=0;i<data.size();i++ ) {
            SpectralLine line = data.get(i);
            tableLines.add("<TR>");
            tableLines.add("<TD>"+ line.getTitle()+"</TD>"); 
            tableLines.add("<TD>"+ line.getWavelength().getValue() +"</TD>"); 
            tableLines.add("<TD>"+ line.getInitialLevel().getEnergy().getValue()+"</TD>"); 
            tableLines.add("<TD>"+line.getFinalLevel().getEnergy().getValue() +"</TD>"); 
            tableLines.add("<TD>"+ line.getEinsteinA().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getInitialElement()+"</TD>"); 
            tableLines.add("<TD>"+ line.getFinalElement()+"</TD>"); 
            tableLines.add("<TD>"+ line.getAirWavelength().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getOscillatorStrength().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getWeightedOscillatorStrength().getValue()+"</TD>"); 
            tableLines.add("</TR>");
        }
        tableLines.add("</TABLEDATA></DATA>");
    }
    public void closeVoTable() {
        tableLines.add("</RESOURCE>");
        tableLines.add("</VOTABLE>");
        
    }
    public void writeToFile(String file) throws IOException {
        BufferedWriter bw = null;
        bw = new BufferedWriter(new FileWriter(file));
        for (String l :tableLines) {
          bw.write(l);
          bw.newLine();
        }
        bw.flush();  
        bw.close();  
    }
    
    
    public void writeToStream(OutputStream out) throws IOException {
        BufferedOutputStream bw = null;
        bw = new BufferedOutputStream(out);
        for (String l :tableLines) {
          bw.write(l.getBytes());
         // bw.newLine();
        }
        bw.flush();  
        bw.close();  
    }
    
    public void writeToFile() throws IOException {
        
        JFileChooser fc = new JFileChooser();
        int returnval = fc.showOpenDialog(null);
        if (returnval == JFileChooser.APPROVE_OPTION) {
            String filename=fc.getSelectedFile().getName();
            writeToFile( filename );
        }
    }
    
   
    public String[] getColumns() {
     
        return columns;
    }
    
   
}
