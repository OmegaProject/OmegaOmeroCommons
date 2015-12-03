package main.java.edu.umassmed.omega.omero.commons.data;

import pojos.GroupData;

public class OmeroGroupWrapper extends OmeroDataWrapper{
	
	private GroupData groupData;
	
	public OmeroGroupWrapper(GroupData data) {
		this.groupData = data;
	}

	@Override
	public Long getID() {
		return groupData.getId();
	}

	@Override
	public String getStringRepresentation() {
		return "[" + this.getID() + "] " + this.groupData.getName();
	}

	public GroupData getGroupData() {
		return groupData;
	}
}
