package edu.umassmed.omega.omero.commons.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import ome.model.units.BigResult;
import edu.umassmed.omega.commons.OmegaLogFileManager;
import edu.umassmed.omega.commons.constants.OmegaConstants;
import edu.umassmed.omega.commons.data.coreElements.OmegaImage;
import edu.umassmed.omega.omero.commons.data.OmeroThumbnailImageInfo;

public class OmeroBrowserTable extends JTable implements TableModelListener,
        ListSelectionListener {

	private static final long serialVersionUID = 7897513040384713703L;

	public static String COLUMN_ID = "ID";
	public static String COLUMN_THUMBNAIL = "Thumbnail";
	public static String COLUMN_NAME = "Name";
	public static String COLUMN_ACQUIRED = "Acquired";
	public static String COLUMN_DIM_XY = "Dimensions (XY)";
	public static String COLUMN_DIM_ZTC = "Dimensions (ZTC)";
	public static String COLUMN_PIXSIZE = "Pixel size (XYZ)";

	private List<OmeroThumbnailImageInfo> data;
	private final DefaultTableModel model;
	private final List<Long> loadedIDs;

	public OmeroBrowserTable(final boolean isMultiSelection) {
		final Object[] ident = { OmeroBrowserTable.COLUMN_ID,
				OmeroBrowserTable.COLUMN_THUMBNAIL,
				OmeroBrowserTable.COLUMN_NAME,
				OmeroBrowserTable.COLUMN_ACQUIRED,
				OmeroBrowserTable.COLUMN_DIM_XY,
				OmeroBrowserTable.COLUMN_DIM_ZTC,
				OmeroBrowserTable.COLUMN_PIXSIZE };
		if (isMultiSelection) {
			this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		} else {
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		this.getSelectionModel().addListSelectionListener(this);
		this.model = new OmeroBrowserTableModel(ident);
		this.setModel(this.model);
		this.model.addTableModelListener(this);
		this.setTableHeader(new JTableHeader(this.getColumnModel()) {

			private static final long serialVersionUID = 7003218673192663859L;

			@Override
			public String getToolTipText(final MouseEvent ev) {
				final java.awt.Point p = ev.getPoint();
				final int index = this.columnModel.getColumnIndexAtX(p.x);
				final int realIndex = this.columnModel.getColumn(index)
						.getModelIndex();
				return (String) ident[realIndex];
			}
		});
		this.getTableHeader().setReorderingAllowed(false);
		// FIXME this should be moved in general options because I think is
		// going to change every table in the application!
		final UIDefaults defaults = UIManager.getLookAndFeelDefaults();
		if (defaults.get("Table.alternateRowColor") == null) {
			defaults.put("Table.alternateRowColor", new Color(240, 240, 240));
		}
		this.setAutoCreateRowSorter(true);
		this.loadedIDs = new ArrayList<Long>();
		this.setShowGrid(false);
	}

	public void clear() {
		this.setRowSorter(null);
		this.model.setRowCount(0);
	}

	public void setElements(final List<OmeroThumbnailImageInfo> data) {
		this.data = data;
		int maxSize = 0;
		this.setRowSorter(null);
		this.model.setRowCount(0);
		for (final OmeroThumbnailImageInfo imageInfo : this.data) {
			final DateFormat format = new SimpleDateFormat(
			        OmegaConstants.OMEGA_DATE_FORMAT);
			String date = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			try {
				final Timestamp ts = imageInfo.getImage().getAcquisitionDate();
				if (ts != null) {
					date = format.format(ts);
				}
			} catch (final IllegalStateException ex) {
				OmegaLogFileManager.handleUncaughtException(ex, false);
			}
			final String sizeXY = (imageInfo.getImage().getSizeX() != -1 ? String
			        .valueOf(imageInfo.getImage().getSizeX())
			        : OmeroPluginGUIConstants.BROWSER_UNKNOWN)
			        + " x "
			        + (imageInfo.getImage().getSizeY() != -1 ? String
			                .valueOf(imageInfo.getImage().getSizeY())
			                : OmeroPluginGUIConstants.BROWSER_UNKNOWN);
			final String sizeZTC = (imageInfo.getImage().getSizeZ() != -1 ? String
			        .valueOf(imageInfo.getImage().getSizeZ())
			        : OmeroPluginGUIConstants.BROWSER_UNKNOWN)
			        + " x "
			        + (imageInfo.getImage().getSizeT() != -1 ? String
			                .valueOf(imageInfo.getImage().getSizeT())
			                : OmeroPluginGUIConstants.BROWSER_UNKNOWN)
			        + " x "
			        + (imageInfo.getImage().getSizeC() != -1 ? String
			                .valueOf(imageInfo.getImage().getSizeC())
			                : OmeroPluginGUIConstants.BROWSER_UNKNOWN);
			String pixelsSizeXYZ = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			String sizeXLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			String sizeYLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			String sizeZLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			try {
				final Double sizeX = imageInfo.getImage().getPixelsSizeX();
				sizeXLabel = sizeX != null ? new BigDecimal(sizeX).setScale(2,
				        RoundingMode.HALF_UP).toString()
				        : OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			} catch (final BigResult ex) {
				OmegaLogFileManager.handleUncaughtException(ex, false);
			}
			try {
				final Double sizeY = imageInfo.getImage().getPixelsSizeY();
				sizeYLabel = sizeY != null ? new BigDecimal(sizeY).setScale(2,
				        RoundingMode.HALF_UP).toString()
				        : OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			} catch (final BigResult ex) {
				OmegaLogFileManager.handleUncaughtException(ex, false);
			}
			try {
				final Double sizeZ = imageInfo.getImage().getPixelsSizeZ();
				sizeZLabel = sizeZ != null ? new BigDecimal(sizeZ).setScale(2,
				        RoundingMode.HALF_UP).toString()
				        : OmeroPluginGUIConstants.BROWSER_UNKNOWN;
			} catch (final BigResult ex) {
				OmegaLogFileManager.handleUncaughtException(ex, false);
			}
			pixelsSizeXYZ = sizeXLabel + " x " + sizeYLabel + " x  "
			        + sizeZLabel;
			final Object[] rowData = { imageInfo.getImageID(),
			        imageInfo.getBufferedImage(), imageInfo.getImageName(),
			        date, sizeXY, sizeZTC, pixelsSizeXYZ };
			this.model.addRow(rowData);
			final int width = imageInfo.getBufferedImage().getWidth();
			if (maxSize < width) {
				maxSize = width;
			}
		}
		if (maxSize == 0) {
			maxSize = 1;
		}
		if (maxSize != 1) {
			TableColumn tc = this.getColumn(OmeroBrowserTable.COLUMN_THUMBNAIL);
			tc.setMinWidth(maxSize);
			// tc.setPreferredWidth(maxSize);
			tc.setMaxWidth(maxSize);
			tc = this.getColumn(OmeroBrowserTable.COLUMN_NAME);
			tc.setPreferredWidth(350);
			tc = this.getColumn(OmeroBrowserTable.COLUMN_ID);
			tc.setMinWidth(50);
			tc.setMaxWidth(50);
			this.setRowHeight(maxSize);
		}
		this.setAutoCreateRowSorter(true);
		this.revalidate();
	}

	@Override
	public void valueChanged(final ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			if (this.getSelectedRow() == -1) {

			} else {
				this.handleSelection();
				super.valueChanged(e);
			}
		}
	}

	@Override
	public void tableChanged(final TableModelEvent ev) {
		// this.handleSelection();
		super.tableChanged(ev);
	}

	private void handleSelection() {
		for (final int i : this.getSelectedRows()) {
			System.out.println(i);
		}
	}

	@Override
	public TableCellRenderer getCellRenderer(final int row, final int column) {
		return new DefaultTableCellRenderer() {

			private static final long serialVersionUID = -5253984072228021404L;
			final Font font = new Font("Tahoma", 0, 10);

			@Override
			public Component getTableCellRendererComponent(final JTable table,
			        final Object value, boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				final OmeroThumbnailImageInfo imageInfo = OmeroBrowserTable.this.data
						.get(row);
				final boolean isLoaded = OmeroBrowserTable.this.loadedIDs
				        .contains(imageInfo.getImageID());
				if (isLoaded) {
					isSelected = false;
				}
				final JLabel label = (JLabel) super
				        .getTableCellRendererComponent(table, value,
				                isSelected, hasFocus, row, column);
				if (isLoaded) {
					label.setBackground(Color.GRAY);
					// label.setEnabled(false);
				}
				switch (column) {
				case 1:
					label.setIcon(new ImageIcon((BufferedImage) value));
					label.setText("");
					label.setToolTipText(imageInfo.getImageName());
					break;
				}
				return label;
			}
		};
	}

	public List<OmeroThumbnailImageInfo> getSelectedThumbnailList() {
		final List<OmeroThumbnailImageInfo> thumbnailList = new ArrayList<OmeroThumbnailImageInfo>();
		for (final int row : this.getSelectedRows()) {
			final OmeroThumbnailImageInfo imageInfo = this.data.get(row);
			if (!this.loadedIDs.contains(imageInfo.getImageID())) {
				thumbnailList.add(imageInfo);
			}
		}
		return thumbnailList;
	}

	class OmeroBrowserTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 3601669657329346340L;

		public OmeroBrowserTableModel(final Object[] identifiers) {
			this.setColumnIdentifiers(identifiers);
		}

		@Override
		public boolean isCellEditable(final int row, final int column) {
			return false;
		}
	}

	public void setLoadedElements(final List<OmegaImage> loadedImages) {
		this.loadedIDs.clear();
		for (final OmegaImage image : loadedImages) {
			this.loadedIDs.add(image.getOmeroId());
		}
	}
}
