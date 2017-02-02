package edu.umassmed.omega.omero.commons;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.RootPaneContainer;

import omero.ServerError;
import pojos.ChannelData;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.GroupData;
import pojos.ImageData;
import pojos.PixelsData;
import pojos.ProjectData;
import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.data.OmegaData;
import edu.umassmed.omega.commons.data.coreElements.OmegaDataset;
import edu.umassmed.omega.commons.data.coreElements.OmegaElement;
import edu.umassmed.omega.commons.data.coreElements.OmegaExperimenter;
import edu.umassmed.omega.commons.data.coreElements.OmegaExperimenterGroup;
import edu.umassmed.omega.commons.data.coreElements.OmegaImage;
import edu.umassmed.omega.commons.data.coreElements.OmegaImagePixels;
import edu.umassmed.omega.commons.data.coreElements.OmegaProject;
import edu.umassmed.omega.commons.data.imageDBConnectionElements.OmegaLoginCredentials;
import edu.umassmed.omega.commons.gui.dialogs.GenericMessageDialog;
import edu.umassmed.omega.omero.commons.data.OmeroExperimenterWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroGroupWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroImageWrapper;
import edu.umassmed.omega.omero.commons.data.OmeroServerInformation;

public class OmeroImporterUtilities {

	public static OmeroExperimenterWrapper getExperimenterWrapper(
			final OmeroGateway gateway) {
		OmeroExperimenterWrapper experimenter = null;
		ExperimenterData experimenterData;
		try {
			experimenterData = gateway.getExperimenter();
		} catch (final omero.ServerError ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return null;
		}
		experimenter = new OmeroExperimenterWrapper(experimenterData);
		return experimenter;
	}

	public static List<OmeroGroupWrapper> getGroupWrapper(
			final OmeroGateway gateway) {
		final List<OmeroGroupWrapper> groupsWrapperList = new ArrayList<OmeroGroupWrapper>();
		List<GroupData> groupsData;
		try {
			groupsData = gateway.getGroups();
		} catch (final omero.ServerError e) {
			// TODO Manage this
			e.printStackTrace();
			return null;
		}// experimenterData.getGroups();
		for (final GroupData groupData : groupsData) {
			final OmeroGroupWrapper wrapper = new OmeroGroupWrapper(groupData);
			groupsWrapperList.add(wrapper);
		}
		return groupsWrapperList;
	}

	public static Map<OmeroGroupWrapper, List<OmeroExperimenterWrapper>> getGroupLeaders(
			final List<OmeroGroupWrapper> groupWrapperList) {
		final Map<OmeroGroupWrapper, List<OmeroExperimenterWrapper>> groupLeadersMap = new LinkedHashMap<OmeroGroupWrapper, List<OmeroExperimenterWrapper>>();

		for (final OmeroGroupWrapper groupDataWrapper : groupWrapperList) {
			final List<OmeroExperimenterWrapper> leaderWrapperList = new ArrayList<OmeroExperimenterWrapper>();
			final GroupData groupData = groupDataWrapper.getGroupData();

			final Set<ExperimenterData> leadersData = groupData.getLeaders();

			for (final ExperimenterData leaderData : leadersData) {
				final OmeroExperimenterWrapper leaderWrapper = new OmeroExperimenterWrapper(
						leaderData);
				leaderWrapperList.add(leaderWrapper);
			}
			groupLeadersMap.put(groupDataWrapper, leaderWrapperList);
		}
		return groupLeadersMap;
	}

	public static OmegaExperimenter getExperimenter(final OmeroGateway gateway) {
		OmegaExperimenter experimenter = null;
		ExperimenterData experimenterData;
		try {
			experimenterData = gateway.getExperimenter();
		} catch (final omero.ServerError ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return null;
		}
		experimenter = new OmegaExperimenter(experimenterData.getFirstName(),
				experimenterData.getLastName());
		experimenter.setOmeroId(experimenterData.getId());
		return experimenter;
	}

	public static boolean loadAndAddGroups(
			final OmegaExperimenter experimenter, final OmeroGateway gateway,
			final OmegaData omegaData) {
		boolean dataChanged = false;
		List<GroupData> groupsData;
		try {
			groupsData = gateway.getGroups();
		} catch (final omero.ServerError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return dataChanged;
		}

		for (final GroupData groupData : groupsData) {
			OmegaExperimenterGroup group = omegaData.getExperimenterGroup(
					groupData.getId(), true);

			if (group != null) {
				if (!experimenter.containsGroup(group)) {
					experimenter.addGroup(group);
					dataChanged = true;
				}
				continue;
			}

			dataChanged = true;

			final Set<ExperimenterData> leadersData = groupData.getLeaders();
			final List<OmegaExperimenter> leaders = new ArrayList<OmegaExperimenter>();
			for (final ExperimenterData leaderData : leadersData) {
				OmegaExperimenter leader = omegaData.getExperimenter(
						leaderData.getId(), true);
				if (leader == null) {
					leader = new OmegaExperimenter(leaderData.getFirstName(),
							leaderData.getLastName());
					leader.setOmeroId(leaderData.getId());
				}
				leaders.add(leader);
			}

			group = new OmegaExperimenterGroup(leaders);
			group.setOmeroId(groupData.getId());
			omegaData.addExperimenterGroup(group);
			experimenter.addGroup(group);

			for (final OmegaExperimenter leader : leaders) {
				if (!leader.containsGroup(group)) {
					leader.addGroup(group);
				}
				omegaData.addExperimenter(leader);
			}
		}
		return dataChanged;
	}

