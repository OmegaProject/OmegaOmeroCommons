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
package main.java.edu.umassmed.omega.omero.commons.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;

import main.java.edu.umassmed.omega.commons.OmegaLogFileManager;
import main.java.edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;
import main.java.edu.umassmed.omega.commons.exceptions.OmegaPluginExceptionStatusPanel;
import main.java.edu.umassmed.omega.commons.gui.GenericPluginPanel;
import main.java.edu.umassmed.omega.commons.gui.GenericStatusPanel;
import main.java.edu.umassmed.omega.commons.gui.checkboxTree.CheckBoxStatus;
import main.java.edu.umassmed.omega.commons.plugins.OmegaPlugin;
import main.java.edu.umassmed.omega.omero.commons.OmeroGateway;
import main.java.edu.umassmed.omega.omero.commons.data.OmeroDataWrapper;
import main.java.edu.umassmed.omega.omero.commons.data.OmeroDatasetWrapper;
import main.java.edu.umassmed.omega.omero.commons.data.OmeroImageWrapper;
import main.java.edu.umassmed.omega.omero.commons.data.OmeroThumbnailImageInfo;
import main.java.edu.umassmed.omega.omero.commons.runnable.OmeroBrowerPanelImageLoader;
import main.java.edu.umassmed.omega.omero.commons.runnable.OmeroDataMessageEvent;
import main.java.edu.umassmed.omega.omero.commons.runnable.OmeroThumbnailMessageEvent;
import main.java.edu.umassmed.omega.omero.commons.runnable.OmeroWrapperMessageEvent;
import pojos.ExperimenterData;
import pojos.GroupData;

