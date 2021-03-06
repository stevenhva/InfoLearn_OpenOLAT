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
package org.olat.course.site.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.DisposedCourseRestartController;
import org.olat.course.ICourse;
import org.olat.course.run.RunMainController;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResourceManager;

/**
 * 
 * Initial date: 17.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DisposedCourseSiteRestartController extends BasicController {

	private VelocityContainer initialContent;
	private Link restartLink;
	private RepositoryEntry courseRepositoryEntry;
	private Panel panel;

	public DisposedCourseSiteRestartController(UserRequest ureq, WindowControl wControl, RepositoryEntry courseRepositoryEntry) {
		super(ureq, wControl);
		setBasePackage(DisposedCourseRestartController.class);
		initialContent = createVelocityContainer("disposedcourserestart");
		restartLink = LinkFactory.createButton("course.disposed.command.restart", initialContent, this);
		this.courseRepositoryEntry = courseRepositoryEntry;
		panel = putInitialPanel(initialContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == restartLink) {
			OLATResourceable ores = OLATResourceManager.getInstance().findResourceable(
					courseRepositoryEntry.getOlatResource().getResourceableId(), courseRepositoryEntry.getOlatResource().getResourceableTypeName());
			if(ores==null) {
				//course was deleted!				
				MessageController msgController = MessageUIFactory.createInfoMessage(ureq, this.getWindowControl(), translate("course.deleted.title"), translate("course.deleted.text"));
				panel.setContent(msgController.getInitialComponent());				
				return;
			}
			
			ICourse course = CourseFactory.loadCourse(ores);
			RunMainController c = new RunMainController(ureq, getWindowControl(), course, courseRepositoryEntry, false, true);
			panel.setContent(c.getInitialComponent());
		}
	}
}
