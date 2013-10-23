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

package org.olat.course.nodes.basiclti;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import org.imsglobal.basiclti.BasicLTIUtil;
import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Encoder;
import org.olat.core.util.SortedProperties;
import org.olat.core.util.StringHelper;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.BasicLTICourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.lti.LTIContext;
import org.olat.ims.lti.LTIManager;
import org.olat.ims.lti.ui.PostDataMapper;
import org.olat.ims.lti.ui.TalkBackMapper;
import org.olat.modules.ModuleConfiguration;
import org.olat.properties.Property;
import org.olat.resource.OLATResource;

/**
 * Description:<br>
 * is the controller for displaying contents in an iframe served by Basic LTI
 * @author guido
 * @author Charles Severance
 * 
 */
public class LTIRunController extends BasicController {
	private static final String PROP_NAME_DATA_EXCHANGE_ACCEPTED = "LtiDataExchageAccepted";
	
	private Link startButton;
	private final Panel mainPanel;
	private VelocityContainer run, startPage, acceptPage;
	private BasicLTICourseNode courseNode;
	private ModuleConfiguration config;
	private final CourseEnvironment courseEnv;
	private UserCourseEnvironment userCourseEnv;
	private SortedProperties userData = new SortedProperties(); 
	private SortedProperties customUserData = new SortedProperties(); 
	private Link acceptLink;
	
	private final Roles roles;
	private final LTIManager ltiManager;
	private final boolean newWindow;
	
	public LTIRunController(WindowControl wControl, ModuleConfiguration config, UserRequest ureq, BasicLTICourseNode ltCourseNode,
			CourseEnvironment courseEnv) {
		super(ureq, wControl);
		this.courseNode = ltCourseNode;
		this.config = config;
		this.roles = ureq.getUserSession().getRoles();
		this.courseEnv = courseEnv;
		newWindow = false;
		ltiManager = CoreSpringFactory.getImpl(LTIManager.class);

		run = createVelocityContainer("run");
		// push title and learning objectives, only visible on intro page
		run.contextPut("menuTitle", courseNode.getShortTitle());
		run.contextPut("displayTitle", courseNode.getLongTitle());

		doBasicLTI(ureq, run);
		mainPanel = putInitialPanel(run);
	}

	/**
	 * Constructor for tunneling run controller
	 * 
	 * @param wControl
	 * @param config The module configuration
	 * @param ureq The user request
	 * @param ltCourseNode The current course node
	 * @param cenv the course environment
	 */
	public LTIRunController(WindowControl wControl, ModuleConfiguration config, UserRequest ureq, BasicLTICourseNode ltCourseNode,
			UserCourseEnvironment userCourseEnv) {
 		super(ureq, wControl);
		this.courseNode = ltCourseNode;
		this.config = config;
		this.userCourseEnv = userCourseEnv;
		this.roles = ureq.getUserSession().getRoles();
		this.courseEnv = userCourseEnv.getCourseEnvironment();
		this.ltiManager = CoreSpringFactory.getImpl(LTIManager.class);
		String display = config.getStringValue(BasicLTICourseNode.CONFIG_DISPLAY, "iframe");
		this.newWindow = "window".equals(display);

		mainPanel = new Panel("ltiContainer");
		putInitialPanel(mainPanel);
		
		// only run directly when user as already accepted to data exchange or no data has to be exchanged
		createExchangeDataProperties();
		String dataExchangeHash = createHashFromExchangeDataProperties();
		if (dataExchangeHash == null || checkHasDataExchangeAccepted(dataExchangeHash)) {
			doRun(ureq);						
		} else {
			doAskDataExchange();
		}

	}