	public static OmegaExperimenter loadAndAddExperimenterAndGroups(
			final OmeroGateway gateway, final OmegaData omegaData) {
		ExperimenterData experimenterData;
		try {
			experimenterData = gateway.getExperimenter();
		} catch (final omero.ServerError ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return null;
		}
		// Create all groups for the actual user create leaders for the groups
		// add everything to the main data
		List<GroupData> groupsData;
		try {
			groupsData = gateway.getGroups();
		} catch (final omero.ServerError e) {
			// TODO Manage this
			e.printStackTrace();
			return null;
		}// experimenterData.getGroups();
		final List<OmegaExperimenterGroup> groups = new ArrayList<OmegaExperimenterGroup>();
		for (final GroupData groupData : groupsData) {

			OmegaExperimenterGroup group = omegaData.getExperimenterGroup(
					groupData.getId(), true);

			final Set<ExperimenterData> leadersData = groupData.getLeaders();

			if (group != null) {
				for (final ExperimenterData leaderData : leadersData) {
					// if (group.containsLeader(leaderData.getId(), true)) {
					// continue;
					// }
					OmegaExperimenter leader = omegaData.getExperimenter(
							leaderData.getId(), true);
					if (leader == null) {
						leader = new OmegaExperimenter(
								leaderData.getFirstName(),
								leaderData.getLastName());
						leader.setOmeroId(leaderData.getId());
					}
					if (!group.containsLeader(leader)) {
						group.addLeader(leader);
					}

				}
				groups.add(group);
				continue;
			}

			final List<OmegaExperimenter> leaders = new ArrayList<OmegaExperimenter>();
			for (final ExperimenterData leaderData : leadersData) {
				final OmegaExperimenter leader = new OmegaExperimenter(
						leaderData.getFirstName(), leaderData.getLastName());
				leader.setOmeroId(leaderData.getId());
				leaders.add(leader);
			}

			group = new OmegaExperimenterGroup(leaders);
			group.setOmeroId(groupData.getId());
			groups.add(group);
			omegaData.addExperimenterGroup(group);

			for (final OmegaExperimenter leader : leaders) {
				if (!leader.containsGroup(group)) {
					leader.addGroup(group);
				}
				omegaData.addExperimenter(leader);
			}
		}

		// Create the actual user with his groups
		// Add it to the main data
		final OmegaExperimenter experimenter = new OmegaExperimenter(
				experimenterData.getFirstName(),
				experimenterData.getLastName(), groups);
		experimenter.setOmeroId(experimenterData.getId());
		omegaData.addExperimenter(experimenter);
		return experimenter;
	}

	public static OmegaExperimenter loadAndAddExperimenter(
			final OmeroGateway gateway, final OmegaData omegaData) {
		ExperimenterData experimenterData;
		try {
			experimenterData = gateway.getExperimenter();
		} catch (final omero.ServerError ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			return null;
		}
		final OmegaExperimenter experimenter = omegaData.getExperimenter(
				experimenterData.getId(), true);
		return experimenter;
	}

