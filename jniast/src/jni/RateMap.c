/*
*+
*  Name:
*     RateMap.c

*  Purpose:
*     JNI implementations of native methods of RateMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     14-DEC-2004 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_RateMap.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_RateMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jMap,         /* Encapsulated mapping */
   jint ax1,             /* Output axis index */
   jint ax2              /* Input axis index */
) {
   AstPointer pointer;
   AstPointer map;

   if ( jniastCheckNotNull( env, jMap ) ) {
      map = jniastGetPointerField( env, jMap );
      THASTCALL( jniastList( 1, map.AstObject ),
         pointer.RateMap = astRateMap( map.Mapping, (int) ax1, (int) ax2, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}
