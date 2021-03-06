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
* <p>
*/ 

package org.olat.core.gui.control.generic.iframe;

import java.io.File;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.components.htmlheader.jscss.CustomCSS;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.textmarker.TextMarkerManagerImpl;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.VFSMediaResource;

/**
 * Class that loads a resource (html) in an Iframe and tries to adjust the size of the Iframe to hide the scrollbars.
 * This is done by injecting some javascript into the head part of the loaded html file which then resizes the iframe itself.
 * See package documentation for details.
 * Initial Date: Dec 9, 2004
 * 
 * @author Felix Jost<br>
 *         
 * @author guido
 */
public class IFrameDisplayController extends BasicController implements GenericEventListener {
	private static final String NEW_URI_EVENT = "newUriEvent";
	protected static final String FILE_SUFFIX_HTM = "htm";
	protected static final String FILE_SUFFIX_JS = ".js";
	private static final String COMMAND_DOWNLOAD = "command.download";

	private VelocityContainer myContent = createVelocityContainer("index");
	private VelocityContainer eventVC = createVelocityContainer("event");
	private Panel newUriEventPanel;
	private Panel main;
	private IFrameDeliveryMapper contentMapper;
	private DeliveryOptions deliveryOptions;
	/**
	 * Base URI of contentMapper
	 */
	private final String baseURI;
	/**
	 * Relative uri of currently loaded page in iframe
	 */
	private String currentUri;
	
	//download link
	private Link downloadLink;
	private boolean allowDownload = false;
	
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param fileRoot File that points to the root directory of the resource 
	 */
	public IFrameDisplayController(UserRequest ureq, WindowControl wControl, File fileRoot) {
		this(ureq, wControl, new LocalFolderImpl(fileRoot), null, null);
	}
	
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param fileRoot
	 * @param ores - send an OLATresourcable of the context (e.g. course) where the iframe runs and it will be checked if the user has textmarking (glossar) enabled in this course
	 */
	public IFrameDisplayController(UserRequest ureq, WindowControl wControl, File fileRoot, OLATResourceable ores) {
		this(ureq, wControl, new LocalFolderImpl(fileRoot), null, ores, null);
	}
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootDir VFSItem that points to the root folder of the resource
	 */
	public IFrameDisplayController(UserRequest ureq, WindowControl wControl, VFSContainer rootDir) {
		this(ureq, wControl, rootDir, null, null, null);
	}
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootDir
	 * @param ores - send an OLATresourcable of the context (e.g. course) where the iframe runs and it will be checked if the user has textmarking (glossar) enabled in this course
	 */
	public IFrameDisplayController(UserRequest ureq, WindowControl wControl, VFSContainer rootDir, OLATResourceable ores, DeliveryOptions deliveryOptions) {
		this(ureq, wControl, rootDir, null, ores, deliveryOptions);
	}
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootDir
	 * @param frameId if you need access to the iframe html id, provide it here
	 * @param enableTextmarking to enable textmakring of the content in the iframe enable it here
	 */
	public IFrameDisplayController(final UserRequest ureq, WindowControl wControl, VFSContainer rootDir, String frameId,
			OLATResourceable contextRecourcable, DeliveryOptions options) {
		super(ureq, wControl);
		
		//register this object for textMarking on/off events
		//TODO:gs how to unregister and where? unregister need ureq so dispose does not work
		if (contextRecourcable != null) {
			ureq.getUserSession().getSingleUserEventCenter().registerFor(this, getIdentity(), contextRecourcable);
		}
		this.deliveryOptions = options;
		
		boolean  enableTextmarking = TextMarkerManagerImpl.getInstance().isTextmarkingEnabled(ureq, contextRecourcable);
		// Set correct user content theme
		String themeBaseUri = wControl.getWindowBackOffice().getWindow().getGuiTheme().getBaseURI();
		if (frameId == null) {
			frameId = "ifdc" + hashCode();
		}
		
		boolean adjusteightAutomatically = true;
		
		//Delivers content files via local mapper to enable session based browser caching for at least this instance
		//FIXME:FG: implement named mapper concept based on business path to allow browser caching and distributed media server
		//TODO:gs may use the same contentMapper if users clicks again on the same singlePage, now each time a new Mapper gets created and 
		//therefore the browser can not reuse the cached elements
		contentMapper = new IFrameDeliveryMapper(rootDir, false, enableTextmarking, adjusteightAutomatically,
				frameId, null /*customCssURL*/, themeBaseUri, null /*customHeaderContent*/);
		contentMapper.setDeliveryOptions(options);

		String mapperID = VFSManager.getRealPath(rootDir);
		if (mapperID == null) {
			// can't cache mapper, no cacheable context available
			baseURI = registerMapper(ureq, contentMapper);
		} else {
			// Add classname to the file path to remove conflicts with other
			// usages of the same file path
			mapperID = this.getClass().getSimpleName() + ":" + mapperID;
			baseURI = registerCacheableMapper(ureq, mapperID, contentMapper);				
		}
		myContent.contextPut("baseURI", baseURI);
		newUriEventPanel = new Panel("newUriEvent");
		newUriEventPanel.setContent(eventVC);
		
		main = new Panel("iframemain");
		
		main.setContent(myContent);
		myContent.contextPut("frameId", frameId);
		myContent.put("newUriEvent", newUriEventPanel);
		// add default iframe height
		if(options == null || DeliveryOptions.CONFIG_HEIGHT_AUTO.equals(options.getHeight())
				|| options.getHeight() == null) {
			myContent.contextPut("iframeHeight", 600); // used as fallback
			myContent.contextPut("adjustAutoHeight", Boolean.TRUE);
		} else {
			myContent.contextPut("iframeHeight", options.getHeight());
			myContent.contextPut("adjustAutoHeight", Boolean.FALSE);
		}

		// Add us as cycle listener to be notified when current dispatch cycle is
		// finished. we then need to add the css which is not yet defined at this
		// point
		getWindowControl().getWindowBackOffice().addCycleListener(this);
		putInitialPanel(main);
	}

