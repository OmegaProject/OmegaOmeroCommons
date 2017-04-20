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
package edu.umassmed.omega.omero.commons;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import omero.ServerError;
import omero.client;
import omero.api.IAdminPrx;
import omero.api.IContainerPrx;
import omero.api.IPixelsPrx;
import omero.api.IQueryPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.RenderingEnginePrx;
import omero.api.ServiceFactoryPrx;
import omero.api.ServiceInterfacePrx;
import omero.api.StatefulServiceInterfacePrx;
import omero.api.ThumbnailStorePrx;
import omero.model.Channel;
import omero.model.Dataset;
import omero.model.Experimenter;
import omero.model.ExperimenterGroup;
import omero.model.IObject;
import omero.model.Pixels;
import omero.model.PlaneInfoI;
import omero.model.Project;
import omero.model.Time;
import omero.romio.PlaneDef;
import omero.sys.ParametersI;
import pojos.ChannelData;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.GroupData;
import pojos.PixelsData;
import pojos.ProjectData;
import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.data.imageDBConnectionElements.OmegaGateway;
import edu.umassmed.omega.commons.data.imageDBConnectionElements.OmegaLoginCredentials;
import edu.umassmed.omega.commons.data.imageDBConnectionElements.OmegaServerInformation;

/**
 * Entry point to access the services. Code should be provided to keep those
 * services alive.
 */
public class OmeroGateway extends OmegaGateway {

	/**
	 * The maximum number of thumbnails retrieved before restarting the
	 * thumbnails service.
	 */
	private static final int MAX_RETRIEVAL = 100;

	/** Keeps the client's session alive. */
	private ScheduledThreadPoolExecutor executor;

	/**
	 * The Blitz client object, this is the entry point to the OMERO Server
	 * using a secure connection.
	 */
	private client secureClient;
	private OmeroKeepClientAlive kca;

	/**
	 * The entry point provided by the connection library to access the various
	 * <i>OMERO</i> services.
	 */
	private ServiceFactoryPrx entryEncrypted;

	/** Collection of services to keep alive. */
	private final List<ServiceInterfacePrx> services;

	/** Collection of services to keep alive. */
	private final Map<Long, StatefulServiceInterfacePrx> reServices;

	/** The container service. */
	private IContainerPrx containerService;

	/** The Admin service. */
	private IAdminPrx adminService;

	/** The thumbnail service. */
	private ThumbnailStorePrx thumbnailService;

	private IPixelsPrx pixelsService;

	/**
	 * The number of thumbnails already retrieved. Resets to <code>0</code> when
	 * the value equals {@link #MAX_RETRIEVAL}.
	 */
	private int thumbnailRetrieval;

	/** Flag indicating if you are connected or not. */
	private boolean isConnected;

	/**
	 * Creates a <code>BufferedImage</code> from the passed array of bytes.
	 *
	 * @param values
	 *            The array of bytes.
	 * @return See above.
	 * @throws RenderingServiceException
	 *             If we cannot create an image.
	 */
	private BufferedImage createImage(final byte[] values) {
		try {
			final ByteArrayInputStream stream = new ByteArrayInputStream(values);
			final BufferedImage image = ImageIO.read(stream);
			image.setAccelerationPriority(1f);
			return image;
		} catch (final Exception e) {
			// TODO ManageException
		}

		return null;
	}

	/**
	 * Returns the {@link ThumbnailStorePrx} service.
	 *
	 * @return See above.
	 * @throws DSOutOfServiceException
	 *             If the connection is broken, or logged in
	 * @throws DSAccessException
	 *             If an error occurred while trying to retrieve data from OMERO
	 *             service.
	 */
	private ThumbnailStorePrx getThumbnailService() {
		try {
			if (this.thumbnailRetrieval == OmeroGateway.MAX_RETRIEVAL) {
				this.thumbnailRetrieval = 0;
				// to be on the save side
				if (this.thumbnailService != null) {
					this.thumbnailService.close();
				}
				this.services.remove(this.thumbnailService);
				this.thumbnailService = null;
			}
			if (this.thumbnailService == null) {
				this.thumbnailService = this.entryEncrypted
						.createThumbnailStore();
				this.services.add(this.thumbnailService);
			}
			this.thumbnailRetrieval++;
			return this.thumbnailService;
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}

		// TODO Manage Null Case
		return null;
	}

