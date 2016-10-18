package edu.umassmed.omega.omero.commons.gui;

import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.umassmed.omega.omero.commons.data.OmeroThumbnailImageInfo;

public class OmeroBrowserList extends JList<OmeroThumbnailImageInfo> implements
        ListSelectionListener {

	private static final long serialVersionUID = 7897513040384713703L;

	private List<OmeroThumbnailImageInfo> data;
	private final DefaultListModel<OmeroThumbnailImageInfo> model;

	public OmeroBrowserList() {
		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		this.setVisibleRowCount(-1);
		this.addListSelectionListener(this);
		this.model = new DefaultListModel<OmeroThumbnailImageInfo>();
		this.setModel(this.model);
	}

	public void clear() {
		this.model.clear();
	}

	public void setElements(final List<OmeroThumbnailImageInfo> data) {
		this.data = data;
		this.model.clear();
		int maxSize = 0;
		for (final OmeroThumbnailImageInfo imageInfo : this.data) {
			this.model.addElement(imageInfo);
			final int width = imageInfo.getBufferedImage().getWidth();
			if (maxSize < width) {
				maxSize = width;
			}
		}
		this.setFixedCellWidth(maxSize + 10);
		this.revalidate();
	}

	@Override
	public void valueChanged(final ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			if (this.getSelectedIndex() == -1) {

			} else {

			}
		}
	}

	@Override
	public ListCellRenderer<? super OmeroThumbnailImageInfo> getCellRenderer() {
		return new DefaultListCellRenderer() {

			private static final long serialVersionUID = -5253984072228021404L;
			final Font font = new Font("Tahoma", 0, 10);

			@Override
			public Component getListCellRendererComponent(final JList list,
					final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final JLabel label = (JLabel) super
						.getListCellRendererComponent(list, value, index,
								isSelected, cellHasFocus);
				final OmeroThumbnailImageInfo imageInfo = (OmeroThumbnailImageInfo) value;
				label.setIcon(new ImageIcon(imageInfo.getBufferedImage()));
				label.setText(imageInfo.getImageName());
				label.setHorizontalTextPosition(SwingConstants.CENTER);
				label.setVerticalTextPosition(SwingConstants.BOTTOM);
				label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				// label.setFont(this.font);
				label.setToolTipText(imageInfo.getImageName());
				return label;
			}
		};
	}
}
