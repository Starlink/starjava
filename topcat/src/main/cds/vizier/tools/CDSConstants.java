//
// Copyright 2002-2003 - Universite Louis Pasteur / Centre National de la
// Recherche Scientifique
//
// ------
//
// CDS design and static values for Java applications
//
// Author:  Andr‰ Schaaff
// Address: Centre de Donnees astronomiques de Strasbourg
//          11 rue de l'Universite
//          67000 STRASBOURG
//          FRANCE
// Email:   schaaff@astro.u-strasbg.fr, question@simbad.u-strasbg.fr
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


package cds.vizier.tools;

import java.awt.Font;
import java.awt.Color;
import java.awt.Toolkit;

/**
 * CDS design and static values interface
 *
 * @author Andr‰ Schaaff [CDS]
 *
 * @version 1.0 beta :  (july 2002), xml format (Astrores or VOTable), glu and url values
 *                      (june 2002), STREAM and FRAME values
 *                      renamed to CDSConstants
 * @version 0.9 : (january 2002), extraction from AladinJava
 *
 *
 */

public interface CDSConstants {

    static final boolean LSCREEN= Toolkit.getDefaultToolkit().getScreenSize().width > 1000;

    /** fonts average size */
    static final int  SIZE   = LSCREEN?12:10;

    static String s = "Helvetica";
    static Font BOLD   = new Font(s,Font.BOLD,  SIZE);
    static Font PLAIN  = new Font(s,Font.PLAIN, SIZE);
    static Font ITALIC = new Font(s,Font.ITALIC,SIZE);
    static int SSIZE  = SIZE-2;
    static Font SBOLD  = new Font(s,Font.BOLD,  SSIZE);
    static Font SPLAIN = new Font(s,Font.PLAIN, SSIZE);
    static Font SITALIC= new Font(s,Font.ITALIC,SSIZE);
    static int LSIZE  = SIZE+2;
    static Font LPLAIN = new Font(s,Font.PLAIN, LSIZE);
    static Font LBOLD  = new Font(s,Font.BOLD,  LSIZE);
    static Font LITALIC= new Font(s,Font.ITALIC,LSIZE);
    static Font LLITALIC= new Font(s,Font.BOLD,18);
    static Font COURIER= new Font("Courier",Font.PLAIN,SIZE);
    static Font BCOURIER= new Font("Courier",Font.PLAIN+Font.BOLD,SIZE);

    static final int DEFAULT 	= 0;
    static final int WAIT 	= 1;
    static final int HAND 	= 2;
    static final int CROSSHAIR	= 3;
    static final int MOVE 	= 4;
    static final int RESIZE 	= 5;
    static final int TEXT 	= 6;

    // background color
    static final Color BKGD   = Color.lightGray;
    static final Color BKGDCOPYRIGHT   = Color.white;
    static Color LGRAY = new Color(229,229,229);

    static final String COPYRIGHT = "(c) ULP/CNRS 1999-2002 - Centre de Données astronomiques de Strasbourg";

    // Les textes associes aux differentes possibilites du menu
    static final int GETHEIGHT  = 17; // Cochonnerie de getHeight()

    // True if standalone mode
    static boolean STANDALONE = true;

    // units
    static final String DEGREE = "deg";
    static final String ARCMIN = "arcmin";
    static final String ARCSEC = "arcsec";

    // output mode, stream or frame
    static final int STREAM = 0;
    static final int FRAME = 1;

    // modal or non modal dialog
    static final boolean MODAL = true;
    static final boolean NONMODAL = false;

    // XML specific format
    static final int ASTRORES = 0;
    static final int VOTABLE = 1;

    // url or glu tag values
    static String VIZIERMETAGLU = "VizieR.Meta";
    static String VIZIERMETACATGLU = "VizieR.MetaCat";

    static String ASTROVIZIERMETA = "http://vizier.u-strasbg.fr/cgi-bin/asu-xml?-meta.aladin=all";
    static String ASTROVIZIERMETACAT = "http://vizier.u-strasbg.fr/cgi-bin/asu-xml?-meta&";

//    static String VOVIZIERMETA = "http://vizier.u-strasbg.fr/viz-bin/nph-metaladin";
    static String VOVIZIERMETA = "http://vizier.u-strasbg.fr/cgi-bin/votable?-meta.aladin=all";
    static String VOVIZIERMETACAT = "http://vizier.u-strasbg.fr/cgi-bin/votable?-meta&";

    // default VizieR query url or glu values
    static String VIZIERMETACAT = VOVIZIERMETACAT;
    static String VIZIERMETA = VOVIZIERMETA;

}