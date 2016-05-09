package org.speedd

import java.net.URL

package object ml {

  object ModuleVersion {
    val logo =
      """
        | __   ___   ____  ____  ___   ___
        |( (` | |_) | |_  | |_  | | \ | | \
        |_)_) |_|   |_|__ |_|__ |_|_/ |_|_/
      """.stripMargin

    val version: String = {
      val clazz = org.speedd.ml.ModuleVersion.getClass
      try {
        val classPath = clazz.getResource("package$" + clazz.getSimpleName + ".class").toString

        if (classPath.startsWith("jar")) {
          val manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"
          val manifest0 = new java.util.jar.Manifest(new URL(manifestPath).openStream)
          val attr = manifest0.getMainAttributes

          "v"+attr.getValue("Specification-Version")
        } else "(undefined version)"
      } catch {
        case ex: NullPointerException => "(undefined version)"
      }
    }

    def apply(): String = logo + "\nMachine Learning Module: " + version
  }

}
