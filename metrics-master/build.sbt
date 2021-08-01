// retrieveManaged := true

lazy val root = (project in file(".")).
  settings(
    name := "SAFIREMetrics",
    version := "1.0.0",
    scalaVersion := "2.12.2"
//    , mainClass in (Compile, run) := Some("automaticityinduction.StringRewriting")
  )

resolvers += Resolver.sonatypeRepo("public")

// End ///////////////////////////////////////////////////////////////
