//
// Copyright 1999-2000 - Universite Louis Pasteur / Centre National de la
// Recherche Scientifique
//
// ------
//
// CDS XML Parser
//
// Author:  Pierre Fernique
// Address: Centre de Donnees astronomiques de Strasbourg
//          11 rue de l'Universite
//          67000 STRASBOURG
//          FRANCE
// Email:   fernique@astro.u-strasbg.fr, question@simbad.u-strasbg.fr
//
// -------
//
// In accordance with the international conventions about intellectual
// property rights this software and associated documentation files
// (the "Software") is protected. The rightholder authorizes :
// the reproduction and representation as a private copy or for educational
// and research purposes outside any lucrative use,
// subject to the following conditions:
//
// The above copyright notice shall be included.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON INFRINGEMENT,
// LOSS OF DATA, LOSS OF PROFIT, LOSS OF BARGAIN OR IMPOSSIBILITY
// TO USE SUCH SOFWARE. IN NO EVENT SHALL THE RIGHTHOLDER BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
// THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// For any other exploitation contact the rightholder.
//
//                        -----------
//
// Conformement aux conventions internationales relatives aux droits de
// propriete intellectuelle ce logiciel et sa documentation sont proteges.
// Le titulaire des droits autorise :
// la reproduction et la representation a titre de copie privee ou des fins
// d'enseignement et de recherche et en dehors de toute utilisation lucrative.
// Cette autorisation est faite sous les conditions suivantes :
//
// La mention du copyright portee ci-dessus devra etre clairement indiquee.
//
// LE LOGICIEL EST LIVRE "EN L'ETAT", SANS GARANTIE D'AUCUNE SORTE.
// LE TITULAIRE DES DROITS NE SAURAIT, EN AUCUN CAS ETRE TENU CONTRACTUELLEMENT
// OU DELICTUELLEMENT POUR RESPONSABLE DES DOMMAGES DIRECTS OU INDIRECTS
// (Y COMPRIS ET A TITRE PUREMENT ILLUSTRATIF ET NON LIMITATIF,
// LA PRIVATION DE JOUISSANCE DU LOGICIEL, LA PERTE DE DONNEES,
// LE MANQUE A GAGNER OU AUGMENTATION DE COUTS ET DEPENSES, LES PERTES
// D'EXPLOITATION,LES PERTES DE MARCHES OU TOUTES ACTIONS EN CONTREFACON)
// POUVANT RESULTER DE L'UTILISATION, DE LA MAUVAISE UTILISATION
// OU DE L'IMPOSSIBILITE D'UTILISER LE LOGICIEL, ALORS MEME
// QU'IL AURAIT ETE AVISE DE LA POSSIBILITE DE SURVENANCE DE TELS DOMMAGES.
//
// Pour toute autre utilisation contactez le titulaire des droits.
//

package cds.xml;

import java.util.Hashtable;

/**
 * Interface of an XML event consumer.
 *
 * Rq: This interface has been inspired by SAX interface. The only difference
 * is that the tag parameter are memorized in a java Hashtable object
 * instead of the AttrList SAX object.
 *
 *
 * Example of usage :
 *
 * <PRE>
 *
 * import cds.xml.*;
 * import java.io.*;
 * import java.util.*;
 *
 * // Simple class to test the XML parser.
 * // Usage: java ParseDemo file.xml
 * public class ParseDemo implements XMLConsumer {
 *    XMLParser xp;
 *
 *    // Creat and launch the XML parsing
 *    ParseDemo(DataInputStream dis) {
 *       xp = new XMLParser(this);
 *       if( !xp.parse(dis) ) System.err.println( xp.getError() );
 *    }
 *
 *    // Method called for each start XML tag
 *    public void startElement(String name, Hashtable atts) {
 *       System.out.println("Begins tag: "+name);
 *
 *       Enumeration e = atts.keys();
 *       while( e.hasMoreElements() ) {
 *          String s = (String)e.nextElement();
 *          System.out.println("   ."+s+" = "+(String)atts.get(s) );
 *       }
 *
 *      if( xp.in("RESOURCE TABLE") )
 *         System.out.println("==> in RESOURCE TABLE");
 *    }
 *
 *    // Method called for each end XML tag
 *    public void endElement(String name) {
 *       System.out.println("Ends tag: "+name);
 *    }
 *
 *    // Method called to send the contain of the current XML tag
 *    public void characters(char [] ch, int start, int lenght) {
 *       System.out.println("tag contain: ["+ (new String(ch,start,lenght)) +"]");
 *    }
 *
 *    // The main method to test it
 *    static public void main(String [] arg) {
 *       try {
 *          // Open the first arg as a file
 *          DataInputStream dis = new DataInputStream( new FileInputStream(arg[0]));
 *
 *          // Parse the file
 *          new ParseDemo(dis);
 *
 *       } catch( Exception e ) {
 *          System.err.println("There is a problem: "+e);
 *          e.printStackTrace();
 *       }
 *    }
 * }
 * </PRE>
 *
 * @version 1.0 3 sep 99 Creation
 * @author P.Fernique [CDS]
 * @Copyright ULP/CNRS 1999
 */
public abstract interface XMLConsumer {

  /** This method is called by the XML parser when it reaches an
   * XML tag (ex: &lt;TAGNAME paramname=paramvalue ...>)
   * Rq: For the specials tags &lt;TAGNAME .../>, this method is always
   * called before the endElement() in order to transmit the eventual
   * parameters.
   * @param name The tag name (TAGNAME in the example)
   * @param atts The tag parameters in an Hashtable. The keys of the
   *             hashtable are the param name.
   */
   public abstract void startElement(String name,Hashtable atts);

  /** This method is called by the XML parser when it reaches an end
   * XML tag (ex: &lt;/TAGNAME> or &lt;TAGNAME .../>)
   * @param name The tag name (TAGNAME in the example)
   */
   public abstract void endElement(String name);

  /** This method is called by the XML parser to transmit the contain
   * of the current XML tag (ex: &lt;TAG> ... the contain ... &lt;/TAG>)
   * Rq: To know the tag name associated, the XML parser implements
   *     the Stack getStack() method. This stack is formed by the
   *     list of hierarchical tag names
   * @param ch The array of char
   * @param start the index of the first character
   * @param length the length of the contain
   * @throws Exception
   */
   public abstract void characters(char [] ch,int start,int length) throws Exception;
}
