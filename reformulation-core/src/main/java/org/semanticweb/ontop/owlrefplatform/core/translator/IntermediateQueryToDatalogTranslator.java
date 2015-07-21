package org.semanticweb.ontop.owlrefplatform.core.translator;

/*
 * #%L
 * ontop-reformulation-core
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.AggregateOperator;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.Avg;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.BinaryValueOperator;
import org.openrdf.query.algebra.Bound;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Compare.CompareOp;
import org.openrdf.query.algebra.Count;
import org.openrdf.query.algebra.Datatype;
import org.openrdf.query.algebra.Distinct;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.Group;
import org.openrdf.query.algebra.IsBNode;
import org.openrdf.query.algebra.IsLiteral;
import org.openrdf.query.algebra.IsURI;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.Lang;
import org.openrdf.query.algebra.LangMatches;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.MathExpr;
import org.openrdf.query.algebra.MathExpr.MathOp;
import org.openrdf.query.algebra.Max;
import org.openrdf.query.algebra.Min;
import org.openrdf.query.algebra.Not;
import org.openrdf.query.algebra.Or;
import org.openrdf.query.algebra.Order;
import org.openrdf.query.algebra.OrderElem;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.Reduced;
import org.openrdf.query.algebra.Regex;
import org.openrdf.query.algebra.SameTerm;
import org.openrdf.query.algebra.Slice;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Str;
import org.openrdf.query.algebra.Sum;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.UnaryValueOperator;
import org.openrdf.query.algebra.Union;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.semanticweb.ontop.model.BooleanExpression;
import org.semanticweb.ontop.model.BooleanOperationPredicate;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.Constant;
import org.semanticweb.ontop.model.DataAtom;
import org.semanticweb.ontop.model.DataTypePredicate;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.DatatypeFactory;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.ImmutableBooleanExpression;
import org.semanticweb.ontop.model.ImmutableFunctionalTerm;
import org.semanticweb.ontop.model.ListenableFunction;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDAQueryModifiers;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.Term;
import org.semanticweb.ontop.model.ValueConstant;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.model.Predicate.COL_TYPE;
import org.semanticweb.ontop.model.impl.MutableQueryModifiersImpl;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.model.impl.OBDAVocabulary;
import org.semanticweb.ontop.owlrefplatform.core.abox.SemanticIndexURIMap;
import org.semanticweb.ontop.model.Substitution;
import org.semanticweb.ontop.owlrefplatform.core.basicoperations.SubstitutionUtilities;
import org.semanticweb.ontop.owlrefplatform.core.basicoperations.UriTemplateMatcher;
import org.semanticweb.ontop.pivotalrepr.ConstructionNode;
import org.semanticweb.ontop.pivotalrepr.DataNode;
import org.semanticweb.ontop.pivotalrepr.FilterNode;
import org.semanticweb.ontop.pivotalrepr.GroupNode;
import org.semanticweb.ontop.pivotalrepr.ImmutableQueryModifiers;
import org.semanticweb.ontop.pivotalrepr.InnerJoinNode;
import org.semanticweb.ontop.pivotalrepr.IntermediateQuery;
import org.semanticweb.ontop.pivotalrepr.JoinLikeNode;
import org.semanticweb.ontop.pivotalrepr.LeftJoinNode;
import org.semanticweb.ontop.pivotalrepr.QueryModifiers;
import org.semanticweb.ontop.pivotalrepr.QueryNode;
import org.semanticweb.ontop.pivotalrepr.UnionNode;
import org.semanticweb.ontop.pivotalrepr.impl.VariableCollector;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

/***
 * Translate a intermediate queries expression into a Datalog program that has the
 * same semantics. We use the built-int predicates Join and Left join. The rules
 * in the program have always 1 or 2 operator atoms, plus (in)equality atoms
 * (due to filters).
 * 
 * 
 * @author mrezk
 */
public class IntermediateQueryToDatalogTranslator {
	
	private final static OBDADataFactory ofac = OBDADataFactoryImpl.getInstance();
	
	//private final DatatypeFactory dtfac = OBDADataFactoryImpl.getInstance().getDatatypeFactory();

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(IntermediateQueryToDatalogTranslator.class);

