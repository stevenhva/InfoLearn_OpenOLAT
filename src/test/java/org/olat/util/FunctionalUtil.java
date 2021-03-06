/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.util.FunctionalAdministrationSiteUtil.AdministrationSiteAction;
import org.olat.util.FunctionalGroupsSiteUtil.GroupsSiteAction;
import org.olat.util.FunctionalHomeSiteUtil.HomeSiteAction;
import org.olat.util.FunctionalRepositorySiteUtil.RepositorySiteAction;
import org.olat.util.FunctionalUserManagementSiteUtil.UserManagementSiteAction;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;

/**
 * 
 * @author jkraehemann, joel.kraehemann@frentix.com, frentix.com
 */
public class FunctionalUtil {
	private final static OLog log = Tracing.createLoggerFor(FunctionalUtil.class);
	
	public final static String DEPLOYMENT_URL = "http://localhost:8080/openolat";
	
	public final static String LOGIN_PAGE = "dmz";
	public final static String ACKNOWLEDGE_CHECKBOX = "acknowledge_checkbox";

	public final static long TIMEOUT = 20000;
	public final static long POLL_INTERVAL = 3000;
	
	public enum WaitLimitAttribute {
		NORMAL("10000"),
		EXTENDED("15000"),
		SAVE("20000"),
		VERY_SAVE("30000");
		
		private String extend;
		private long extendAsLong;
		
		WaitLimitAttribute(String extend){
			setExtend(extend);
			setExtendAsLong(Long.parseLong(extend));
		}

		public String getExtend() {
			return extend;
		}

		public void setExtend(String extend) {
			this.extend = extend;
		}

		public long getExtendAsLong() {
			return extendAsLong;
		}

		public void setExtendAsLong(long extendAsLong) {
			this.extendAsLong = extendAsLong;
		}
	}
	
	public enum WaitForContentFlag {
		EQUALS,
		STRIP_TAGS;
	}
	
	public final static WaitLimitAttribute DEFAULT_WAIT_LIMIT = WaitLimitAttribute.VERY_SAVE;
	public final static String DEFAULT_IDLE = "-1";
	
	public enum OlatSite {
		HOME,
		GROUPS,
		LEARNING_RESOURCES,
		USER_MANAGEMENT,
		ADMINISTRATION,
	}

	public final static String OLAT_TOP_NAVIGATION_LOGOUT_CSS = "b_logout";
	
	public final static String OLAT_NAVIGATION_SITE_CSS = "b_nav_site";
	public final static String OLAT_ACTIVE_NAVIGATION_SITE_CSS = "b_nav_active";
	
	public final static String OLAT_SITE_HOME_CSS = "o_site_home";
	public final static String OLAT_SITE_GROUPS_CSS = "o_site_groups";
	public final static String OLAT_SITE_LEARNING_RESOURCES_CSS = "o_site_repository";
	public final static String OLAT_SITE_USER_MANAGEMENT_CSS = "o_site_useradmin";
	public final static String OLAT_SITE_ADMINISTRATION_CSS = "o_site_admin";

	public final static String CONTENT_CSS = "b_main";
	public final static String CONTENT_TAB_CSS = "b_item_";
	public final static String ACTIVE_CONTENT_TAB_CSS = "b_active";
	public final static String CONTENT_SEGMENT_CONTAINER_CSS = "b_segments_container";
	public final static String CONTENT_SEGMENT_CSS = "b_segment";
	
	public final static String WINDOW_CSS = "b_window";
	
	public final static String WIZARD_CSS = "b_wizard";
	public final static String WIZARD_NEXT_CSS = "b_wizard_button_next";
	public final static String WIZARD_FINISH_CSS = "b_wizard_button_finish";
	
	public final static String MCE_CONTENT_BODY_CSS = "mceContentBody";
	
	public final static String BUTTON_CSS = "b_button";
	public final static String BUTTON_DIRTY_CSS = "b_button_dirty";
	public final static String BACK_BUTTON_CSS = "b_link_back";
	
	public final static String TABLE_FIRST_CHILD_CSS = "b_first_child";
	public final static String TABLE_LAST_CHILD_CSS = "b_last_child";
	public final static String TABLE_ALL_CSS = "b_table_page_all";
	
	public final static String TREE_NODE_ANCHOR_CSS = "b_tree_item_wrapper";
	public final static String TREE_NODE_CSS = "b_tree_item_wrapper";
	public final static String TREE_NODE_LOADING_CSS = null;
	public final static String TREE_LEVEL0_CSS = "b_tree_l0";
	public final static String TREE_LEVEL1_CSS = "b_tree_l1";
	public final static String TREE_LEVEL2_CSS = "b_tree_l2";
	public final static String TREE_LEVEL_OPEN_CSS = "b_tree_level_open";
	public final static String TREE_LEVEL_CLOSE_CSS = "b_tree_level_close";
	
	public final static String WINDOW_CLOSE_LINK_CSS = "b_link_close";
	
	public final static String FORM_ELEMENT_CSS = "b_form_element";
	public final static String FORM_SAVE_XPATH = "//button[@type='button' and last()]";

	public final static String INFO_CSS = "b_info";
	public final static String INFO_MESSAGE_BOX_CSS = "o_sel_info_message";
	
	private String username;
	private String password;
	
	private String deploymentUrl;
	private String waitLimit;
	
	private String loginPage;
	private String acknowledgeCheckbox;
	
	private String olatTopNavigationLogoutCss;
	
	private String olatNavigationSiteCss;
	private String olatActiveNavigationSiteCss;
	
	private String olatSiteHomeCss;
	private String olatSiteGroupsCss;
	private String olatSiteLearningResourcesCss;
	private String olatSiteUserManagementCss;
	private String olatSiteAdministrationCss;
	
	private String contentCss;
	private String contentTabCss;
	private String activeContentTabCss;
	private String contentSegmentContainerCss;
	private String contentSegmentCss;
	
	private String formSaveXPath;
	
	private String windowCss;
	
	private String wizardCss;
	private String wizardNextCss;
	private String wizardFinishCss;
	
	private String mceContentBodyCss;
	
	private String windowCloseLinkCss;
	
	private String infoCss;
	
	private String buttonCss;
	private String buttonDirtyCss;
	private String backButtonCss;
	
