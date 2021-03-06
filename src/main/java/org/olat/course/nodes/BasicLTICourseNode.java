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

package org.olat.course.nodes;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.iframe.DeliveryOptions;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.Util;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.basiclti.LTIConfigForm;
import org.olat.course.nodes.basiclti.LTIEditController;
import org.olat.course.nodes.basiclti.LTIRunController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.lti.ui.LTIResultDetailsController;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;

/**
 * @author guido
 * @author Charles Severance
 */
public class BasicLTICourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {

	private static final long serialVersionUID = 2210572148308757127L;
	private static final String TYPE = "lti";

	public static final String CONFIG_KEY_AUTHORROLE = "authorRole";
	public static final String CONFIG_KEY_COACHROLE = "coachRole";
	public static final String CONFIG_KEY_PARTICIPANTROLE = "participantRole";
	public static final String CONFIG_KEY_SCALEVALUE = "scaleFactor";
	public static final String CONFIG_KEY_HAS_SCORE_FIELD = MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD;
	public static final String CONFIG_KEY_HAS_PASSED_FIELD = MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD;
	public static final String CONFIG_KEY_PASSED_CUT_VALUE = MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE;
	public static final String CONFIG_HEIGHT = "displayHeight";
	public static final String CONFIG_WIDTH = "displayWidth";
	public static final String CONFIG_HEIGHT_AUTO = DeliveryOptions.CONFIG_HEIGHT_AUTO;
	public static final String CONFIG_DISPLAY = "display";
	
	
	// NLS support:
	
	private static final String NLS_ERROR_HOSTMISSING_SHORT = "error.hostmissing.short";
	private static final String NLS_ERROR_HOSTMISSING_LONG = "error.hostmissing.long";

