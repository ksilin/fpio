/*
 * Copyright 2018 ksilin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.fpio

import cats.data.Kleisli
import cats.implicits._
import org.scalatest.{ FreeSpec, MustMatchers }

https://typelevel.org/cats/datatypes/kleisli.html

class KleisliCatsSpec extends FreeSpec with MustMatchers {

  // Kleisli enables composition of functions that return a monadic value, for instance an Option[Int]
  // or a Either[String, List[Double]], without having functions take an Option or Either as a parameter

  // We may also have several functions which depend on some environment
  // and want a nice way to compose these functions to ensure they all receive the same environment.
  // Or perhaps we have functions which depend on their own “local” configuration
  // and all the configurations together make up a “global” application configuration.

  val parse: String => Option[Int] =
    s => if (s.matches("-?[0-9]+")) Some(s.toInt) else None

  val reciprocal: Int => Option[Double] =
    i => if (i != 0) Some(1.0 / i) else None

  // At its core, Kleisli[F[_], A, B] is just a wrapper around the function A => F[B]

  // if F[_] has a FlatMap[F] instance (we can call flatMap on F[A] values)
  // we can compose two Kleislis much like we can two functions

//  final case class Kleisli[F[_], A, B](run: A => F[B]) {
//    def compose[Z](k: Kleisli[F, Z, A])(implicit F: FlatMap[F]): Kleisli[F, Z, B] =
//      Kleisli[F, Z, B](z => k.run(z).flatMap(run))
//  }

  val parseK: Kleisli[Option, String, Int] = Kleisli[Option, String, Int](parse)

  val reciprokalK: Kleisli[Option, Int, Double] = Kleisli(reciprocal)

  // compose requires a FlatMap or Monad instance for F[_]
  val parseAndReciprocal: Kleisli[Option, String, Double] = reciprokalK.compose(parseK)

  val shiftReciprocal: Int => Double =
    i => if (i != 0) 1.0 / i else i

  val parseShiftRecoprocal: Kleisli[Option, String, Double] = parseK.map(shiftReciprocal)

  // a Functor instance would provide map
  // Applicative instance would provide traverse

  "lets kleisli that shit" in {
    val res = parseAndReciprocal("5")
    res mustBe Some(0.2)

    val res2 = parseShiftRecoprocal("5")
    res2 mustBe Some(0.2)

  }

  // Kleisli can be viewed as the monad transformer for functions

  // bc we want A => B but kleisli needs an F[]
  type Id[A]        = A
  type Reader[A, B] = Kleisli[Id, A, B]

  object Reader {
    def apply[A, B](f: A => B): Reader[A, B] = Kleisli[Id, A, B](f)
  }

  type ReaderT[F[_], A, B] = Kleisli[F, A, B]
  val ReaderT = Kleisli

  val twice: Int => Int        = i => i * 2
  val twiceR: Reader[Int, Int] = Reader(twice)

  // TODO - now what?

  "kleisli as monad transformers" in {}

  // config combination

  case class DbConfig(url: String, user: String, pass: String)
  trait Db
  object Db {
    val fromDbConfig: Kleisli[Option, DbConfig, Db] = ???
  }

  case class ServiceConfig(addr: String, port: Int)
  trait Service
  object Service {
    val fromServiceConfig: Kleisli[Option, ServiceConfig, Service] = ???
  }

  case class AppConfig(dbConfig: DbConfig, serviceConfig: ServiceConfig)

  case class App(db: Db, service: Service)

  // final case class Kleisli[F[_], A, B](run: A => F[B]) {
  // def local[AA](f: AA => A): Kleisli[F, AA, B] = Kleisli(f.andThen(run))

  // def local[AppConfig](f: AppConfig => DbConfig): Kleisli[Option, AppConfig, DbConfig] = Kleisli(f.andThen(run))

  // What local allows us to do is essentially “expand” our input type to a more “general” one

  val c: Kleisli[Option, AppConfig, Db] = Db.fromDbConfig.local[AppConfig](_.dbConfig)

  def appFromAppConfig: Kleisli[Option, AppConfig, App] =
    for {
      db <- Db.fromDbConfig.local[AppConfig](_.dbConfig)
      sv <- Service.fromServiceConfig.local[AppConfig](_.serviceConfig)
    } yield App(db, sv)

}
