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
package org.olat.ims.qti.statistics;

import org.dom4j.Document;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.nodes.CourseNodeConfiguration;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.course.nodes.IQSELFCourseNode;
import org.olat.course.nodes.IQSURVCourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.nodes.QTICourseNode;
import org.olat.course.nodes.TitledWrapperHelper;
import org.olat.course.statistic.StatisticResourceNode;
import org.olat.course.statistic.StatisticResourceResult;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.QTIDocument;
import org.olat.ims.qti.editor.beecom.objects.Section;
import org.olat.ims.qti.editor.beecom.parser.ParserManager;
import org.olat.ims.qti.editor.tree.ItemNode;
import org.olat.ims.qti.editor.tree.SectionNode;
import org.olat.ims.qti.process.ImsRepositoryResolver;
import org.olat.ims.qti.statistics.model.StatisticAssessment;
import org.olat.ims.qti.statistics.ui.QTI12AssessmentStatisticsController;
import org.olat.ims.qti.statistics.ui.QTI12ItemStatisticsController;
import org.olat.ims.qti.statistics.ui.QTI12SurveyItemStatisticsController;
import org.olat.ims.qti.statistics.ui.QTI12SurveyStatisticsController;
import org.olat.ims.qti.statistics.ui.QTI21OnyxAssessmentStatisticsController;
import org.olat.repository.RepositoryEntry;

import de.bps.onyx.plugin.OnyxModule;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTIStatisticResourceResult implements StatisticResourceResult {
	
	private final QTICourseNode courseNode;
	private final QTIStatisticsManager qtiStatisticsManager;
	
	private StatisticAssessment statisticAssessment;
	private QTIDocument qtiDocument;
	private ImsRepositoryResolver resolver;
	private QTIStatisticSearchParams searchParams;
	private final RepositoryEntry qtiRepositoryEntry;
	
	private QTIType type;
	
	public QTIStatisticResourceResult(QTICourseNode courseNode, QTIStatisticSearchParams searchParams) {
		this.courseNode = courseNode;
		this.searchParams = searchParams;
		qtiStatisticsManager = CoreSpringFactory.getImpl(QTIStatisticsManager.class);

		qtiRepositoryEntry = courseNode.getReferencedRepositoryEntry();
		if(OnyxModule.isOnyxTest(qtiRepositoryEntry.getOlatResource())) {
			type = QTIType.onyx;
		} else {
			resolver = new ImsRepositoryResolver(qtiRepositoryEntry);
			Document doc = resolver.getQTIDocument();
			ParserManager parser = new ParserManager();
			qtiDocument = (QTIDocument) parser.parse(doc);
			if (courseNode instanceof IQSURVCourseNode) {
				type = QTIType.survey;
			} else if (courseNode instanceof IQTESTCourseNode) {
				type = QTIType.test;
			} else if (courseNode instanceof IQSELFCourseNode){
				type = QTIType.self;
			}
		}
	}
	
	public QTIType getType() {
		return type;
	}

	public QTICourseNode getTestCourseNode() {
		return courseNode;
	}
	
	public RepositoryEntry getQTIRepositoryEntry() {
		return qtiRepositoryEntry;
	}
	
	public QTIDocument getQTIDocument() {
		return qtiDocument;
	}
	
	public QTIStatisticSearchParams getSearchParams() {
		return searchParams;
	}
	
	public StatisticAssessment getQTIStatisticAssessment() {
		if(statisticAssessment == null) {
			statisticAssessment = qtiStatisticsManager.getAssessmentStatistics(searchParams);
		}
		return statisticAssessment;
	}
	
	public ImsRepositoryResolver getResolver() {
		return resolver;
	}

	@Override
	public TreeModel getSubTreeModel() {
		if(qtiDocument == null) return null;
		
		GenericTreeModel subTreeModel = new GenericTreeModel();
		StatisticResourceNode rootTreeNode = new StatisticResourceNode(courseNode, this);
		subTreeModel.setRootNode(rootTreeNode);
		buildQTICourseNodeSubTree(qtiDocument, rootTreeNode);
		return subTreeModel;
	}

	@Override
	public Controller getController(UserRequest ureq, WindowControl wControl, TreeNode selectedNode) {
		return getController(ureq, wControl, selectedNode, false);
	}
	
	public Controller getController(UserRequest ureq, WindowControl wControl, TreeNode selectedNode, boolean printMode) {	
		if(selectedNode instanceof StatisticResourceNode) {
			return createAssessmentController(ureq, wControl, printMode);
		} else if(selectedNode instanceof SectionNode) {
			return createAssessmentController(ureq, wControl, printMode);	
		} else if(selectedNode instanceof ItemNode) {
			Item item = (Item)((ItemNode)selectedNode).getUserObject();
			return createItemController(ureq, wControl, item, printMode);
		}
		return null;
	}
	
	private Controller createAssessmentController(UserRequest ureq, WindowControl wControl, boolean printMode) {
		Controller ctrl;
		if(type == QTIType.survey) {
			ctrl = new QTI12SurveyStatisticsController(ureq, wControl, this, printMode);
		} else if (type == QTIType.onyx){
			ctrl = new QTI21OnyxAssessmentStatisticsController(ureq, wControl, this, printMode);
		} else {
			ctrl = new QTI12AssessmentStatisticsController(ureq, wControl, this, printMode);
		}
		CourseNodeConfiguration cnConfig = CourseNodeFactory.getInstance()
				.getCourseNodeConfigurationEvenForDisabledBB(courseNode.getType());
		String iconCssClass = cnConfig.getIconCSSClass();
		return TitledWrapperHelper.getWrapper(ureq, wControl, ctrl, courseNode, iconCssClass);
	}
	
	private Controller createItemController(UserRequest ureq, WindowControl wControl, Item item, boolean printMode) {
		if(type == QTIType.survey) {
			return new QTI12SurveyItemStatisticsController(ureq, wControl, item, this, printMode);
		} else {
			return new QTI12ItemStatisticsController(ureq, wControl, item, this, printMode);
		}
	}
	
	private void buildQTICourseNodeSubTree(QTIDocument qtiDocument, GenericTreeNode rootNode) {	
		for(Section section:qtiDocument.getAssessment().getSections()) {
			GenericTreeNode sectionNode = new SectionNode(section, null);
			sectionNode.setUserObject(section);
			rootNode.addChild(sectionNode);
			for (Item item : section.getItems()) {
				GenericTreeNode itemNode = new ItemNode(item);
				itemNode.setUserObject(item);
				sectionNode.addChild(itemNode);
			}
		}
	}
}