	/**
	 * Helper method to check if user has already accepted. this info is stored
	 * in a user property, the accepted values are stored as an MD5 hash (save
	 * space, privacy)
	 * 
	 * @param hash
	 *            MD5 hash with all user data
	 * @return true: user has already accepted for this hash; false: user has
	 *         not yet accepted or for other values
	 */
	private boolean checkHasDataExchangeAccepted(String hash) {
		// 
		CoursePropertyManager propMgr = this.userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		Property prop = propMgr.findCourseNodeProperty(this.courseNode, getIdentity(), null, PROP_NAME_DATA_EXCHANGE_ACCEPTED);
		if (prop != null) {
			// compare if value in property is the same as calculated today. If not, user as to accept again
			String storedHash = prop.getStringValue();
			if (storedHash != null && hash != null && storedHash.equals(hash)) {
				return true;
			} else {
				// remove property, not valid anymore
				propMgr.deleteProperty(prop);
			}
		}
		return false;
	}

	/**
	 * Helper to initialize the ask-for-data-exchange screen
	 */
	private void doAskDataExchange() {
		acceptPage = createVelocityContainer("accept");
		acceptPage.contextPut("userData", userData);
		acceptPage.contextPut("customUserData", customUserData);
		acceptLink = LinkFactory.createButton("accept", acceptPage, this);
		mainPanel.setContent(acceptPage);
	}
	
	/**
	 * Helper to save the user accepted data exchange
	 */
	private void storeDataExchangeAcceptance() {
		CoursePropertyManager propMgr = this.userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		String hash = createHashFromExchangeDataProperties();
		Property prop = propMgr.createCourseNodePropertyInstance(this.courseNode, getIdentity(), null, PROP_NAME_DATA_EXCHANGE_ACCEPTED, null, null, hash, null);
		propMgr.saveProperty(prop);
	}
	
	/**
	 * Helper to read all user data that is exchanged with LTI tool and saves it
	 * to the userData and customUserData properties fields
	 */
	private void createExchangeDataProperties() {
		final User user = getIdentity().getUser();
		//user data
		if (config.getBooleanSafe(LTIConfigForm.CONFIG_KEY_SENDNAME, false)) {
			userData.put("lastName", user.getProperty(UserConstants.LASTNAME, getLocale()));
			userData.put("firstName", user.getProperty(UserConstants.FIRSTNAME, getLocale()));
		}
		if (config.getBooleanSafe(LTIConfigForm.CONFIG_KEY_SENDEMAIL, false)) {
			userData.put("email", user.getProperty(UserConstants.EMAIL, getLocale()));
		}
		// customUserData
		String custom = (String)config.get(LTIConfigForm.CONFIG_KEY_CUSTOM);
		if (StringHelper.containsNonWhitespace(custom)) {
			String[] params = custom.split("[\n;]");
			for (int i = 0; i < params.length; i++) {
				String param = params[i];
				if (!StringHelper.containsNonWhitespace(param)) {
					continue;
				}
				
				int pos = param.indexOf("=");
				if (pos < 1 || pos + 1 > param.length()) {
					continue;
				}
				
				String key = BasicLTIUtil.mapKeyName(param.substring(0, pos));
				if(!StringHelper.containsNonWhitespace(key)) {
					continue;
				}
				
				String value = param.substring(pos + 1).trim();
				if(value.length() < 1) {
					continue;
				}
				
				if(value.startsWith(LTIManager.USER_PROPS_PREFIX)) {
					String userProp = value.substring(LTIManager.USER_PROPS_PREFIX.length(), value.length());
					value = user.getProperty(userProp, null);
					if (value!= null) {
						customUserData.put(userProp, value);
					}
				}
			}
		}
	}
	
