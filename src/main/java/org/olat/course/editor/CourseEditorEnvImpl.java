/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <p>
*/ 

package org.olat.course.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org._3pq.jgrapht.DirectedGraph;
import org._3pq.jgrapht.Edge;
import org._3pq.jgrapht.alg.CycleDetector;
import org._3pq.jgrapht.edge.EdgeFactories;
import org._3pq.jgrapht.edge.EdgeFactories.DirectedEdgeFactory;
import org._3pq.jgrapht.graph.DefaultDirectedGraph;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.tree.TreeVisitor;
import org.olat.core.util.tree.Visitor;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.interpreter.ConditionErrorMessage;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.ENCourseNode;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.group.area.BGArea;

/**
 * Description:<br>
 * TODO: guido Class Description for CourseEditorEnvImpl
 */
public class CourseEditorEnvImpl implements CourseEditorEnv {
	/**
	 * the course editor tree model used in this editing session, exist only once
	 * per open course editor
	 */
	private CourseEditorTreeModel cetm;
	String currentCourseNodeId = null;
	/**
	 * the course group manager is used for answering the existXXX questions
	 * concering, groups and areas
	 */
	private CourseGroupManager cgm;
	/**
	 * the editor locale, it is used in the condition interpreter to provide
	 * localized error messages.
	 */
	private Locale editorLocale;
	/**
	 * book keeping of (coursNodeId,
	 * {conditionexpression,conditionexpression,...}) TODO: do we really need the
	 * information splitted up by category and condition expression?
	 */
	Map softRefs = new HashMap();
	/**
	 * book keeping of (courseNodeId, StatusDescription)
	 */
	Map statusDescs = new HashMap();
	/**
	 * current active condition expression, it is activated by a call to
	 * <code>validateConditionExpression(..)</code> the condition interpreter is
	 * then asked for validating the expression. This validation parses the
	 * expression into the atomic functions etc, which in turn access the
	 * <code>CourseEditorEnvImpl</code> to <code>pushError()</code> and
	 * <code>addSoftReference()</code>.
	 */
	ConditionExpression currentConditionExpression = null;
	/**
	 * different organized info as in softRefs: (nodeId,{nodeid,nodeId,...})
	 */
	Map<String, Set<String>> nodeRefs = new HashMap<String, Set<String>>();
	/**
	 * the condition interpreter for evaluating the condtion expressions.
	 */
	ConditionInterpreter ci = null;

	public CourseEditorEnvImpl(CourseEditorTreeModel cetm, CourseGroupManager cgm, Locale editorLocale) {
		this.cetm = cetm;
		this.cgm = cgm;
		this.editorLocale = editorLocale;
	}

