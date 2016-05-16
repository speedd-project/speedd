package org.speedd.ml.util.logic

import java.io.{BufferedReader, File, FileReader}
import auxlib.log.{Logger, Logging}

class Atom2SQLParser extends CommonAtom2SQLParser with Logging {

  def modeDeclarations: Parser[List[AtomMapping]] = rep(atom2SQL)

  private def atom2SQL: Parser[AtomMapping] = upperCaseID ~ "(" ~ repsep(lowerCaseID, ",") ~ ")" ~ "->" ~ ".+".r ^^ {

    case predicateSymbol ~ "(" ~ domains ~ ")" ~ "->" ~ sqlConstraint =>
      AtomMapping(predicateSymbol, domains.toVector, sqlConstraint)
  }

  def parseAtomMapping(src: String): AtomMapping = parse(atom2SQL, src) match {
    case Success(expr, _) if expr.isInstanceOf[AtomMapping] => expr.asInstanceOf[AtomMapping]
    case x => fatal("Can't parse the following expression: " + x)
  }

}

object Atom2SQLParser {

  private lazy val parser = new Atom2SQLParser
  private lazy val log = Logger(this.getClass)
  import log._

  def parseFrom(sources: Iterable[String]): Iterable[AtomMapping] = sources.map(parser.parseAtomMapping)

  def parseFrom(atomMappingsFile: File): List[AtomMapping] = {

    val fileReader = new BufferedReader(new FileReader(atomMappingsFile))

    parser.parseAll(parser.modeDeclarations, fileReader) match {
      case parser.Success(expr, _) => expr.asInstanceOf[List[AtomMapping]]
      case x => fatal("Can't parse the following expression: " + x +" in file: " + atomMappingsFile.getPath)
    }
  }
}
