package it.unibz.inf.ontop.reformulation.owlapi3;

/*
 * #%L
 * ontop-quest-owlapi
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

import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWL;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLConfiguration;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLFactory;
import junit.framework.TestCase;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.Class;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

public class InconsistencyCheckingTest extends TestCase{

	private QuestOWL reasoner;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	
	String prefix = "http://www.example.org/";
	OWLClass c1 = Class(IRI.create(prefix + "Male"));
	OWLClass c2 = Class(IRI.create(prefix + "Female"));
	
	OWLObjectProperty r1 = ObjectProperty(IRI.create(prefix + "hasMother"));
	OWLObjectProperty r2 = ObjectProperty(IRI.create(prefix + "hasFather"));
	
	OWLDataProperty d1 = DataProperty(IRI.create(prefix + "hasAgeFirst"));
	OWLDataProperty d2 = DataProperty(IRI.create(prefix + "hasAge"));

	OWLNamedIndividual a = NamedIndividual(IRI.create(prefix + "a"));
	OWLNamedIndividual b = NamedIndividual(IRI.create(prefix + "b"));
	OWLNamedIndividual c = NamedIndividual(IRI.create(prefix + "c"));
	
	@Override
	public void setUp() throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		ontology = Ontology(manager, //
				Declaration(c1),
				Declaration(c2),
				Declaration(r1), //
				Declaration(r2), //
				Declaration(d1), //
				Declaration(d2)
		);
	}
	
	private void startReasoner() {
		QuestPreferences p = new QuestPreferences();
		p.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
		p.setCurrentValueOf(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");

		QuestOWLFactory factory = new QuestOWLFactory();
        QuestOWLConfiguration config = QuestOWLConfiguration.builder().preferences(p).build();
        reasoner = factory.createReasoner(ontology, config);
	}
	
	@Test
	public void testInitialConsistency() {
		//initially the ontology is consistent
		startReasoner();
		assertTrue(reasoner.isQuestConsistent());
	}
	
	@Test
	public void testDisjointClassInconsistency() throws OWLOntologyCreationException {

		//Male(a), Female(a), disjoint(Male, Female)
		manager.addAxiom(ontology, ClassAssertion(c1, a));
		manager.addAxiom(ontology, ClassAssertion(c2, a));
		manager.addAxiom(ontology, DisjointClasses(c1, c2));
		
		startReasoner();
		
		boolean consistent = reasoner.isQuestConsistent();
		assertFalse(consistent);

	} 
	
	@Test
	public void testDisjointObjectPropInconsistency() throws OWLOntologyCreationException {

		//hasMother(a, b), hasFather(a,b), disjoint(hasMother, hasFather)
		manager.addAxiom(ontology,ObjectPropertyAssertion(r1, a, b)); //
		manager.addAxiom(ontology,ObjectPropertyAssertion(r2, a, b)); //
		manager.addAxiom(ontology, DisjointObjectProperties(r1, r2));
		
		startReasoner();
		
		boolean consistent = reasoner.isQuestConsistent();
		assertFalse(consistent);

	} 
	
	@Test
	public void testDisjointDataPropInconsistency() throws OWLOntologyCreationException {

		//hasAgeFirst(a, 21), hasAge(a, 21), disjoint(hasAgeFirst, hasAge)
		manager.addAxiom(ontology, DataPropertyAssertion(d1, a, Literal(21)));
		manager.addAxiom(ontology, DataPropertyAssertion(d2, a, Literal(21)));
		manager.addAxiom(ontology, DisjointDataProperties(d1, d2));
		
		startReasoner();
		
		boolean consistent = reasoner.isQuestConsistent();
		assertFalse(consistent);

	} 
	
	@Test
	public void testFunctionalObjPropInconsistency() throws OWLOntologyCreationException {

		//hasMother(a,b), hasMother(a,c), func(hasMother)
		manager.addAxiom(ontology,ObjectPropertyAssertion(r1, a, b)); //
		manager.addAxiom(ontology,ObjectPropertyAssertion(r1, a, c)); //
		manager.addAxiom(ontology, FunctionalObjectProperty(r1));
		
		startReasoner();
		
		boolean consistent = reasoner.isQuestConsistent();
		assertFalse(consistent);

	} 
	
	@Test
	public void testFunctionalDataPropInconsistency() throws OWLOntologyCreationException {

		//hasAge(a, 18), hasAge(a, 21), func(hasAge)
		manager.addAxiom(ontology, DataPropertyAssertion(d1, a, Literal(18)));
		manager.addAxiom(ontology, DataPropertyAssertion(d1, a, Literal(21)));
		manager.addAxiom(ontology, FunctionalDataProperty(d1));
		
		startReasoner();
		
		boolean consistent = reasoner.isQuestConsistent();
		assertFalse(consistent);

	} 
}
