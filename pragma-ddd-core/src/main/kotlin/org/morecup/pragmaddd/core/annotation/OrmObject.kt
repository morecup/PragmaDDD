package org.morecup.pragmaddd.core.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class OrmObject(
    val objectNameList: Array<String> = [],
)
