/*******************************************************************************
 * Copyright (C) 2014 University of Massachusetts Medical School
 * Alessandro Rigano (Program in Molecular Medicine)
 * Caterina Strambio De Castillia (Program in Molecular Medicine)
 *
 * Created by the Open Microscopy Environment inteGrated Analysis (OMEGA) team:
 * Alex Rigano, Caterina Strambio De Castillia, Jasmine Clark, Vanni Galli,
 * Raffaello Giulietti, Loris Grossi, Eric Hunter, Tiziano Leidi, Jeremy Luban,
 * Ivo Sbalzarini and Mario Valle.
 *
 * Key contacts:
 * Caterina Strambio De Castillia: caterina.strambio@umassmed.edu
 * Alex Rigano: alex.rigano@umassmed.edu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package edu.umassmed.omega.omero.commons.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.RootPaneContainer;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.ProjectData;
import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.data.coreElements.OmegaImage;
import edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;
import edu.umassmed.omega.commons.gui.GenericPanel;
import edu.umassmed.omega.omero.commons.OmeroGateway;
import edu.umassmed.omega.omero.commons.data.OmeroDataWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroDatasetWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroExperimenterWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroProjectWrapper;
import edu.umassmed.omega.omero.commons.runnable.OmeroListPanelProjectAndDatasetLoader;

public class OmeroTreeBrowserPanel extends GenericPanel {

	private static final long serialVersionUID = -5868897435063007049L;

	private final List<OmeroDatasetWrapper> selectedDatasetList;
	private final List<OmeroExperimenterWrapper> expList;

	private final List<OmegaImage> loadedImages;

	private OmeroGateway gateway;
	private final OmeroAbstractBrowserInterface browserPanel;

	private final Map<String, OmeroDataWrapper> nodeMap;
	private final DefaultMutableTreeNode root;

	private JTree dataTree;

	private final boolean isMultiSelection;

	private List<OmeroProjectWrapper> expandedProjects;
	private List<OmeroExperimenterWrapper> expandedExperimenter;
	private OmeroDatasetWrapper currentSelection;

	public OmeroTreeBrowserPanel(final RootPaneContainer parentContainer,
	        final OmeroAbstractBrowserInterface browserPanel,
	        final OmeroGateway gateway, final boolean isMultiSelection) {
		super(parentContainer);
		this.isMultiSelection = isMultiSelection;

		this.currentSelection = null;
		this.selectedDatasetList = new ArrayList<OmeroDatasetWrapper>();

		this.expList = new ArrayList<OmeroExperimenterWrapper>();

		this.loadedImages = new ArrayList<OmegaImage>();

		this.root = new DefaultMutableTreeNode();
		this.root.setUserObject(OmeroPluginGUIConstants.TREE_TITLE);
		this.nodeMap = new LinkedHashMap<String, OmeroDataWrapper>();

		this.setLayout(new BorderLayout());

		this.createAndAddWidgets();
		this.addListeners();

		// this.updateTree();

		this.gateway = gateway;
		this.browserPanel = browserPanel;
	}

	public void createAndAddWidgets() {
		this.dataTree = new JTree(this.root);

		this.dataTree.expandRow(0);
		this.dataTree.setRootVisible(false);
		this.dataTree.setEditable(true);

		final JScrollPane scrollPane = new JScrollPane(this.dataTree);
		scrollPane.setBorder(new TitledBorder(
		        OmeroPluginGUIConstants.TREE_TITLE));

		this.add(scrollPane, BorderLayout.CENTER);
	}

	private void addListeners() {
		this.dataTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent evt) {
				OmeroTreeBrowserPanel.this.handleMouseClicked(evt.getX(),
				        evt.getY());
			}
		});
		this.dataTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(final TreeSelectionEvent evt) {
				OmeroTreeBrowserPanel.this.handleSelection();
			}
		});
	}

	private void handleSelection() {
		this.selectedDatasetList.clear();
		for (final TreePath path : OmeroTreeBrowserPanel.this.dataTree
				.getSelectionPaths()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
			        .getLastPathComponent();
			final String s = node.toString();
			final OmeroDataWrapper element = this.nodeMap.get(s);
			if (element instanceof OmeroDatasetWrapper) {
				this.selectedDatasetList.add((OmeroDatasetWrapper) element);
			} else if (element instanceof OmeroProjectWrapper) {
				this.selectedDatasetList.addAll(((OmeroProjectWrapper) element)
				        .getDatasets());
			} else if (element instanceof OmeroExperimenterWrapper) {
				for (final OmeroProjectWrapper project : ((OmeroExperimenterWrapper) element)
				        .getProjects()) {
					this.selectedDatasetList.addAll(project.getDatasets());
				}
			}
		}
	}

	private void handleMouseClicked(final int x, final int y) {
		final TreePath path = this.dataTree.getPathForLocation(x, y);
		if (path == null)
			return;
		final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
		        .getLastPathComponent();
		final String s = node.toString();
		final OmeroDataWrapper element = this.nodeMap.get(s);
		if (element instanceof OmeroDatasetWrapper) {
			final OmeroDatasetWrapper datasetWrapper = (OmeroDatasetWrapper) element;
			if (this.currentSelection != datasetWrapper) {
				this.currentSelection = datasetWrapper;
				this.browserPanel.browseDataset(datasetWrapper);
			}
			this.browserPanel.updateImagesSelection();
		}
	}

	public void resetExperimenterData() {
		this.expList.clear();
	}

	public void addExperimenterData(final ExperimenterData experimenterData)
	        throws ServerError {
		this.browserPanel.updateMessageStatus(new OmegaMessageEvent(
		        OmeroPluginGUIConstants.LOADING_PROJECT_AND_DATASET));
		final OmeroListPanelProjectAndDatasetLoader loader = new OmeroListPanelProjectAndDatasetLoader(
		        this.browserPanel, this.gateway, experimenterData);
		final Thread t = new Thread(loader);
		t.setName(loader.getClass().getSimpleName()
				+ OmeroListPanelProjectAndDatasetLoader.getCounter());
		OmegaLogFileManager.registerAsExceptionHandlerOnThread(t);
		t.start();
	}

	public void updateOmeData(final ExperimenterData expData,
	        final Map<ProjectData, List<DatasetData>> datas) {

		final OmeroExperimenterWrapper expWrapper = new OmeroExperimenterWrapper(
		        expData);

		final List<ProjectData> projects = new ArrayList<>(datas.keySet());
		Collections.sort(projects, new Comparator<ProjectData>() {
			@Override
			public int compare(final ProjectData o1, final ProjectData o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		expWrapper.setProjects(projects);

		for (final ProjectData proj : projects) {
			final List<DatasetData> dataset = new ArrayList<>(datas.get(proj));
			Collections.sort(dataset, new Comparator<DatasetData>() {
				@Override
				public int compare(final DatasetData o1, final DatasetData o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			expWrapper.setDatasets(proj, dataset);
		}

		this.expList.add(expWrapper);
	}

	public void removeExperimenterData(final ExperimenterData experimenterData) {
		// TODO remove projects / dataset from list
		OmeroExperimenterWrapper omeExpToRemove = null;
		for (final OmeroExperimenterWrapper omeExp : this.expList) {
			if (omeExp.getID() == experimenterData.getId()) {
				omeExpToRemove = omeExp;
				break;
			}
		}
		this.expList.remove(omeExpToRemove);
		this.updateTree();
	}

	public void updateTree() {
		this.dataTree.setRootVisible(true);

		String s = null;
		this.root.removeAllChildren();

		((DefaultTreeModel) this.dataTree.getModel()).reload();
		this.nodeMap.clear();

		for (final OmeroExperimenterWrapper expWrapper : this.expList) {
			final List<OmeroProjectWrapper> projects = expWrapper.getProjects();
			final DefaultMutableTreeNode expNode = new DefaultMutableTreeNode();
			for (final OmeroProjectWrapper projWrapper : projects) {
				final List<OmeroDatasetWrapper> datasets = projWrapper
				        .getDatasets();
				final DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode();
				for (final OmeroDatasetWrapper datasetWrapper : datasets) {
					final DefaultMutableTreeNode datasetNode = new DefaultMutableTreeNode();
					s = datasetWrapper.getStringRepresentation();
					this.nodeMap.put(s, datasetWrapper);
					datasetNode.setUserObject(s);
					projectNode.add(datasetNode);
				}
				s = projWrapper.getStringRepresentation();
				this.nodeMap.put(s, projWrapper);
				projectNode.setUserObject(s);
				expNode.add(projectNode);
			}
			s = expWrapper.getStringRepresentation();
			this.nodeMap.put(s, expWrapper);
			expNode.setUserObject(s);
			this.root.add(expNode);
		}

		this.dataTree.expandRow(0);
		this.dataTree.setRootVisible(false);
		this.dataTree.repaint();
	}

	public List<OmeroDatasetWrapper> getSelectedDatasets() {
		return this.selectedDatasetList;
	}

	public void updateLoadedElements(final List<OmegaImage> loadedImages) {
		this.loadedImages.clear();
		if (loadedImages != null) {
			this.loadedImages.addAll(loadedImages);
		}
		this.selectedDatasetList.clear();
		this.updateTree();
	}

	public void setGateway(final OmeroGateway gateway) {
		this.gateway = gateway;
	}
}
