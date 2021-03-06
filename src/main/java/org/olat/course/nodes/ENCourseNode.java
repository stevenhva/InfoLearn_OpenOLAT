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

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.course.ICourse;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.nodes.en.ENEditController;
import org.olat.course.nodes.en.ENRunController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;

/**
 * Description:<BR>
 * Enrollement Course Node: users can enroll in group / groups / areas
 * <P>
 * 
 * Initial Date: Sep 8, 2004
 * @author Felix Jost, Florian Gn�gi
 */
public class ENCourseNode extends AbstractAccessableCourseNode {
	private static final String PACKAGE = Util.getPackageName(ENCourseNode.class);

	/**
	 * property name for the initial enrollment date will be set only the first
	 * time the users enrolls to this node.
	 */
	public static final String PROPERTY_INITIAL_ENROLLMENT_DATE = "initialEnrollmentDate";
	/**
	 * property name for the recent enrollemtn date will be changed everytime the
	 * user enrolls to this node.
	 */
	public static final String PROPERTY_RECENT_ENROLLMENT_DATE = "recentEnrollmentDate";

	private static final String TYPE = "en";

	/**
	 * property name for the initial waiting-list date will be set only the first
	 * time the users is put into the waiting-list of this node.
	 */
	public static final String PROPERTY_INITIAL_WAITINGLIST_DATE = "initialWaitingListDate";
	/**
	 * property name for the recent waiting-list date will be changed everytime the
	 * user is put into the waiting-list of this node.
	 */
	public static final String PROPERTY_RECENT_WAITINGLIST_DATE = "recentWaitingListDate";

	/** CONFIG_GROUPNAME configuration parameter key. */
	public static final String CONFIG_GROUPNAME = "groupname";
	/** CONFIG_GROUPNAME configuration parameter key. */
	public static final String CONFIG_GROUP_IDS = "groupkeys";
	
	/** CONFIG_AREANAME configuration parameter key. */
	public static final String CONFIG_AREANAME = "areaname";
	/** CONFIG_AREANAME configuration parameter key. */
	public static final String CONFIG_AREA_IDS = "areakeys";
	
	
	
	/** CONF_CANCEL_ENROLL_ENABLED configuration parameter key. */
	public static final String CONF_CANCEL_ENROLL_ENABLED = "cancel_enroll_enabled";

	private static final int CURRENT_CONFIG_VERSION = 2;

