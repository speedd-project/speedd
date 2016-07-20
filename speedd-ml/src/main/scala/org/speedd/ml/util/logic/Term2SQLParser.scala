package org.speedd.ml.util.logic

import java.io.{BufferedReader, File, FileReader}
import auxlib.log.{Logger, Logging}

class Term2SQLParser extends CommonTerm2SQLParser with Logging {

  def atomTermMappings: Parser[List[TermMapping]] = rep(atom2SQL)

  def functionTermMappings: Parser[List[TermMapping]] = rep(function2SQL)

  private def atom2SQL: Parser[TermMapping] = upperCaseID ~ "(" ~ repsep(lowerCaseID, ",") ~ ")" ~ "->" ~ ".+".r ^^ {
    case predicateSymbol ~ "(" ~ domains ~ ")" ~ "->" ~ sqlConstraint =>
      TermMapping(predicateSymbol, domains.toVector, sqlConstraint)
  }

  private def function2SQL: Parser[TermMapping] = lowerCaseID ~ "(" ~ repsep(lowerCaseID, ",") ~ ")" ~ "->" ~ ".+".r ^^ {
    case functionSymbol ~ "(" ~ domains ~ ")" ~ "->" ~ sqlConstraint =>
      TermMapping(functionSymbol, domains.toVector, sqlConstraint)
  }

  def parseAtomTermMapping(src: String): TermMapping = parse(atom2SQL, src) match {
    case Success(expr, _) if expr.isInstanceOf[TermMapping] => expr.asInstanceOf[TermMapping]
    case x => fatal("Can't parse the following expression: " + x)
  }

  def parseFunctionTermMapping(src: String): TermMapping = parse(function2SQL, src) match {
    case Success(expr, _) if expr.isInstanceOf[TermMapping] => expr.asInstanceOf[TermMapping]
    case x => fatal("Can't parse the following expression: " + x)
  }
}

object Term2SQLParser {

  private lazy val parser = new Term2SQLParser
  private lazy val log = Logger(this.getClass)
  import log._

  def parseAtomTermFrom(sources: Iterable[String]): Iterable[TermMapping] = sources.map(parser.parseAtomTermMapping)

  def parseFunctionTermFrom(sources: Iterable[String]): Iterable[TermMapping] = sources.map(parser.parseFunctionTermMapping)

  def parseAtomTermFrom(termMappingsFile: File): List[TermMapping] = {

    val fileReader = new BufferedReader(new FileReader(termMappingsFile))

    parser.parseAll(parser.atomTermMappings, fileReader) match {
      case parser.Success(expr, _) => expr.asInstanceOf[List[TermMapping]]
      case x => fatal("Can't parse the following expression: " + x +" in file: " + termMappingsFile.getPath)
    }
  }

  def parseFunctionTermFrom(termMappingsFile: File): List[TermMapping] = {

    val fileReader = new BufferedReader(new FileReader(termMappingsFile))

    parser.parseAll(parser.functionTermMappings, fileReader) match {
      case parser.Success(expr, _) => expr.asInstanceOf[List[TermMapping]]
      case x => fatal("Can't parse the following expression: " + x +" in file: " + termMappingsFile.getPath)
    }
  }
}
