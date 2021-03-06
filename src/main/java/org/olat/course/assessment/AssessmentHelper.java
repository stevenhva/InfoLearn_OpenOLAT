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
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.course.assessment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.tree.TreeVisitor;
import org.olat.core.util.tree.Visitor;
import org.olat.course.ICourse;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.STCourseNode;
import org.olat.course.nodes.ScormCourseNode;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<br>
 * Helper methods for the course assessment system
 * <P>
 * Initial Date: Oct 28, 2004<br>
 * @author gnaegi
 */
public class AssessmentHelper {
	
	private static final OLog log = Tracing.createLoggerFor(AssessmentHelper.class);

	/**
	 * String to symbolize 'not available' or 'not assigned' in assessments
	 * details *
	 */
	public static final String DETAILS_NA_VALUE = "n/a";

	/** Highes score value supported by OLAT * */
	public static final float MAX_SCORE_SUPPORTED = 10000f;
	/** Lowest score value supported by OLAT * */
	public static final float MIN_SCORE_SUPPORTED = -10000f;
	//fxdiff VCRP-4: assessment overview with max score
	private final static DecimalFormat scoreFormat = new DecimalFormat("#0.###", new DecimalFormatSymbols(Locale.ENGLISH));

	/**
	 * Wraps an identity and it's score evaluation / attempts in a wrapper object
	 * for a given course node
	 * 
	 * @param identity
	 * @param localUserCourseEnvironmentCache
	 * @param course the course
	 * @param courseNode an assessable course node or null if no details and
	 *          attempts must be fetched
	 * @return a wrapped identity
	 */
	public static AssessedIdentityWrapper wrapIdentity(Identity identity, Map<Long,UserCourseEnvironment> localUserCourseEnvironmentCache,
			Map<Long, Date> initialLaunchDates, ICourse course, AssessableCourseNode courseNode) {
		// Try to get user course environment from local hash map cache. If not
		// successful
		// create the environment and add it to the map for later performance
		// optimization
			UserCourseEnvironment uce = localUserCourseEnvironmentCache.get(identity.getKey());
			if (uce == null) {
				uce = createAndInitUserCourseEnvironment(identity, course);
				// add to cache for later usage
				localUserCourseEnvironmentCache.put(identity.getKey(), uce);
				if (log.isDebug()){
					log.debug("localUserCourseEnvironmentCache hit failed, adding course environment for user::"
						+ identity.getName());
				}
			}
			
			Date initialLaunchDate = initialLaunchDates.get(identity.getKey());
			return wrapIdentity(uce, initialLaunchDate, courseNode);
	}

	/**
	 * Wraps an identity and it's score evaluation / attempts in a wrapper object
	 * for a given course node
	 * 
	 * @param uce The users course environment. Must be initialized
	 *          (uce.getScoreAccounting().evaluateAll() must be called previously)
	 * @param courseNode an assessable course node or null if no details and
	 *          attempts must be fetched
	 * @return a wrapped identity
	 */
	public static AssessedIdentityWrapper wrapIdentity(UserCourseEnvironment uce, Date initialLaunchDate, AssessableCourseNode courseNode) {
		// Fetch attempts and details for this node if available
		Integer attempts = null;
		String details = null;
		if (courseNode != null) {
			if (courseNode.hasAttemptsConfigured()) {
				attempts = courseNode.getUserAttempts(uce);
			}
			if (courseNode.hasDetails()) {
				details = courseNode.getDetailsListView(uce);
				if (details == null) details = DETAILS_NA_VALUE;
			}
		}

		Identity identity = uce.getIdentityEnvironment().getIdentity();
		Date lastModified = uce.getCourseEnvironment().getAssessmentManager().getScoreLastModifiedDate(courseNode, identity);
		AssessedIdentityWrapper aiw = new AssessedIdentityWrapper(uce, attempts, details, initialLaunchDate, lastModified);
		return aiw;
	}

