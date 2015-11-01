/*
 *  __   ___   ____  ____  ___   ___
 * ( (` | |_) | |_  | |_  | | \ | | \
 * _)_) |_|   |_|__ |_|__ |_|_/ |_|_/
 *
 * SPEEDD project (www.speedd-project.eu)
 * Machine Learning module
 *
 * Copyright (c) Complex Event Recognition Group (cer.iit.demokritos.gr)
 *
 * NCSR Demokritos
 * Institute of Informatics and Telecommunications
 * Software and Knowledge Engineering Laboratory
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.speedd

import java.net.URL


import lomrf.logic.AtomSignature

import scala.language.implicitConversions

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