	private String tableFirstChildCss;
	private String tableLastChildCss;
	private String tableAllCss;
	
	private String treeNodeAnchorCss;
	private String treeNodeCss;
	private String treeNodeLoadingCss;
	private String treeLevel0Css;
	private String treeLevel1Css;
	private String treeLevel2Css;
	private String treeLevelOpenCss;
	private String treeLevelCloseCss;
	
	private FunctionalHomeSiteUtil functionalHomeSiteUtil;
	private FunctionalGroupsSiteUtil functionalGroupsSiteUtil;
	private FunctionalRepositorySiteUtil functionalRepositorySiteUtil;
	private FunctionalUserManagementSiteUtil functionalUserManagementSiteUtil;
	private FunctionalAdministrationSiteUtil functionalAdministrationSiteUtil;
	
	private FunctionalHtmlUtil functionalHtmlUtil;
	
	public FunctionalUtil(){
		Properties properties = new Properties();
		
		try {
			//TODO:JK: use default properties file
			properties.load(FunctionalUtil.class.getResourceAsStream("credentials.properties"));
			
			username = properties.getProperty("admin.login");
			password = properties.getProperty("admin.password");
		} catch (IOException e) {

			username = "administrator";
			password = "openolat";
		}
		
		deploymentUrl = DEPLOYMENT_URL;
		waitLimit = DEFAULT_IDLE;
		
		loginPage = LOGIN_PAGE;
		acknowledgeCheckbox = ACKNOWLEDGE_CHECKBOX;
		
		olatTopNavigationLogoutCss = OLAT_TOP_NAVIGATION_LOGOUT_CSS;
		
		olatNavigationSiteCss = OLAT_NAVIGATION_SITE_CSS;
		olatActiveNavigationSiteCss = OLAT_ACTIVE_NAVIGATION_SITE_CSS;
		
		olatSiteHomeCss = OLAT_SITE_HOME_CSS;
		olatSiteGroupsCss = OLAT_SITE_GROUPS_CSS;
		olatSiteLearningResourcesCss = OLAT_SITE_LEARNING_RESOURCES_CSS;
		olatSiteUserManagementCss = OLAT_SITE_USER_MANAGEMENT_CSS;
		olatSiteAdministrationCss = OLAT_SITE_ADMINISTRATION_CSS;
		
		contentCss = CONTENT_CSS;
		contentTabCss = CONTENT_TAB_CSS;
		activeContentTabCss = ACTIVE_CONTENT_TAB_CSS;
		contentSegmentContainerCss = CONTENT_SEGMENT_CONTAINER_CSS;
		contentSegmentCss = CONTENT_SEGMENT_CSS;
		
		formSaveXPath = FORM_SAVE_XPATH;
		
		windowCss = WINDOW_CSS;
		
		wizardCss = WIZARD_CSS;
		wizardNextCss = WIZARD_NEXT_CSS;
		wizardFinishCss = WIZARD_FINISH_CSS;
		
		mceContentBodyCss = MCE_CONTENT_BODY_CSS;
		
		windowCloseLinkCss = WINDOW_CLOSE_LINK_CSS;
		
		infoCss = INFO_CSS;
		
		buttonCss = BUTTON_CSS;
		buttonDirtyCss = BUTTON_DIRTY_CSS;
		backButtonCss = BACK_BUTTON_CSS;
		
		tableFirstChildCss = TABLE_FIRST_CHILD_CSS;
		tableLastChildCss = TABLE_LAST_CHILD_CSS;
		tableAllCss = TABLE_ALL_CSS;
		
		treeNodeAnchorCss = TREE_NODE_ANCHOR_CSS;
		treeNodeCss = TREE_NODE_CSS;
		treeNodeLoadingCss = TREE_NODE_LOADING_CSS;
		treeLevel0Css = TREE_LEVEL0_CSS;
		treeLevel1Css = TREE_LEVEL1_CSS;
		treeLevel2Css = TREE_LEVEL2_CSS;
		treeLevelOpenCss = TREE_LEVEL_OPEN_CSS;
		treeLevelCloseCss = TREE_LEVEL_CLOSE_CSS;
		
		functionalHomeSiteUtil = new FunctionalHomeSiteUtil(this);
		functionalGroupsSiteUtil = new FunctionalGroupsSiteUtil(this);
		functionalRepositorySiteUtil = new FunctionalRepositorySiteUtil(this);
		functionalUserManagementSiteUtil = new FunctionalUserManagementSiteUtil(this);
		functionalAdministrationSiteUtil = new FunctionalAdministrationSiteUtil(this);
		
		functionalHtmlUtil = new FunctionalHtmlUtil();
	}
	
	/**
	 * Loads the specified page with default deployment url.
	 * 
	 * @param browser
	 * @param page
	 */
	public void loadPage(Selenium browser, String page){
		/* create url */
		StringBuilder urlBuffer = new StringBuilder();
		urlBuffer.append(deploymentUrl).append('/').append(page);
		
		String url = urlBuffer.toString();
		
		/* open url and wait specified time */
		browser.open(url);
		waitForPageToLoad(browser);
	}
	
	/**
	 * Is idle as long as link is busy. 
	 * 
	 * @param browser
	 */
	public void idle(Selenium browser){
		long startTime = Calendar.getInstance().getTimeInMillis();
		long currentTime = startTime;
		long waitLimit = TIMEOUT;

		//FIXME:JK: this is really ugly. For better performance revise confirmation
		if(browser.isConfirmationPresent()){
			browser.getConfirmation();
		}
		
		while(linkBusy(browser) && waitLimit >  currentTime - startTime){
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			currentTime = Calendar.getInstance().getTimeInMillis();
		}
	}
	
	/**
	 * 
	 * @param browser
	 */
	public void waitForPageToLoad(Selenium browser){
		waitForPageToLoad(browser, DEFAULT_WAIT_LIMIT);
	}

