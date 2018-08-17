package com.example.fpio.crawler

// mock http service
trait Http[F[_]] {

  def get(url: Url): F[String]

}
