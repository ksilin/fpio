// *****************************************************************************
// Projects
// *****************************************************************************

lazy val fpio =
  project
    .in(file("."))
    .settings(settings)
    .settings(
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        library.catsEffect,
        library.akkaActor,
        library.zio,
        library.monix,
        library.scalaLogging,
        library.logback,
        library.scalaCheck            % Test,
        library.specs                 % Test,
        library.scalaTest             % Test,
        library.akkaTestkit           % Test,
        library.akkaActorTestkitTyped % Test
      ),
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck = "1.14.0"
      val utest      = "0.6.4"
      val akka       = "2.5.14"
    }

    val akkaActor             = "com.typesafe.akka"          %% "akka-actor-typed"         % Version.akka
    val akkaTestkit           = "com.typesafe.akka"          %% "akka-testkit"             % Version.akka
    val akkaActorTestkitTyped = "com.typesafe.akka"          %% "akka-actor-testkit-typed" % Version.akka
    val zio                   = "org.scalaz"                 %% "scalaz-zio"               % "0.1.0-SNAPSHOT"
    val monix                 = "io.monix"                   %% "monix"                    % "3.0.0-RC1"
    val catsEffect            = "org.typelevel"              %% "cats-effect"              % "1.0.0-RC2"
    val scalaCheck            = "org.scalacheck"             %% "scalacheck"               % Version.scalaCheck
    val specs                 = "org.specs2"                 %% "specs2-core"              % "4.2.0"
    val scalaTest             = "org.scalatest"              %% "scalatest"                % "3.0.5"
    val scalaLogging          = "com.typesafe.scala-logging" %% "scala-logging"            % "3.9.0"
    val logback               = "ch.qos.logback"             % "logback-classic"           % "1.2.3"
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
commonSettings ++
scalafmtSettings

lazy val commonSettings =
  Seq(
    // scalaVersion from .travis.yml via sbt-travisci
    // scalaVersion := "2.12.4",
    organization := "default",
    organizationName := "ksilin",
    startYear := Some(2018),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    updateOptions := updateOptions.value.withLatestSnapshots(true) // already the default
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