	/**
	 * 
	 * @param browser
	 * @param wait
	 */
	public void waitForPageToLoad(Selenium browser, WaitLimitAttribute wait){
		String waitLimit = Long.toString(Long.parseLong(getWaitLimit()) + Long.parseLong(wait.getExtend()));
		
		browser.waitForPageToLoad(waitLimit);
		
		idle(browser);
	}

	
	/**
	 * Waits at most (waitLimit + WaitLimitAttribute.VERY_SAVE) amount of time for element to load
	 * specified by locator.
	 * 
	 * @param browser
	 * @param locator
	 * @param throwException
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToLoadElement(Selenium browser, String locator) throws SeleniumException{
		return(waitForPageToLoadElement(browser, locator, DEFAULT_WAIT_LIMIT, true));
	}
	
	/**
	 * Waits at most (waitLimit + WaitLimitAttribute.VERY_SAVE) amount of time for element to load
	 * specified by locator.
	 * 
	 * @param browser
	 * @param locator
	 * @param throwException
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToLoadElement(Selenium browser, String locator, boolean throwException) throws SeleniumException{
		return(waitForPageToLoadElement(browser, locator, DEFAULT_WAIT_LIMIT, throwException));
	}
	
	/** 
	 * Waits at most (waitLimit + wait) amount of time for element to load
	 * specified by locator.
	 *
	 * @param browser
	 * @param locator
	 * @param wait
	 * @param throwException
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToLoadElement(Selenium browser, String locator, WaitLimitAttribute wait, boolean throwException) throws SeleniumException {
		idle(browser);
		
		long startTime = Calendar.getInstance().getTimeInMillis();
		long currentTime = startTime;
		long waitLimit = Long.parseLong(getWaitLimit()) + Long.parseLong(wait.getExtend());

		log.info("waiting for page to load element");
		
		int count = 0;
		do{
			if(browser.isElementPresent(locator)){
				log.info("found element after " + (currentTime - startTime) + "ms");
				return true;
			} else if(count > 2) {
				log.info("Waiting for: " + locator);
			}
			
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
				log.error("", e);
			}

			currentTime = Calendar.getInstance().getTimeInMillis();
			count++;
		} while(waitLimit >  currentTime - startTime);
		
		log.warn("giving up after " + waitLimit + "ms");
		
		if(throwException){
			throw new SeleniumException("timed out after " + waitLimit + "ms");
		}
		
		return(false);
	}
	
	public boolean waitForPageToLoadContent(Selenium browser, String iframeSelectors, String content, WaitLimitAttribute wait, boolean throwException){
		return(waitForPageToLoadContent(browser, new String[]{ iframeSelectors }, content, wait, new WaitForContentFlag[]{WaitForContentFlag.EQUALS, WaitForContentFlag.STRIP_TAGS}, throwException));
	}
	
	/**
	 * 
	 * @param browser
	 * @param iframeSelectors
	 * @param content
	 * @param wait
	 * @param throwException
	 * @return
	 */
	public boolean waitForPageToLoadContent(Selenium browser, String[] iframeSelectors, String content, WaitLimitAttribute wait, boolean throwException){
		return(waitForPageToLoadContent(browser, iframeSelectors, content, wait, new WaitForContentFlag[]{WaitForContentFlag.EQUALS, WaitForContentFlag.STRIP_TAGS}, throwException));
	}
	
	public boolean waitForPageToLoadContent(Selenium browser, String[] iframeSelectors, String content, WaitLimitAttribute wait, WaitForContentFlag[] flags, boolean throwException){
		idle(browser);
		
		long startTime = Calendar.getInstance().getTimeInMillis();
		long currentTime = startTime;
		long waitLimit = Long.parseLong(getWaitLimit()) + Long.parseLong(wait.getExtend());

		log.info("waiting for page to load content element");
		
		/* traverse iframes */
		if(iframeSelectors != null){
			for(int i=0; i < iframeSelectors.length;i++) {
				browser.selectFrame(iframeSelectors[i]);
			}
		}
		
		do{
			String source = browser.getHtmlSource();
			
			if(flags != null && ArrayUtils.contains(flags, WaitForContentFlag.STRIP_TAGS)){
				source = functionalHtmlUtil.stripTags(source, true);
			}
			
			boolean useEquals = (flags != null && ArrayUtils.contains(flags, WaitForContentFlag.EQUALS));
			
			if((content == null && source == null) || 
					(useEquals && content != null && source != null && content.trim().equals(source.trim())) ||
					(source != null && content != null && !ArrayUtils.contains(flags, WaitForContentFlag.EQUALS) && source.trim().contains(content.trim()))) {
				log.info("found content after " + (currentTime - startTime) + "ms");
				
				/* go back to toplevel */
				if(iframeSelectors != null){
					browser.selectFrame("relative=top");
				}
				
				return true;
			}
			
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			currentTime = Calendar.getInstance().getTimeInMillis();
		}while(waitLimit >  currentTime - startTime);
		
		log.warn("giving up after " + waitLimit + "ms");
		
		/* go back to toplevel */
		browser.selectFrame("relative=top");
		
		if(throwException){
			throw new SeleniumException("timed out after " + waitLimit + "ms");
		}
		return false;
	}
	
	/**
	 * Waits at most (waitLimit + WaitLimitAttribute.VERY_SAVE) amount of time for element to load
	 * specified by locator.
	 * 
	 * @param browser
	 * @param locator
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToUnloadElement(Selenium browser, String locator) throws SeleniumException {
		return(waitForPageToUnloadElement(browser, locator, DEFAULT_WAIT_LIMIT, true));
	}
	
	/**
	 * Waits at most (waitLimit + WaitLimitAttribute.VERY_SAVE) amount of time for element to load
	 * specified by locator.
	 * 
	 * @param browser
	 * @param locator
	 * @param throwException
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToUnloadElement(Selenium browser, String locator, boolean throwException) throws SeleniumException {
		return(waitForPageToUnloadElement(browser, locator, DEFAULT_WAIT_LIMIT, throwException));
	}
	
	/**
	 * Waits at most (waitLimit + wait) amount of time for element to load
	 * specified by locator.
	 * 
	 * @param browser
	 * @param locator
	 * @param wait
	 * @param throwException
	 * @return true on success otherwise false
	 * @throws SeleniumException
	 */
	public boolean waitForPageToUnloadElement(Selenium browser, String locator, WaitLimitAttribute wait, boolean throwException) throws SeleniumException {
		idle(browser);
		
		long startTime = Calendar.getInstance().getTimeInMillis();
		long currentTime = startTime;
		long waitLimit = Long.parseLong(getWaitLimit()) + Long.parseLong(wait.getExtend());

		log.info("waiting for page to unload element");
		
		do{
			if(!browser.isElementPresent(locator)){
				log.info("didn't find element after " + (currentTime - startTime) + "ms");
				
				return(true);
			}
			
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			currentTime = Calendar.getInstance().getTimeInMillis();
		}while(waitLimit >  currentTime - startTime);
		
		log.warn("giving up after " + waitLimit + "ms");
		
		if(throwException){
			throw new SeleniumException("timed out after " + waitLimit + "ms");
		}
		
		return(false);
	}
	