	/**
	 * Sets the start page, may be null, a relative or an absolute URI
	 * 
	 * @param newCurrentURI The currentURI to set
	 */
	public void setCurrentURI(String newCurrentURI) {
		// set new uri and the content dirty to redraw on screen
		changeCurrentURI(newCurrentURI, true);
	}
	
	/**
	 * Allow download for all types but html
	 * @param allow
	 */
	public void setAllowDownload(boolean allow) {
		this.allowDownload = allow;
		setPageDownload(isPageDownloadAllowed(currentUri));
	}
	
	/**
	 * Configuration method to use an explicit height for the iframe instead of
	 * the default automatic sizeing code. If you don't call this method, OLAT
	 * will try to size the iframe so that no scrollbars appear. In most cases
	 * this works. If it does not work, use this method to set an explicit height.
	 * <br />
	 * Set 0 to reset to automatic behaviour.
	 * 
	 * @param height
	 */
	public void setHeightPX(int height) {
		if (height == 0) {			
			myContent.contextPut("iframeHeight", 600);
			contentMapper.setAdjusteightAutomatically(true);
			myContent.contextPut("adjustAutoHeight", Boolean.TRUE);			
			
		} else {
			myContent.contextPut("iframeHeight", height);
			contentMapper.setAdjusteightAutomatically(false);
			myContent.contextPut("adjustAutoHeight", Boolean.FALSE);			
		}
	}
	
	public void setRawContent(boolean rawContent) {
		contentMapper.setRawContent(rawContent);
	}
	
	public void setContentEncoding(String encoding) {
		contentMapper.setContentEncoding(encoding);
	}
	
	public void setJSEncoding(String encoding) {
		contentMapper.setJsEncoding(encoding);
	}
	
	public DeliveryOptions getDeliveryOptions() {
		return deliveryOptions;
	}
	
	public void setDeliveryOptions(DeliveryOptions config) {
		deliveryOptions = config;
		contentMapper.setDeliveryOptions(config);
	}

	/**
	 * Add a custom HTML header element. This string will be added into the HTML
	 * HEAD part of the HTML page. This could be CSS, JS or other header elements.
	 * 
	 * @param customHeaderContent A custom HEAD element or NULL
	 */
	public void setCustomHeaderContent(String customHeaderContent) {
		contentMapper.setCustomHeaderContent(customHeaderContent);
	}


	/**
	 * Change the start page, may be null
	 * 
	 * @param currentURI
	 *            The currentURI to set
	 * @param forceLoading
	 *            true: force rendering of iframe velocity container, will
	 *            redraw everything; 
	 *            false: set new uri without redrawing
	 *            iframe. This implies that the content in the iframe has
	 *            already been loaded (e.g. by an in line user click)
	 */
	private void changeCurrentURI(String uri, boolean forceLoading) {
		if (uri == null) {
			uri = "";
			myContent.contextPut("isAbsoluteURI", Boolean.FALSE);
		} else if (uri.startsWith("http")) {
			// http and https urls are absolute urls that do not need be fetched from
			// the OLAT server, they retrieve their content from an external content server
			//TODO:gs a if an absolut uri is loaded the iframe should size to the default site, see functions.js stuff
			myContent.contextPut("isAbsoluteURI", Boolean.TRUE);
		} else {
			// Check for problematic URI that start with '/' (would lead to a logout (login screen)).
			if (uri.startsWith("/")) uri = uri.substring(1);
			myContent.contextPut("isAbsoluteURI", Boolean.FALSE);
		}
		// set new current uri and push to velocity
		this.currentUri = uri;
		myContent.contextPut("currentURI", this.currentUri);
		if (forceLoading) {
			// Serve new URI as currentURI, no need to check for inline events
			contentMapper.setCheckForInlineEvent(false);
		} else {
			// Don't redraw iframe
			myContent.setDirty(false);
			contentMapper.setCheckForInlineEvent(true);
		}
		
		setPageDownload(isPageDownloadAllowed(currentUri));
	}
	
