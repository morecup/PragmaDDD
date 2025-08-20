package org.morecup.pragmaddd.core.proxy

import org.morecup.pragmaddd.core.util.ConcurrentWeakHashMap

val aggregateRootToOrmEntityClassCache = ConcurrentWeakHashMap<Class<*>, List<Class<*>>>()
