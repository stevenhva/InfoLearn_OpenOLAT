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

package org.olat.course.nodes.iq;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.Windows;
import org.olat.core.util.UserSession;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.InstantMessagingService;

/**
 * 
 * Description:<br>
 * This is a MultiUserEvent that must be instantiated just before fireEventToListenersOf() is called,
 * in case an assessment is started or stopped. 
 * <p> 
 * It also have a "helper" responsability, namely it stores and retrieves the isAssessmentStarted 
 * information via the userSession.
 * 
 * <P>
 * Initial Date:  23.06.2009 <br>
 * @author Lavinia Dumitrescu
 */
public class AssessmentEvent extends MultiUserEvent {

	private static final long serialVersionUID = -4619743031390573124L;

	public static enum TYPE {STARTED, STOPPED}
	
	private TYPE eventType = TYPE.STARTED;
	
	private static final String ASSESSMENT_STARTED_KEY = "asessmentStarted";
	
	/**
	 * Create a new assessment event at start/stop assessment and disable/enable chat. <p>
	 * The information about assessment started/stopped is stored as windows attribute.
	 * @param command
	 */
	public AssessmentEvent(TYPE type, UserSession userSession) {
		super("");

		this.eventType = type; 
		if(TYPE.STARTED.equals(type)) {			
			Integer assessmentCounter = (Integer)Windows.getWindows(userSession).getAttribute(ASSESSMENT_STARTED_KEY);
			int counter = assessmentCounter==null ? 0 : assessmentCounter.intValue();						
			//increment assessmentCounter
			counter++;		
		  Windows.getWindows(userSession).setAttribute(ASSESSMENT_STARTED_KEY, counter);
		  if (CoreSpringFactory.getImpl(InstantMessagingModule.class).isEnabled()) {
				CoreSpringFactory.getImpl(InstantMessagingService.class).disableChat(userSession.getIdentity());
		  }
		} else if(TYPE.STOPPED.equals(type)) {
			Integer assessmentCounter = (Integer)Windows.getWindows(userSession).getAttribute(ASSESSMENT_STARTED_KEY);
			int counter = assessmentCounter==null ? 0 : assessmentCounter.intValue();
		  //decrement assessmentCounter
			counter--;			
			Windows.getWindows(userSession).setAttribute(ASSESSMENT_STARTED_KEY, counter);
			if (CoreSpringFactory.getImpl(InstantMessagingModule.class).isEnabled() && counter==0) {
				CoreSpringFactory.getImpl(InstantMessagingService.class).enableChat(userSession.getIdentity());
			}
		}
	}

	/**
	 * 
	 * @return the event type
	 */
	public TYPE getEventType() {
		return eventType;
	}
	
	/**
	 * This is a static method! 
	 * The reason why this method resides here is "encapsulation" 
	 * (only this class knows where the info about isAssessmentStarted is stored.)
	 * @param userSession
	 * @return
	 */
	public static boolean isAssessmentStarted(UserSession userSession) {
		Integer assessmentCounter = (Integer)Windows.getWindows(userSession).getAttribute(AssessmentEvent.ASSESSMENT_STARTED_KEY);
		if(assessmentCounter!=null && assessmentCounter.intValue()>0) {
			return true;
		}
		return false;
	}
	
}
