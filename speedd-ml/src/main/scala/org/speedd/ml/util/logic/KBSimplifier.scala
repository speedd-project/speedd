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

package org.speedd.ml.util.logic

import auxlib.log.Logger
import lomrf.logic._
import lomrf.mln.model._


import scala.util.{Failure, Success, Try}

object KBSimplifier {

  private val log = Logger(this.getClass)

  def simplify(kb: KB,
               inputSignatures: Set[AtomSignature],
               targetSignatures: Set[AtomSignature],
               domains: ConstantsDomain,
               infixSignatures: Set[AtomSignature] = INFIX_SIGNATURES): Try[(Iterable[RuleTransformation], PredicateSchema)] ={

    transform(kb.definiteClauses, inputSignatures, targetSignatures, domains, infixSignatures) map { transformations =>

      // ---
      // --- Create final predicate schema:
      // ---
      // That is, query predicates, evidence predicates and all derived atoms from rule transformations.
      // Please note that weight learning does not support hidden predicates, therefore we do not include the
      // signatures of target predicates.

      val predicateSchema = transformations.foldLeft(kb.predicateSchema -- targetSignatures){
        (schema, transformation) => schema ++ transformation.schema
      }

      (transformations, predicateSchema)
    }
  }


  def transform(rules: Iterable[WeightedDefiniteClause],
                inputSignatures: Set[AtomSignature],
                targetSignatures: Set[AtomSignature],
                domains: ConstantsDomain,
                infixSignatures: Set[AtomSignature] = INFIX_SIGNATURES): Try[Iterable[RuleTransformation]] = {

    var result = List.empty[RuleTransformation]

    for {
      (rule, ruleIndex) <- rules.zipWithIndex
      head = rule.clause.head
      if targetSignatures.contains(head.signature)
      weight = rule.weight
      body = rule.clause.body
      headVars = head.variables
      bodyVars = body.variables
      bodyOnlyVars = bodyVars.diff(headVars)} {

      val atoms = body.findAtoms {
        atom =>
          inputSignatures.contains(atom.signature) && atom.variables.intersect(bodyOnlyVars).nonEmpty
      }

      // TODO: should also variabilize target atoms having constants in their arguments
      //
      // For example, `HappensAt(aggr(lid, Onramp), t)` contains a single constant 'Onramp' and thus it
      // should converted to `HappensAt(aggr(lid,x), t) ^ x = Onramp`
      //
      //val (atomsWithConstants, atomsWithVariables) = atoms.partition(_.constants.nonEmpty)
      //variabilizeAtom(atomsWithConstants.head)()

      // -- Phase 1: Variable bounds
      log.info(s"Phase 1: Processing variable bounds for rule '${rule.toText}'")
      val boundedVariables = atoms.flatMap(_.variables).toSet

      val unboundedVariables = rule.variables -- headVars -- boundedVariables

      log.debug(
        s"""
           |Variable bounds for rule: '${rule.toText}' {
           |\t- Variables that appear only in body: '${bodyOnlyVars.map(_.toText).mkString(", ")}'
           |\t- Bounded variables: '${boundedVariables.map(_.toText).mkString(", ")}'
           |\t- Unbounded variables: '${unboundedVariables.map(_.toText).mkString(", ")}'
           |""".stripMargin)


      if (unboundedVariables.nonEmpty) {
        val errorMsg = s"Cannot simplify rule '${rule.toText}', because it contains the following unbounded variable(s): '${unboundedVariables.map(_.toText).mkString(", ")}'"
        log.error(errorMsg)
        return Failure(new IllegalStateException(errorMsg))
      }


      if (boundedVariables.nonEmpty) {
        // -- Phase 2: Process map bounded variables
        log.info(s"Phase 2: Processing domain map for all variables in rule '${rule.toText}'")
        //val mappedVariables = rule.variables.map(v => v.symbol -> domainOf(v.symbol)).toMap
        val mappedVariables = rule.variables.map(v => v.symbol -> v.domain).toMap
        log.debug(s"Domain map of variables in rule '${rule.toText}': '$mappedVariables'")



        // -- Phase 3: Process infix operators and create generators
        log.info(s"Phase 3: Processing infix operators for rule '${rule.toText}'")
        val infixExpressions = body.findAtoms(atom => infixSignatures.contains(atom.signature))
        log.debug(s"Found infix expressions in rule '${rule.toText}': '${infixExpressions.mkString(", ")}'")



        // -- Phase 4: Create constraint generators
        log.info(s"Phase 4: Creating constraint generators for rule '${rule.toText}'")

        val wherePart = infixExpressions.map {
          case AtomicFormula(symbol, terms) if terms.size == 2 =>
            val leftTerm = terms.head match {
              case v:Variable => v.domain
              case other: Term => "'"+other.toText+"'"
            }

            val rightTerm = terms.last match {
              case v:Variable => v.domain
              case other: Term => "'"+other.toText+"'"
            }


            val op = symbol match {
              case "equals" => " = "
              case "not_equals" => " != "
              case "greaterThan" => " > "
              case "greaterThanEq" => " >= "
              case "lessThan" => " < "
              case "lessThanEq" => " =< "
              case _ => throw new Exception(s"Invalid infix operator '$symbol'")
            }

            leftTerm + op + rightTerm

          case otherExpression =>
            throw new Exception(s"Invalid expression '${otherExpression.toText}'")
        }.mkString(" AND ")

        log.debug(s"SQL-based constraints for rule '${rule.toText}': $wherePart")

        // --- Phase 5: create derived atom
        log.info(s"Phase 5: Creating derived atom for rule '${rule.toText}'")
        val sharedVars = boundedVariables.intersect(headVars).toVector
        val derivedAtom = new DerivedAtom("da_"+ruleIndex, sharedVars)
        log.debug(s"Derived atom for rule '${rule.toText}': ${derivedAtom.toText}")

        // --- Phase 6: rewrite rule
        log.info(s"Phase 6: Rewriting rule '${rule.toText}'")
        val transformedAtoms = body.findAtoms(_.variables.intersect(boundedVariables).isEmpty) ++ Vector(derivedAtom)

        // create conjunctions of transformed atoms (i.e., atoms that form the resulting body)
        val transformedBody = transformedAtoms
          .map(_.asInstanceOf[DefiniteClauseConstruct])
          .reduceRight(And)


        val transformedRule = WeightedDefiniteClause(weight, DefiniteClause(head, transformedBody))
        log.info(s"Rule '${rule.toText}' is transformed to '${transformedRule.toText}', " +
          s"where '${derivedAtom.toText}' is SQL-mapped using '$wherePart' as constraint(s).")

        val daSchema = Map(derivedAtom.signature -> sharedVars.map(_.domain))



        result = RuleTransformation(rule, Map(derivedAtom -> wherePart), daSchema, transformedRule) :: result
      }
      else {
        result = RuleTransformation(rule, Map.empty, Map.empty) :: result
      }

    }

    // Everything went fine! Give all resulting transformations
    Success(result)
  }
  

}
