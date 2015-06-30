/*
 * Copyright 2013 ChronoTrack
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.chronotrack.flurry.gen

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors, TimeUnit}

import com.chronotrack.flurry.Generator
import com.chronotrack.flurry.bootstrap.ServiceInjector
import com.google.inject.Inject
import org.scalatest.FunSuite

import scala.collection.JavaConversions._

class ConfigurableGeneratorTest @Inject()(val generator:Generator) extends FunSuite with ServiceInjector {


  test("generate a few ids") {
    println(generator.getId)
    println(generator.getId)
    println(generator.getId)
  }

  test("generate multiple ids in thread and make sure no dups are generated") {
    val ids = new ConcurrentHashMap[Long, AtomicInteger]()

    val pool: ExecutorService = Executors.newFixedThreadPool(50)
    val tasks = for (i <- 0 until 100) yield
      new Runnable {
        def run() {
          for (i <- 0 until 10) {
            val id = generator.getId
            ids.putIfAbsent(id, new AtomicInteger(0))
            ids.get(id).incrementAndGet()
          }
        }
      }
    tasks.foreach(pool.submit)
    pool.awaitTermination(2, TimeUnit.SECONDS)

    ids.values().foreach(v => assert(v.get() === 1))

  }

}