	/**
	 * Constructor for enrollment buildig block
	 */
	public ENCourseNode() {
		super(TYPE);
		initDefaultConfig();
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, StackedController stackPanel, ICourse course, UserCourseEnvironment euce) {
		migrateConfig();
		ENEditController childTabCntrllr = new ENEditController(getModuleConfiguration(), ureq, wControl, this, course, euce);
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
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		Controller controller;
		migrateConfig();
		// Do not allow guests to enroll to groups
		Roles roles = ureq.getUserSession().getRoles();
		if (roles.isGuestOnly()) {
			Translator trans = new PackageTranslator(PACKAGE, ureq.getLocale());
			String title = trans.translate("guestnoaccess.title");
			String message = trans.translate("guestnoaccess.message");
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			controller = new ENRunController(getModuleConfiguration(), ureq, wControl, userCourseEnv, this);
		}
		Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, controller, this, "o_en_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		boolean isValid = ENEditController.isConfigValid(getModuleConfiguration());
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions
			String shortKey = "error.nogroupdefined.short";
			String longKey = "error.nogroupdefined.long";
			String[] params = new String[] { this.getShortTitle() };
			String translPackage = Util.getPackageName(ENEditController.class);
			sd = new StatusDescription(StatusDescription.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(ENEditController.PANE_TAB_ENCONFIG);
		}
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		// this must be nulled before isConfigValid() is called!!
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		String translatorStr = Util.getPackageName(ConditionEditController.class);

		List<StatusDescription> condErrs = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		List<StatusDescription> missingNames = new ArrayList<StatusDescription>();
		/*
		 * check group and area names for existence
		 */
		ModuleConfiguration mc = getModuleConfiguration();
		String areaStr = (String) mc.get(CONFIG_AREANAME);
		String nodeId = getIdent();
		if (areaStr != null) {
			String[] areas = areaStr.split(",");
			for (int i = 0; i < areas.length; i++) {
				String trimmed = areas[i] != null ? areas[i].trim() : areas[i];
				if (!trimmed.equals("") && !cev.existsArea(trimmed)) {
					StatusDescription sd = new StatusDescription(StatusDescription.WARNING, "error.notfound.name", "solution.checkgroupmanagement",
							new String[] { "NONE", trimmed }, translatorStr);
					sd.setDescriptionForUnit(nodeId);
					missingNames.add(sd);
				}
			}
		}
		String groupStr = (String) mc.get(CONFIG_GROUPNAME);
		if (groupStr != null) {
			String[] groups = groupStr.split(",");
			for (int i = 0; i < groups.length; i++) {
				String trimmed = groups[i] != null ? groups[i].trim() : groups[i];
				if (!trimmed.equals("") && !cev.existsGroup(trimmed)) {
					StatusDescription sd = new StatusDescription(StatusDescription.WARNING, "error.notfound.name", "solution.checkgroupmanagement",
							new String[] { "NONE", trimmed }, translatorStr);
					sd.setDescriptionForUnit(nodeId);
					missingNames.add(sd);
				}
			}
		}
		missingNames.addAll(condErrs);
		/*
		 * sort -> Errors > Warnings > Infos and remove NOERRORS, if
		 * Error/Warning/Info around.
		 */
		oneClickStatusCache = StatusDescriptionHelper.sort(missingNames);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	public void cleanupOnDelete(ICourse course) {
		super.cleanupOnDelete(course);
		CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		cpm.deleteNodeProperties(this, PROPERTY_INITIAL_ENROLLMENT_DATE);
		cpm.deleteNodeProperties(this, PROPERTY_RECENT_ENROLLMENT_DATE);
	}
	
	/**
	 * Init config parameter with default values for a new course node.
	 */
	private void initDefaultConfig() {
		ModuleConfiguration config = getModuleConfiguration();
		// defaults
		config.set(CONF_CANCEL_ENROLL_ENABLED, Boolean.TRUE);
    config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
	}
	
	@Override
	public void postImport(CourseEnvironmentMapper envMapper) {
		super.postImport(envMapper);
		
		ModuleConfiguration mc = getModuleConfiguration();
		String groupNames = (String)mc.get(ENCourseNode.CONFIG_GROUPNAME);
		@SuppressWarnings("unchecked")
		List<Long> groupKeys = (List<Long>) mc.get(ENCourseNode.CONFIG_GROUP_IDS);
		if(groupKeys == null) {
			groupKeys = envMapper.toGroupKeyFromOriginalNames(groupNames);
		} else {
			groupKeys = envMapper.toGroupKeyFromOriginalKeys(groupKeys);
		}
		mc.set(ENCourseNode.CONFIG_GROUP_IDS, groupKeys);
	
		String areaNames = (String)mc.get(ENCourseNode.CONFIG_AREANAME);
		@SuppressWarnings("unchecked")
		List<Long> areaKeys = (List<Long>) mc.get(ENCourseNode.CONFIG_AREA_IDS);
		if(areaKeys == null) {
			areaKeys = envMapper.toGroupKeyFromOriginalNames(areaNames);
		} else {
			areaKeys = envMapper.toAreaKeyFromOriginalKeys(groupKeys);
		}
		mc.set(ENCourseNode.CONFIG_AREA_IDS, areaKeys);
	}

	@Override
	public void postExport(CourseEnvironmentMapper envMapper, boolean backwardsCompatible) {
		super.postExport(envMapper, backwardsCompatible);

		ModuleConfiguration mc = getModuleConfiguration();
		@SuppressWarnings("unchecked")
		List<Long> groupKeys = (List<Long>) mc.get(ENCourseNode.CONFIG_GROUP_IDS);
		if(groupKeys != null) {
			String groupNames = envMapper.toGroupNames(groupKeys);
			mc.set(ENCourseNode.CONFIG_GROUPNAME, groupNames);
		}

		@SuppressWarnings("unchecked")
		List<Long> areaKeys = (List<Long>) mc.get(ENCourseNode.CONFIG_AREA_IDS);
		if(areaKeys != null ) {
			String areaNames = envMapper.toAreaNames(areaKeys);
			mc.set(ENCourseNode.CONFIG_AREANAME, areaNames);
		}
		
		if(backwardsCompatible) {
			mc.remove(ENCourseNode.CONFIG_GROUP_IDS);
			mc.remove(ENCourseNode.CONFIG_AREA_IDS);
		}
	}
	
	/**
	 * Migrate (add new config parameter/values) config parameter for a existing course node.
	 */
	private void migrateConfig() {
		ModuleConfiguration config = getModuleConfiguration();
		int version = config.getConfigurationVersion();
		if (version < CURRENT_CONFIG_VERSION) {
			// Loaded config is older than current config version => migrate
			if (version == 1) {
				// migrate V1 => V2
				config.set(CONF_CANCEL_ENROLL_ENABLED, Boolean.TRUE);
				version = 2;
			}
			config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
		}
	}
	
}