	private boolean isPageDownloadAllowed(String uri) {
		if(!allowDownload || !StringHelper.containsNonWhitespace(uri)) {
			return false;
		}
		String uriLc = uri.toLowerCase();
		if(uriLc.endsWith(".html") || uriLc.endsWith(".htm") || uriLc.endsWith(".xhtml")) {
			return false;
		}
		return true;
	}
	
	private void setPageDownload(boolean allow) {
		if(allow) {
			if(downloadLink == null) {
				downloadLink = LinkFactory.createCustomLink(COMMAND_DOWNLOAD, COMMAND_DOWNLOAD, "", Link.NONTRANSLATED, myContent, this);
				downloadLink.setCustomEnabledLinkCSS("b_content_download");
				downloadLink.setTooltip(getTranslator().translate(COMMAND_DOWNLOAD));
			} else if (!downloadLink.isVisible()) {
				downloadLink.setVisible(true);
			}
			//update always the name
			downloadLink.setCustomDisplayText(currentUri);
		} else {
			if(downloadLink != null) {
				downloadLink.setVisible(false);
			}
		}
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == eventVC) {
			if (event.getCommand().equals(NEW_URI_EVENT)) {
				// This event gets triggered from the iframe content by calling a js function outside 
				// Get new uri from JS method and fire to parents
				String newUri = ureq.getModuleURI();
				int baseUriPos = newUri.indexOf(baseURI);
				if (baseUriPos != -1) {
					int newUriPos =  baseUriPos + baseURI.length();
					if (newUri.length() > newUriPos) {
						newUri = newUri.substring(newUriPos);
						String hash = ureq.getHttpReq().getParameter("hash");
						if (StringHelper.containsNonWhitespace(hash)) {
							// force iframe reload to fix truncated page problem
							changeCurrentURI(newUri + '#' + hash, true);
							fireEvent(ureq, new NewIframeUriEvent(newUri));													
						}
						if (newUri.startsWith("/")) {
							// clean newUri to make equals check work
							newUri = newUri.substring(1);
						}
						if (! newUri.equals(this.currentUri)) {
							changeCurrentURI(newUri, false);
							fireEvent(ureq, new NewIframeUriEvent(currentUri));													
						}
						// else probably a reload, no need to propagate new uri event
					}
					// else ?? don't do anything
				}
				// else ?? don't do anything
				//
				// don't mark as dirty to prevent re-rendering in AJAX mode - event was a
				// background event only
				eventVC.setDirty(false);
			}
		} else if (source == downloadLink) {
			MediaResource mediaResource = contentMapper.deliverFile(ureq.getHttpReq(), currentUri, false);
			if(mediaResource instanceof VFSMediaResource) {
				((VFSMediaResource)mediaResource).setDownloadable(true);
			}
			ureq.getDispatchResult().setResultingMediaResource(mediaResource);
		}
	}



	@Override
	protected void doDispose() {
		//contentMapper get's unregistered automatically with basic controller
		// remove us as listener if not already done
		getWindowControl().getWindowBackOffice().removeCycleListener(this);
	}

	/**
	 * this event gets fired from the TextMarkerController when the user swiches on/off textmarking
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	public void event(Event event) {
		if (event instanceof MultiUserEvent) {
			if (event.getCommand().equals("glossaryOn")) {
				contentMapper.setEnableTextmarking(true);
			} else if (event.getCommand().equals("glossaryOff")) {
				contentMapper.setEnableTextmarking(false);
			}
		}
		else if (event.equals(Window.BEFORE_INLINE_RENDERING)){
			// Set the custom CSS URL that is used by the current tab or site if
			// available. The reason why we do this here and not in the constructor is
			// that during the constructing phase this property is not yet set on the
			// window. 
			Window myWindow = getWindowControl().getWindowBackOffice().getWindow();
			CustomCSS currentCustomCSS = myWindow.getCustomCSS();
			if (currentCustomCSS != null) {
				contentMapper.setCustomCssURL(currentCustomCSS.getCSSURLIFrame());
			}
			// done, remove us as listener
			getWindowControl().getWindowBackOffice().removeCycleListener(this);
		}
	}
}
