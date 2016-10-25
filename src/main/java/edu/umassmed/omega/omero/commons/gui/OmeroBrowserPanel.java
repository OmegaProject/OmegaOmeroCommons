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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.RootPaneContainer;

import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.data.coreElements.OmegaImage;
import edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;
import edu.umassmed.omega.commons.gui.GenericPanel;
import edu.umassmed.omega.omero.commons.OmeroGateway;
import edu.umassmed.omega.omero.commons.data.OmeroDatasetWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroImageWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroThumbnailImageInfo;
import edu.umassmed.omega.omero.commons.runnable.OmeroBrowerPanelImageLoader;

public class OmeroBrowserPanel extends GenericPanel {
	private static final long serialVersionUID = 7625488987526070516L;

	private final OmeroAbstractBrowserInterface browserPanel;
	private OmeroGateway gateway;

	private JPanel mainPanel;
	private JRadioButton gridView_btt, listView_btt;

	private Dimension panelSize;
	private OmeroDatasetWrapper datasetWrapper;

	private boolean updating, isListView;

	private final boolean isMultiSelection;

	private final List<OmegaImage> loadedImages;

	private List<OmeroThumbnailImageInfo> imagesInfo;

	private OmeroBrowserList list;
	private OmeroBrowserTable table;
	private JScrollPane scrollPaneBrowser;

	/**
	 * Create a new instance of this JPanel.
	 */
	public OmeroBrowserPanel(final RootPaneContainer parentContainer,
	        final OmeroAbstractBrowserInterface browserPanel,
	        final OmeroGateway gateway, final boolean isMultiSelection) {
		super(parentContainer);

		this.updating = false;
		this.isListView = true;
		this.isMultiSelection = isMultiSelection;

		this.datasetWrapper = null;
		this.loadedImages = new ArrayList<OmegaImage>();

		this.browserPanel = browserPanel;

		this.gateway = gateway;

		this.setLayout(new BorderLayout());

		this.createAndAddWidgets();

		this.addListeners();
	}

	private void createAndAddWidgets() {
		final JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		final ButtonGroup group = new ButtonGroup();
		this.gridView_btt = new JRadioButton(
		        OmeroPluginGUIConstants.BROWSER_VIEW_GRID);
		this.gridView_btt.setSelected(true);
		this.listView_btt = new JRadioButton(
		        OmeroPluginGUIConstants.BROWSER_LIST_GRID);
		group.add(this.gridView_btt);
		group.add(this.listView_btt);

		topPanel.add(this.gridView_btt);
		topPanel.add(this.listView_btt);

		this.add(topPanel, BorderLayout.NORTH);

		this.list = new OmeroBrowserList(this.isMultiSelection);
		this.table = new OmeroBrowserTable(this.isMultiSelection);
		this.scrollPaneBrowser = new JScrollPane(this.list);
		this.add(this.scrollPaneBrowser, BorderLayout.CENTER);
	}

	private void addListeners() {
		this.gridView_btt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				OmeroBrowserPanel.this.setListView();
			}
		});
		this.listView_btt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				OmeroBrowserPanel.this.setTableView();
			}
		});
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent evt) {
				OmeroBrowserPanel.this.createAndAddSingleImagePanels();
			}
		});
	}

	private void setListView() {
		this.isListView = true;
		this.scrollPaneBrowser.setViewportView(this.list);
	}

	private void setTableView() {
		this.isListView = false;
		this.scrollPaneBrowser.setViewportView(this.table);
	}

	protected void createAndAddSingleImagePanels() {
		if (this.imagesInfo == null) {
			this.list.clear();
			this.table.clear();
			return;
		}

		this.list.setLoadedElements(this.loadedImages);
		this.table.setLoadedElements(this.loadedImages);

		this.list.setElements(this.imagesInfo);
		this.table.setElements(this.imagesInfo);
	}

	/**
	 * Sets the images to display.
	 *
	 * @param images
	 *            the images to display.
	 */
	public void setImagesAndRecreatePanels(
	        final List<OmeroThumbnailImageInfo> imageInfo) {
		this.imagesInfo = imageInfo;
		// TODO refactoring?!
		if (this.imagesInfo != null) {
			Collections.sort(this.imagesInfo,
			        new Comparator<OmeroThumbnailImageInfo>() {
				        @Override
				        public int compare(final OmeroThumbnailImageInfo o1,
				                final OmeroThumbnailImageInfo o2) {
					        return o1.getImageName().compareTo(
					                o2.getImageName());
				        }
			        });
		}
		this.createAndAddSingleImagePanels();
	}

	/**
	 * Browses the specified datasets.
	 *
	 * @param id
	 *            The identifier of the dataset.
	 */
	public void browseDataset(final OmeroDatasetWrapper datasetWrapper) {
		this.datasetWrapper = datasetWrapper;
		this.setImagesAndRecreatePanels(null);
		if (datasetWrapper == null)
			return;
		// this.createAndAddSingleImagePanels();
		final OmeroBrowerPanelImageLoader loader = new OmeroBrowerPanelImageLoader(
		        this.browserPanel, this.gateway, datasetWrapper, true);
		this.browserPanel.updateMessageStatus(new OmegaMessageEvent(
		        OmeroPluginGUIConstants.LOADING_IMAGES));
		final Thread t = new Thread(loader);
		t.setName(loader.getClass().getSimpleName());
		OmegaLogFileManager.registerAsExceptionHandlerOnThread(t);
		t.start();
	}

	public void setCompSize(final Dimension size) {
		this.panelSize = size;
	}

	public void updateImagesSelection() {
		this.updating = true;
		// TODO is this neded?
		this.updating = false;
	}

	public void updateLoadedElements(final List<OmegaImage> loadedImages) {
		this.loadedImages.clear();
		if (loadedImages != null) {
			this.loadedImages.addAll(loadedImages);
		}
		this.createAndAddSingleImagePanels();
	}

	public boolean isMultiSelection() {
		return this.isMultiSelection;
	}

	public void setGateway(final OmeroGateway gateway) {
		this.gateway = gateway;
	}

	public Map<OmeroDatasetWrapper, List<OmeroImageWrapper>> getImagesToBeLoaded() {
		final Map<OmeroDatasetWrapper, List<OmeroImageWrapper>> map = new LinkedHashMap<OmeroDatasetWrapper, List<OmeroImageWrapper>>();
		List<OmeroThumbnailImageInfo> thumbnailList = null;
		if (this.isListView) {
			thumbnailList = this.list.getSelectedThumbnailList();
		} else {
			thumbnailList = this.table.getSelectedThumbnailList();
		}
		final List<OmeroImageWrapper> imageList = new ArrayList<OmeroImageWrapper>();
		for (final OmeroThumbnailImageInfo imageInfo : thumbnailList) {
			imageList.add(imageInfo.getImage());
		}
		if (!imageList.isEmpty()) {
			map.put(this.datasetWrapper, imageList);
		}
		return map;
	}
}