	/**
	 * Create a user course environment for the given user and course. After
	 * creation, the users score accounting will be initialized.
	 * 
	 * @param identity
	 * @param course
	 * @return Initialized user course environment
	 */
	public static UserCourseEnvironment createAndInitUserCourseEnvironment(Identity identity, ICourse course) {
		// create an identenv with no roles, no attributes, no locale
		IdentityEnvironment ienv = new IdentityEnvironment(); 
		ienv.setIdentity(identity);
		UserCourseEnvironment uce = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
		// Fetch all score and passed and calculate score accounting for the entire
		// course
		uce.getScoreAccounting().evaluateAll();
		return uce;
	}

	/**
	 * check the given node for assessability.
	 * @param node
	 * @return
	 */
	public static boolean checkIfNodeIsAssessable(CourseNode node) {
		if (node instanceof AssessableCourseNode) {
			if (node instanceof STCourseNode) {
				STCourseNode scn = (STCourseNode) node;
				if (scn.hasPassedConfigured() || scn.hasScoreConfigured()) {
					return true;
				}
			} else if (node instanceof ScormCourseNode) {
				ScormCourseNode scormn = (ScormCourseNode) node;
				if (scormn.hasPassedConfigured() || scormn.hasScoreConfigured()) {
					return true;
				}
			} else if (node instanceof ProjectBrokerCourseNode) {
				return false;// TODO:cg 28.01.2010 ProjectBroker : no assessment-tool in V1.0 return always false
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks recursivley a course structure or a part of it for assessable nodes
	 * or for structure course nodes (subtype of assessable node), which
	 * 'hasPassedConfigured' or 'hasScoreConfigured' is true. If founds the first
	 * node that meets the criterias, it returns true.
	 * 
	 * @param node
	 * @return boolean
	 */
	public static boolean checkForAssessableNodes(CourseNode node) {
		if(checkIfNodeIsAssessable(node)) {
			return true;
		}
		// check children now
		int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			CourseNode cn = (CourseNode) node.getChildAt(i);
			if (checkForAssessableNodes(cn)) return true;
		}
		return false;
	}

	/**
	 * Get all assessable nodes including the root node (if assessable)
	 * 
	 * @param editorModel
	 * @param excludeNode Node that should be excluded in the list, e.g. the
	 *          current node or null if all assessable nodes should be used
	 * @return List of assessable course nodes
	 */
	public static List<CourseNode> getAssessableNodes(final CourseEditorTreeModel editorModel, final CourseNode excludeNode) {
		CourseEditorTreeNode rootNode = (CourseEditorTreeNode) editorModel.getRootNode();
		final List<CourseNode> nodes = new ArrayList<CourseNode>();
		// visitor class: takes all assessable nodes if not the exclude node and
		// puts
		// them into the nodes list
		Visitor visitor = new Visitor() {
			public void visit(INode node) {
				CourseEditorTreeNode editorNode = (CourseEditorTreeNode) node;
				CourseNode courseNode = editorModel.getCourseNode(node.getIdent());
				if (!editorNode.isDeleted() && (courseNode != excludeNode)) {
					if(checkIfNodeIsAssessable(courseNode)) {
						nodes.add(courseNode);
					}
				}
			}
		};
		// not visit beginning at the root node
		TreeVisitor tv = new TreeVisitor(visitor, rootNode, false);
		tv.visitAll();

		return nodes;
	}

	/**
	 * @param score The score to be rounded
	 * @return The rounded score for GUI presentation
	 */
	//fxdiff VCRP-4: assessment overview with max score
	public static String getRoundedScore(Float score) {
		if (score == null) return null;

		//cluster_OK the formatter is not multi-thread and costly to create
		synchronized(scoreFormat) {
			return scoreFormat.format(score);
		}
		//return Formatter.roundToString(score.floatValue(), 3);
	}
	
	public static Float getRoundedScore(String score) {
		if (!StringHelper.containsNonWhitespace(score)) return null;

		//cluster_OK the formatter is not multi-thread and costly to create
		synchronized(scoreFormat) {
			try {
				return new Float(scoreFormat.parse(score).floatValue());
			} catch (ParseException e) {
				log.error("", e);
				return null;
			}
		}
	}

	public static final String KEY_TYPE = "type";
	public static final String KEY_IDENTIFYER = "identifyer";
	public static final String KEY_INDENT = "indent";

	public static final String KEY_TITLE_SHORT = "short.title";
	public static final String KEY_TITLE_LONG = "long.title";
	public static final String KEY_PASSED = "passed";
	public static final String KEY_SCORE = "score";
	public static final String KEY_SCORE_F = "fscore";
	public static final String KEY_ATTEMPTS = "attempts";
	public static final String KEY_DETAILS = "details";
	public static final String KEY_SELECTABLE = "selectable";
	//fxdiff VCRP-4: assessment overview with max score
	public static final String KEY_MIN = "minScore";
	public static final String KEY_MAX = "maxScore";
	public static final String KEY_TOTAL_NODES = "totalNodes";
	public static final String KEY_ATTEMPTED_NODES = "attemptedNodes";
	public static final String KEY_PASSED_NODES = "attemptedNodes";
	

	
	/**
	 * Add all assessable nodes and the scoring data to a list. Each item in the list is an object array
	 * that has the following data:
	 * @param recursionLevel
	 * @param courseNode
	 * @param userCourseEnv
	 * @param discardEmptyNodes
	 * @param discardComments
	 * @return list of object arrays or null if empty
	 */
	static List<Map<String,Object>> addAssessableNodeAndDataToList(int recursionLevel, CourseNode courseNode, UserCourseEnvironment userCourseEnv, boolean discardEmptyNodes, boolean discardComments) {
		// 1) Get list of children data using recursion of this method
		List<Map<String, Object>> childrenData = new ArrayList<Map<String, Object>>(50);
		for (int i = 0; i < courseNode.getChildCount(); i++) {
			CourseNode child = (CourseNode) courseNode.getChildAt(i);
			List<Map<String, Object>> childData = addAssessableNodeAndDataToList( (recursionLevel + 1),  child, userCourseEnv, discardEmptyNodes, discardComments);
			if (childData != null)
				childrenData.addAll(childData);
		}
		
		// 2) Get data of this node only if
		// - it has any wrapped children  or
		// - it is of an assessable course node type
		boolean hasDisplayableValuesConfigured = false;
		boolean hasDisplayableUserValues = false;
		if ( (childrenData.size() > 0 || courseNode instanceof AssessableCourseNode) && !(courseNode instanceof ProjectBrokerCourseNode) ) {
		  // TODO:cg 04.11.2010 ProjectBroker : no assessment-tool in V1.0 , remove projectbroker completely form assessment-tool gui
			// Store node and user data in object array. This object array serves as data model for 
			// the user assessment overview table
			Map<String,Object> nodeData = new HashMap<String, Object>();
			// indent
			nodeData.put(KEY_INDENT, new Integer(recursionLevel));
			// course node data
			nodeData.put(KEY_TYPE, courseNode.getType());
			nodeData.put(KEY_TITLE_SHORT, courseNode.getShortTitle());
			nodeData.put(KEY_TITLE_LONG, courseNode.getLongTitle());
			nodeData.put(KEY_IDENTIFYER, courseNode.getIdent());
			
			if (courseNode instanceof AssessableCourseNode) {
				AssessableCourseNode assessableCourseNode = (AssessableCourseNode) courseNode;
				ScoreEvaluation scoreEvaluation = userCourseEnv.getScoreAccounting().getScoreEvaluation(courseNode);
				// details 
				if (assessableCourseNode.hasDetails()) {
					hasDisplayableValuesConfigured = true;
					String detailValue = assessableCourseNode.getDetailsListView(userCourseEnv);
					if (detailValue == null) {
						// ignore unset details in discardEmptyNodes mode
						nodeData.put(KEY_DETAILS, AssessmentHelper.DETAILS_NA_VALUE);
					} else {
						nodeData.put(KEY_DETAILS, detailValue);
						hasDisplayableUserValues = true;
					}
				}
				// attempts
				if (assessableCourseNode.hasAttemptsConfigured()) {
					hasDisplayableValuesConfigured = true;
					Integer attemptsValue = assessableCourseNode.getUserAttempts(userCourseEnv); 
					if (attemptsValue != null) {
						nodeData.put(KEY_ATTEMPTS, attemptsValue);
						if (attemptsValue.intValue() > 0) {
								// ignore attempts = 0  in discardEmptyNodes mode
								hasDisplayableUserValues = true;
						}
					}
				}
				// score
				if (assessableCourseNode.hasScoreConfigured()) {
					hasDisplayableValuesConfigured = true;
					Float score = scoreEvaluation.getScore();
					if (score != null) {
						//fxdiff VCRP-4: assessment overview with max score
						nodeData.put(KEY_SCORE, AssessmentHelper.getRoundedScore(score));
						nodeData.put(KEY_SCORE_F, score);
						hasDisplayableUserValues = true;
					}
					//fxdiff VCRP-4: assessment overview with max score
					if(!(assessableCourseNode instanceof STCourseNode)) {
						Float maxScore = assessableCourseNode.getMaxScoreConfiguration();
						nodeData.put(KEY_MAX, maxScore);
						Float minScore = assessableCourseNode.getMinScoreConfiguration();
						nodeData.put(KEY_MIN, minScore);
					}
					
				}
				// passed
				if (assessableCourseNode.hasPassedConfigured()) {
					hasDisplayableValuesConfigured = true;
					Boolean passed = scoreEvaluation.getPassed();
					if (passed != null) {
						nodeData.put(KEY_PASSED, passed);
						hasDisplayableUserValues = true;
					}
				}
				// selection command available
				AssessableCourseNode acn = (AssessableCourseNode) courseNode;
				if (acn.isEditableConfigured()) {
					// Assessable course nodes are selectable
					nodeData.put(KEY_SELECTABLE, Boolean.TRUE);
				} else {
					// assessable nodes that do not have score or passed are not selectable
					// (e.g. a st node with no defined rule
					nodeData.put(KEY_SELECTABLE, Boolean.FALSE);
				}
				if (!hasDisplayableUserValues && assessableCourseNode.hasCommentConfigured() && !discardComments) {
				  // comments are invisible in the table but if configured the node must be in the list
					// for the efficiency statement this can be ignored, this is the case when discardComments is true
					hasDisplayableValuesConfigured = true;
					if (assessableCourseNode.getUserUserComment(userCourseEnv) != null) {
						hasDisplayableUserValues = true;
					}
				}
			} else {
				// Not assessable nodes are not selectable. (e.g. a node that 
				// has an assessable child node but is itself not assessable)
				nodeData.put(KEY_SELECTABLE, Boolean.FALSE);
			}
			// 3) Add data of this node to mast list if node assessable or children list has any data.
			// Do only add nodes when they have any assessable element, otherwhise discard (e.g. empty course, 
			// structure nodes without scoring rules)! When the discardEmptyNodes flag is set then only
			// add this node when there is user data found for this node.
			if (childrenData.size() > 0 
					|| (discardEmptyNodes && hasDisplayableValuesConfigured && hasDisplayableUserValues)
					|| (!discardEmptyNodes && hasDisplayableValuesConfigured)) {
				List<Map<String, Object>> nodeAndChildren = new ArrayList<Map<String, Object>>();
				nodeAndChildren.add(nodeData);
				// 4) Add children data list to master list
				nodeAndChildren.addAll(childrenData);
				return nodeAndChildren;
			}
		}
		return null;
	}
	
	/**
	 * Evaluates if the results are visble or not in respect of the configured CONFIG_KEY_DATE_DEPENDENT_RESULTS parameter. <br>
	 * The results are always visible if no date dependent, 
	 * or if date dependent only in the period: startDate-endDate. 
	 * EndDate could be null, that is there is no restriction for the end date.
	 * 
	 * @return true if is visible.
	 */
	public static boolean isResultVisible(ModuleConfiguration modConfig) {
		boolean isVisible = false;
		Boolean showResultsActive = (Boolean)modConfig.get(IQEditController.CONFIG_KEY_DATE_DEPENDENT_RESULTS);
		if(showResultsActive != null && showResultsActive.booleanValue()) {
			Date startDate = (Date)modConfig.get(IQEditController.CONFIG_KEY_RESULTS_START_DATE);
			Date endDate = (Date)modConfig.get(IQEditController.CONFIG_KEY_RESULTS_END_DATE);
			Date currentDate = new Date();
			if(startDate != null && currentDate.after(startDate) && (endDate == null || currentDate.before(endDate))) {
				isVisible = true;
			}
		} else {
			isVisible = true;
		}
		return isVisible;
	}
}