	/**
	 * Translate an intermediate query tree into a Datalog program 
	 * 
	 */
	public static DatalogProgram translate(IntermediateQuery te) {
		

		
		DatalogProgram dProgram = ofac.getDatalogProgram();
		ConstructionNode root = te.getRootConstructionNode();
		
		Optional<ImmutableQueryModifiers> optionalModifiers =  root.getOptionalModifiers();
		
		if (optionalModifiers.isPresent()){
			QueryModifiers immutableQueryModifiers = optionalModifiers.get();

			// Mutable modifiers (used by the Datalog)
			OBDAQueryModifiers mutableModifiers = new MutableQueryModifiersImpl(immutableQueryModifiers);
			// TODO: support GROUP BY (distinct QueryNode)

			dProgram.setQueryModifiers(mutableModifiers);
			
		}
		
		
	
		Queue<ConstructionNode> rulesToDo = new LinkedList<ConstructionNode>();
		rulesToDo.add(root);

		//In rulesToDo we keep the nodes that represent sub-rules in the program, e.g. ans5 :- LeftJoin(....)
		while(!rulesToDo.isEmpty()){
			translate(te,  dProgram, rulesToDo);
		}
		
	
		
		
		return dProgram;
	}
	
	/**
	 * Translate a given IntermediateQuery query object to datalog program.
	 * 
	 *           
	 * @return Datalog program that represents the construction of the SPARQL
	 *         query.
	 */
	private static void translate(IntermediateQuery te,   DatalogProgram pr, Queue<ConstructionNode> rulesToDo  ) {
		
		ConstructionNode root = rulesToDo.poll();
		
		DataAtom head= root.getProjectionAtom();
	
		//Applying substitutions in the head.
		ImmutableFunctionalTerm substitutedHead= root.getSubstitution().applyToFunctionalTerm(head);
		List<QueryNode> listNodes=  te.getCurrentSubNodesOf(root);
		
		List<Function> atoms = new LinkedList<Function>();
		
		//Constructing the rule
		CQIE newrule = ofac.getCQIE(substitutedHead, atoms);
		
		pr.appendRule(newrule);
		
		//Iterating over the nodes and constructing the rule
		for (QueryNode node: listNodes){
			
			List<Function> uAtoms= getAtomFrom(te, node, rulesToDo);
			newrule.getBody().addAll(uAtoms);	
			
		} //end-for
	}

	

