package edu.umassmed.omega.omero.commons.runnable;

import java.util.List;

import edu.umassmed.omega.omero.commons.data.OmeroThumbnailImageInfo;

public class OmeroThumbnailMessageEvent extends OmeroMessageEvent {
	private final List<OmeroThumbnailImageInfo> thumbnails;
	
	public OmeroThumbnailMessageEvent(final String msg, final Exception error,
			final boolean wasTerminated,
			final List<OmeroThumbnailImageInfo> thumbnails) {
		super(msg, error, wasTerminated);
		this.thumbnails = thumbnails;
	}
	
	public List<OmeroThumbnailImageInfo> getThumbnails() {
		return this.thumbnails;
	}
}
