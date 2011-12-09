package org.olat.admin.user.bulkChange;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.filter.FilterFactory;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;

/**
 * 
 * Description:<br>
 * model for group add overview at last step in bulk-change
 * 
 * <P>
 * Initial Date:  09.05.2011 <br>
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GroupAddOverviewModel extends DefaultTableDataModel {

	private Translator translator;
	private List<Long> mailGroupIDs;
	private List<Long> ownGroupIDs;
	private List<Long> partGroupIDs;
	private BusinessGroupManager bGM;

	public GroupAddOverviewModel(List<Long> allGroupIDs, List<Long> ownGroupIDs, List<Long> partGroupIDs, List<Long> mailGroups, Translator trans) {
		super(allGroupIDs);
		this.translator = trans;
		this.ownGroupIDs = ownGroupIDs;
		this.partGroupIDs = partGroupIDs;
		this.mailGroupIDs = mailGroups;
		bGM = BusinessGroupManagerImpl.getInstance();
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Long key = (Long) getObject(row);			
		BusinessGroup group = bGM.loadBusinessGroup(key, false); 
		if (group == null) return "error";
		
		switch (col) {
			case 0: // name
				String name = group.getName();
				name = StringEscapeUtils.escapeHtml(name).toString();
				return name; 
			case 1: // description
				String desc = group.getDescription();
				desc = FilterFactory.getHtmlTagAndDescapingFilter().filter(desc);
				return desc;		
			case 2: // type
				return translator.translate(group.getType());	
			case 3: // users role
				if(partGroupIDs.contains(key) && ownGroupIDs.contains(key)) {
					return translator.translate("attende.and.owner");
				}
				else if(partGroupIDs.contains(key)) {
					return translator.translate("attende");
				}
				else if(ownGroupIDs.contains(key)) {
					return translator.translate("owner");
				}
			case 4: // send email
				if (mailGroupIDs.contains(key)){
					return translator.translate("yes");
				} else {
					return translator.translate("no");
				}
			default: 
				return "error";
		}
	}
}