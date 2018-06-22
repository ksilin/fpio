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

import cats.Apply
import cats.data.Kleisli
import cats.implicits._
import org.scalatest.{ FreeSpec, MustMatchers }

// https://blog.ssanj.net/posts/2017-06-12-reading-configuration-with-kleisli-arrows.html

final case class Name(first: String, last: String)
final case class Age(age: Int)
final case class Person(name: Name, age: Age)
final case class Config(name: String, age: Int)

class KleisliConfigSpec extends FreeSpec with MustMatchers {

  val readName: Config => Option[Name] = c => {
    val parts = c.name.split(" ")
    if (parts.length > 1) Option(Name(parts.head, parts.drop(1).mkString(" "))) else None
  }

  val readAge: Config => Option[Age] = c => {
    if (c.age > 1 && c.age < 150) Option(Age(c.age)) else None
  }

  val readNameK: Kleisli[Option, Config, Name] = Kleisli(readName)
  val readAgeK: Kleisli[Option, Config, Age]   = Kleisli(readAge)

  val cfg            = Config("a b", 100)
  val expectedPerson = Person(Name("a", "b"), Age(100))
  val invalidNameCfg = Config("x", 65)
  val invalidAgeCfg  = Config("x y", 165)

  "combining kleislis for object construction" in {

    val personK: Kleisli[Option, Config, Person] = for {
      name <- readNameK
      age  <- readAgeK
    } yield Person(name, age)

    val person: Option[Person] = personK(cfg)

    person mustBe Some(expectedPerson)

    val p2 = personK(invalidNameCfg)
    p2 mustBe None

    val p3 = personK(invalidAgeCfg)
    p3 mustBe None
  }

  // same with Either

  val nameOrError: Config => Either[String, Name] = c => {
    val parts = c.name.split(" ")
    if (parts.length > 1) Right(Name(parts.head, parts.drop(1).mkString(" ")))
    else Left(s"more than one name part required, got $parts")
  }

  val ageOrError: Config => Either[String, Age] = c => {
    if (c.age > 1 && c.age < 150) Right(Age(c.age))
    else Left(s"age between 2 and 149 required, got ${c.age}")
  }

  "combining either with kleisli" in {

//    Kleisli[Either, Config, Name] - does not work, Kleisli expects F[_], while Either has two params
    type AOrErrString[A] = Either[String, A]

    val nameOrErrorK: Kleisli[AOrErrString, Config, Name] = Kleisli(nameOrError)
    val ageOrErrorK: Kleisli[AOrErrString, Config, Age]   = Kleisli(ageOrError)

    val personK: Kleisli[AOrErrString, Config, Person] = for {
      name <- nameOrErrorK
      age  <- ageOrErrorK
    } yield Person(name, age)

    val p = personK(cfg)
    p mustBe Right(expectedPerson)
  }

  // nameOrError & ageOrError do not depend on each other, so we dont strictly need to flatMap here
  // The Apply typeclass is an Applicative without the pure function. The Kleisli data type has an instance for Apply
  // Apply[Kleisli[F, A, ?]]

  // for combining, we can use the ap2 method
  // def ap2[A, B, Z](ff: F[(A, B) => Z])(fa: F[A], fb: F[B]): F[Z]

  "using Applicative with Kleisli to combine independent fns" in {
    type KOptionConfig[A] = Kleisli[Option, Config, A]
    type PersonFunc       = (Name, Age) => Person

    // optional
//    val readNameKOC: KOptionConfig[Name] = readNameK
//    val readAgeKOC: KOptionConfig[Age]   = readAgeK

    // TODO how does this underscore stiff works here?
    val personKOC: KOptionConfig[PersonFunc] = Kleisli((_: Config) => Option(Person(_, _)))

    val personK: KOptionConfig[Person] = Apply[KOptionConfig].ap2(personKOC)(readNameK, readAgeK)

    val person = personK(cfg)
    person mustBe Some(expectedPerson)
  }

  "using Functor (map2), we no longer have to define" in {
    type KOptionConfig[A] = Kleisli[Option, Config, A]

    val personK: KOptionConfig[Person] = Apply[KOptionConfig].map2(readNameK, readAgeK)(Person)
    val person                         = personK(cfg)
    person mustBe Some(expectedPerson)
  }

  // local - widening the input type

  val readNameStr: String => Option[Name] = s => {
    val parts = s.split(" ")
    if (parts.length > 1) Option(Name(parts.head, parts.drop(1).mkString(" "))) else None
  }

  val readAgeInt: Int => Option[Age] = i => {
    if (i > 1 && i < 150) Option(Age(i)) else None
  }

  val readNameStrK = Kleisli(readNameStr)
  val readAgeIntK  = Kleisli(readAgeInt)

  "combining fns with different inputs with local" in {
    val personK: Kleisli[Option, Config, Person] = for {
      name <- readNameStrK.local[Config](_.name)
      age  <- readAgeIntK.local[Config](_.age)
    } yield Person(name, age)

    val person = personK(cfg)
    person mustBe Some(expectedPerson)
  }

  "different inputs with map2" in {
    type KOptionConfig[A] = Kleisli[Option, Config, A]

    val readNameLocalK: Kleisli[Option, Config, Name] = readNameStrK.local[Config](_.name)
    val readAgeLocalK: Kleisli[Option, Config, Age]   = readAgeIntK.local[Config](_.age)

    val personK = Apply[KOptionConfig].map2(readNameLocalK, readAgeLocalK)(Person)
    val person  = personK(cfg)
    person mustBe Some(expectedPerson)
  }

}
