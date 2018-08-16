package com.example.fpio

trait Timed {

  def timed[A](description: String)(x: => A): A = {
    val start  = System.nanoTime()
    val result = x
    println(s"timed $description: ${(System.nanoTime() - start) / 1e6} ms")
    result
  }

}