	/**
	 * @param ci
	 */
	public void setConditionInterpreter(ConditionInterpreter ci) {
		this.ci = ci;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#isEnrollmentNode(java.lang.String)
	 */
	public boolean isEnrollmentNode(String nodeId) {
		CourseEditorTreeNode cen = cetm.getCourseEditorNodeById(nodeId);
		if (cen == null) return false;
		if (cen.isDeleted()) return false;
		// node exists and is not marked as deleted, check the associated course
		// node correct type
		return (cen.getCourseNode() instanceof ENCourseNode);
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#isAssessable(java.lang.String)
	 */
	public boolean isAssessable(String nodeId) {
		CourseEditorTreeNode cen = cetm.getCourseEditorNodeById(nodeId);
		if (cen == null) return false;
		if (cen.isDeleted()) return false;
		// node exists and is not marked as deleted, check the associated course
		// node for assessability.
		return AssessmentHelper.checkIfNodeIsAssessable(cen.getCourseNode());
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#existsNode(java.lang.String)
	 */
	public boolean existsNode(String nodeId) {
		CourseEditorTreeNode cen = cetm.getCourseEditorNodeById(nodeId);
		boolean retVal = cen != null && !cen.isDeleted();
		return retVal;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#existsGroup(java.lang.String)
	 */
	public boolean existsGroup(String groupname) {
		// FIXME:fg:b improve performance by adding a special query for the existence
		// check!
		List cnt = cgm.getLearningGroupsFromAllContexts(groupname);
		return (cnt != null && cnt.size() > 0);
	}

	public boolean existsRightGroup(String groupname) {
		List cnt = cgm.getRightGroupsFromAllContexts(groupname);
		return (cnt != null && cnt.size() > 0);
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#existsArea(java.lang.String)
	 */
	public boolean existsArea(String areaname) {
		// FIXME:fg:b improve performance by adding a special query for the existence
		// check!
		List cnt = cgm.getAllAreasFromAllContexts();
		for (Iterator iter = cnt.iterator(); iter.hasNext();) {
			BGArea element = (BGArea) iter.next();
			if (element.getName().equals(areaname)) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#getCurrentCourseNodeId()
	 */
	public String getCurrentCourseNodeId() {
		return currentCourseNodeId;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#setCurrentCourseNodeId(java.lang.String)
	 */
	public void setCurrentCourseNodeId(String courseNodeId) {
		this.currentCourseNodeId = courseNodeId;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#getEditorEnvLocale()
	 */
	public Locale getEditorEnvLocale() {
		return editorLocale;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#validateConditionExpression(org.olat.course.condition.interpreter.ConditionExpression)
	 */
	public ConditionErrorMessage[] validateConditionExpression(ConditionExpression condExpr) {
		// first set the active condition expression, which will be accessed from
		// the conditions functions inserting soft references
		currentConditionExpression = condExpr;
		if(condExpr.getExptressionString()==null) {
			return null;
		}
		// evaluate expression
		ConditionErrorMessage[] cems = ci.syntaxTestExpression(condExpr);
		if (softRefs.containsKey(this.currentCourseNodeId)) {
			List condExprs = (ArrayList) softRefs.get(this.currentCourseNodeId);
			for (Iterator iter = condExprs.iterator(); iter.hasNext();) {
				ConditionExpression element = (ConditionExpression) iter.next();
				if (element.getId().equals(currentConditionExpression.getId())) {
					condExprs.remove(element);
					break;
				}
			}
			condExprs.add(currentConditionExpression);
		} else {
			List condExprs = new ArrayList();
			condExprs.add(currentConditionExpression);
			softRefs.put(currentCourseNodeId, condExprs);
		}

		//
		return cems;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#addSoftReference(java.lang.String,
	 *      java.lang.String)
	 */
	public void addSoftReference(String category, String softReference) {
		currentConditionExpression.addSoftReference(category, softReference);

	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#pushError(java.lang.Exception)
	 */
	public void pushError(Exception e) {
		currentConditionExpression.pushError(e);
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#validateCourse()
	 */
	public void validateCourse() {
		/*
		 * collect all condition error messages and soft references collect all
		 * configuration errors.
		 */
		String currentNodeWas = currentCourseNodeId;
		// reset all
		softRefs = new HashMap();
		nodeRefs = new HashMap<String, Set<String>>();
		Visitor v = new CollectConditionExpressionsVisitor();
		(new TreeVisitor(v, cetm.getRootNode(), true)).visitAll();
		for (Iterator iter = softRefs.keySet().iterator(); iter.hasNext();) {
			String nodeId = (String) iter.next();
			List conditionExprs = (List) softRefs.get(nodeId);
			for (int i = 0; i < conditionExprs.size(); i++) {
				ConditionExpression ce = (ConditionExpression) conditionExprs.get(i);
				// DO NOT validateConditionExpression(ce) as this is already done in the
				// CollectConditionExpressionsVisitor
				Set<String> refs = new HashSet<String>(ce.getSoftReferencesOf("courseNodeId"));
				if (refs != null && refs.size() > 0) {
					Set<String> oldOnes = nodeRefs.put(nodeId, null);
					if (oldOnes != null) {
						refs.addAll(oldOnes);
					}
					nodeRefs.put(nodeId, refs);
				}
			}

		}
		// refresh,create status descriptions of the course
		statusDescs = new HashMap();
		v = new CollectStatusDescriptionVisitor(this);
		(new TreeVisitor(v, cetm.getRootNode(), true)).visitAll();
		//
		currentCourseNodeId = currentNodeWas;
	}

	/**
	 * @see org.olat.course.editor.CourseEditorEnv#getCourseStatus()
	 */
	public StatusDescription[] getCourseStatus() {
		String[] a = new String[statusDescs.keySet().size()];
		a = (String[]) statusDescs.keySet().toArray(a);
		Arrays.sort(a);
		List all2gether = new ArrayList();
		for (int i = a.length - 1; i >= 0; i--) {
			all2gether.addAll((List) statusDescs.get(a[i]));
		}
		StatusDescription[] retVal = new StatusDescription[all2gether.size()];
		retVal = (StatusDescription[]) all2gether.toArray(retVal);
		return retVal;
	}

	public List getReferencingNodeIdsFor(String ident) {
		List refNodes = new ArrayList();
		for (Iterator iter = nodeRefs.keySet().iterator(); iter.hasNext();) {
			String nodeId = (String) iter.next();
			if (!nodeId.equals(ident)) {
				// self references are catched during form entering
				Set refs = (Set) nodeRefs.get(nodeId);
				if (refs.contains(ident)) {
					// nodeId references ident
					refNodes.add(nodeId);
				}
			}
		}
		return refNodes;
	}

	
	public String toString() {
		String retVal = "";
		Set keys = softRefs.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String nodId = (String) iter.next();
			retVal += "nodeId:" + nodId + "\n";
			List conditionExprs = (List) softRefs.get(nodId);
			for (Iterator iterator = conditionExprs.iterator(); iterator.hasNext();) {
				ConditionExpression ce = (ConditionExpression) iterator.next();
				retVal += "\t" + ce.toString() + "\n";
			}
			retVal += "\n";
		}
		return retVal;
	}

	class CollectStatusDescriptionVisitor implements Visitor {
		private CourseEditorEnv cev;

		public CollectStatusDescriptionVisitor(CourseEditorEnv cev) {
			this.cev = cev;
		}

		/**
		 * @see org.olat.core.util.tree.Visitor#visit(org.olat.core.util.nodes.INode)
		 */
		public void visit(INode node) {
			/**
			 * collect only status descriptions of not deleted nodes
			 */
			CourseEditorTreeNode tmp = (CourseEditorTreeNode) node;
			if (!tmp.isDeleted()) {
				CourseNode cn = tmp.getCourseNode();
				String key = cn.getIdent();
				StatusDescription[] allSds = cn.isConfigValid(cev);
				if (allSds.length > 0) {
					for (int i = 0; i < allSds.length; i++) {
						StatusDescription sd = allSds[i];
						if (sd != StatusDescription.NOERROR) {
							if (!statusDescs.containsKey(key)) {
								statusDescs.put(key, new ArrayList());
							}
							List sds = (List) statusDescs.get(key);
							sds.add(sd);
						}
					}
				}
			}
		}

	}

	class CollectConditionExpressionsVisitor implements Visitor {
		/**
		 * @see org.olat.core.util.tree.Visitor#visit(org.olat.core.util.nodes.INode)
		 */
		public void visit(INode node) {
			/**
			 * collect condition expressions only for not deleted nodes
			 */
			CourseEditorTreeNode tmp = (CourseEditorTreeNode) node;
			CourseNode cn = tmp.getCourseNode();
			String key = cn.getIdent();
			List condExprs = cn.getConditionExpressions();
			if (condExprs.size() > 0 && !tmp.isDeleted()) {
				// evaluate each expression
				for (Iterator iter = condExprs.iterator(); iter.hasNext();) {
					ConditionExpression ce = (ConditionExpression) iter.next();
					currentCourseNodeId = key;
					currentConditionExpression = ce;
					ci.syntaxTestExpression(ce);
				}
				// add it to the cache.
				softRefs.put(key, condExprs);
			}
		}

	}

	class Convert2DGVisitor implements Visitor{
		private DirectedEdgeFactory def;
		private DirectedGraph dg;
		public Convert2DGVisitor(DirectedGraph dg) {
			this.dg = dg;
			def = new EdgeFactories.DirectedEdgeFactory();
		}
		public void visit(INode node) {
			CourseEditorTreeNode tmp = (CourseEditorTreeNode) node;
			CourseNode cn = tmp.getCourseNode();
			String key = cn.getIdent();
			dg.addVertex(key);
			/*
			 * add edge from parent to child. This directed edge represents the visibility accessability inheritance direction.
			 */
			INode parent = tmp.getParent();
			if(parent!=null) {
				dg.addVertex(parent.getIdent());
				Edge toParent = def.createEdge( parent.getIdent(),key);
				dg.addEdge(toParent);
			}
		}
		
	}
	
	/**
	 * 
	 * @see org.olat.course.editor.CourseEditorEnv#listCycles()
	 */
	public Set<String> listCycles() {
		/*
		 * convert nodeRefs datastructure to a directed graph 
		 */
		DirectedGraph dg = new DefaultDirectedGraph();
		DirectedEdgeFactory def = new EdgeFactories.DirectedEdgeFactory();
		/*
		 * add the course structure as directed graph, where 
		 */
		Visitor v = new Convert2DGVisitor(dg);
		(new TreeVisitor(v, cetm.getRootNode(), true)).visitAll();
		/*
		 * iterate over nodeRefs, add each not existing node id as vertex, for each
		 * key - child relation add an edge to the directed graph.
		 */
		Iterator<String> keys = nodeRefs.keySet().iterator();
		while(keys.hasNext()) {
			//a node
			String key = keys.next();
			if(!dg.containsVertex(key)) {
				dg.addVertex(key);
			}
			//and its children
			Set<String> children = nodeRefs.get(key);
			Iterator<String> childrenIt = children.iterator();
			while(childrenIt.hasNext()){
				String child = childrenIt.next();
				if(!dg.containsVertex(child)) {
					dg.addVertex(child);
				}
				//add edge, precondition: vertex key - child are already added to the graph
				Edge de = def.createEdge(key, child);
				dg.addEdge(de);
			}
		}
		/*
		 * find the id's participating in the cycle, and return the intersection
		 * with set of id's which actually produce references.
		 */
		CycleDetector cd = new CycleDetector(dg);
		Set<String> cycleIds = cd.findCycles();
		cycleIds.retainAll(nodeRefs.keySet());
		return cycleIds;
	}

	/**
	 * 
	 * @return CourseGroupManager for this course environment
	 */
	public CourseGroupManager getCourseGroupManager() {
		return cgm;
	}


}