	/**
	 * Constructor for a course node of type learning content tunneling
	 */
	public BasicLTICourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, StackedController stackPanel, ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		LTIEditController childTabCntrllr = new LTIEditController(getModuleConfiguration(), ureq, wControl, stackPanel, this, course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment()
				.getCourseGroupManager(), euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		updateModuleConfigDefaults(false);
		LTIRunController runCtrl = new LTIRunController(wControl, getModuleConfiguration(), ureq, this, userCourseEnv);
		Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, runCtrl, this, "o_lti_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		return createNodeRunConstructionResult(ureq, wControl, userCourseEnv, ne, null).getRunController();
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {

		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		String host = (String) getModuleConfiguration().get(LTIConfigForm.CONFIGKEY_HOST);
		boolean isValid = host != null;
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions
			String[] params = new String[] { this.getShortTitle() };
			String translPackage = Util.getPackageName(LTIConfigForm.class);
			sd = new StatusDescription(StatusDescription.ERROR, NLS_ERROR_HOSTMISSING_SHORT, NLS_ERROR_HOSTMISSING_LONG, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(LTIEditController.PANE_TAB_LTCONFIG);
		}
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		String translatorStr = Util.getPackageName(LTIEditController.class);
		List<StatusDescription> sds =  isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags
	 * set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading
	 *          from previous node configuration version, set default to maintain
	 *          previous behaviour
	 */
	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.FALSE.booleanValue());
			config.setConfigurationVersion(2);
		} else {
			// clear old popup configuration
			config.remove(NodeEditController.CONFIG_INTEGRATION);
			config.remove("width");
			config.remove("height");
			if (config.getConfigurationVersion() < 2) {
				// update new configuration options using default values for existing nodes
				config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.TRUE.booleanValue());
				config.setConfigurationVersion(2);
			}
			// else node is up-to-date - nothing to do
		}
	}

	@Override
	public Float getMaxScoreConfiguration() {
		if (!hasScoreConfigured()) {
			throw new OLATRuntimeException(MSCourseNode.class, "getMaxScore not defined when hasScore set to false", null);
		}
		ModuleConfiguration config = getModuleConfiguration();
		Float scaleFactor = (Float) config.get(CONFIG_KEY_SCALEVALUE);
		if(scaleFactor == null || scaleFactor.floatValue() < 0.0000001f) {
			return new Float(1.0f);
		}
		return 1.0f * scaleFactor.floatValue();//LTI 1.1 return between 0.0 - 1.0
	}

	@Override
	public Float getMinScoreConfiguration() {
		if (!hasScoreConfigured()) { 
			throw new OLATRuntimeException(MSCourseNode.class, "getMaxScore not defined when hasScore set to false", null);
		}
		return new Float(0.0f);
	}

	@Override
	public Float getCutValueConfiguration() {
		if (!hasPassedConfigured()) { 
			throw new OLATRuntimeException(MSCourseNode.class, "getCutValue not defined when hasPassed set to false", null);
		}
		ModuleConfiguration config = getModuleConfiguration();
		return config.getFloatEntry(CONFIG_KEY_PASSED_CUT_VALUE);
	}

	@Override
	public boolean hasScoreConfigured() {
		ModuleConfiguration config = getModuleConfiguration();
		Boolean score = config.getBooleanEntry(CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public boolean hasPassedConfigured() {
		ModuleConfiguration config = getModuleConfiguration();
		Boolean passed = config.getBooleanEntry(CONFIG_KEY_HAS_PASSED_FIELD);
		return (passed == null) ? false : passed.booleanValue();
	}

	@Override
	public boolean hasCommentConfigured() {
		return false;
	}

	@Override
	public boolean hasAttemptsConfigured() {
		// having score defined means the node is assessable
		ModuleConfiguration config = getModuleConfiguration();
		Boolean score = config.getBooleanEntry(CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public boolean hasDetails() {
		// having score defined means the node is assessable
		ModuleConfiguration config = getModuleConfiguration();
		Boolean score = config.getBooleanEntry(CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public boolean hasStatusConfigured() {
		return false;
	}

	@Override
	public boolean isEditableConfigured() {
		// having score defined means the node is assessable
		ModuleConfiguration config = getModuleConfiguration();
		Boolean score = config.getBooleanEntry(CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public ScoreEvaluation getUserScoreEvaluation(UserCourseEnvironment userCourseEnv) {
		// read score from properties
		AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnv.getIdentityEnvironment().getIdentity();
		Boolean passed = null;
		Float score = null;
		// only db lookup if configured, else return null
		if (hasPassedConfigured()) passed = am.getNodePassed(this, mySelf);
		if (hasScoreConfigured()) score = am.getNodeScore(this, mySelf);

		ScoreEvaluation se = new ScoreEvaluation(score, passed);
		return se;
	}

	@Override
	public String getUserUserComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		String userCommentValue = am.getNodeComment(this, mySelf);
		return userCommentValue;
	}

	@Override
	public String getUserCoachComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		String coachCommentValue = am.getNodeCoachComment(this, mySelf);
		return coachCommentValue;
	}

	@Override
	public String getUserLog(UserCourseEnvironment userCourseEnvironment) {
		// having score defined means the node is assessable
 		UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		String logValue = am.getUserNodeLog(this, mySelf);
		return logValue;
	}

	@Override
	public Integer getUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;
	}

	@Override
	public String getDetailsListView(UserCourseEnvironment userCourseEnvironment) {
		return null;
	}

	@Override
	public String getDetailsListViewHeaderKey() {
		return null;
	}

	@Override
	public Controller getDetailsEditController(UserRequest ureq, WindowControl wControl,
			StackedController stackPanel, UserCourseEnvironment userCourseEnvironment) {
		Identity assessedIdentity = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		OLATResource resource = userCourseEnvironment.getCourseEnvironment().getCourseGroupManager().getCourseResource();
		return new LTIResultDetailsController(ureq, wControl, assessedIdentity, resource, getIdent());
	}

	@Override
	public void updateUserScoreEvaluation(ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnvironment,
			Identity coachingIdentity, boolean incrementAttempts) {
		
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.saveScoreEvaluation(this, coachingIdentity, mySelf, new ScoreEvaluation(scoreEvaluation.getScore(), scoreEvaluation.getPassed()), userCourseEnvironment, incrementAttempts);
	}

	@Override
	public void updateUserUserComment(String userComment, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		if (userComment != null) {
			AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	@Override
	public void incrementUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	@Override
	public void updateUserAttempts(Integer userAttempts, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		if (userAttempts != null) {
			AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeAttempts(this, coachingIdentity, mySelf, userAttempts);
		}
	}

	@Override
	public void updateUserCoachComment(String coachComment, UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity(), coachComment);
		}
	}
}