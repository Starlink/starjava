/*
*+
*  Name:
*     ShiftMap.c

*  Purpose:
*     JNI implementations of native methods of ShiftMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     4-FEB-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_ShiftMap.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_ShiftMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jdoubleArray jShift   /* Translation vector */
) {
   AstPointer pointer;
   const double *shift = NULL;
   int nshift;

   ENSURE_SAME_TYPE(double,jdouble)

   if ( jniastCheckNotNull( env, jShift ) ) {
      nshift = (*env)->GetArrayLength( env, jShift );
      shift = (const double *) 
              (*env)->GetDoubleArrayElements( env, jShift, NULL );
      ASTCALL(
          pointer.ShiftMap = astShiftMap( nshift, shift, "" );
      )
      ALWAYS(
          if ( shift ) {
             (*env)->ReleaseDoubleArrayElements( env, jShift, (jdouble *) shift,
                                                 JNI_ABORT );
          }
      )
      jniastInitObject( env, this, pointer );
   }
}