	/**
	 * Retrieves the business path.
	 * 
	 * @param browser
	 * @return
	 */
	public String currentBusinessPath(Selenium browser){
		return(browser.getEval("window.o_info.businessPath"));
	}
	
	/**
	 * Retrieves the business path.
	 * 
	 * @param browser
	 * @return
	 */
	public String openBusinessPath(Selenium browser, String businessPath){
		idle(browser);
		
		browser.open(businessPath);

		idle(browser);
		
		return(null);
	}
	
	
	/**
	 * Retrieves the linkbusy JavaScript variable.
	 * 
	 * @param browser
	 * @return
	 */
	public boolean linkBusy(Selenium browser){
		String val = null;
		
		try{
			val = browser.getEval("window.o_info.linkbusy");
		}catch(Exception e){
			log.warn("caught exception while retrieving o_info.linkbusy: assuming not busy", e);
			
			return(false);
		}
			
		if(val == null || val.isEmpty()){
			return(false);
		}else{
			return(Boolean.parseBoolean(val));
		}
	}
	
	/**
	 * Retrieve the user count who currently are logged in by parsing openolat footer.
	 * 
	 * @param browser
	 * @return
	 */
	public int retrieveUserCount(Selenium browser){
		StringBuffer selectorBuffer = new StringBuffer();
		selectorBuffer.append("xpath=//span[@id='b_counter']//span");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		String str = browser.getText(selectorBuffer.toString());
		
		return(Integer.valueOf(str));
	}
	
	/**
	 * Find CSS mapping for specific olat site.
	 * 
	 * @param site
	 * @return the matching CSS class
	 */
	public String findCssClassOfSite(OlatSite site){
		String selectedCss;
		
		selectedCss = null;
		
		switch(site){
		case HOME:
		{
			selectedCss = getOlatSiteHomeCss();
			break;
		}
		case GROUPS:
		{
			selectedCss = getOlatSiteGroupsCss();
			break;
		}
		case LEARNING_RESOURCES:
		{
			selectedCss = getOlatSiteLearningResourcesCss();
			break;
		}
		case USER_MANAGEMENT:
		{
			selectedCss = getOlatSiteUserManagementCss();
			break;
		}
		case ADMINISTRATION:
		{
			selectedCss = getOlatSiteAdministrationCss();
			break;
		}
		}
		
		return(selectedCss);
	}
	
	/**
	 * Check if the correct olat site is open.
	 * 
	 * @param browser
	 * @param site
	 * @return true if match otherwise false
	 */
	public boolean checkCurrentSite(Selenium browser, OlatSite site){
		return(checkCurrentSite(browser, site, -1));
	}
	
