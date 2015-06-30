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

package com.chronotrack.flurry

import com.chronotrack.flurry.worker.WorkerIdGenerator
import com.typesafe.scalalogging.LazyLogging

/**
 * User: ilya
 * Date: 8/15/13
 * Time: 10:48 AM
 */
trait Generator extends LazyLogging {

  protected[this] val workerIdGenerator: WorkerIdGenerator
  lazy val workerId: Long = workerIdGenerator.workerId

  // Configurable variables
  val epochStart: Long
  val workerIdBits: Long
  val sequenceBits: Long

  private[this] val maxWorkerId = -1L ^ (-1L << workerIdBits)
  private[this] final val workerIdShift = sequenceBits
  private[this] final val timestampLeftShift = sequenceBits + workerIdBits
  private[this] final val sequenceMask = -1L ^ (-1L << sequenceBits)

  private var lastTimestamp = -1L

  private[this] var sequence = 0L

  // sanity check for workerId
  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  logger.info(
    s"""Using ${this.getClass.getName}:
      |\tBits used for time: ${64-workerIdBits-sequenceBits} allowing ${Math.pow(2, 64-workerIdBits-sequenceBits)/3.15569e10} years
      |\tBits used for worker: $workerIdBits allowing ${Math.pow(2, workerIdBits).toInt} workers
      |\tBits used for sequence: $sequenceBits allowing ${Math.pow(2, sequenceBits).toInt} ids per millisecond
    """.stripMargin)

  protected def timeGen(): Long = System.currentTimeMillis()

  def getId = nextId

  private[this] final def nextId: Long = synchronized {
    var timestamp = timeGen()

    if (timestamp < lastTimestamp) {
      logger.error("clock is moving backwards.  Rejecting requests until %d.", Array(lastTimestamp))
      throw new RuntimeException("Clock moved backwards.  Refusing to generate id for %d milliseconds".format(
        lastTimestamp - timestamp))
    }

    sequence = if (lastTimestamp == timestamp) {
      val seq = (sequence + 1) & sequenceMask
      if (seq == 0) { // This means we went above the Long upper bound and we have to wait
        timestamp = tilNextMillis(lastTimestamp)
      }
      seq
    } else {
      0
    }

//    println(sequence)

    lastTimestamp = timestamp
    ((timestamp - epochStart) << timestampLeftShift) |
        (workerId << workerIdShift) |
        sequence
  }

  private[this] def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = timeGen()
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen()
    }
    timestamp
  }


}
