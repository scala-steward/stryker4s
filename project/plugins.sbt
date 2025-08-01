addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.1")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.18.0")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.2")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")

// Protobuf plugin and its dependencies
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.19"
