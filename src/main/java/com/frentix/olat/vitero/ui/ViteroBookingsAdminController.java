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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package com.frentix.olat.vitero.ui;

import java.util.List;

import org.olat.NewControllerFactory;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.util.resource.OresHelper;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

import com.frentix.olat.vitero.manager.ViteroManager;
import com.frentix.olat.vitero.manager.VmsNotAvailableException;
import com.frentix.olat.vitero.model.ViteroBooking;
import com.frentix.olat.vitero.model.ViteroGroup;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  10 oct. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ViteroBookingsAdminController extends BasicController {
	
	private final ViteroManager viteroManager;
	
	private DialogBoxController dialogCtr;
	private final TableController tableCtr;
	private CloseableModalController cmc;
	private ViteroAdminBookingInfosController infoController;
	
	public ViteroBookingsAdminController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		
		viteroManager = (ViteroManager) CoreSpringFactory.getBean("viteroManager");
		
		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("table.empty"));
		tableConfig.setDownloadOffered(true);
		tableConfig.setColumnMovingOffered(false);
		tableConfig.setSortingEnabled(true);
		tableConfig.setDisplayTableHeader(true);
		tableConfig.setDisplayRowCount(false);
		tableConfig.setPageingEnabled(false);
		
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);
		
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("booking.begin", ViteroBookingDataModel.Column.begin.ordinal(), null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("booking.end", ViteroBookingDataModel.Column.end.ordinal(), null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("booking.roomSize", ViteroBookingDataModel.Column.roomSize.ordinal(), null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("booking.resource", ViteroBookingDataModel.Column.resource.ordinal(), "resource", ureq.getLocale()));

		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("infos", "table.action", translate("booking.infos")));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("delete", "table.action", translate("delete")));
		
		tableCtr.setSortColumn(0, false);

		reloadModel();
		
		putInitialPanel(tableCtr.getInitialComponent());
	}
	
	@Override
	protected void doDispose() {
		//auto disposed
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//nothing to do
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == tableCtr) {
			if(event instanceof TableEvent) {
				TableEvent e = (TableEvent)event;
				int row = e.getRowId();
				ViteroBooking booking = (ViteroBooking)tableCtr.getTableDataModel().getObject(row);
				if("delete".equals(e.getActionId())) {
					confirmDeleteVitero(ureq, booking);
				} else if("infos".equals(e.getActionId())) {
					openInfoBox(ureq, booking);
				} else if("resource".equals(e.getActionId())) {
					openResource(ureq, booking);
				}
			}
		} else if(source == dialogCtr) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				ViteroBooking booking = (ViteroBooking)dialogCtr.getUserObject();
				deleteBooking(ureq, booking);
			}
		} else if (source == cmc ) {
			removeAsListenerAndDispose(infoController);
			removeAsListenerAndDispose(cmc);
		} else if (source == infoController) {
			cmc.deactivate();
			removeAsListenerAndDispose(infoController);
			removeAsListenerAndDispose(cmc);
			reloadModel();
		}
	}
	
	protected void openResource(UserRequest ureq, ViteroBooking booking) {
		Property prop = booking.getProperty();
		if(prop != null) {
			String url;
			if(prop.getGrp() != null) {
				url = "[BusinessGroup:" + prop.getGrp().getKey() + "]";
			} else {
				OLATResourceable ores = OresHelper.createOLATResourceableInstance(prop.getResourceTypeName(), prop.getResourceTypeId());
				RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(ores, false);
				if(re != null) {
					url = "[RepositoryEntry:" + re.getKey() + "]";
				} else {
					showWarning("resource.dont.exist");
					return;
				}
			}
			BusinessControl bc = BusinessControlFactory.getInstance().createFromString(url);
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
			NewControllerFactory.getInstance().launch(ureq, bwControl);
		}
	}
	
	protected void openInfoBox(UserRequest ureq, ViteroBooking booking) {
		removeAsListenerAndDispose(infoController);
		removeAsListenerAndDispose(cmc);
		
		try {
			ViteroGroup group = viteroManager.getGroup(booking.getGroupId());
			infoController = new ViteroAdminBookingInfosController(ureq, getWindowControl(), booking, group);
			listenTo(infoController);

			cmc = new CloseableModalController(getWindowControl(), translate("close"), infoController.getInitialComponent(), true, translate("booking.raw.title"));
			listenTo(cmc);
			cmc.activate();
		} catch (VmsNotAvailableException e) {
			showError(VmsNotAvailableException.I18N_KEY);
		}
	}
	
	protected void deleteBooking(UserRequest ureq, ViteroBooking booking) {
		try {
			if( viteroManager.deleteBooking(booking)) {
				showInfo("delete.ok");
			} else {
				showError("delete.nok");
			}
			reloadModel();
		} catch (VmsNotAvailableException e) {
			showError(VmsNotAvailableException.I18N_KEY);
		}
	}
	
	protected void confirmDeleteVitero(UserRequest ureq, ViteroBooking booking) {
		String title = translate("delete");
		String text = translate("delete.confirm");
		dialogCtr = activateOkCancelDialog(ureq, title, text, dialogCtr);
		dialogCtr.setUserObject(booking);
	}
	
	protected void reloadModel() {
		List<ViteroBooking> bookings = viteroManager.getBookings(null, null);
		ViteroBookingDataModel tableModel = new ViteroBookingDataModel(bookings);
		tableCtr.setTableDataModel(tableModel);
	}
}