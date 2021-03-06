/*******************************************************************************
 * Copyright (C) 2014 University of Massachusetts Medical School Alessandro
 * Rigano (Program in Molecular Medicine) Caterina Strambio De Castillia
 * (Program in Molecular Medicine)
 *
 * Created by the Open Microscopy Environment inteGrated Analysis (OMEGA) team:
 * Alex Rigano, Caterina Strambio De Castillia, Jasmine Clark, Vanni Galli,
 * Raffaello Giulietti, Loris Grossi, Eric Hunter, Tiziano Leidi, Jeremy Luban,
 * Ivo Sbalzarini and Mario Valle.
 *
 * Key contacts: Caterina Strambio De Castillia: caterina.strambio@umassmed.edu
 * Alex Rigano: alex.rigano@umassmed.edu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package edu.umassmed.omega.omero.commons.runnable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;
import edu.umassmed.omega.commons.gui.interfaces.OmegaMessageDisplayerPanelInterface;
import edu.umassmed.omega.omero.commons.OmeroGateway;

public class OmeroListPanelProjectAndDatasetLoader implements Runnable {

	private static int counter = 0;

	public static int getCounter() {
		return OmeroListPanelProjectAndDatasetLoader.counter++;
	}

	// private final OmeroListPanel omeroProjectsPanel;
	private final OmegaMessageDisplayerPanelInterface displayerPanel;
	private final OmeroGateway gateway;
	private final ExperimenterData expData;

	private int projectLoaded;
	private int projectToLoad;

	private final Map<ProjectData, List<DatasetData>> data;

	public OmeroListPanelProjectAndDatasetLoader(
			final OmegaMessageDisplayerPanelInterface displayerPanel,
			final OmeroGateway gateway, final ExperimenterData expData) {
		this.displayerPanel = displayerPanel;
		this.gateway = gateway;
		this.expData = expData;

		this.data = new HashMap<ProjectData, List<DatasetData>>();

		this.projectToLoad = 0;
		this.projectLoaded = 0;
	}

	@Override
	public void run() {
		try {
			final List<ProjectData> projects = this.gateway
					.getProjects(this.expData);
			this.projectToLoad = projects.size();
			for (final ProjectData proj : projects) {
				final int currentlyLoading = 1 + this.projectLoaded;
				this.updateLoadingStatus(currentlyLoading);
				final List<DatasetData> datasets = this.gateway
						.getDatasets(proj);
				this.data.put(proj, datasets);
				this.projectLoaded++;
			}
		} catch (final Exception ex) {
			OmegaLogFileManager.handleCoreException(ex, true);
		}

		this.updateLoadingStatus(this.projectLoaded);
	}

	private void updateLoadingStatus(final int currentlyLoading) {
		final String loadingStatus = currentlyLoading + "/"
				+ this.projectToLoad + " project(s) displayed for "
				+ this.expData.getFirstName() + " "
				+ this.expData.getLastName();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {

					if (OmeroListPanelProjectAndDatasetLoader.this.projectLoaded == OmeroListPanelProjectAndDatasetLoader.this.projectToLoad) {
						OmeroListPanelProjectAndDatasetLoader.this.displayerPanel
								.updateMessageStatus(new OmeroDataMessageEvent(
										loadingStatus,
										OmeroListPanelProjectAndDatasetLoader.this.expData,
										OmeroListPanelProjectAndDatasetLoader.this.data));
					} else {
						OmeroListPanelProjectAndDatasetLoader.this.displayerPanel
								.updateMessageStatus(new OmegaMessageEvent(
										loadingStatus));
					}
				}
			});
		} catch (final InvocationTargetException | InterruptedException ex) {
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}
	}
}
