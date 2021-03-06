package edu.umassmed.omega.omero.commons.runnable;

import java.util.List;
import java.util.Map;

import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import edu.umassmed.omega.commons.eventSystem.events.OmegaMessageEvent;

public class OmeroDataMessageEvent extends OmegaMessageEvent {
	
	private final ExperimenterData expData;
	private final Map<ProjectData, List<DatasetData>> data;
	
	public OmeroDataMessageEvent(final String msg,
			final ExperimenterData expData,
			final Map<ProjectData, List<DatasetData>> data) {
		super(msg);
		this.expData = expData;
		this.data = data;
	}
	
	public ExperimenterData getExperimenterData() {
		return this.expData;
	}
	
	public Map<ProjectData, List<DatasetData>> getData() {
		return this.data;
	}
	
}
