/*==============================================================*/
/*                                                              */
/*                UK Astronomy Technology Centre                */
/*                 Royal Observatory, Edinburgh                 */
/*                 Joint Astronomy Centre, Hilo                 */
/*                   Copyright (c) PPARC 2001                   */
/*                                                              */
/*==============================================================*/
// $Id$

package edfreq;

/**
 * @author Dennis Kelly ( bdk@roe.ac.uk )
 */
public class LineDetails
{
   public String name;
   public String transition;
   public double frequency;

   public LineDetails ( String name, String transition, double frequency )
   {
      this.name = name;
      this.transition = transition;
      this.frequency = frequency;
   }
}
