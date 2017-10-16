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
import java.util.Calendar;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
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

public class OmeroBrowserTable extends JTable {

	private static final long serialVersionUID = 7897513040384713703L;

	public static String COLUMN_ID = "ID";
	public static String COLUMN_THUMBNAIL = "Thumbnail";
	public static String COLUMN_NAME = "Name";
	public static String COLUMN_ACQUIRED = "Acquired";
	public static String COLUMN_DIM_XY = "Dimensions (XY)";
	public static String COLUMN_DIM_ZTC = "Dimensions (ZTC)";
	public static String COLUMN_PIXSIZE = "Pixel size (XYZ)";
	
	private static final Object[] IDENT = { OmeroBrowserTable.COLUMN_ID,
			OmeroBrowserTable.COLUMN_THUMBNAIL, OmeroBrowserTable.COLUMN_NAME,
			OmeroBrowserTable.COLUMN_ACQUIRED, OmeroBrowserTable.COLUMN_DIM_XY,
			OmeroBrowserTable.COLUMN_DIM_ZTC, OmeroBrowserTable.COLUMN_PIXSIZE };

	private final DefaultTableModel model;
	private final List<Long> loadedIDs;

	public OmeroBrowserTable(final boolean isMultiSelection) {
		
		this.loadedIDs = new ArrayList<Long>();
		if (isMultiSelection) {
			this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		} else {
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		this.model = new OmeroBrowserTableModel(OmeroBrowserTable.IDENT,
				new ArrayList<OmeroThumbnailImageInfo>());
		this.setModel(this.model);

		this.setTableHeader(new JTableHeader(this.getColumnModel()) {

			private static final long serialVersionUID = 7003218673192663859L;

			@Override
			public String getToolTipText(final MouseEvent ev) {
				final java.awt.Point p = ev.getPoint();
				final int index = this.columnModel.getColumnIndexAtX(p.x);
				final int realIndex = this.columnModel.getColumn(index)
						.getModelIndex();
				return (String) OmeroBrowserTable.IDENT[realIndex];
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
		this.setShowGrid(false);

	}

	public void clear() {
		
	}

	public void setElements(final List<OmeroThumbnailImageInfo> data) {
		final OmeroBrowserTableModel myModel = (OmeroBrowserTableModel) this
				.getModel();
		myModel.setElements(data);
		myModel.fireTableStructureChanged();
		final int maxSize = myModel.getThumbMaxSize();
		if (maxSize == 1)
			return;
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

	@Override
	public TableCellRenderer getCellRenderer(final int row, final int column) {
		return new DefaultTableCellRenderer() {

			private static final long serialVersionUID = -5253984072228021404L;
			final Font font = new Font("Tahoma", 0, 10);

			@Override
			public Component getTableCellRendererComponent(final JTable table,
					final Object value, boolean isSelected,
					final boolean hasFocus, final int row, final int column) {
				final OmeroThumbnailImageInfo imageInfo = (OmeroThumbnailImageInfo) OmeroBrowserTable.this
						.getValueAt(row, -1);
				// final Long imageID = OmeroBrowserTable.this.rowToIDMapping
				// .get(row);
				// final OmeroThumbnailImageInfo imageInfo =
				// OmeroBrowserTable.this.dataMap
				// .get(imageID);
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
				this.setBorder(DefaultTableCellRenderer.noFocusBorder);
				switch (column) {
					case 1:
						label.setIcon(new ImageIcon((BufferedImage) value));
						label.setText("");
						label.setToolTipText(imageInfo.getImageName());
						break;
					default:
						final Object obj = OmeroBrowserTable.this.getValueAt(
								row, column);
						if (obj != null) {
							label.setToolTipText(obj.toString());
						}
				}
				return label;
			}
		};
	}

	public List<OmeroThumbnailImageInfo> getSelectedThumbnailList() {
		final List<OmeroThumbnailImageInfo> thumbnailList = new ArrayList<OmeroThumbnailImageInfo>();
		for (final int row : this.getSelectedRows()) {
			final OmeroThumbnailImageInfo thumb = (OmeroThumbnailImageInfo) OmeroBrowserTable.this
					.getValueAt(row, -1);
			if (!this.loadedIDs.contains(thumb.getImageID())) {
				thumbnailList.add(thumb);
			}
		}
		return thumbnailList;
	}

	class OmeroBrowserTableModel extends DefaultTableModel {

		private int maxSize = 0;

		private static final long serialVersionUID = 3601669657329346340L;

		private List<OmeroThumbnailImageInfo> data;
		private final DateFormat format;

		public OmeroBrowserTableModel(final Object[] identifiers,
				final List<OmeroThumbnailImageInfo> data) {
			this.setColumnIdentifiers(identifiers);
			this.data = data;
			this.format = new SimpleDateFormat(OmegaConstants.OMEGA_DATE_FORMAT);
		}

		@Override
		public boolean isCellEditable(final int row, final int column) {
			return false;
		}

		public void setElements(final List<OmeroThumbnailImageInfo> data) {
			this.maxSize = 0;
			for (final OmeroThumbnailImageInfo thumb : data) {
				final int width = thumb.getBufferedImage().getWidth();
				if (this.maxSize < width) {
					this.maxSize = width;
				}
			}
			if (this.maxSize == 0) {
				this.maxSize = 1;
			}
			this.data = data;
		}

		public int getThumbMaxSize() {
			return this.maxSize;
		}

		@Override
		public int getRowCount() {
			if (this.data == null)
				return 0;
			return this.data.size();
		}
		
		@Override
		public Object getValueAt(final int row, final int column) {
			// ID || thumb || name || date || sizeXY || sizeZTC || pixelsSizeXYZ
			final OmeroThumbnailImageInfo imageInfo = this.data.get(row);
			if (column == -1)
				return imageInfo;
			switch (column) {
				case 0:
					return imageInfo.getImageID();
				case 1:
					return imageInfo.getBufferedImage();
				case 2:
					return imageInfo.getImageName();
				case 3:
					String date = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					try {
						final Timestamp ts = imageInfo.getImage()
								.getAcquisitionDate();
						final Calendar start = Calendar.getInstance();
						if (ts != null) {
							start.setTimeInMillis(ts.getTime());
							date = this.format.format(start.getTime());
						} else {
							final StringBuffer buf = new StringBuffer();
							buf.append("Aquisition date not found for:\n"
									+ imageInfo.getImageName()
									+ "\nMarked as unknown");
							OmegaLogFileManager.appendToCoreLog(buf.toString());
						}
					} catch (final IllegalStateException ex) {
						OmegaLogFileManager.handleUncaughtException(ex, false);
					}
					return date;
				case 4:
					final String sizeXY = (imageInfo.getImage().getSizeX() != -1
							? String.valueOf(imageInfo.getImage().getSizeX())
							: OmeroPluginGUIConstants.BROWSER_UNKNOWN)
							+ " x "
							+ (imageInfo.getImage().getSizeY() != -1 ? String
									.valueOf(imageInfo.getImage().getSizeY())
									: OmeroPluginGUIConstants.BROWSER_UNKNOWN);
					return sizeXY;
				case 5:
					final String sizeZTC = (imageInfo.getImage().getSizeZ() != -1
							? String.valueOf(imageInfo.getImage().getSizeZ())
							: OmeroPluginGUIConstants.BROWSER_UNKNOWN)
							+ " x "
							+ (imageInfo.getImage().getSizeT() != -1 ? String
									.valueOf(imageInfo.getImage().getSizeT())
									: OmeroPluginGUIConstants.BROWSER_UNKNOWN)
							+ " x "
							+ (imageInfo.getImage().getSizeC() != -1 ? String
									.valueOf(imageInfo.getImage().getSizeC())
									: OmeroPluginGUIConstants.BROWSER_UNKNOWN);
					return sizeZTC;
				case 6:
					String pixelsSizeXYZ = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					String sizeXLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					String sizeYLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					String sizeZLabel = OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					try {
						final Double sizeX = imageInfo.getImage()
								.getPixelsSizeX();
						sizeXLabel = sizeX != null ? new BigDecimal(sizeX)
								.setScale(2, RoundingMode.HALF_UP).toString()
								: OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					} catch (final BigResult ex) {
						OmegaLogFileManager.handleUncaughtException(ex, false);
					}
					try {
						final Double sizeY = imageInfo.getImage()
								.getPixelsSizeY();
						sizeYLabel = sizeY != null ? new BigDecimal(sizeY)
								.setScale(2, RoundingMode.HALF_UP).toString()
								: OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					} catch (final BigResult ex) {
						OmegaLogFileManager.handleUncaughtException(ex, false);
					}
					try {
						final Double sizeZ = imageInfo.getImage()
								.getPixelsSizeZ();
						sizeZLabel = sizeZ != null ? new BigDecimal(sizeZ)
								.setScale(2, RoundingMode.HALF_UP).toString()
								: OmeroPluginGUIConstants.BROWSER_UNKNOWN;
					} catch (final BigResult ex) {
						OmegaLogFileManager.handleUncaughtException(ex, false);
					}
					pixelsSizeXYZ = sizeXLabel + " x " + sizeYLabel + " x  "
							+ sizeZLabel;
					return pixelsSizeXYZ;
				default:
					return null;
					
			}
		}
	}

	public void setLoadedElements(final List<OmegaImage> loadedImages) {
		this.loadedIDs.clear();
		for (final OmegaImage image : loadedImages) {
			this.loadedIDs.add(image.getOmeroId());
		}
	}
}
