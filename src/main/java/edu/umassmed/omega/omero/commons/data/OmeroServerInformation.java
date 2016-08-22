package edu.umassmed.omega.omero.commons.data;

import edu.umassmed.omega.commons.data.imageDBConnectionElements.OmegaServerInformation;

public class OmeroServerInformation extends OmegaServerInformation {
	
	/** The default port value. */
	public static final int DEFAULT_PORT = 4064;

	public OmeroServerInformation(String hostName, int port) {
		super(hostName, port);
	}
	
	public OmeroServerInformation(final String hostName) {
		this(hostName, DEFAULT_PORT);
	}

}
