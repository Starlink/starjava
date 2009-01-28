/*
*+
*  Name:
*     TimeFrame.c

*  Purpose:
*     JNI implementations of native methods of TimeFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     4-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_TimeFrame.h"

/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_TimeFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;

   ASTCALL(
      pointer.TimeFrame = astTimeFrame( "" );
   )
   jniastInitObject( env, this, pointer );
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_TimeFrame_currentTime(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   double curtime;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      curtime = astCurrentTime( pointer.TimeFrame );
   )
   return (jdouble) curtime;
}