	/**
	 * Returns the {@link IContainerPrx} service.
	 *
	 * @return See above.
	 * @throws DSOutOfServiceException
	 *             If the connection is broken, or logged in
	 * @throws DSAccessException
	 *             If an error occurred while trying to retrieve data from OMERO
	 *             service.
	 */
	private IContainerPrx getContainerService() {
		try {
			if (this.containerService == null) {
				this.containerService = this.entryEncrypted
						.getContainerService();
				this.services.add(this.containerService);
			}
			return this.containerService;
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}

		// TODO Manage Null Case
		return null;
	}

	private IPixelsPrx getPixelsService() {
		try {
			if (this.pixelsService == null) {
				this.pixelsService = this.entryEncrypted.getPixelsService();
				this.services.add(this.pixelsService);
			}
			return this.pixelsService;
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}

		// TODO Manage Null Case
		return null;
	}

	/**
	 * Returns the {@link IAdminPrx} service.
	 *
	 * @return See above.
	 */
	private IAdminPrx getAdminService() {
		try {
			if (this.adminService == null) {
				this.adminService = this.entryEncrypted.getAdminService();
				this.services.add(this.adminService);
			}
			return this.adminService;
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}

		// TODO Manage Null Case
		return null;
	}

	/**
	 * Returns the {@link RenderingEnginePrx Rendering service}.
	 *
	 * @return See above.
	 * @throws DSOutOfServiceException
	 *             If the connection is broken, or logged in
	 * @throws DSAccessException
	 *             If an error occurred while trying to retrieve data from OMERO
	 *             service.
	 */
	private RenderingEnginePrx createRenderingService() {
		try {
			final RenderingEnginePrx engine = this.entryEncrypted
					.createRenderingEngine();
			return engine;
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}

		// TODO Manage Null Case
		return null;
	}

	/** Creates a new instance. */
	public OmeroGateway() {
		this.isConnected = false;
		this.services = new ArrayList<ServiceInterfacePrx>();
		this.reServices = new HashMap<Long, StatefulServiceInterfacePrx>();
	}

	public client getClient() {
		return this.secureClient;
	}

	/** Keeps the services alive. */
	protected void keepSessionAlive() {
		final int n = this.services.size() + this.reServices.size();
		final ServiceInterfacePrx[] entries = new ServiceInterfacePrx[n];
		final Iterator<ServiceInterfacePrx> i = this.services.iterator();
		int index = 0;
		while (i.hasNext()) {
			entries[index] = i.next();
			index++;
		}
		final Iterator<Long> j = this.reServices.keySet().iterator();
		while (j.hasNext()) {
			entries[index] = this.reServices.get(j.next());
			index++;
		}
		try {
			this.entryEncrypted.keepAllAlive(entries);
		} catch (final Exception e) {
			// Handle exception. Here
		}
	}

	/**
	 * Logs in. otherwise.
	 *
	 * @param loginCred
	 *            Host the information to connect.
	 * @return <code>true</code> if connected, <code>false</code>
	 * @throws Exception
	 * @throws ServerError
	 * @throws PermissionDeniedException
	 * @throws CannotCreateSessionException
	 */
	@Override
	public void connect(final OmegaLoginCredentials loginCred,
			final OmegaServerInformation serverInfo) throws Exception {
		// TODO check with cases should throw exception and what shouldn't
		this.setConnected(false);
		if (this.secureClient == null) {
			this.secureClient = new client(serverInfo.getHostName(),
					serverInfo.getPort());
		}

		try {
			this.entryEncrypted = this.secureClient.createSession(
					loginCred.getUserName(), loginCred.getPassword());
		} catch (final Exception ex) {
			this.disconnect();
			throw ex;
		}

		this.setConnected(true);
		this.kca = new OmeroKeepClientAlive(this);
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.executor
		.scheduleWithFixedDelay(this.kca, 60, 60, TimeUnit.SECONDS);
	}

