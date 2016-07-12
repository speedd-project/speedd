package org.speedd.ml.util.logic

import scala.util.parsing.combinator.{RegexParsers, JavaTokenParsers}

/**
  * Regular expressions for term mappings to sql constraints parser.
  */
trait CommonTerm2SQLParser extends JavaTokenParsers with RegexParsers {

  val lowerCaseID = """([a-z]([a-zA-Z0-9]|_[a-zA-Z0-9])*)""".r
  val upperCaseID = """([A-Z0-9]([a-zA-Z0-9]|_[a-zA-Z0-9])*)""".r
  val connective = """AND|OR""".r
  val operator = """(=|>=|<=|>|<|!=)""".r

  override val whiteSpace = """(\s|//.*\n|(/\*(?:.|[\n\r])*?\*/))+""".r
}
