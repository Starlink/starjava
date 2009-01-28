/*
*+
*  Name:
*     SkyFrame.c

*  Purpose:
*     JNI implementations of native methods of SkyFrame class.

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
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SkyFrame.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SkyFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.SkyFrame = astSkyFrame( "" );
   )
   jniastInitObject( env, this, pointer );
}

/* $Id$ */
