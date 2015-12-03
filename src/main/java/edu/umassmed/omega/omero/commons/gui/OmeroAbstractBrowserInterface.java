package main.java.edu.umassmed.omega.omero.commons.gui;

import main.java.edu.umassmed.omega.commons.gui.checkboxTree.CheckBoxStatus;
import main.java.edu.umassmed.omega.commons.gui.interfaces.OmegaMessageDisplayerPanelInterface;
import main.java.edu.umassmed.omega.omero.commons.data.OmeroDatasetWrapper;

public interface OmeroAbstractBrowserInterface extends
        OmegaMessageDisplayerPanelInterface {

	public abstract void updateDatasetSelection(final int size);

	public abstract void browseDataset(final OmeroDatasetWrapper datasetWrapper);

	public abstract void updateImagesSelection(CheckBoxStatus status);
}