public abstract class OmeroPanel extends GenericPluginPanel implements
OmeroAbstractBrowserInterface {

	private static final long serialVersionUID = -5740459087763362607L;

	private JSplitPane mainPanel;
	private JMenu loadableUserMenu;
	private JMenuItem notLoggedVisualMItem;

	private OmeroTreeBrowserPanel projectPanel;
	private OmeroBrowserPanel browserPanel;
	private GenericStatusPanel statusPanel;

	private JButton loadImages_butt, loadAndSelectImages_butt, close_butt;

	private OmeroGateway gateway;

	private final List<OmeroImageWrapper> imageWrapperToBeLoadedList;

	private int completedThreadsCounter, numOfThreads;

	private int previousLoc;
	private Dimension oldSplitPaneDimension;
	private double dividerLocation;

	private final boolean isMultiSelection;

	// the collection of already loaded datasets
	// TODO implement a caching system of loaded dataset to avoid loading each
	// time

	public OmeroPanel(final RootPaneContainer parent, final OmegaPlugin plugin,
			final int index, final OmeroGateway gateway) {
		super(parent, plugin, index);

		this.isMultiSelection = true;

		this.imageWrapperToBeLoadedList = new ArrayList<OmeroImageWrapper>();

		this.gateway = gateway;

		this.numOfThreads = 0;
		this.completedThreadsCounter = 0;

		this.previousLoc = -1;
		this.oldSplitPaneDimension = null;
		this.dividerLocation = 0.4;

		this.setPreferredSize(new Dimension(750, 500));
		this.setLayout(new BorderLayout());
		this.createMenu();
		this.createAndAddWidgets();
		this.addListeners();
	}

	public OmeroPanel(final RootPaneContainer parent, final OmeroGateway gateway) {
		super(parent);

		this.isMultiSelection = false;

		this.imageWrapperToBeLoadedList = new ArrayList<OmeroImageWrapper>();

		this.gateway = gateway;

		this.numOfThreads = 0;
		this.completedThreadsCounter = 0;

		this.previousLoc = -1;
		this.oldSplitPaneDimension = null;
		this.dividerLocation = 0.4;

		this.setPreferredSize(new Dimension(750, 500));
		this.setLayout(new BorderLayout());
		this.createMenu();
		this.createAndAddWidgets();
		this.addListeners();
	}

	private void createMenu() {
		final JMenuBar menu = super.getMenu();

		this.loadableUserMenu = new JMenu(
				OmeroPluginGUIConstants.MENU_BROWSE_GROUPS);
		this.notLoggedVisualMItem = new JMenuItem(
				OmeroPluginGUIConstants.MENU_BROWSE_GROUPS_NOT_CONNECTED);
		this.loadableUserMenu.add(this.notLoggedVisualMItem);

		menu.add(this.loadableUserMenu);
	}

	public void updateVisualizationMenu() throws ServerError {
		if (!this.gateway.isConnected()) {
			this.loadableUserMenu.add(this.notLoggedVisualMItem);
			return;
		}
		this.loadableUserMenu.removeAll();

		final List<JMenuItem> menuItems = new ArrayList<JMenuItem>();
		ExperimenterData loggedUser;
		try {
			loggedUser = this.gateway.getExperimenter();
		} catch (final omero.ServerError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		List<GroupData> groups;
		try {
			groups = this.gateway.getGroups();
		} catch (final omero.ServerError e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		for (final GroupData group : groups) {
			final JMenu menuItem = new JMenu(group.getName());
			List<ExperimenterData> exps;
			try {
				exps = this.gateway.getExperimenters(group);
			} catch (final omero.ServerError e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				continue;
			}
			for (final ExperimenterData exp : exps) {
				final JCheckBoxMenuItem subMenuItem = new JCheckBoxMenuItem(
				        exp.getFirstName() + " " + exp.getLastName());
				subMenuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent evt) {
						if (subMenuItem.isSelected()) {
							try {
								OmeroPanel.this.projectPanel
								.addExperimenterData(exp);
							} catch (final ServerError e) {
								e.printStackTrace();
							}
						} else {
							OmeroPanel.this.projectPanel
							.removeExperimenterData(exp);
						}
						OmeroPanel.this.checkSameUserInOtherGroups(menuItems,
								exp, subMenuItem.isSelected());
					}
				});
				if (exp.getId() == loggedUser.getId()) {
					subMenuItem.setSelected(true);
				}
				menuItem.add(subMenuItem);
			}
			this.loadableUserMenu.add(menuItem);
		}

		this.projectPanel.resetExperimenterData();
		try {
			this.projectPanel.addExperimenterData(loggedUser);
		} catch (final ServerError ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		this.projectPanel.updateTree(/* this.omegaData */);
	}

	public void checkSameUserInOtherGroups(final List<JMenuItem> menuItems,
			final ExperimenterData exp, final boolean selected) {
		final String name = exp.getFirstName() + " " + exp.getLastName();
		for (int i = 0; i < menuItems.size(); i++) {
			final JMenuItem menuItem = menuItems.get(i);
			if (!(menuItem instanceof JMenu))
				return;
			final JMenu menu = (JMenu) menuItem;
			for (int k = 0; k < menu.getItemCount(); k++) {
				final JMenuItem subMenuItem = menu.getItem(k);
				if (subMenuItem.getText().equals(name)) {
					subMenuItem.setSelected(selected);
				}
			}
		}
	}

	public void createAndAddWidgets() {
		this.projectPanel = new OmeroTreeBrowserPanel(
				this.getParentContainer(), this, this.gateway,
				this.isMultiSelection);
		final JScrollPane scrollPaneList = new JScrollPane(this.projectPanel);

		this.browserPanel = new OmeroBrowserPanel(this.getParentContainer(),
				this, this.gateway, this.isMultiSelection);

		this.mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.mainPanel.setLeftComponent(scrollPaneList);
		this.mainPanel.setRightComponent(this.browserPanel);
		// this.mainPanel.setDividerLocation(0.4);
		this.add(this.mainPanel, BorderLayout.CENTER);

		// TODO add button to open isSelected images
		final JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());

		this.loadImages_butt = new JButton(
				OmeroPluginGUIConstants.CONNECTION_DIALOG_LOAD);
		buttonPanel.add(this.loadImages_butt);

		// this.loadAndSelectImages_butt = new
		// JButton("Load and select images");
		// buttonPanel.add(this.loadAndSelectImages_butt);

		this.close_butt = new JButton(
				OmeroPluginGUIConstants.CONNECTION_DIALOG_SAVE);
		buttonPanel.add(this.close_butt);

		bottomPanel.add(buttonPanel, BorderLayout.NORTH);

		this.statusPanel = new GenericStatusPanel(1);

		bottomPanel.add(this.statusPanel, BorderLayout.SOUTH);

		this.add(bottomPanel, BorderLayout.SOUTH);
	}

	private void addListeners() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent evt) {
				OmeroPanel.this.manageComponentResized();
			}
		});
		this.loadImages_butt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent evt) {
				try {
					OmeroPanel.this.loadSelectedData();
				} catch (final ServerError ex) {
					ex.printStackTrace();
					return;
				} catch (final IOException ex) {
					ex.printStackTrace();
					return;
				}
			}
		});
		this.mainPanel.addPropertyChangeListener(
				JSplitPane.DIVIDER_LOCATION_PROPERTY,
				new PropertyChangeListener() {

					@Override
					public void propertyChange(final PropertyChangeEvent evt) {
						final JSplitPane source = (JSplitPane) evt.getSource();
						OmeroPanel.this.manageDividerPositionChanged(source
								.getSize());
					}
				});
	}

	private void manageComponentResized() {
		this.mainPanel.setDividerLocation(this.dividerLocation);
	}

	private void manageDividerPositionChanged(final Dimension dimension) {
		boolean resize = true;
		if (this.oldSplitPaneDimension != null) {
			final boolean widthEqual = this.oldSplitPaneDimension.width == dimension.width;
			final boolean heightEqual = this.oldSplitPaneDimension.height == dimension.height;
			resize = !widthEqual || !heightEqual;
		}
		if (!resize) {
			final int loc = this.mainPanel.getDividerLocation();
			if (this.previousLoc != -1) {
				final int diff = Math.abs(loc - this.previousLoc);
				if (diff <= 15)
					return;
			}
			this.previousLoc = loc;
			final int width = this.mainPanel.getWidth();
			double tmp = (loc * 100) / width;
			tmp /= 100;
			this.dividerLocation = tmp;
		}
		this.browserPanel.setCompSize(this.mainPanel.getRightComponent()
				.getSize());
		this.browserPanel.checkForResize();
		this.browserPanel.redrawImagePanels();
		this.oldSplitPaneDimension = dimension;
	}

	@Override
	public void updateParentContainer(final RootPaneContainer parent) {
		super.updateParentContainer(parent);
		this.browserPanel.updateParentContainer(parent);
		this.projectPanel.updateParentContainer(parent);
	}

	private void loadSelectedData() throws ServerError, IOException {
		final Map<OmeroDatasetWrapper, List<OmeroImageWrapper>> imagesToBeLoaded = this.browserPanel
				.getImagesToBeLoaded();
		final List<OmeroDatasetWrapper> datasetsToBeLoaded = this.projectPanel
				.getSelectedDatasets();
		for (final OmeroDataWrapper datasetWrapper : imagesToBeLoaded.keySet()) {
			if (datasetsToBeLoaded.contains(datasetWrapper)) {
				continue;
			}
			this.imageWrapperToBeLoadedList.addAll(imagesToBeLoaded
					.get(datasetWrapper));
		}

		final Map<Thread, OmeroBrowerPanelImageLoader> threads = new LinkedHashMap<Thread, OmeroBrowerPanelImageLoader>();
		this.numOfThreads = this.projectPanel.getSelectedDatasets().size();
		this.completedThreadsCounter = 0;
		for (final OmeroDatasetWrapper datasetWrapper : datasetsToBeLoaded) {
			final OmeroBrowerPanelImageLoader runnable = new OmeroBrowerPanelImageLoader(
					this, this.gateway, datasetWrapper, false);
			final Thread t = new Thread(runnable);
			threads.put(t, runnable);
			t.setName(runnable.getClass().getSimpleName());
			OmegaLogFileManager.registerAsExceptionHandlerOnThread(t);
			t.start();
		}

		if (this.numOfThreads == 0) {
			this.loadData(true);
		}

		// TODO to fix because the threads started try to update the gui and the
		// fact we are still in progress here dont allow the correct handling of
		// everything
		// Insert the threads size and a counter at class level count in the
		// feedback method addToBeLoadedImages if we reach threads size then we
		// start the loading process (that has to be refactore in another
		// method)
		// This has to be done only if threads isEmpty returns false otherwise
		// we should invoke it directly here
	}

	protected abstract void loadData(final boolean hasToSelect)
			throws ServerError, IOException;

	private void addToBeLoadedImages(
			final List<OmeroImageWrapper> imageWrapperList) {
		this.imageWrapperToBeLoadedList.addAll(imageWrapperList);
		this.completedThreadsCounter++;
		if (this.completedThreadsCounter == this.numOfThreads) {
			try {
				this.loadData(true);
			} catch (final ServerError ex) {
				// TODO handle error
				ex.printStackTrace();
			} catch (final IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void browseDataset(final OmeroDatasetWrapper datasetWrap) {
		this.browserPanel.browseDataset(datasetWrap);
	}

	private void setBrowsingImages(
			final List<OmeroThumbnailImageInfo> imageInfoList) {
		this.browserPanel.setImagesAndRecreatePanels(imageInfoList);
	}

	@Override
	public void updateMessageStatus(final OmegaMessageEvent evt) {
		try {
			this.statusPanel.updateStatus(0, evt.getMessage());
		} catch (final OmegaPluginExceptionStatusPanel ex) {
			OmegaLogFileManager.handlePluginException(this.getPlugin(), ex);
		}
		if (evt instanceof OmeroThumbnailMessageEvent) {
			this.setBrowsingImages(((OmeroThumbnailMessageEvent) evt)
					.getThumbnails());
		} else if (evt instanceof OmeroWrapperMessageEvent) {
			this.addToBeLoadedImages(((OmeroWrapperMessageEvent) evt)
					.getWrappers());
		} else if (evt instanceof OmeroDataMessageEvent) {
			final OmeroDataMessageEvent specificEvent = (OmeroDataMessageEvent) evt;
			this.projectPanel.updateOmeData(
					specificEvent.getExperimenterData(),
					specificEvent.getData());
			this.projectPanel.updateTree(/* this.omegaData */);
		}
	}

	public void updateDialogStatus(final String s) {

	}

	@Override
	public void updateImagesSelection(final CheckBoxStatus datasetStatus) {
		this.browserPanel.updateImagesSelection(datasetStatus);
	}

	@Override
	public void updateDatasetSelection(final int selectedImages) {
		this.projectPanel.updateDatasetSelection(selectedImages);
	}

	public OmeroGateway getGateway() {
		return this.gateway;
	}

	public List<OmeroImageWrapper> getImageWrapperToBeLoadedList() {
		return this.imageWrapperToBeLoadedList;
	}

	public OmeroTreeBrowserPanel getProjectPanel() {
		return this.projectPanel;
	}

	public OmeroBrowserPanel getBrowserPanel() {
		return this.browserPanel;
	}

	@Override
	public void onCloseOperation() {
		// TODO Auto-generated method stub
	}

	public void setGateway(final OmeroGateway gateway) {
		this.gateway = gateway;
		this.projectPanel.setGateway(gateway);
		this.browserPanel.setGateway(gateway);
	}
}
