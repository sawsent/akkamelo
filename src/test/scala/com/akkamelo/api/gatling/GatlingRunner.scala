package com.akkamelo.api.gatling

import io.gatling.app.Gatling

object GatlingRunner extends App {
  Gatling.main(Array(
    "--simulation", "com.akkamelo.api.gatling.RinhaBackendCrebitosSimulation",
    "--results-folder", "target/gatling"
  ))
}
