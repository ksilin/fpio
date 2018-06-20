// *****************************************************************************
// Projects
// *****************************************************************************

lazy val fpio =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.catsEffect,
        library.scalaCheck % Test,
        library.specs      % Test,
        library.scalaTest  % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val scalaCheck = "1.14.0"
      val utest      = "0.6.4"
    }
    val catsEffect = "org.typelevel"  %% "cats-effect" % "1.0.0-RC2"
    val scalaCheck = "org.scalacheck" %% "scalacheck"  % Version.scalaCheck
    val specs      = "org.specs2"     %% "specs2-core" % "4.2.0"
    val scalaTest  = "org.scalatest"  %% "scalatest"   % "3.0.5"
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
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
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
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