	public void disconnect() throws Exception {
		if (this.executor != null) {
			this.executor.shutdown();
		}
		this.kca = null;
		this.executor = null;
		this.setConnected(false);
		this.thumbnailService = null;
		this.adminService = null;
		this.containerService = null;
		this.services.clear();
		this.reServices.clear();

		this.entryEncrypted = null;

		this.secureClient.closeSession();
		this.secureClient = null;
	}

	public List<DatasetData> getDatasets(final ProjectData project)
			throws Exception {
		final List<DatasetData> datasets = new ArrayList<DatasetData>();
		final ParametersI po = new ParametersI();
		// po.add(Project.class.getName(), omero.rtypes.rlong(project.getId()));
		po.exp(omero.rtypes.rlong(project.getOwner().getId()));
		po.leaves();

		final List<Long> ids = new ArrayList<Long>();
		final Set<DatasetData> set = project.getDatasets();
		for (final DatasetData obj : set) {
			ids.add(obj.getId());
		}

		final IContainerPrx service = this.getContainerService();
		final List<IObject> objects = service.loadContainerHierarchy(
				Dataset.class.getName(), ids, po);
		if (objects == null)
			return datasets;

		final Iterator<IObject> i = objects.iterator();
		while (i.hasNext()) {
			datasets.add(new DatasetData((Dataset) i.next()));
		}

		return datasets;
	}

	public List<ChannelData> getChannels(final PixelsData pixels)
			throws Exception {
		new ArrayList<ChannelData>();
		final ParametersI po = new ParametersI();
		// po.add(Project.class.getName(), omero.rtypes.rlong(project.getId()));
		po.exp(omero.rtypes.rlong(pixels.getId()));
		po.leaves();

		final Pixels pix = this.getPixelsService().retrievePixDescription(
				pixels.getId());

		final List<ChannelData> channels = new ArrayList<ChannelData>();
		final List<Channel> l = pix.copyChannels();
		for (int i = 0; i < l.size(); i++) {
			channels.add(new ChannelData(i, l.get(i)));
		}

		return channels;
	}

	public List<ProjectData> getProjects(final ExperimenterData user)
			throws Exception {
		final List<ProjectData> projects = new ArrayList<ProjectData>();
		final ParametersI po = new ParametersI();
		po.exp(omero.rtypes.rlong(user.getId()));
		po.noLeaves();
		final IContainerPrx service = this.getContainerService();
		final List<IObject> objects = service.loadContainerHierarchy(
				Project.class.getName(), null, po);
		if (objects == null)
			return projects;
		final Iterator<IObject> i = objects.iterator();

		while (i.hasNext()) {
			projects.add(new ProjectData((Project) i.next()));
		}
		return projects;
	}

	public ExperimenterData getExperimenter() throws Exception {
		final IAdminPrx service = this.getAdminService();
		final Long expId = service.getEventContext().userId;
		final Experimenter exp = service.getExperimenter(expId);
		return new ExperimenterData(exp);
	}

	public List<GroupData> getGroups() throws Exception {
		final List<GroupData> dataGroups = new ArrayList<GroupData>();
		List<ExperimenterGroup> groups = new ArrayList<ExperimenterGroup>();
		final IAdminPrx service = this.getAdminService();
		final Long expId = service.getEventContext().userId;
		groups = service.containedGroups(expId);
		for (final ExperimenterGroup group : groups) {
			final GroupData dataGroup = new GroupData(group);
			dataGroups.add(dataGroup);
		}
		return dataGroups;
	}

