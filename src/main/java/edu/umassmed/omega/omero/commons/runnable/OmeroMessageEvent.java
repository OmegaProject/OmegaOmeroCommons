package edu.umassmed.omega.omero.commons.runnable;

import edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;

public class OmeroMessageEvent extends OmegaMessageEvent {

	private final Exception error;
	private boolean isTerminated;

	public OmeroMessageEvent(final String msg, final Exception error,
			final boolean wasTerminated) {
		super(msg);

		this.error = error;
	}

	public Exception getError() {
		return this.error;
	}
	
	public boolean wasTerminated() {
		return this.isTerminated;
	}
}
