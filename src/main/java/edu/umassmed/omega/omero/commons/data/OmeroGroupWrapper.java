package edu.umassmed.omega.omero.commons.data;

import omero.gateway.model.GroupData;

public class OmeroGroupWrapper extends OmeroDataWrapper {

	private final GroupData groupData;

	public OmeroGroupWrapper(final GroupData data) {
		this.groupData = data;
	}
	
	@Override
	public Long getID() {
		return this.groupData.getId();
	}
	
	@Override
	public String getStringRepresentation() {
		return "[" + this.getID() + "] " + this.groupData.getName();
	}
	
	public GroupData getGroupData() {
		return this.groupData;
	}
}