	public List<ExperimenterData> getExperimenters(final GroupData group)
			throws Exception {
		final List<ExperimenterData> dataExps = new ArrayList<ExperimenterData>();
		final IAdminPrx service = this.getAdminService();
		final Long groupId = group.getId();
		final List<Experimenter> exps = service.containedExperimenters(groupId);
		for (final Experimenter exp : exps) {
			final ExperimenterData expData = new ExperimenterData(exp);
			dataExps.add(expData);
		}
		return dataExps;
	}

	public RenderingEnginePrx getRenderingService(final Long pixelsID)
			throws Exception {
		RenderingEnginePrx service = (RenderingEnginePrx) this.reServices
				.get(pixelsID);
		if (service != null)
			return service;
		service = this.createRenderingService(pixelsID);
		return service;
	}

	/**
	 * Loads the rendering control corresponding to the specified set of pixels.
	 *
	 * @param pixelsID
	 *            The identifier of the pixels set.
	 * @return See above.
	 * @throws ServerError
	 */
	public RenderingEnginePrx createRenderingService(final long pixelsID)
			throws Exception {
		final RenderingEnginePrx service = this.createRenderingService();
		this.reServices.put(pixelsID, service);
		service.lookupPixels(pixelsID);
		if (!(service.lookupRenderingDef(pixelsID))) {
			// TODO TO BE CHECKED
			service.resetDefaultSettings(false);
			// service.resetDefaults();
			service.lookupRenderingDef(pixelsID);
		}
		service.load();
		return service;
	}

	/**
	 * Retrieves the specified images.
	 *
	 * @param pixelsID
	 *            The identifier of the images.
	 * @param max
	 *            The maximum length of a thumbnail.
	 * @return See above.
	 * @throws Exception
	 */
	// TODO check if used
	public List<BufferedImage> getThumbnailSet(final List pixelsID,
			final int max) throws Exception {
		final List<BufferedImage> images = new ArrayList<BufferedImage>();
		
		final ThumbnailStorePrx service = this.getThumbnailService();
		final Map<Long, byte[]> results = service.getThumbnailByLongestSideSet(
				omero.rtypes.rint(max), pixelsID);
		
		if (results == null)
			return images;
		Entry entry;
		final Iterator i = results.entrySet().iterator();
		BufferedImage image;
		while (i.hasNext()) {
			entry = (Entry) i.next();
			image = this.createImage((byte[]) entry.getValue());
			if (image != null) {
				images.add(image);
			}
		}
		
		return images;
	}

	@Override
	public synchronized byte[] getImageData(final Long pixelsID, final int z,
			final int t, final int c) throws Exception {
		RawPixelsStorePrx service = null;
		service = this.entryEncrypted.createRawPixelsStore();
		service.setPixelsId(pixelsID, false);

		return service.getPlane(z, c, t);
	}

	// TODO check if used
	@Override
	public int getByteWidth(final Long pixelsID) throws Exception {
		RawPixelsStorePrx service = null;
		service = this.entryEncrypted.createRawPixelsStore();
		service.setPixelsId(pixelsID, false);
		return service.getByteWidth();
	}

	@Override
	public double getDeltaT(final Long pixelsID, final int z, final int t,
			final int channel) throws Exception {
		// GLogManager.log("maxT is: " + maxT);

		double sizeT = 0.0;
		final List<IObject> planeInfoObjects = this.loadPlaneInfo(pixelsID, z,
				t, channel);
		
		if ((planeInfoObjects == null) || (planeInfoObjects.size() == 0))
			return sizeT;
		
		final PlaneInfoI pi = (PlaneInfoI) planeInfoObjects.get(0);
		
		final Time tTemp = pi.getDeltaT();
		
		if (tTemp != null) {
			sizeT = tTemp.getValue();
		}
		
		return sizeT;
	}