	/**
	 * Helper to create an MD5 hash from the exchanged user properties. 
	 * @return
	 */
	private String createHashFromExchangeDataProperties() {
		String data = "";
		String hash = null;
		if (userData != null && userData.size() > 0) {
			Enumeration<Object> keys = userData.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				data += userData.getProperty((String)key);				
			}
		}
		if (customUserData != null && customUserData.size() > 0) {
			Enumeration<Object> keys = customUserData.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				data += customUserData.getProperty((String)key);				
			}
		}
		if (data.length() > 0) {
			hash = Encoder.encrypt(data);
		}
		if (isLogDebugEnabled()) {
			logDebug("Create accept hash::" + hash + " for data::" + data, null);
		}
		return hash;
	}
	
	/**
	 * Helper to initialize the LTI run view after user has accepted data exchange.
	 * @param ureq
	 */
	private void doRun(UserRequest ureq) {
		if (newWindow) {
			// Use other container for popup opening. Rest of code is the same
			run = createVelocityContainer("runPopup");			
		} else {			
			run = createVelocityContainer("run");
		}
		// push title and learning objectives, only visible on intro page
		run.contextPut("menuTitle", courseNode.getShortTitle());
		run.contextPut("displayTitle", courseNode.getLongTitle());
		

		startPage = createVelocityContainer("overview");
		startPage.contextPut("menuTitle", courseNode.getShortTitle());
		startPage.contextPut("displayTitle", courseNode.getLongTitle());
		
		startButton = LinkFactory.createButton("start", startPage, this);

		Boolean assessable = config.getBooleanEntry(BasicLTICourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		if(assessable != null && assessable.booleanValue()) {
	    startPage.contextPut("isassessable", assessable);
	    
	    Integer attempts = courseNode.getUserAttempts(userCourseEnv);
	    startPage.contextPut("attempts", attempts);
	    
	    ScoreEvaluation eval = courseNode.getUserScoreEvaluation(userCourseEnv);
	    Float cutValue = config.getFloatEntry(BasicLTICourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
	    if(cutValue != null) {
	    	startPage.contextPut("hasPassedValue", Boolean.TRUE);
	    	startPage.contextPut("passed", eval.getPassed());
	    }
	    startPage.contextPut("score", eval.getScore()); 
	    mainPanel.setContent(startPage);
		} else if(newWindow) {
			mainPanel.setContent(startPage);
		} else {
			doBasicLTI(ureq, run);
			mainPanel.setContent(run);
		}

	}
	
	public void event(UserRequest ureq, Component source, Event event) {
		if(source == startButton) {
			courseNode.incrementUserAttempts(userCourseEnv);
			// container is "run" or "runPopup" depending in configuration
			doBasicLTI(ureq, run);
			mainPanel.setContent(run);
		} else if (source == acceptLink) {
			storeDataExchangeAcceptance();
			doRun(ureq);
		}
	}
	
	private String getUrl() {
		// put url in template to show content on extern page
		URL url = null;
		try {
			url = new URL((String)config.get(LTIConfigForm.CONFIGKEY_PROTO), (String) config.get(LTIConfigForm.CONFIGKEY_HOST), ((Integer) config
					.get(LTIConfigForm.CONFIGKEY_PORT)).intValue(), (String) config.get(LTIConfigForm.CONFIGKEY_URI));
		} catch (MalformedURLException e) {
			// this should not happen since the url was already validated in edit mode
			return null;
		}

		StringBuilder querySb = new StringBuilder(128);
		querySb.append(url.toString());
		// since the url only includes the path, but not the query (?...), append
		// it here, if any
		String query = (String) config.get(LTIConfigForm.CONFIGKEY_QUERY);
		if (query != null) {
			querySb.append("?");
			querySb.append(query);
		}
		return querySb.toString();
	}
	


	private void doBasicLTI(UserRequest ureq, VelocityContainer container) {
		String url = getUrl();
		container.contextPut("url", url == null ? "" : url);

		String oauth_consumer_key = (String) config.get(LTIConfigForm.CONFIGKEY_KEY);
		String oauth_secret = (String) config.get(LTIConfigForm.CONFIGKEY_PASS);
		String debug = (String) config.get(LTIConfigForm.CONFIG_KEY_DEBUG);
		String serverUri = Settings.createServerURI();
		String sourcedId = courseEnv.getCourseResourceableId() + "_" + courseNode.getIdent() + "_" + getIdentity().getKey();
		container.contextPut("sourcedId", sourcedId);
		OLATResource courseResource = courseEnv.getCourseGroupManager().getCourseResource();
		
		Mapper talkbackMapper = new TalkBackMapper();
		String backMapperUrl = registerCacheableMapper(ureq, sourcedId + "_talkback", talkbackMapper);
		String backMapperUri = serverUri + backMapperUrl + "/";

		Mapper outcomeMapper = new CourseNodeOutcomeMapper(getIdentity(), courseResource, courseNode.getIdent(),
				oauth_consumer_key, oauth_secret, sourcedId);
		String outcomeMapperUrl = registerCacheableMapper(ureq, sourcedId, outcomeMapper, LTIManager.EXPIRATION_TIME);
		String outcomeMapperUri = serverUri + outcomeMapperUrl + "/";

		boolean sendname = config.getBooleanSafe(LTIConfigForm.CONFIG_KEY_SENDNAME, false);
		boolean sendmail = config.getBooleanSafe(LTIConfigForm.CONFIG_KEY_SENDEMAIL, false);
		String ltiRoles = getLTIRoles();
		String target = config.getStringValue(BasicLTICourseNode.CONFIG_DISPLAY);
		String width = config.getStringValue(BasicLTICourseNode.CONFIG_WIDTH);
		String height = config.getStringValue(BasicLTICourseNode.CONFIG_HEIGHT);
		String custom = (String)config.get(LTIConfigForm.CONFIG_KEY_CUSTOM);
		container.contextPut("height", height);
		container.contextPut("width", width);
		LTIContext context = new LTICourseNodeContext(courseEnv, courseNode, ltiRoles,
				sourcedId, backMapperUri, outcomeMapperUri, custom, target, width, height);
		Map<String,String> props = ltiManager.forgeLTIProperties(getIdentity(), getLocale(), context, sendname, sendmail);
		props = ltiManager.sign(props, url, oauth_consumer_key, oauth_secret);

		String postData = BasicLTIUtil.postLaunchHTML(props, url, "true".equals(debug));
		Mapper contentMapper = new PostDataMapper(postData);
		logDebug("Basic LTI Post data: " + postData, null);

		String mapperUri = registerMapper(ureq, contentMapper);
		container.contextPut("mapperUri", mapperUri + "/");
	}

	
	private String getLTIRoles() {
		if (roles.isGuestOnly()) {
			return "Guest";
		}
		CourseGroupManager groupManager = courseEnv.getCourseGroupManager();
		boolean admin = groupManager.isIdentityCourseAdministrator(getIdentity());
		if(admin || roles.isOLATAdmin()) {
			String authorRole = config.getStringValue(BasicLTICourseNode.CONFIG_KEY_AUTHORROLE);
			if(StringHelper.containsNonWhitespace(authorRole)) {
				return authorRole;
			}
			return "Instructor,Administrator";
		}
		boolean coach = groupManager.isIdentityCourseCoach(getIdentity());
		if(coach) {
			String coachRole = config.getStringValue(BasicLTICourseNode.CONFIG_KEY_COACHROLE);
			if(StringHelper.containsNonWhitespace(coachRole)) {
				return coachRole;
			}
			return "Instructor";
		}
		
		String participantRole = config.getStringValue(BasicLTICourseNode.CONFIG_KEY_PARTICIPANTROLE);
		if(StringHelper.containsNonWhitespace(participantRole)) {
			return participantRole;
		}
		return "Learner";
	}
	
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}
	
	private class LTIPopedController extends BasicController {
		
		public LTIPopedController(UserRequest ureq, WindowControl wControl) {
			super(ureq, wControl);
			VelocityContainer run = createVelocityContainer("run");
			doBasicLTI(ureq,  run);
			putInitialPanel(run);
		}

		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			//
		}

		@Override
		protected void doDispose() {
			//
		}
	}
}