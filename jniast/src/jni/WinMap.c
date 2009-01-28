/*
*+
*  Name:
*     WinMap.c

*  Purpose:
*     JNI implementations of native mathods of WinMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     27-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_WinMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_WinMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint ncoord,          /* Number of coordinates */
   jdoubleArray jIna,    /* Coordinates of A corner of input window */
   jdoubleArray jInb,    /* Coordinates of B corner of input window */
   jdoubleArray jOuta,   /* Coordinates of A corner of output window */
   jdoubleArray jOutb    /* Coordinates of B corner of output window */
) {
   AstPointer pointer;
   const double *ina = NULL;
   const double *inb = NULL;
   const double *outa = NULL;
   const double *outb = NULL;

   ENSURE_SAME_TYPE(double,jdouble)

   /* Check that our arrays are large enough. */
   if ( jniastCheckArrayLength( env, jIna, ncoord ) &&
        jniastCheckArrayLength( env, jInb, ncoord ) &&
        jniastCheckArrayLength( env, jOuta, ncoord ) &&
        jniastCheckArrayLength( env, jOutb, ncoord ) ) {

      /* Map elements of java arrays. */
      ina = (const double *)
            (*env)->GetDoubleArrayElements( env, jIna, NULL );
      inb = (const double *)
            (*env)->GetDoubleArrayElements( env, jInb, NULL );
      outa = (const double *)
             (*env)->GetDoubleArrayElements( env, jOuta, NULL );
      outb = (const double *)
             (*env)->GetDoubleArrayElements( env, jOutb, NULL );

      /* Call the C function to do the work. */
      ASTCALL(
         pointer.WinMap = astWinMap( (int) ncoord, ina, inb, outa, outb, "" );
      )

      /* Release resources. */
      ALWAYS(
         if ( ina ) {
            (*env)->ReleaseDoubleArrayElements( env, jIna, 
                                                (jdouble *) ina, JNI_ABORT );
         }
         if ( inb ) {
            (*env)->ReleaseDoubleArrayElements( env, jInb,
                                                (jdouble *) inb, JNI_ABORT );
         }
         if ( outa ) {
            (*env)->ReleaseDoubleArrayElements( env, jOuta,
                                                (jdouble *) outa, JNI_ABORT );
         }
         if ( outb ) {
            (*env)->ReleaseDoubleArrayElements( env, jOutb,
                                                (jdouble *) outb, JNI_ABORT );
         }
      )

      /* Set the pointer field of the java object. */
      jniastInitObject( env, this, pointer );
   }
}

/* $Id$ */