	public static boolean loadAndAddData(final OmeroImageWrapper imageWrapper,
			final OmeroGateway gateway, final OmegaData omegaData,
			final boolean hasToSelect, final List<OmegaElement> loadedElements,
			final boolean dataChanged) {
		boolean loadDataChanged = dataChanged;
		final ProjectData projectData = imageWrapper.getProjectData();
		final DatasetData datasetData = imageWrapper.getDatasetData();
		final ImageData imageData = imageWrapper.getImageData();

		// TODO introdurre controlli se project/dataset/image gia
		// presenti

		OmegaProject project = omegaData.getProject(projectData.getId(), true);
		OmegaDataset dataset = omegaData.getDataset(datasetData.getId(), true);
		OmegaImage image = omegaData.getImage(imageData.getId(), true);

		// final List<Long> ids = new ArrayList<Long>();
		// ids.add(imageData.getId());
		// final ImageData dlImage = this.gateway.getImages(datasetData,
		// ids)
		// .get(0);

		// Create pixels
		List<OmegaImagePixels> pixelsList;
		if (image == null) {
			pixelsList = new ArrayList<OmegaImagePixels>();
		} else {
			pixelsList = image.getPixels();
		}

		for (final PixelsData pixelsData : imageData.getAllPixels()) {
			if ((image != null)
					&& image.containsPixels(pixelsData.getId(), true)) {
				continue;
			}

			final Map<Integer, String> channelNames = new LinkedHashMap<Integer, String>();
			List<ChannelData> channels = null;
			try {
				channels = gateway.getChannels(pixelsData);
			} catch (final ServerError ex) {
				OmegaLogFileManager.handleUncaughtException(ex, false);
			}
			if (channels != null) {
				for (final ChannelData chan : channels) {
					channelNames.put(chan.getIndex(), chan.getName());
				}
			}

			final OmegaImagePixels pixels = new OmegaImagePixels(
					pixelsData.getPixelType(), pixelsData.getSizeX(),
					pixelsData.getSizeY(), pixelsData.getSizeZ(),
					pixelsData.getSizeC(), pixelsData.getSizeT(),
					pixelsData.getPixelSizeX(), pixelsData.getPixelSizeY(),
					pixelsData.getPixelSizeZ(), channelNames);
			pixels.setOmeroId(pixelsData.getId());
			final int defaultT = gateway.getDefaultT(pixelsData.getId());
			pixels.setSelectedT(defaultT);
			final int defaultZ = gateway.getDefaultZ(pixelsData.getId());
			pixels.setSelectedZ(defaultZ);
			for (int i = 0; i < pixelsData.getSizeC(); i++) {
				pixels.setSelectedC(i, true);
			}
			pixelsList.add(pixels);
		}

		// Create image
		if (image == null) {
			final ExperimenterData expData = imageData.getOwner();
			OmegaExperimenter exp = omegaData.getExperimenter(expData.getId(),
					true);
			if (exp == null) {
				exp = new OmegaExperimenter(expData.getFirstName(),
						expData.getLastName());
				exp.setOmeroId(expData.getId());
			}
			final long acquisition = imageData.getAcquisitionDate().getTime();
			final Calendar acqui = Calendar.getInstance();
			acqui.setTimeInMillis(acquisition);
			final Date acquisitionDate = acqui.getTime();
			final long importation = imageData.getInserted().getTime();
			final Calendar impor = Calendar.getInstance();
			impor.setTimeInMillis(importation);
			final Date importedDate = impor.getTime();
			// put exp instead of experimenter
			image = new OmegaImage(imageData.getName(), exp, acquisitionDate,
					importedDate, pixelsList);
			image.setOmeroId(imageData.getId());
			loadDataChanged = true;
		} else {
			for (final OmegaImagePixels pixels : pixelsList) {
				if (!image.containsPixels(pixels.getOmeroId(), true)) {
					image.addPixels(pixels);
				}
			}
		}

		for (final OmegaImagePixels pixels : pixelsList) {
			pixels.setParentImage(image);
		}

		if (hasToSelect && !loadedElements.contains(image)) {
			loadedElements.add(image);
		}

		// Create dataset
		if (dataset == null) {
			final List<OmegaImage> images = new ArrayList<OmegaImage>();
			images.add(image);
			dataset = new OmegaDataset(datasetData.getName(), images);
			dataset.setOmeroId(datasetData.getId());
			image.addParentDataset(dataset);
			loadDataChanged = true;
		} else {
			if (!image.getParentDatasets().contains(dataset)) {
				image.addParentDataset(dataset);
			}
			if (!dataset.containsImage(image.getOmeroId(), true)) {
				dataset.addImage(image);
				loadDataChanged = true;
			}
		}

		// if (hasToSelect && !loadedElements.contains(dataset)) {
		// loadedElements.add(dataset);
		// }

		if (project == null) {
			// Create project
			final List<OmegaDataset> datasets = new ArrayList<OmegaDataset>();
			datasets.add(dataset);
			project = new OmegaProject(projectData.getName(), datasets);
			project.setOmeroId(projectData.getId());
			omegaData.addProject(project);
			loadDataChanged = true;
		} else {
			if (!project.containsDataset(dataset.getOmeroId(), true)) {
				project.addDataset(dataset);
				loadDataChanged = true;
			}
		}
		dataset.setParentProject(project);

		// if (hasToSelect && !loadedElements.contains(project)) {
		// loadedElements.add(project);
		// }
		return loadDataChanged;
	}

	public static void connectToGateway(final RootPaneContainer container,
			final OmeroGateway gateway, final String host, final int port,
			final String username, final String psw) {
		final int error = gateway.connect(new OmegaLoginCredentials(username,
				psw), new OmeroServerInformation(host, port));
		OmeroImporterUtilities.handleConnectionError(container, error);
	}

	public static void handleConnectionError(final RootPaneContainer container,
			final int error) {
		String errorMsg = null;
		switch (error) {
			case 0:
				break;
			case 1:
				errorMsg = "Unable to create a session.";
				break;
			case 2:
				errorMsg = "<html>Access denied.<br>Verify username and/or password.</html>";
				break;
			case 3:
				errorMsg = "Server error.";
				break;
			case 4:
				errorMsg = "<html>Unable to find the server<br>Verify server address.</html>";
				break;
			case 5:
				errorMsg = "<html>Server refused the connection.<br>Verify port.</html>";
				break;
			default:
				errorMsg = "Unknown error.";
		}
		if (errorMsg != null) {
			final GenericMessageDialog errorDialog = new GenericMessageDialog(
					container, "Omero server connection error", errorMsg, true);
			errorDialog.enableClose();
			errorDialog.setVisible(true);
		}
	}
}
