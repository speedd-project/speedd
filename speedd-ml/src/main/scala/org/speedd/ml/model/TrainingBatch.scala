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
package org.speedd.ml.model

import lomrf.logic.{Clause, AtomSignature}
import lomrf.mln.model.{Evidence, MLNSchema}

/**
 * This class holds the data of a single (micro) batch
 *
 * @param mlnSchema the MLN schema definition for LoMRF
 * @param trainingEvidence the training evidence of this batch (i.e., ground input predicates and ground annotation predicates)
 * @param nonEvidenceAtoms the set of atomic signatures of all non-evidence atoms in the given training evidence (i.e., annotation predicates)
 * @param clauses a collection of clauses in Clausal Normal Form
 */
case class TrainingBatch(mlnSchema: MLNSchema,
                         trainingEvidence: Evidence,
                         nonEvidenceAtoms: Set[AtomSignature],
                         clauses: Vector[Clause])
