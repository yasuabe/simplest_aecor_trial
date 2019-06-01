import sbt.Keys.scalacOptions
import sbt._
import sbt.Keys._

val aecorVer         = "0.18.0"
val aecorPostgresVer = "0.3.0"
val akkaVer          = "2.5.18"
val circeVer         = "0.10.1"
val doobieVer        = "0.6.0"
val paradiseVer      = "3.0.0-M11"
val pureConfigVer    = "0.10.0"
val shapelessVer     = "2.3.3"
val logbackVer       = "1.2.3"

val kindProjector    = compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
val macroParadise    = compilerPlugin("org.scalameta" % "paradise" % paradiseVer cross CrossVersion.full)

val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
val scalapbTargets = PB.targets in Compile := Seq(
  scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
)

val baseDependencies = Seq(
  kindProjector,
  betterMonadicFor,
  macroParadise,
  scalapbRuntime,
  "io.aecor"          %% "core"                    % aecorVer,
  "io.aecor"          %% "akka-cluster-runtime"    % aecorVer,
  "io.aecor"          %% "distributed-processing"  % aecorVer,
  "io.aecor"          %% "boopickle-wire-protocol" % aecorVer,
  "io.aecor"          %% "aecor-postgres-journal"  % aecorPostgresVer,
  "io.chrisdavenport" %% "cats-par"                % "0.2.0",
  "io.monix"          %% "monix"                   % "3.0.0-RC2",
  "org.tpolecat"      %% "doobie-core"             % doobieVer,
  "org.tpolecat"      %% "doobie-postgres"         % doobieVer,
  "io.circe"          %% "circe-core"              % circeVer,
  "io.circe"          %% "circe-generic"           % circeVer,
  "io.circe"          %% "circe-parser"            % circeVer,
  "ch.qos.logback"    % "logback-classic"          % logbackVer,
  "com.typesafe.akka" %% "akka-slf4j"              % akkaVer,
  "com.github.pureconfig" %% "pureconfig"          % pureConfigVer,
)
val baseOptions = Seq(
  "-encoding", "UTF-8",
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ypartial-unification",
  "-Xplugin-require:macroparadise"
)
val baseSettings = Seq(
  scalaVersion in ThisBuild := "2.12.8",
  resolvers           ++= Seq("releases", "snapshots").map(Resolver.sonatypeRepo),
  scalacOptions       ++= baseOptions ,
  libraryDependencies ++= baseDependencies,
  scalapbTargets
)
lazy val model = (project in file("modules/simplest/model"))
  .settings(baseSettings)

lazy val common = (project in file("modules/simplest/common"))
  .settings(baseSettings)
  .aggregate(model)
  .dependsOn(model)

lazy val writerSide = (project in file("modules/simplest/writerSide"))
  .settings(baseSettings)
  .aggregate(common)
  .dependsOn(common)

lazy val readerSide = (project in file("modules/simplest/readerSide"))
  .settings(baseSettings)
  .aggregate(common)
  .dependsOn(common)