	// TODO to be removed because unused
	@Override
	public Double computeSizeT(final Long pixelsID, final int sizeT,
			final int currentMaxT) throws Exception {
		Double physicalSizeT = null;
		final int maxT = currentMaxT - 1;
		try {
			final List<IObject> planeInfoObjects = this.loadPlaneInfo(pixelsID,
					0, maxT, 0);
			if ((planeInfoObjects == null) || (planeInfoObjects.size() == 0))
				return physicalSizeT;

			final PlaneInfoI pi = (PlaneInfoI) planeInfoObjects.get(0);

			final Time tTemp = pi.getDeltaT();

			if (tTemp != null) {
				physicalSizeT = tTemp.getValue() / sizeT;
			}
		} catch (final Exception ex) {
			// TODO handle differently
			OmegaLogFileManager.handleUncaughtException(ex, true);
		}
		return physicalSizeT;
	}

	// TODO check if used
	public List<IObject> loadPlaneInfo(final long pixelsID, final int z,
			final int t, final int channel) throws Exception {
		// isSessionAlive();
		final IQueryPrx service = this.entryEncrypted.getQueryService();
		final StringBuilder sb = new StringBuilder();
		final ParametersI param = new ParametersI();
		sb.append("select info from PlaneInfo as info ");
		sb.append("where pixels.id =:id");
		param.addLong("id", pixelsID);

		if (z >= 0) {
			sb.append(" and info.theZ =:z");
			param.map.put("z", omero.rtypes.rint(z));
		}
		if (t >= 0) {
			sb.append(" and info.theT =:t");
			param.map.put("t", omero.rtypes.rint(t));
		}
		if (channel >= 0) {
			sb.append(" and info.theC =:c");
			param.map.put("c", omero.rtypes.rint(channel));
		}
		try {
			final List<IObject> info = service.findAllByQuery(sb.toString(),
					param);
			return info;
		} catch (final Exception ex) {
			throw new Exception("Cannot load the plane info for pixels: "
					+ pixelsID, ex);
		}
	}

	@Override
	public int[] renderAsPackedInt(final Long pixelsID, final int t, final int z)
			throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		final PlaneDef planeDef = new PlaneDef();
		// time choice (sliding)
		planeDef.t = t;
		// Z-plan choice
		planeDef.z = z;
		// display the XY plane
		planeDef.slice = omero.romio.XY.value;

		return engine.renderAsPackedInt(planeDef);
	}

	@Override
	public int[] renderAsPackedInt(final Long pixelsID) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		return this.renderAsPackedInt(pixelsID, engine.getDefaultT(),
				engine.getDefaultZ());
	}

	@Override
	public byte[] renderCompressed(final Long pixelsID, final int t, final int z)
			throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		final PlaneDef planeDef = new PlaneDef();
		// time choice (sliding)
		planeDef.t = t;
		// Z-plan choice
		planeDef.z = z;
		// display the XY plane
		planeDef.slice = omero.romio.XY.value;

		return engine.renderCompressed(planeDef);
	}

	@Override
	public byte[] renderCompressed(final Long pixelsID) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		return this.renderCompressed(pixelsID, engine.getDefaultT(),
				engine.getDefaultZ());
	}

	@Override
	public void setActiveChannel(final Long pixelsID, final int channel,
			final boolean active) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		engine.setActive(channel, active);
	}

	@Override
	public void setDefaultZ(final Long pixelsID, final int z) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		engine.setDefaultZ(z);
	}

	@Override
	public int getDefaultZ(final Long pixelsID) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		return engine.getDefaultZ();
	}

	@Override
	public void setDefaultT(final Long pixelsID, final int t) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		engine.setDefaultT(t);
	}

	@Override
	public int getDefaultT(final Long pixelsID) throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		return engine.getDefaultT();
	}

	@Override
	public void setCompressionLevel(final Long pixelsID, final float compression)
			throws Exception {
		final RenderingEnginePrx engine = this.getRenderingService(pixelsID);
		engine.setCompressionLevel(compression);
	}

	@Override
	public boolean isConnected() {
		return this.isConnected;
	}

	@Override
	public void setConnected(final boolean isConnected) {
		this.isConnected = isConnected;
	}
}