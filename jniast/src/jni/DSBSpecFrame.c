/*
*+
*  Name:
*     DSBSpecFrame.c

*  Purpose:
*     JNI implementations of native methods of DSBSpecFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     9-AUG-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_DSBSpecFrame.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_DSBSpecFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.DSBSpecFrame = astDSBSpecFrame( "" );
   )
   jniastInitObject( env, this, pointer );
}
