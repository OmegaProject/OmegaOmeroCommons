package edu.umassmed.omega.omero.commons.runnable;

import java.util.List;

import edu.umassmed.omega.omero.commons.data.OmeroImageWrapper;

public class OmeroWrapperMessageEvent extends OmeroMessageEvent {

	private final List<OmeroImageWrapper> wrappers;

	public OmeroWrapperMessageEvent(final String msg, final Exception error,
			final boolean wasTerminated, final List<OmeroImageWrapper> wrappers) {
		super(msg, error, wasTerminated);
		this.wrappers = wrappers;
	}

	public List<OmeroImageWrapper> getWrappers() {
		return this.wrappers;
	}
}
