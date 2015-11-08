package org.speedd.ml

import lomrf.logic._
import lomrf.mln.model.ConstantsSet
import org.scalatest.{FunSpec, Matchers}
import org.speedd.ml.util.logic.KBSimplifier

class KBSimplifierSpecTest extends FunSpec with Matchers {

  private val inputPredicateSchema = Map(
    AtomSignature("HappensAt", 2) -> Vector("event", "timestamp")
  )

  private val targetPredicateSchema = Map(
    AtomSignature("InitiatedAt", 2) -> Vector("fluent", "timestamp"),
    AtomSignature("TerminatedAt", 2) -> Vector("fluent", "timestamp")
  )

  private  val otherPredicateSchema = Map(
    AtomSignature("HoldsAt", 2) -> Vector("fluent", "timestamp")
  )

  private val inputSignatures = inputPredicateSchema.keySet

  private val targetSignatures = targetPredicateSchema.keySet

  private val predicateSchema = inputPredicateSchema ++ targetPredicateSchema ++ otherPredicateSchema

  private val functionSchema = Map(
    AtomSignature("aggr", 5) -> ("event", Vector("locid", "lane", "occupancy", "vehicles", "avgSpeed")),
    AtomSignature("idle", 2) -> ("fluent", Vector("locid", "lane")),
    AtomSignature("congested", 2) -> ("fluent", Vector("locid", "lane"))
  )

  private val constants = Map[String, ConstantsSet](
    "timestamp" -> ConstantsSet((1 to 10).map(_.toString): _* ),
    "event" -> ConstantsSet("Walking", "Running", "Active", "Inactive", "Exit", "Enter"),
    "fluent" -> ConstantsSet("Move", "Meet"),
    "locid" -> ConstantsSet("10314363961353708", "10314363961353709"),
    "lane" -> ConstantsSet("Onramp", "Exit"),
    "avg_speed" -> ConstantsSet((0 to 150).map(_.toString): _* ),
    "occupancy" -> ConstantsSet((0 to 100).map(_.toDouble.toString): _* ),
    "vehicles" -> ConstantsSet((0 to 100).map(_.toDouble.toString): _* )
  )


  val kbParser = new KBParser(predicateSchema, functionSchema)

  val transformableKB = Seq(
    // Requires compilation, since it contains an existentially quantified variable `e` in the body and `e` is bounded
    "InitiatedAt(f,t) :- HappensAt(e,t) ^ e = Walking",

    // Requires compilation, since it contains an existentially quantified variable `e` in the body and `e` is bounded
    "InitiatedAt(f,t) :- HappensAt(e,t) ^ e < 10 ^ e > 0",

    // Does not need any compilation, since it doesn't contain any existentially quantified variable in the body
    "TerminatedAt(f,t) :- HappensAt(Exit, t) ^ t > 10",

    "InitiatedAt(idle(lid, l), t) :- HappensAt(aggr(lid,l,occ,v,avgs), t) ^ v = 0",

    "InitiatedAt(congested(lid, l), t) :- HappensAt(aggr(lid,l,occ,v,avgs), t) ^ v > 0 ^ v < 3 ^ avgs < 10")
    .map(c => kbParser.parseDefiniteClause(c))

  // Cannot be efficiently translated, since the existentially quantified variable tU is unbounded
  val notTransformableKB = Seq(
    "InitiatedAt(f,t) :- HappensAt(e, t) ^ HoldsAt(f, tU) ^ e = Walking.")
    .map(c => kbParser.parseDefiniteClause(c))



  describe("Rules with bounded body variables"){
    val results = KBSimplifier.transform(transformableKB, inputSignatures, targetSignatures, constants)

    they("can be successfully transformed"){
      assert(results.isSuccess)
    }

    they(s"result to '${transformableKB.size}' simplified rules"){
      assert(results.get.size  == transformableKB.size)
    }

    they("produce simplified rules with the same head declaration"){
      for(attempt <- results; result <- attempt)
        assert(result.originalRule.clause.head == result.transformedRule.clause.head,
          "The transformed rule should use the same head predicate with the original rule " +
            s"(${result.originalRule.clause.head} != ${result.transformedRule.clause.head})")
    }

    they("have derived atoms composed of shared variables with head predicates"){
      for(attempt <- results; result <- attempt) {
        val headVars = result.originalRule.clause.head.variables
        val bodyVars = result.originalRule.clause.body.variables
        val commonVars = headVars.intersect(bodyVars)
        val derivedBodyVars = result.transformedRule.clause.body.variables
        assert(commonVars.forall(derivedBodyVars.contains))
      }
    }

  }


  describe("Rules with unbounded body variables"){
    val results = KBSimplifier.transform(notTransformableKB, inputSignatures, targetSignatures, constants)

    they("cannot transformed"){
      assert(results.isFailure)
    }

  }


}
