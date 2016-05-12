package org.speedd.ml.loaders.cnrs

import org.speedd.ml.util.data._

package object collected {

  /**
    * PR0 to PR11 relative distances in meters from PR0.
    */
  val prDistances = Array(0, 962, 1996, 2971, 3961, 4961, 5948, 6960, 7971, 8961, 9966, 11035)

  /**
    * User defined occupancy levels
    */
  val occLevels = Array(0.0, 25.0, 101.0)

  /**
    * User defined average speed levels
    */
  val speedLevels = Array(0.0, 55.0, 100.0)

  /**
    * User defined vehicle numbers
    */
  val vehicleLevels = Array(0.0, 4.0, 8.0, 16.0, 32.0)

  /**
    * Symbols to domains
    */
  val symbols2domain = Map("O" -> "occupancy", "S" -> "avg_speed", "V" -> "vehicles")

  /**
    * Mapping of column name to user defined function
    */
  val domain2udf = Map(
    "occupancy" -> mkSymbolic(occLevels, "O"),
    "avg_speed" -> mkSymbolic(speedLevels, "S"),
    "vehicles" -> mkSymbolic(vehicleLevels, "V")
  )

  /**
    * Mapping of symbol names to user defined function producing intervals
    */
  val symbols2udf = Map(
    "O" -> mkInterval(occLevels, symbols2domain),
    "S" -> mkInterval(speedLevels, symbols2domain),
    "V" -> mkInterval(vehicleLevels,symbols2domain)
  )
}