	/**
	 * 
	 * @param browser
	 * @param site
	 * @param timeout
	 * @return
	 */
	public boolean checkCurrentSite(Selenium browser, OlatSite site, long timeout){
		idle(browser);
		
		String selectedCss = findCssClassOfSite(site);
		
		if(selectedCss == null){
			return(false);
		}
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("css=.")
		.append(getOlatNavigationSiteCss())
		.append(".")
		.append(getOlatActiveNavigationSiteCss())
		.append(".")
		.append(selectedCss);

		long timeElapsed = 0;
		long startTime = Calendar.getInstance().getTimeInMillis();
		
		do{
			if(browser.isElementPresent(selectorBuffer.toString())){
				return(true);
			}
			
			if(timeout != -1){
				try {
					Thread.sleep(POLL_INTERVAL);
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
			
			timeElapsed = Calendar.getInstance().getTimeInMillis() - startTime;
		}while(timeElapsed <= timeout && timeout != -1);
		
		return(false);
	}
	
	/**
	 * Clicks the appropriate action on a site that it would be
	 * on it's initial state i.e. it looks like the user wouldn't
	 * have visited site before.
	 * 
	 * @param browser
	 * @param site
	 * @return true on success
	 */
	private boolean resetSite(Selenium browser, OlatSite site){
		boolean retval = false;
		
		switch(site){
		case HOME:
		{
			retval = functionalHomeSiteUtil.openActionByMenuTree(browser, HomeSiteAction.PORTAL);
		}
		break;
		case GROUPS:
		{
			retval = functionalGroupsSiteUtil.openActionByMenuTree(browser, GroupsSiteAction.MY_GROUPS);
		}
		break;
		case LEARNING_RESOURCES:
		{
			retval = functionalRepositorySiteUtil.openActionByMenuTree(browser, RepositorySiteAction.MY_ENTRIES);
		}
		break;
		case USER_MANAGEMENT:
		{
			retval = functionalUserManagementSiteUtil.openActionByMenuTree(browser, UserManagementSiteAction.USER_SEARCH);
		}
		break;
		case ADMINISTRATION:
		{
			retval = functionalAdministrationSiteUtil.openActionByMenuTree(browser, AdministrationSiteAction.SYSTEM);
		}
		break;
		}
		
		return(retval);
	}
	
	/**
	 * Open a specific olat site.
	 * 
	 * @param browser
	 * @param site
	 * @return true on success otherwise false
	 */
	public boolean openSite(Selenium browser, OlatSite site){
		String selectedCss = findCssClassOfSite(site);
		
		if(selectedCss == null){
			return(false);
		}
		
		//FIXME:JK: this is a known bottleneck, but can't be set to -1 until info messages will be clicked away!
		if(checkCurrentSite(browser, site, Long.parseLong(getWaitLimit()))){
			if(resetSite(browser, site)){
				return(true);
			}else{
				return(false);
			}
		}
		
		/* open the appropriate site */
		idle(browser);
		
		StringBuilder selectorBuffer = new StringBuilder();
		
		selectorBuffer.append("css=.")
		.append(getOlatNavigationSiteCss())
		.append(".")
		.append(selectedCss)
		.append(" a");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		waitForPageToLoad(browser);
		waitForPageToLoadElement(browser, selectorBuffer.toString(), WaitLimitAttribute.NORMAL, false);
		
		/* set it to it's initial state */
		resetSite(browser, site);
		
		return(true);
	}
	
	/**
	 * 
	 * @param browser
	 */
	public void clickAwayInfoMessages(Selenium browser){
		//TODO:JK: the impatients and fearless may want to implement this method but be aware there may be more than one notification at the same time
	}
	
	/**
	 * Login to olat using selenium.
	 * 
	 * @param browser
	 * @return true on success otherwise false
	 */
	public boolean login(Selenium browser){
		return login(browser, true);
	}
	
	/**
	 * 
	 * @param browser
	 * @param closeDialogs
	 * @return
	 */
	public boolean login(Selenium browser, boolean closeDialogs){
		return(login(browser, getUsername(), getPassword(), closeDialogs));
	}
	
	/**
	 * 
	 * @param browser
	 * @param username
	 * @param password
	 * @param closeDialogs
	 * @return
	 */
	public boolean login(Selenium browser, String username, String password, boolean closeDialogs){
		loadPage(browser, getLoginPage());
		waitForPageToLoadElement(browser, "xpath=//button[@id='o_fiooolat_login_button']");
		
		/* fill in login form */
		browser.type("id=o_fiooolat_login_name", username);
		browser.type("id=o_fiooolat_login_pass", password);
	    browser.click("xpath=//button[@id='o_fiooolat_login_button']");
	    waitForPageToLoad(browser, DEFAULT_WAIT_LIMIT);
	    
	    if(closeDialogs){
	    	/* check if it's our first login */
	    	if(browser.isElementPresent("name=" + getAcknowledgeCheckbox())){
	    		browser.click("name=" + getAcknowledgeCheckbox());

	    		/* click accept button */
	    		browser.click("xpath=//div[contains(@class, 'b_window')]//button[last()]");
	    		waitForPageToLoad(browser);
	    	}

	    	/* click away info dialogs eg. restore session */
	    	//TODO:JK: find a way to solve endless loop
	    	//while(browser.isElementPresent("class="+ getInfoDialog())){
	    		/* click last button */
	    	if(browser.isElementPresent("//div[contains(@class, '"+ getInfoCss() + "')]")){
	    		browser.click("xpath=//form//div//button[@type='button']/../../span/a[contains(@class, '" + getButtonCss() + "')]");
	    		waitForPageToLoad(browser);
	    	}
	    	//}
	    }
		
		/* validate page */
	    return(true);
	}
	
	/**
	 * Logout from olat LMS.
	 * 
	 * @param browser
	 * @return
	 */
	public boolean logout(Selenium browser){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("css=#")
		.append(getOlatTopNavigationLogoutCss());
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Opens a tab at the specific tabIndex.
	 * 
	 * @param browser
	 * @param tabIndex
	 * @return true on success otherwise false
	 */
	public boolean openContentTab(Selenium browser, int tabIndex){
		idle(browser);
		
		StringBuffer activeTabSelectorBuffer = new StringBuffer();
		
		activeTabSelectorBuffer.append("css=#")
		.append(getContentCss())
		.append(" ul .")
		.append(getContentTabCss())
		.append(tabIndex + 1)
		.append('.')
		.append(getActiveContentTabCss());
		
		if(!browser.isElementPresent(activeTabSelectorBuffer.toString())){
			StringBuffer selectorBuffer = new StringBuffer();
			
			selectorBuffer.append("css=#")
			.append(getContentCss())
			.append(" ul .")
			.append(getContentTabCss())
			.append(tabIndex + 1)
			.append(" * a");
			
			waitForPageToLoadElement(browser, selectorBuffer.toString());
			
			browser.click(selectorBuffer.toString());
			waitForPageToLoad(browser);
		}
		
		return(true);
	}
	
	/**
	 * Opens a segment at the specified segmentIndex.
	 * 
	 * @param browser
	 * @param segmentIndex
	 * @return
	 */
	public boolean openContentSegment(Selenium browser, int segmentIndex){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=(//div[contains(@class, '")
		.append(getContentSegmentContainerCss())
		.append("')]//ul//li[contains(@class, '")
		.append(getContentSegmentCss())
		.append("')])[")
		.append(segmentIndex + 1)
		.append("]//a");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Save the form at the position formIndex within content element.
	 * 
	 * @param browser
	 * @param formIndex
	 * @return true on success
	 */
	public boolean saveForm(Selenium browser, int formIndex){
		saveForm(browser, formIndex, getWaitLimit());
		
		return(true);
	}
	
	/**
	 * 
	 * @param browser
	 * @param formIndex
	 * @param waitLimit
	 * @return
	 */
	public boolean saveForm(Selenium browser, int formIndex, String waitLimit){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form[")
		.append(formIndex + 1)
		.append("]")
		.append(getFormSaveXPath());
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		waitForPageToLoad(browser);
		
		return(true);
	}
	
	/**
	 * Clicks the checkbox at position checkboxIndex from the selection at position checkboxGroupIndex.
	 * 
	 * @param browser
	 * @param formIndex
	 * @param checkboxGroupIndex
	 * @param checkboxIndex
	 * @return true on success
	 */
	@Deprecated
	public boolean clickCheckbox(Selenium browser, int formIndex, int checkboxGroupIndex, int checkboxIndex){
		idle(browser);
	
		StringBuffer selectorBuffer = new StringBuffer();
	
		selectorBuffer.append("xpath=//form[")
		.append(formIndex)
		.append("]")
		.append("//div[contains(@class, 'b_form_selection_vertical') or contains(@class, 'b_form_selection_horizontal')][")
		.append(checkboxGroupIndex + 1)
		.append("]")
		.append("//input[@type='checkbox'][")
		.append(checkboxIndex + 1)
		.append("]");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Clicks the radio button with specific value attribute in groupCss container.
	 * 
	 * @param browser
	 * @param groupCss
	 * @param value
	 * @return true on success
	 */
	public boolean clickCheckbox(Selenium browser, String groupCss, String value){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form")
		.append("//div[contains(@class, 'b_form_selection_vertical') or contains(@class, 'b_form_selection_horizontal')]")
		.append("//input[@type='checkbox' and @value='")
		.append(value)
		.append("']");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Clicks the radio button at position radioIndex from the selection at position radioGroupIndex.
	 * 
	 * @param browser
	 * @param formIndex
	 * @param radioGroupIndex
	 * @param radioIndex
	 * @return true on success
	 */
	@Deprecated
	public boolean clickRadio(Selenium browser, int formIndex, int radioGroupIndex, int radioIndex){
		idle(browser);
	
		StringBuffer selectorBuffer = new StringBuffer();
	
		selectorBuffer.append("xpath=//form[")
		.append(formIndex)
		.append("]")
		.append("//div[contains(@class, 'b_form_selection_vertical') or contains(@class, 'b_form_selection_horizontal')][")
		.append(radioGroupIndex)
		.append("]")
		.append("//input[@type='radio'][")
		.append(radioIndex)
		.append("]");
		
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		browser.click(selectorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Clicks the radio button with specific value attribute in groupCss container.
	 * 
	 * @param browser
	 * @param groupCss
	 * @param value
	 * @return true on success
	 */
	public boolean clickRadio(Selenium browser, String groupCss, String value){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("//form")
		.append("//div[contains(@class, 'b_form_selection_vertical') or contains(@class, 'b_form_selection_horizontal')]")
		.append("//input[@type='radio' and @value='")
		.append(value)
		.append("']");

		waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		browser.click(selectorBuffer.toString());

		return(true);
	}
	
	/**
	 * Type text in the specified text entry at textIndex position within form at formIndex.
	 * 
	 * @param browser
	 * @param formIndex
	 * @param textIndex
	 * @param text
	 * @return true on success
	 */
	@Deprecated
	public boolean typeText(Selenium browser, int formIndex, int textIndex, String text){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form[")
		.append(formIndex)
		.append("]")
		.append("//input[@type='text'][")
		.append(textIndex)
		.append("]");
		
		browser.type(selectorBuffer.toString(), text);
		
		return(true);
	}
	
	/**
	 * Types text to the with CSS class specified entry.
	 * 
	 * @param browser
	 * @param entryCss
	 * @param text
	 * @return true on success
	 */
	public boolean typeText(Selenium browser, String entryCss, String text){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form")
		.append("//div[contains(@class, '")
		.append(entryCss)
		.append("')]")
		.append("//input[@type='text']");
		
		browser.type(selectorBuffer.toString(), text);
		
		return(true);
	}
	
	
	
	/**
	 * 
	 * @param browser
	 * @param content
	 * @return
	 */
	public void typeMCE(Selenium browser, String content){
		if(content == null)return;
		
		idle(browser);
		
		String iframeSelector = "dom=document.getElementsByClassName('mce-edit-area')[0].getElementsByTagName('iframe')[0]";

		browser.runScript("top.tinymce.activeEditor.setContent('" + content + "')");

		String strippedContent = functionalHtmlUtil.stripTags(content, true);
		waitForPageToLoadContent(browser, iframeSelector, strippedContent, DEFAULT_WAIT_LIMIT, true);
	}

	/**
	 * 
	 * @param browser
	 * @param cssClass
	 * @param content
	 * @return
	 */
	public void typeMCE(Selenium browser, String cssClass, String content) {
		if(content == null) return;

		idle(browser);
		
		//wait tiny
		StringBuilder iframeSelectorBuffer = new StringBuilder();
		iframeSelectorBuffer.append("dom=document.getElementsByClassName('")
		  .append(cssClass)
		  .append("')[0].getElementsByClassName('mce-edit-area')[0].getElementsByTagName('iframe')[0]");
		
		StringBuilder selectorBuffer = new StringBuilder(iframeSelectorBuffer);
		selectorBuffer.append(".contentDocument.body");
		waitForPageToLoadElement(browser, selectorBuffer.toString());
		
		//write in tiny
		try {
			String selIframe = "//div[contains(@class,'" + cssClass + "')]//div[contains(@class,'mce-tinymce')]//iframe";
			String iframeId = browser.getAttribute("" + selIframe + "@id");
			int index = iframeId.lastIndexOf('_');
			String editorId = iframeId.substring(0, index);
			content = content.replace("\n", "<br/>");
			browser.runScript("top.tinymce.editors['" + editorId + "'].setContent('" + content + "')");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//wait until it's done
		String strippedContent = functionalHtmlUtil.stripTags(content, true);
		waitForPageToLoadContent(browser, iframeSelectorBuffer.toString(),
				strippedContent, DEFAULT_WAIT_LIMIT, true);
	}
	
	/**
	 * Types text to the with CSS class specified password entry.
	 * 
	 * @param browser
	 * @param entryCss
	 * @param text
	 * @return true on success
	 */
	public boolean typePassword(Selenium browser, String entryCss, String text){
		idle(browser);
		
		StringBuffer selectorBuffer = new StringBuffer();
		
		selectorBuffer.append("xpath=//form")
		.append("//div[contains(@class, '")
		.append(entryCss)
		.append("')]")
		.append("//input[@type='password']");
		
		browser.type(selectorBuffer.toString(), text);
		
		return(true);
	}
	
	/**
	 * Select an item of an option box.
	 * 
	 * @param browser
	 * @param id
	 * @param value
	 * @return
	 */
	public boolean selectOption(Selenium browser, String id, String value){
		idle(browser);
		
		StringBuffer selectLocatorBuffer = new StringBuffer();
		
		selectLocatorBuffer.append("xpath=//form")
		.append("//select[@id='")
		.append(id)
		.append("']");
		
		StringBuffer optionLocatorBuffer = new StringBuffer();
		
		optionLocatorBuffer.append("value=")
		.append(value);
		
		waitForPageToLoadElement(browser, selectLocatorBuffer.toString());
		browser.select(selectLocatorBuffer.toString(), optionLocatorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Clicks the next button of a wizard.
	 * 
	 * @param browser
	 * @return
	 */
	public boolean clickWizardNext(Selenium browser){
		idle(browser);
		
		StringBuffer locatorBuffer = new StringBuffer();
		
		locatorBuffer.append("xpath=//form//a[contains(@class, '")
		.append(getWizardNextCss())
		.append("')]");
		
		waitForPageToLoadElement(browser, locatorBuffer.toString());
		browser.click(locatorBuffer.toString());
		
		return(true);
	}
	
	public boolean clickWizardNext(Selenium browser, String cssClass){
		idle(browser);
		
		StringBuffer locatorBuffer = new StringBuffer();
		
		locatorBuffer.append("xpath=//div[contains(@class, '")
		.append(cssClass)
		.append("')]//a[contains(@class, '")
		.append(getWizardNextCss())
		.append("')]");
		
		waitForPageToLoadElement(browser, locatorBuffer.toString());
		browser.click(locatorBuffer.toString());
		
		return(true);
	}
	
	/**
	 * Clicks the finish button of a wizard.
	 * 
	 * @param browser
	 * @return
	 */
	public boolean clickWizardFinish(Selenium browser){
		idle(browser);
		
		StringBuffer locatorBuffer = new StringBuffer();
		
		locatorBuffer.append("xpath=//form//a[contains(@class, '")
		.append(getWizardFinishCss())
		.append("')]");
		
		waitForPageToLoadElement(browser, locatorBuffer.toString());
		
		browser.focus(locatorBuffer.toString());
		browser.click(locatorBuffer.toString());
		waitForPageToUnloadElement(browser, locatorBuffer.toString());
		
		return(true);
	}
	
	public boolean clickWizardFinish(Selenium browser, String cssClass){
		idle(browser);
		
		StringBuffer locatorBuffer = new StringBuffer();
		
		locatorBuffer.append("xpath=//div[contains(@class, '")
		.append(cssClass)
		.append("')]//a[contains(@class, '")
		.append(getWizardFinishCss())
		.append("')]");
		
		waitForPageToLoadElement(browser, locatorBuffer.toString());
		
		browser.focus(locatorBuffer.toString());
		browser.click(locatorBuffer.toString());
		waitForPageToUnloadElement(browser, locatorBuffer.toString());
		
		idle(browser);
		
		return(true);
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDeploymentUrl() {
		return deploymentUrl;
	}

	public void setDeploymentUrl(String deploymentUrl) {
		if(!deploymentUrl.endsWith("/")){
			this.deploymentUrl = deploymentUrl;
		}else{
			this.deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
		}
	}

	public String getWaitLimit() {
		return waitLimit;
	}

	public void setWaitLimit(String waitLimit) {
		this.waitLimit = waitLimit;
	}

	public String getLoginPage() {
		return loginPage;
	}

	public void setLoginPage(String loginPage) {
		this.loginPage = loginPage;
	}

	public String getAcknowledgeCheckbox() {
		return acknowledgeCheckbox;
	}

	public void setAcknowledgeCheckbox(String acknowledgeCheckbox) {
		this.acknowledgeCheckbox = acknowledgeCheckbox;
	}

	public String getOlatTopNavigationLogoutCss() {
		return olatTopNavigationLogoutCss;
	}

	public void setOlatTopNavigationLogoutCss(String olatTopNavigationLogoutCss) {
		this.olatTopNavigationLogoutCss = olatTopNavigationLogoutCss;
	}

	public String getOlatNavigationSiteCss() {
		return olatNavigationSiteCss;
	}

	public void setOlatNavigationSiteCss(String olatNavigationSiteCss) {
		this.olatNavigationSiteCss = olatNavigationSiteCss;
	}

	public String getOlatActiveNavigationSiteCss() {
		return olatActiveNavigationSiteCss;
	}

	public void setOlatActiveNavigationSiteCss(String olatActiveNavigationSiteCss) {
		this.olatActiveNavigationSiteCss = olatActiveNavigationSiteCss;
	}

	public String getOlatSiteHomeCss() {
		return olatSiteHomeCss;
	}

	public void setOlatSiteHomeCss(String olatSiteHomeCss) {
		this.olatSiteHomeCss = olatSiteHomeCss;
	}

	public String getOlatSiteGroupsCss() {
		return olatSiteGroupsCss;
	}

	public void setOlatSiteGroupsCss(String olatSiteGroupsCss) {
		this.olatSiteGroupsCss = olatSiteGroupsCss;
	}

	public String getOlatSiteLearningResourcesCss() {
		return olatSiteLearningResourcesCss;
	}

	public void setOlatSiteLearningResourcesCss(String olatSiteLearningResourcesCss) {
		this.olatSiteLearningResourcesCss = olatSiteLearningResourcesCss;
	}
	
	public String getOlatSiteUserManagementCss() {
		return olatSiteUserManagementCss;
	}

	public void setOlatSiteUserManagementCss(String olatSiteUserManagementCss) {
		this.olatSiteUserManagementCss = olatSiteUserManagementCss;
	}

	public String getOlatSiteAdministrationCss() {
		return olatSiteAdministrationCss;
	}

	public void setOlatSiteAdministrationCss(String olatSiteAdministrationCss) {
		this.olatSiteAdministrationCss = olatSiteAdministrationCss;
	}
	
	public String getContentCss() {
		return contentCss;
	}

	public void setContentCss(String contentCss) {
		this.contentCss = contentCss;
	}

	public String getContentTabCss() {
		return contentTabCss;
	}

	public void setContentTabCss(String contentTabCss) {
		this.contentTabCss = contentTabCss;
	}

	public String getActiveContentTabCss() {
		return activeContentTabCss;
	}

	public void setActiveContentTabCss(String activeContentTabCss) {
		this.activeContentTabCss = activeContentTabCss;
	}

	public String getContentSegmentContainerCss() {
		return contentSegmentContainerCss;
	}

	public void setContentSegmentContainerCss(String contentSegmentContainerCss) {
		this.contentSegmentContainerCss = contentSegmentContainerCss;
	}

	public String getContentSegmentCss() {
		return contentSegmentCss;
	}

	public void setContentSegmentCss(String contentSegmentCss) {
		this.contentSegmentCss = contentSegmentCss;
	}

	public String getFormSaveXPath() {
		return formSaveXPath;
	}

	public void setFormSaveXPath(String formSaveXPath) {
		this.formSaveXPath = formSaveXPath;
	}

	public String getWindowCss() {
		return windowCss;
	}

	public void setWindowCss(String windowCss) {
		this.windowCss = windowCss;
	}

	public String getWizardCss() {
		return wizardCss;
	}

	public void setWizardCss(String wizardCss) {
		this.wizardCss = wizardCss;
	}

	public String getWizardNextCss() {
		return wizardNextCss;
	}

	public void setWizardNextCss(String wizardNextCss) {
		this.wizardNextCss = wizardNextCss;
	}

	public String getWizardFinishCss() {
		return wizardFinishCss;
	}

	public void setWizardFinishCss(String wizardFinishCss) {
		this.wizardFinishCss = wizardFinishCss;
	}

	public String getMceContentBodyCss() {
		return mceContentBodyCss;
	}

	public void setMceContentBodyCss(String mceContentBodyCss) {
		this.mceContentBodyCss = mceContentBodyCss;
	}

	public FunctionalHomeSiteUtil getFunctionalHomeSiteUtil() {
		return functionalHomeSiteUtil;
	}

	public void setFunctionalHomeSiteUtil(
			FunctionalHomeSiteUtil functionalHomeSiteUtil) {
		this.functionalHomeSiteUtil = functionalHomeSiteUtil;
	}

	public FunctionalGroupsSiteUtil getFunctionalGroupsSiteUtil() {
		return functionalGroupsSiteUtil;
	}

	public void setFunctionalGroupsSiteUtil(
			FunctionalGroupsSiteUtil functionalGroupsSiteUtil) {
		this.functionalGroupsSiteUtil = functionalGroupsSiteUtil;
	}

	public FunctionalRepositorySiteUtil getFunctionalRepositorySiteUtil() {
		return functionalRepositorySiteUtil;
	}

	public void setFunctionalRepositorySiteUtil(
			FunctionalRepositorySiteUtil functionalRepositorySiteUtil) {
		this.functionalRepositorySiteUtil = functionalRepositorySiteUtil;
	}

	public FunctionalUserManagementSiteUtil getFunctionalUserManagementSiteUtil() {
		return functionalUserManagementSiteUtil;
	}

	public void setFunctionalUserManagementSiteUtil(
			FunctionalUserManagementSiteUtil functionalUserManagementSiteUtil) {
		this.functionalUserManagementSiteUtil = functionalUserManagementSiteUtil;
	}

	public FunctionalAdministrationSiteUtil getFunctionalAdministrationSiteUtil() {
		return functionalAdministrationSiteUtil;
	}

	public void setFunctionalAdministrationSiteUtil(
			FunctionalAdministrationSiteUtil functionalAdministrationSiteUtil) {
		this.functionalAdministrationSiteUtil = functionalAdministrationSiteUtil;
	}

	public FunctionalHtmlUtil getFunctionalHtmlUtil() {
		return functionalHtmlUtil;
	}

	public void setFunctionalHtmlUtil(FunctionalHtmlUtil functionalHtmlUtil) {
		this.functionalHtmlUtil = functionalHtmlUtil;
	}

	public String getWindowCloseLinkCss() {
		return windowCloseLinkCss;
	}

	public void setWindowCloseLinkCss(String windowCloseLinkCss) {
		this.windowCloseLinkCss = windowCloseLinkCss;
	}

	public String getInfoCss() {
		return infoCss;
	}

	public void setInfoCss(String infoCss) {
		this.infoCss = infoCss;
	}

	public String getButtonCss() {
		return buttonCss;
	}

	public void setButtonCss(String buttonCss) {
		this.buttonCss = buttonCss;
	}

	public String getButtonDirtyCss() {
		return buttonDirtyCss;
	}

	public void setButtonDirtyCss(String buttonDirtyCss) {
		this.buttonDirtyCss = buttonDirtyCss;
	}

	public String getBackButtonCss() {
		return backButtonCss;
	}

	public void setBackButtonCss(String backButtonCss) {
		this.backButtonCss = backButtonCss;
	}

	public String getTableFirstChildCss() {
		return tableFirstChildCss;
	}

	public void setTableFirstChildCss(String tableFirstChildCss) {
		this.tableFirstChildCss = tableFirstChildCss;
	}

	public String getTableLastChildCss() {
		return tableLastChildCss;
	}

	public void setTableLastChildCss(String tableLastChildCss) {
		this.tableLastChildCss = tableLastChildCss;
	}

	public String getTableAllCss() {
		return tableAllCss;
	}

	public void setTableAllCss(String tableAllCss) {
		this.tableAllCss = tableAllCss;
	}

	public String getTreeNodeAnchorCss() {
		return treeNodeAnchorCss;
	}

	public void setTreeNodeAnchorCss(String treeNodeAnchorCss) {
		this.treeNodeAnchorCss = treeNodeAnchorCss;
	}

	public String getTreeNodeCss() {
		return treeNodeCss;
	}

	public void setTreeNodeCss(String treeNodeCss) {
		this.treeNodeCss = treeNodeCss;
	}

	public String getTreeNodeLoadingCss() {
		return treeNodeLoadingCss;
	}

	public void setTreeNodeLoadingCss(String treeNodeLoadingCss) {
		this.treeNodeLoadingCss = treeNodeLoadingCss;
	}

	public String getTreeLevel0Css() {
		return treeLevel0Css;
	}

	public void setTreeLevel0Css(String treeLevel0Css) {
		this.treeLevel0Css = treeLevel0Css;
	}

	public String getTreeLevel1Css() {
		return treeLevel1Css;
	}

	public void setTreeLevel1Css(String treeLevel1Css) {
		this.treeLevel1Css = treeLevel1Css;
	}

	public String getTreeLevel2Css() {
		return treeLevel2Css;
	}

	public void setTreeLevel2Css(String treeLevel2Css) {
		this.treeLevel2Css = treeLevel2Css;
	}

	public String getTreeLevelOpenCss() {
		return treeLevelOpenCss;
	}

	public void setTreeLevelOpenCss(String treeLevelOpenCss) {
		this.treeLevelOpenCss = treeLevelOpenCss;
	}

	public String getTreeLevelCloseCss() {
		return treeLevelCloseCss;
	}

	public void setTreeLevelCloseCss(String treeLevelCloseCss) {
		this.treeLevelCloseCss = treeLevelCloseCss;
	}
}
