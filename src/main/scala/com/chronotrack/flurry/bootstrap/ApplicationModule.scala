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

package com.chronotrack.flurry.bootstrap

import com.google.inject.{Scopes, Key, AbstractModule}
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.LoggerFactory
import com.google.inject.name.Names
import ch.qos.logback.classic.{Level, LoggerContext}
import scala.collection.JavaConversions._
import com.typesafe.config.{ConfigValueType, ConfigValue, ConfigFactory, Config}
import com.chronotrack.flurry.Generator
import com.chronotrack.flurry.worker.WorkerIdGenerator

/**
 * User: ilya
 * Date: 8/15/13
 * Time: 6:44 PM
 */
class ApplicationModule extends AbstractModule with LazyLogging {

  override protected def configure: Unit = {
    try {
      val config = loadConfig
      bind(classOf[Config]).toInstance(config)
      configureLoggingFrom(config)
      configureGeneratorFrom(config)

    }
    catch {
      case e: Exception => {
        throw new RuntimeException("Couldn't load configuration properties", e)
      }
    }
  }

  private def configureLoggingFrom(config: Config): Unit = {
    val lc: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    for (l <- lc.getLoggerList) {
      val logKey = "logging.level." + l.getName
      if (config.hasPath(logKey)) {
        println("Configuring logger: " + l.getName + " with " + config.getString(logKey))
        l.setLevel(Level.toLevel(config.getString(logKey)))
      }
    }
  }


  private def loadConfig: Config = {
    ConfigFactory.load()
  }


  def configureGeneratorFrom(config: Config) {
    bind(classOf[WorkerIdGenerator]).to(Key.get(Class.forName(config.getString("flurry.generator-config.worker-id-generator-class")).asInstanceOf[Class[WorkerIdGenerator]])).in(Scopes.SINGLETON)
    bind(classOf[Generator]).to(Key.get(Class.forName(config.getString("flurry.generator-class")).asInstanceOf[Class[Generator]])).in(Scopes.SINGLETON)
    if (config.hasPath("flurry.generator-config")) {
      val c = config.getObject("flurry.generator-config")
      c.entrySet().foreach {
        case e: java.util.Map.Entry[String, ConfigValue] => {
          val k = e.getKey
          val v = e.getValue
          v.valueType() match {
            case ConfigValueType.STRING =>
              bind(classOf[String]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[String])
            case ConfigValueType.BOOLEAN =>
              bind(classOf[Boolean]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Boolean])
            case ConfigValueType.NUMBER =>
              if (v.unwrapped().isInstanceOf[Int]) {
                bind(classOf[Long]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Int].toLong)
                bind(classOf[Int]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Int])
              }
              else if (v.unwrapped().isInstanceOf[Long]) {
                bind(classOf[Long]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Long])
                bind(classOf[Int]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Long].toInt)
              }
              else if (v.unwrapped().isInstanceOf[Double]) {
                bind(classOf[Double]).annotatedWith(Names.named(k)).toInstance(v.unwrapped().asInstanceOf[Double])
              }
            case _ =>
          }
        }
      }
      bind(classOf[Config]).annotatedWith(Names.named("generator-config")).toInstance(config.atPath("flurry.generator-config"))
    }
  }


}