	/**
	 * This is the MAIN recursive method in this class!!
	 * Takes a node and return the list of functions (atoms) that it represents.
	 * Usually it will be a single atom, but it is different for the filter case.
	 */
	private static List<Function> getAtomFrom(IntermediateQuery te, QueryNode node,  Queue<ConstructionNode> rulesToDo  ) {
		
		List<Function> body = new ArrayList<Function>();
		
		/**
		 * Basic Atoms
		 */
		
		if (node instanceof ConstructionNode) {
			DataAtom newAns = ((ConstructionNode) node).getProjectionAtom();
			Function mutAt = convertToMutableFunction(newAns);
			rulesToDo.add((ConstructionNode)node);
			body.add(mutAt);
			return body;
			
		} else if (node instanceof FilterNode) {
			ImmutableBooleanExpression filter = ((FilterNode) node).getFilterCondition();
			BooleanExpression mutFilter =  convertToMutableFunction(filter);
			List<QueryNode> listnode =  te.getCurrentSubNodesOf(node);
			body.addAll(getAtomFrom(te, listnode.get(0), rulesToDo));
			body.add(mutFilter);
			return body;
			
					
		} else if (node instanceof DataNode) {
			DataAtom atom = ((DataNode)node).getAtom();
			Function mutAt = convertToMutableFunction(atom);
			body.add(mutAt);
			return body;
				
			
			
		/**
		 * Nested Atoms	
		 */
		} else  if (node instanceof InnerJoinNode) {
			Optional<ImmutableBooleanExpression> filter = ((InnerJoinNode)node).getOptionalFilterCondition();
			List<Function> atoms = new LinkedList<Function>();
			List<QueryNode> listnode =  te.getCurrentSubNodesOf(node);
			for (QueryNode childnode: listnode) {
				List<Function> atomsList = getAtomFrom(te, childnode, rulesToDo);
				atoms.addAll(atomsList);
			}
			if (filter.isPresent()){
				ImmutableBooleanExpression filter2 = filter.get();
				Function mutFilter = convertToMutableFunction(filter2);
				Function newJ = ofac.getSPARQLJoin(atoms, mutFilter);
				body.add(newJ);
				return body;
			}else{
				Function newJ = ofac.getSPARQLJoin(atoms);
				body.add(newJ);
				return body;
			}
			
		} else if (node instanceof LeftJoinNode) {
			Optional<ImmutableBooleanExpression> filter = ((LeftJoinNode)node).getOptionalFilterCondition();
			List<QueryNode> listnode =  te.getCurrentSubNodesOf(node);
		
			List<Function> atomsListLeft = getAtomFrom(te, listnode.get(0), rulesToDo);
			List<Function> atomsListRight = getAtomFrom(te, listnode.get(1), rulesToDo);

				
			if (filter.isPresent()){
				ImmutableBooleanExpression filter2 = filter.get();
				BooleanExpression mutFilter =  convertToMutableFunction(filter2);
				Function newLJAtom = ofac.getSPARQLLeftJoin(atomsListLeft, atomsListRight, mutFilter);
				body.add(newLJAtom);
				return body;
			}else{
				Function newLJAtom = ofac.getSPARQLLeftJoin(atomsListLeft, atomsListRight);
				body.add(newLJAtom);
				return body;
			}

		} else if (node instanceof UnionNode) {
		
			List<QueryNode> listnode =  te.getCurrentSubNodesOf(node);
			
			for (QueryNode nod: listnode){
				rulesToDo.add((ConstructionNode)nod);
		
			} //end for
		
			QueryNode nod= listnode.get(0);
			if (nod instanceof ConstructionNode) {
					Function newAns = ((ConstructionNode) nod).getProjectionAtom();
					body.add(newAns);
					return body;
				}else{
					 throw new UnsupportedOperationException("The Union should have only construct");
				}
			

						
		} else {
			 throw new UnsupportedOperationException("Type od node in the intermediate tree is unknown!!");
		}
	
	}

	
	
	
	
	
	/**
	 * This method takes a immutable term and convert it into an old mutable function.
	 * 
	 * @param term
	 * @return
	 */
	private static Function convertToMutableFunction( ImmutableFunctionalTerm term  ) {
		
		Predicate pred= term.getFunctionSymbol();
		ImmutableList<Term> otherTerms =  term.getTerms();
		List<Term> mutableList = new LinkedList<Term>();
		UnmodifiableIterator<Term> iterator = otherTerms.iterator();
		while ( iterator.hasNext()){

			Term nextTerm = iterator.next();
			if (nextTerm instanceof ImmutableFunctionalTerm ){
				ImmutableFunctionalTerm term2Change = (ImmutableFunctionalTerm) nextTerm;
				Function newTerm = convertToMutableFunction(term2Change);
				mutableList.add(newTerm);
			} else{
				mutableList.add(nextTerm);
			}
			
		}
		Function mutFunc = ofac.getFunction(pred, mutableList);
		return mutFunc;
		
	}
	
	/**
	 * This method takes a immutable booelan term and convert it into an old mutable boolean function.
	 * 
	 * @param term
	 * @return
	 */
	private static BooleanExpression convertToMutableFunction( ImmutableBooleanExpression filter  ) {
		
		BooleanOperationPredicate pred= (BooleanOperationPredicate) filter.getFunctionSymbol();
		ImmutableList<Term> otherTerms =  filter.getTerms();
		List<Term> mutableList = new LinkedList<Term>();
		
		UnmodifiableIterator<Term> iterator = otherTerms.iterator();
		while ( iterator.hasNext()){

			Term nextTerm = iterator.next();
			if (nextTerm instanceof ImmutableFunctionalTerm ){
				ImmutableFunctionalTerm term2Change = (ImmutableFunctionalTerm) nextTerm;
				Function newTerm = convertToMutableFunction(term2Change);
				mutableList.add(newTerm);
			} else{
				mutableList.add(nextTerm);
			}
			
		}
		BooleanExpression mutFunc = ofac.getBooleanExpression(pred,mutableList); 
		return mutFunc;
		
	}
		
	
}
