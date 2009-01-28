/*
*+
*  Name:
*     ZoomMap.c

*  Purpose:
*     JNI implementations of native mathods of ZoomMap class.

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
#include "uk_ac_starlink_ast_ZoomMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_ZoomMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint ncoord,          /* Number of coordinates */
   jdouble zoom          /* Zoom factor */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.ZoomMap = astZoomMap( (int) ncoord, (double) zoom, "" );
   )
   jniastInitObject( env, this, pointer );
}
/* $Id$ */
