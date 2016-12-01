package com.github.zabbicook.api

import java.time.Duration
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

sealed trait Throttle {
  def start(): Unit
  def end(): Unit
}

object NonStrictThrottle extends Throttle {
  // do nothing
  def start(): Unit = {}
  def end(): Unit = {}
}

class StrictThrottle(
  concurrency: Int,
  timeout: Duration,
  startInterval: Duration
) extends Throttle {
  private[this] val queue = new LinkedBlockingQueue[Boolean](concurrency)
  queue.offer(true)

  // blocking
  def start(): Unit = {
    queue.poll(timeout.toMillis, TimeUnit.MILLISECONDS)
    if (startInterval != Duration.ZERO) {
      Thread.sleep(startInterval.toMillis)
    }
  }

  def end(): Unit = {
    queue.offer(true)
  }
}
