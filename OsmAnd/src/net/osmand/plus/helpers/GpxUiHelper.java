package net.osmand.plus.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.Elevation;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Speed;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;
import net.osmand.plus.myplaces.SaveCurrentTrackTask;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.track.ChartLabel;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxAppearanceAdapter.AppearanceListItem;
import net.osmand.plus.track.GpxMarkerView;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.track.GpxAppearanceAdapter.SHOW_START_FINISH_ATTR;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public class GpxUiHelper {

	private static final Log LOG = PlatformUtil.getLog(GpxUiHelper.class);

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1005;
	private static final int MAX_CHART_DATA_ITEMS = 10000;

	public static final long SECOND_IN_MILLIS = 1000L;
	public static final long HOUR_IN_MILLIS = 60 * 60 * SECOND_IN_MILLIS;

	public static String getDescription(OsmandApplication app, GPXFile result, File f, boolean html) {
		GPXTrackAnalysis analysis = result.getAnalysis(f == null ? 0 : f.lastModified());
		return getDescription(app, analysis, html);
	}

	public static String getDescription(OsmandApplication app, TrkSegment t, boolean html) {
		return getDescription(app, GPXTrackAnalysis.segment(0, t), html);
	}


	public static String getColorValue(String clr, String value, boolean html) {
		if (!html) {
			return value;
		}
		return "<font color=\"" + clr + "\">" + value + "</font>";
	}

	public static String getColorValue(String clr, String value) {
		return getColorValue(clr, value, true);
	}

	public static String getDescription(OsmandApplication app, GPXTrackAnalysis analysis, boolean html) {
		StringBuilder description = new StringBuilder();
		String nl = html ? "<br/>" : "\n";
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		// OUTPUT:
		// 1. Total distance, Start time, End time
		description.append(app.getString(R.string.gpx_info_distance, getColorValue(distanceClr,
				OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app), html),
				getColorValue(distanceClr, analysis.points + "", html)));
		if (analysis.totalTracks > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.totalTracks + "", html)));
		}
		if (analysis.wptPoints > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.wptPoints + "", html)));
		}
		if (analysis.isTimeSpecified()) {
			description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.startTime));
			description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.endTime));
		}

		// 2. Time span
		if (analysis.timeSpan > 0 && analysis.timeSpan / 1000 != analysis.timeMoving / 1000) {
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if (analysis.isTimeMoving()) {
				//Next few lines for Issue 3222 heuristic testing only
				//final String formatDuration0 = Algorithms.formatDuration((int) (analysis.timeMoving0 / 1000), app.accessibilityEnabled());
				//description.append(nl).append(app.getString(R.string.gpx_timemoving,
				//		getColorValue(timeSpanClr, formatDuration0, html)));
				//description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving0, app), html) + ")");
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
			description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app), html) + ")");
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if (analysis.isElevationSpecified()) {
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_avg_altitude,
					getColorValue(speedClr, OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app), html)));
			description.append(nl);
			String min = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app), html);
			String asc = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app), html);
			String desc = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app), html);
			description.append(app.getString(R.string.gpx_info_diff_altitude, min + " - " + max));
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_asc_altitude, "\u2193 " + desc + "   \u2191 " + asc + ""));
		}


		if (analysis.isSpeedSpecified()) {
			String avg = getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app), html);
			description.append(nl).append(app.getString(R.string.gpx_info_average_speed, avg));
			description.append(nl).append(app.getString(R.string.gpx_info_maximum_speed, max));
		}
		return description.toString();
	}

	public static AlertDialog selectGPXFiles(List<String> selectedGpxList, final Activity activity,
	                                         final CallbackWithObject<GPXFile[]> callbackWithObject,
	                                         int dialogThemeRes, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final List<GPXInfo> orderedAllGpxList = listGpxInfo(app, selectedGpxList, false);
		if (orderedAllGpxList.size() < 2) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		final ContextMenuAdapter adapter = createGpxContextMenuAdapter(app, orderedAllGpxList, true);
		return createDialog(activity, true, true, true, callbackWithObject, orderedAllGpxList, adapter, dialogThemeRes, nightMode);
	}

	private static List<GPXInfo> listGpxInfo(OsmandApplication app, List<String> selectedGpxFiles, boolean absolutePath) {
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> allGpxList = getSortedGPXFilesInfo(gpxDir, selectedGpxFiles, absolutePath);
		GPXInfo currentTrack = new GPXInfo(app.getString(R.string.show_current_gpx_title), 0, 0);
		currentTrack.setSelected(selectedGpxFiles.contains(""));
		allGpxList.add(0, currentTrack);
		return allGpxList;
	}

	public static AlertDialog selectGPXFile(final Activity activity, final boolean showCurrentGpx,
	                                        final boolean multipleChoice,
	                                        final CallbackWithObject<GPXFile[]> callbackWithObject,
	                                        boolean nightMode) {
		int dialogThemeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<GPXInfo> list = getSortedGPXFilesInfo(dir, null, false);
		if (list.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if (!list.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(app, list, false);
			return createDialog(activity, showCurrentGpx, multipleChoice, false, callbackWithObject, list, adapter, dialogThemeRes, nightMode);
		}
		return null;
	}

	public static void selectSingleGPXFile(final FragmentActivity activity, boolean showCurrentGpx,
	                                       final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		int gpxDirLength = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath().length();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		final List<GPXInfo> list = new ArrayList<>(selectedGpxFiles.size() + 1);
		if (!OsmandPlugin.isActive(OsmandMonitoringPlugin.class)) {
			showCurrentGpx = false;
		}
		if (!selectedGpxFiles.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(new GPXInfo(activity.getString(R.string.shared_string_currently_recording_track), 0, 0));
			}

			for (SelectedGpxFile selectedGpx : selectedGpxFiles) {
				GPXFile gpxFile = selectedGpx.getGpxFile();
				if (!gpxFile.showCurrentTrack && gpxFile.path.length() > gpxDirLength + 1) {
					list.add(new GPXInfo(gpxFile.path.substring(gpxDirLength + 1), gpxFile.modifiedTime, 0));
				}
			}
			SelectGpxTrackBottomSheet.showInstance(activity.getSupportFragmentManager(), showCurrentGpx, callbackWithObject, list);
		}
	}

	private static ContextMenuAdapter createGpxContextMenuAdapter(OsmandApplication app,
	                                                              List<GPXInfo> allGpxList,
	                                                              boolean needSelectItems) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		fillGpxContextMenuAdapter(adapter, allGpxList, needSelectItems);
		return adapter;
	}

	private static void fillGpxContextMenuAdapter(ContextMenuAdapter adapter, List<GPXInfo> allGpxFiles,
	                                              boolean needSelectItems) {
		for (GPXInfo gpxInfo : allGpxFiles) {
			adapter.addItem(new ContextMenuItem(null)
					.setTitle(getGpxTitle(gpxInfo.getFileName()))
					.setSelected(needSelectItems && gpxInfo.selected)
					.setIcon(R.drawable.ic_action_polygom_dark));
		}
	}

	@NonNull
	public static String getGpxTitle(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return "";
		}
		String gpxTitle = name;
		if (gpxTitle.toLowerCase().endsWith(GPX_FILE_EXT)) {
			gpxTitle = gpxTitle.substring(0, gpxTitle.length() - GPX_FILE_EXT.length());
		}
		return gpxTitle.replace('_', ' ');
	}

	@NonNull
	public static String getGpxDirTitle(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return "";
		}
		String groupName = name.replaceAll("_", " ").replace(IndexConstants.GPX_FILE_EXT, "");
		return capitalizeFirstLetter(groupName);
	}

	private static class DialogGpxDataItemCallback implements GpxDataItemCallback {

		private static final int UPDATE_GPX_ITEM_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 6;
		private static final long MIN_UPDATE_INTERVAL = 500;

		private final OsmandApplication app;
		private long lastUpdateTime;
		private boolean updateEnable = true;
		private ArrayAdapter<String> listAdapter;

		DialogGpxDataItemCallback(OsmandApplication app) {
			this.app = app;
		}

		public boolean isUpdateEnable() {
			return updateEnable;
		}

		public void setUpdateEnable(boolean updateEnable) {
			this.updateEnable = updateEnable;
		}

		public ArrayAdapter<String> getListAdapter() {
			return listAdapter;
		}

		public void setListAdapter(ArrayAdapter<String> listAdapter) {
			this.listAdapter = listAdapter;
		}

		private final Runnable updateItemsProc = new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					lastUpdateTime = System.currentTimeMillis();
					listAdapter.notifyDataSetChanged();
				}
			}
		};

		@Override
		public boolean isCancelled() {
			return !updateEnable;
		}

		@Override
		public void onGpxDataItemReady(GpxDataItem item) {
			if (System.currentTimeMillis() - lastUpdateTime > MIN_UPDATE_INTERVAL) {
				updateItemsProc.run();
			}
			app.runMessageInUIThreadAndCancelPrevious(UPDATE_GPX_ITEM_MSG_ID, updateItemsProc, MIN_UPDATE_INTERVAL);
		}
	}

	private static AlertDialog createDialog(final Activity activity,
	                                        final boolean showCurrentGpx,
	                                        final boolean multipleChoice,
	                                        final boolean showAppearanceSetting,
	                                        final CallbackWithObject<GPXFile[]> callbackWithObject,
	                                        final List<GPXInfo> gpxInfoList,
	                                        final ContextMenuAdapter adapter,
	                                        final int themeRes,
	                                        final boolean nightMode) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		final int layout = R.layout.gpx_track_item;
		final Map<String, String> gpxAppearanceParams = new HashMap<>();
		final DialogGpxDataItemCallback gpxDataItemCallback = new DialogGpxDataItemCallback(app);

		List<String> modifiableGpxFileNames = CtxMenuUtils.getNames(adapter.getItems());
		final ArrayAdapter<String> alertDialogAdapter = new ArrayAdapter<String>(activity, layout, R.id.title, modifiableGpxFileNames) {

			@Override
			public int getItemViewType(int position) {
				return showCurrentGpx && position == 0 ? 1 : 0;
			}

			@Override
			public int getViewTypeCount() {
				return 2;
			}

			private GpxDataItem getDataItem(GPXInfo info) {
				return app.getGpxDbHelper().getItem(
						new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()),
						gpxDataItemCallback);
			}

			@Override
			@NonNull
			public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				boolean checkLayout = getItemViewType(position) == 0;
				if (v == null) {
					v = View.inflate(new ContextThemeWrapper(activity, themeRes), layout, null);
				}

				ContextMenuItem item = adapter.getItem(position);
				GPXInfo info = gpxInfoList.get(position);
				boolean currentlyRecordingTrack = showCurrentGpx && position == 0;

				GPXTrackAnalysis analysis = null;
				if (currentlyRecordingTrack) {
					analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
				} else {
					GpxDataItem dataItem = getDataItem(info);
					if (dataItem != null) {
						analysis = dataItem.getAnalysis();
					}
				}
				updateGpxInfoView(v, item.getTitle(), info, analysis, app);

				if (item.getSelected() == null) {
					v.findViewById(R.id.check_item).setVisibility(View.GONE);
					v.findViewById(R.id.check_local_index).setVisibility(View.GONE);
				} else {
					if (checkLayout) {
						final CheckBox ch = v.findViewById(R.id.check_local_index);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								item.setSelected(isChecked);
							}
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					} else {
						final SwitchCompat ch = v.findViewById(R.id.toggle_item);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_checkbox_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								item.setSelected(isChecked);
							}
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					}
					v.findViewById(R.id.check_item).setVisibility(View.VISIBLE);
				}
				return v;
			}
		};

		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
			}
		};
		gpxDataItemCallback.setListAdapter(alertDialogAdapter);
		builder.setAdapter(alertDialogAdapter, onClickListener);
		if (multipleChoice) {
			if (showAppearanceSetting) {
				final RenderingRuleProperty trackWidthProp;
				final RenderingRuleProperty trackColorProp;
				final RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (renderer != null) {
					trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
					trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
				} else {
					trackWidthProp = null;
					trackColorProp = null;
				}
				if (trackWidthProp == null || trackColorProp == null) {
					builder.setTitle(R.string.show_gpx);
				} else {
					final View apprTitleView = View.inflate(new ContextThemeWrapper(activity, themeRes), R.layout.select_gpx_appearance_title, null);

					final CommonPreference<String> prefWidth
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
					final CommonPreference<String> prefColor
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);

					updateAppearanceTitle(activity, app, trackWidthProp, renderer, apprTitleView, prefWidth.get(), prefColor.get());

					apprTitleView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							final ListPopupWindow popup = new ListPopupWindow(new ContextThemeWrapper(activity, themeRes));
							popup.setAnchorView(apprTitleView);
							popup.setContentWidth(AndroidUtils.dpToPx(activity, 200f));
							popup.setModal(true);
							popup.setDropDownGravity(Gravity.END | Gravity.TOP);
							popup.setVerticalOffset(AndroidUtils.dpToPx(activity, -48f));
							popup.setHorizontalOffset(AndroidUtils.dpToPx(activity, -6f));
							final GpxAppearanceAdapter gpxApprAdapter = new GpxAppearanceAdapter(new ContextThemeWrapper(activity, themeRes),
									gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get(),
									GpxAppearanceAdapter.GpxAppearanceAdapterType.TRACK_WIDTH_COLOR,
									gpxAppearanceParams.containsKey(SHOW_START_FINISH_ATTR) ? "true".equals(gpxAppearanceParams.get(SHOW_START_FINISH_ATTR)) : app.getSettings().SHOW_START_FINISH_ICONS.get(), nightMode);
							popup.setAdapter(gpxApprAdapter);
							popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									AppearanceListItem item = gpxApprAdapter.getItem(position);
									if (item != null) {
										if (CURRENT_TRACK_WIDTH_ATTR.equals(item.getAttrName())) {
											gpxAppearanceParams.put(CURRENT_TRACK_WIDTH_ATTR, item.getValue());
										} else if (CURRENT_TRACK_COLOR_ATTR.equals(item.getAttrName())) {
											gpxAppearanceParams.put(CURRENT_TRACK_COLOR_ATTR, item.getValue());
										} else if (SHOW_START_FINISH_ATTR.equals(item.getAttrName())) {
											gpxAppearanceParams.put(SHOW_START_FINISH_ATTR, item.getValue());
										}
									}
									popup.dismiss();
									updateAppearanceTitle(activity, app, trackWidthProp, renderer,
											apprTitleView,
											gpxAppearanceParams.containsKey(CURRENT_TRACK_WIDTH_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_WIDTH_ATTR) : prefWidth.get(),
											gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get());
								}
							});
							popup.show();
						}
					});
					builder.setCustomTitle(apprTitleView);
				}
			} else {
				builder.setTitle(R.string.show_gpx);
			}
			builder.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (gpxAppearanceParams.size() > 0) {
						for (Map.Entry<String, String> entry : gpxAppearanceParams.entrySet()) {
							if (SHOW_START_FINISH_ATTR.equals(entry.getKey())) {
								app.getSettings().SHOW_START_FINISH_ICONS.set("true".equals(entry.getValue()));
							} else {
								final CommonPreference<String> pref
										= app.getSettings().getCustomRenderProperty(entry.getKey());
								pref.set(entry.getValue());
							}
						}
						if (activity instanceof MapActivity) {
							((MapActivity) activity).refreshMapComplete();
						}
					}
					GPXFile currentGPX = null;
					//clear all previously selected files before adding new one
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					if (app.getSelectedGpxHelper() != null) {
						app.getSelectedGpxHelper().clearAllGpxFilesToShow(false);
					}
					if (showCurrentGpx && adapter.getItem(0).getSelected()) {
						currentGPX = app.getSavingTrackHelper().getCurrentGpx();
					}
					List<String> selectedGpxNames = new ArrayList<>();
					for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
						if (adapter.getItem(i).getSelected()) {
							selectedGpxNames.add(gpxInfoList.get(i).getFileName());
						}
					}
					dialog.dismiss();
					updateSelectedTracksAppearance(app, selectedGpxNames, gpxAppearanceParams);
					loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
							selectedGpxNames.toArray(new String[0]));
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
					&& gpxInfoList.size() > 1 || !showCurrentGpx && gpxInfoList.size() > 0) {
				builder.setNeutralButton(R.string.gpx_add_track, null);
			}
		}

		final AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(true);
		if (gpxInfoList.size() == 0 || showCurrentGpx && gpxInfoList.size() == 1) {
			final View footerView = activity.getLayoutInflater().inflate(R.layout.no_gpx_files_list_footer, null);
			TextView descTextView = footerView.findViewById(R.id.descFolder);
			String descPrefix = app.getString(R.string.gpx_no_tracks_title_folder);
			SpannableString spannableDesc = new SpannableString(descPrefix + ": " + dir.getAbsolutePath());
			spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
					descPrefix.length() + 1, spannableDesc.length(), 0);
			descTextView.setText(spannableDesc);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				footerView.findViewById(R.id.button).setVisibility(View.GONE);
			} else {
				footerView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
					}
				});
			}
			dlg.getListView().addFooterView(footerView, null, false);
		}
		dlg.getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (multipleChoice) {
					ContextMenuItem item = adapter.getItem(position);
					item.setSelected(!item.getSelected());
					alertDialogAdapter.notifyDataSetInvalidated();
					if (position == 0 && showCurrentGpx && item.getSelected()) {
						OsmandMonitoringPlugin monitoringPlugin = OsmandPlugin.getActivePlugin(OsmandMonitoringPlugin.class);
						if (monitoringPlugin == null) {
							AlertDialog.Builder confirm = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
							confirm.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Bundle params = new Bundle();
									params.putBoolean(PluginsFragment.OPEN_PLUGINS, true);
									MapActivity.launchMapActivityMoveToTop(activity, null, null, params);
								}
							});
							confirm.setNegativeButton(R.string.shared_string_cancel, null);
							confirm.setMessage(activity.getString(R.string.enable_plugin_monitoring_services));
							confirm.show();
						} else if (!app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							monitoringPlugin.controlDialog(activity);
						}
					}
				} else {
					dlg.dismiss();
					if (showCurrentGpx && position == 0) {
						callbackWithObject.processResult(null);
					} else {
						String fileName = gpxInfoList.get(position).getFileName();
						SelectedGpxFile selectedGpxFile =
								app.getSelectedGpxHelper().getSelectedFileByName(fileName);
						if (selectedGpxFile != null) {
							callbackWithObject.processResult(new GPXFile[] {selectedGpxFile.getGpxFile()});
						} else {
							loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
						}
					}
				}
			}
		});
		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button addTrackButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
				if (addTrackButton != null) {
					addTrackButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
						}
					});
				}
			}
		});
		dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				gpxDataItemCallback.setUpdateEnable(false);
			}
		});
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	private static void updateSelectedTracksAppearance(final OsmandApplication app, List<String> fileNames, final Map<String, String> params) {
		GpxDataItemCallback callback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				updateTrackAppearance(app, item, params);
			}
		};
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		for (String name : fileNames) {
			GpxDataItem item = gpxDbHelper.getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), name), callback);
			if (item != null) {
				updateTrackAppearance(app, item, params);
			}
		}
	}

	private static void updateTrackAppearance(OsmandApplication app, GpxDataItem item, Map<String, String> params) {
		OsmandSettings settings = app.getSettings();
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		if (params.containsKey(CURRENT_TRACK_COLOR_ATTR)) {
			String savedColor = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).get();
			int color = GpxAppearanceAdapter.parseTrackColor(app.getRendererRegistry().getCurrentSelectedRenderer(), savedColor);
			gpxDbHelper.updateColor(item, color);
		}
		if (params.containsKey(CURRENT_TRACK_WIDTH_ATTR)) {
			String width = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).get();
			gpxDbHelper.updateWidth(item, width);
		}
		if (params.containsKey(SHOW_START_FINISH_ATTR)) {
			boolean showStartFinish = settings.SHOW_START_FINISH_ICONS.get();
			gpxDbHelper.updateShowStartFinish(item, showStartFinish);
		}
	}

	public static void updateGpxInfoView(final @NonNull OsmandApplication app,
	                                     final @NonNull View v,
	                                     final @NonNull String itemTitle,
	                                     final @Nullable Drawable iconDrawable,
	                                     final @NonNull GPXInfo info) {
		GpxDataItem item = getDataItem(app, info, new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				updateGpxInfoView(app, v, itemTitle, iconDrawable, info, item);
			}
		});
		if (item != null) {
			updateGpxInfoView(app, v, itemTitle, iconDrawable, info, item);
		}
	}

	private static void updateGpxInfoView(@NonNull OsmandApplication app,
	                                      @NonNull View v,
	                                      @NonNull String itemTitle,
	                                      @Nullable Drawable iconDrawable,
	                                      @NonNull GPXInfo info,
	                                      @NonNull GpxDataItem dataItem) {
		updateGpxInfoView(v, itemTitle, info, dataItem.getAnalysis(), app);
		if (iconDrawable != null) {
			ImageView icon = v.findViewById(R.id.icon);
			icon.setImageDrawable(iconDrawable);
			icon.setVisibility(View.VISIBLE);
		}
	}

	public static void updateGpxInfoView(View v,
	                                     String itemTitle,
	                                     GPXInfo info,
	                                     GPXTrackAnalysis analysis,
	                                     OsmandApplication app) {
		TextView viewName = v.findViewById(R.id.name);
		viewName.setText(itemTitle.replace("/", " • ").trim());
		viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		ImageView icon = v.findViewById(R.id.icon);
		icon.setVisibility(View.GONE);
		//icon.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_polygom_dark));

		boolean sectionRead = analysis == null;
		if (sectionRead) {
			v.findViewById(R.id.read_section).setVisibility(View.GONE);
			v.findViewById(R.id.unknown_section).setVisibility(View.VISIBLE);
			String date = "";
			String size = "";
			if (info.getFileSize() >= 0) {
				size = AndroidUtils.formatSize(v.getContext(), info.getFileSize());
			}
			DateFormat df = app.getResourceManager().getDateFormat();
			long fd = info.getLastModified();
			if (fd > 0) {
				date = (df.format(new Date(fd)));
			}
			TextView sizeText = v.findViewById(R.id.date_and_size_details);
			sizeText.setText(date + " \u2022 " + size);

		} else {
			v.findViewById(R.id.read_section).setVisibility(View.VISIBLE);
			v.findViewById(R.id.unknown_section).setVisibility(View.GONE);
			ImageView distanceI = v.findViewById(R.id.distance_icon);
			distanceI.setVisibility(View.VISIBLE);
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsI = v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeI = v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_16));
			TextView time = v.findViewById(R.id.time);
			TextView distance = v.findViewById(R.id.distance);
			TextView pointsCount = v.findViewById(R.id.points_count);
			pointsCount.setText(analysis.wptPoints + "");
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));

			if (analysis.isTimeSpecified()) {
				time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()) + "");
			} else {
				time.setText("");
			}
		}

		TextView descr = v.findViewById(R.id.description);
		if (descr != null) {
			descr.setVisibility(View.GONE);
		}

		View checkbox = v.findViewById(R.id.check_item);
		if (checkbox != null) {
			checkbox.setVisibility(View.GONE);
		}
	}

	private static GpxDataItem getDataItem(@NonNull OsmandApplication app,
	                                       @NonNull GPXInfo info,
	                                       @Nullable GpxDataItemCallback callback) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		String fileName = info.getFileName();
		File file = new File(dir, fileName);
		return app.getGpxDbHelper().getItem(file, callback);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static void addTrack(Activity activity, ArrayAdapter<String> listAdapter,
	                             ContextMenuAdapter contextMenuAdapter, List<GPXInfo> allGpxFiles) {
		if (activity instanceof MapActivity) {
			final MapActivity mapActivity = (MapActivity) activity;
			OnActivityResultListener listener = (resultCode, resultData) -> {
				if (resultCode != Activity.RESULT_OK || resultData == null) {
					return;
				}

				ImportHelper importHelper = mapActivity.getImportHelper();
				importHelper.setGpxImportCompleteListener(new ImportHelper.OnGpxImportCompleteListener() {
					@Override
					public void onImportComplete(boolean success) {
					}

					@Override
					public void onSaveComplete(boolean success, GPXFile result) {
						if (success) {
							OsmandApplication app = (OsmandApplication) activity.getApplication();
							app.getSelectedGpxHelper().selectGpxFile(result, true, false);
							updateGpxDialogAfterImport(activity, listAdapter, contextMenuAdapter, allGpxFiles, result.path);
						}
					}
				});

				Uri uri = resultData.getData();
				importHelper.handleGpxImport(uri, null, false);
			};

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.setType("*/*");
			try {
				mapActivity.startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
				mapActivity.registerActivityResultListener(new ActivityResultListener(OPEN_GPX_DOCUMENT_REQUEST, listener));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mapActivity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			}
		}
	}

	private static void updateGpxDialogAfterImport(final Activity activity,
	                                               ArrayAdapter<String> dialogAdapter,
	                                               ContextMenuAdapter adapter, List<GPXInfo> allGpxFiles,
	                                               String importedGpx) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();

		List<String> selectedGpxFiles = new ArrayList<>();
		selectedGpxFiles.add(importedGpx);
		for (int i = 0; i < allGpxFiles.size(); i++) {
			GPXInfo gpxInfo = allGpxFiles.get(i);
			ContextMenuItem menuItem = adapter.getItem(i);
			if (menuItem.getSelected()) {
				boolean isCurrentTrack =
						gpxInfo.getFileName().equals(app.getString(R.string.show_current_gpx_title));
				selectedGpxFiles.add(isCurrentTrack ? "" : gpxInfo.getFileName());
			}
		}
		allGpxFiles.clear();
		allGpxFiles.addAll(listGpxInfo(app, selectedGpxFiles, false));
		adapter.clear();
		fillGpxContextMenuAdapter(adapter, allGpxFiles, true);
		dialogAdapter.clear();
		dialogAdapter.addAll(CtxMenuUtils.getNames(adapter.getItems()));
		dialogAdapter.notifyDataSetInvalidated();
	}

	private static void updateAppearanceTitle(Activity activity, OsmandApplication app,
											  RenderingRuleProperty trackWidthProp,
											  RenderingRulesStorage renderer,
											  View apprTitleView,
											  String prefWidthValue,
											  String prefColorValue) {
		TextView widthTextView = apprTitleView.findViewById(R.id.widthTitle);
		ImageView colorImageView = apprTitleView.findViewById(R.id.colorImage);
		if (!Algorithms.isEmpty(prefWidthValue)) {
			widthTextView.setText(AndroidUtils.getRenderingStringPropertyValue(activity, prefWidthValue));
		}
		int color = GpxAppearanceAdapter.parseTrackColor(renderer, prefColorValue);
		if (color == -1) {
			colorImageView.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	@NonNull
	public static List<String> getSelectedTrackPaths(OsmandApplication app) {
		List<String> trackNames = new ArrayList<>();
		for (SelectedGpxFile file : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			trackNames.add(file.getGpxFile().path);
		}
		return trackNames;
	}

	public static List<GPXInfo> getSortedGPXFilesInfoByDate(File dir, boolean absolutePath) {
		final List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo object1, GPXInfo object2) {
				long lhs = object1.getLastModified();
				long rhs = object2.getLastModified();
				return lhs < rhs ? 1 : (lhs == rhs ? 0 : -1);
			}
		});
		return list;
	}

	@Nullable
	public static GPXInfo getGpxInfoByFileName(@NonNull OsmandApplication app, @NonNull String fileName) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		File file = new File(dir, fileName);
		if (file.exists() && file.getName().endsWith(GPX_FILE_EXT)) {
			return new GPXInfo(fileName, file.lastModified(), file.length());
		}
		return null;
	}

	@NonNull
	public static List<GPXInfo> getSortedGPXFilesInfo(File dir, final List<String> selectedGpxList, boolean absolutePath) {
		final List<GPXInfo> allGpxFiles = new ArrayList<>();
		readGpxDirectory(dir, allGpxFiles, "", absolutePath);
		if (selectedGpxList != null) {
			for (GPXInfo gpxInfo : allGpxFiles) {
				for (String selectedGpxName : selectedGpxList) {
					if (selectedGpxName.endsWith(gpxInfo.getFileName())) {
						gpxInfo.setSelected(true);
						break;
					}
				}
			}
		}
		Collections.sort(allGpxFiles, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				int res = i1.isSelected() == i2.isSelected() ? 0 : i1.isSelected() ? -1 : 1;
				if (res != 0) {
					return res;
				}

				String name1 = i1.getFileName();
				String name2 = i2.getFileName();
				int d1 = depth(name1);
				int d2 = depth(name2);
				if (d1 != d2) {
					return d1 - d2;
				}
				int lastSame = 0;
				for (int i = 0; i < name1.length() && i < name2.length(); i++) {
					if (name1.charAt(i) != name2.charAt(i)) {
						break;
					}
					if (name1.charAt(i) == '/') {
						lastSame = i + 1;
					}
				}

				boolean isDigitStarts1 = isLastSameStartsWithDigit(name1, lastSame);
				boolean isDigitStarts2 = isLastSameStartsWithDigit(name2, lastSame);
				res = isDigitStarts1 == isDigitStarts2 ? 0 : isDigitStarts1 ? -1 : 1;
				if (res != 0) {
					return res;
				}
				if (isDigitStarts1) {
					return -name1.compareToIgnoreCase(name2);
				}
				return name1.compareToIgnoreCase(name2);
			}

			private int depth(String name1) {
				int d = 0;
				for (int i = 0; i < name1.length(); i++) {
					if (name1.charAt(i) == '/') {
						d++;
					}
				}
				return d;
			}

			private boolean isLastSameStartsWithDigit(String name1, int lastSame) {
				if (name1.length() > lastSame) {
					return Character.isDigit(name1.charAt(lastSame));
				}

				return false;
			}
		});
		return allGpxFiles;
	}

	public static void readGpxDirectory(@Nullable File dir, @NonNull List<GPXInfo> list,
	                                    @NonNull String parent, boolean absolutePath) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					if (file.isFile() && name.toLowerCase().endsWith(GPX_FILE_EXT)) {
						String fileName = absolutePath ? file.getAbsolutePath() : parent + name;
						list.add(new GPXInfo(fileName, file.lastModified(), file.length()));
					} else if (file.isDirectory()) {
						readGpxDirectory(file, list, parent + name + "/", absolutePath);
					}
				}
			}
		}
	}

	public static void loadGPXFileInDifferentThread(final Activity activity, final CallbackWithObject<GPXFile[]> callbackWithObject,
	                                         final File dir, final GPXFile currentFile, final String... filename) {
		final ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading_smth, ""),
				activity.getString(R.string.loading_data));
		new Thread(new Runnable() {
			@Override
			public void run() {
				final GPXFile[] result = new GPXFile[filename.length + (currentFile == null ? 0 : 1)];
				int k = 0;
				String w = "";
				if (currentFile != null) {
					result[k++] = currentFile;
				}
				for (String fname : filename) {
					final File f = new File(dir, fname);
					GPXFile res = GPXUtilities.loadGPXFile(f);
					if (res.error != null && !Algorithms.isEmpty(res.error.getMessage())) {
						w += res.error.getMessage() + "\n";
					} else {
						res.addGeneralTrack();
					}
					result[k++] = res;
				}
				dlg.dismiss();
				final String warn = w;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (warn.length() > 0) {
							Toast.makeText(activity, warn, Toast.LENGTH_LONG).show();
						} else {
							callbackWithObject.processResult(result);
						}
					}
				});
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}

	public static void setupGPXChart(@NonNull LineChart mChart) {
		setupGPXChart(mChart, 24f, 16f, true);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale) {
		setupGPXChart(mChart, topOffset, bottomOffset, useGesturesAndScale, null);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale, @Nullable Drawable markerIcon) {
		GpxMarkerView markerView = new GpxMarkerView(mChart.getContext(), markerIcon);
		setupGPXChart(mChart, markerView, topOffset, bottomOffset, useGesturesAndScale);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, @NonNull GpxMarkerView markerView,
	                                 float topOffset, float bottomOffset, boolean useGesturesAndScale) {
		Context context = mChart.getContext();

		mChart.setHardwareAccelerationEnabled(true);
		mChart.setTouchEnabled(useGesturesAndScale);
		mChart.setDragEnabled(useGesturesAndScale);
		mChart.setScaleEnabled(useGesturesAndScale);
		mChart.setPinchZoom(useGesturesAndScale);
		mChart.setScaleYEnabled(false);
		mChart.setAutoScaleMinMaxEnabled(true);
		mChart.setDrawBorders(false);
		mChart.getDescription().setEnabled(false);
		mChart.setMaxVisibleValueCount(10);
		mChart.setMinOffset(0f);
		mChart.setDragDecelerationEnabled(false);

		mChart.setExtraTopOffset(topOffset);
		mChart.setExtraBottomOffset(bottomOffset);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		markerView.setChartView(mChart); // For bounds control
		mChart.setMarker(markerView); // Set the marker to the chart
		mChart.setDrawMarkers(true);

		ChartLabel chartLabel = new ChartLabel(context, R.layout.chart_label);
		chartLabel.setChart(mChart);
		mChart.setYAxisLabelView(chartLabel);

		int xAxisRulerColor = ContextCompat.getColor(context, R.color.gpx_chart_black_grid);
		int labelsColor = ContextCompat.getColor(context, R.color.description_font_and_bottom_sheet_icons);
		XAxis xAxis = mChart.getXAxis();
		xAxis.setDrawAxisLine(true);
		xAxis.setDrawAxisLineBehindData(false);
		xAxis.setAxisLineWidth(1);
		xAxis.setAxisLineColor(xAxisRulerColor);
		xAxis.setDrawGridLines(true);
		xAxis.setDrawGridLinesBehindData(false);
		xAxis.setGridLineWidth(1.5f);
		xAxis.setGridColor(xAxisRulerColor);
		xAxis.enableGridDashedLine(25f, Float.MAX_VALUE, 0f);
		xAxis.setPosition(BOTTOM);
		xAxis.setTextColor(labelsColor);

		int dp4 = AndroidUtils.dpToPx(context, 4);
		int yAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_grid_line_color);

		YAxis leftYAxis = mChart.getAxisLeft();
		leftYAxis.enableGridDashedLine(dp4, dp4, 0f);
		leftYAxis.setGridColor(yAxisGridColor);
		leftYAxis.setGridLineWidth(1f);
		leftYAxis.setDrawBottomYGridLine(false);
		leftYAxis.setDrawAxisLine(false);
		leftYAxis.setDrawGridLinesBehindData(false);
		leftYAxis.setPosition(YAxisLabelPosition.INSIDE_CHART);
		leftYAxis.setXOffset(16f);
		leftYAxis.setYOffset(-6f);
		leftYAxis.setLabelCount(3, true);

		YAxis rightYAxis = mChart.getAxisRight();
		rightYAxis.setDrawAxisLine(false);
		rightYAxis.setDrawGridLines(false);
		rightYAxis.setPosition(YAxisLabelPosition.INSIDE_CHART);
		rightYAxis.setXOffset(16f);
		rightYAxis.setYOffset(-6f);
		rightYAxis.setLabelCount(3, true);
		rightYAxis.setEnabled(false);

		Legend legend = mChart.getLegend();
		legend.setEnabled(false);
	}

	private static float setupAxisDistance(OsmandApplication ctx, AxisBase axisBase, float meters) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		float divX;

		String format1 = "{0,number,0.#} ";
		String format2 = "{0,number,0.##} ";
		String fmt = null;
		float granularity = 1f;
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == MetricsConstants.NAUTICAL_MILES) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}
		if (meters > 9.99f * mainUnitInMeters) {
			fmt = format1;
			granularity = .1f;
		}
		if (meters >= 100 * mainUnitInMeters ||
				meters > 9.99f * mainUnitInMeters ||
				meters > 0.999f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.NAUTICAL_MILES && meters > 0.99f * mainUnitInMeters) {

			divX = mainUnitInMeters;
			if (fmt == null) {
				fmt = format2;
				granularity = .01f;
			}
		} else {
			fmt = null;
			granularity = 1f;
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				divX = 1f;
				mainUnitStr = R.string.m;
			} else if (mc == MetricsConstants.MILES_AND_FEET) {
				divX = 1f / FEET_IN_ONE_METER;
				mainUnitStr = R.string.foot;
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				divX = 1f / YARDS_IN_ONE_METER;
				mainUnitStr = R.string.yard;
			} else {
				divX = 1f;
				mainUnitStr = R.string.m;
			}
		}

		final String formatX = fmt;
		final String mainUnitX = ctx.getString(mainUnitStr);

		axisBase.setGranularity(granularity);
		axisBase.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatX)) {
				return MessageFormat.format(formatX + mainUnitX, value);
			} else {
				return OsmAndFormatter.formatInteger((int) value, mainUnitX, ctx);
			}
		});

		return divX;
	}

	private static float setupXAxisTime(XAxis xAxis, long timeSpan) {
		final boolean useHours = timeSpan / HOUR_IN_MILLIS > 0;
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((value, axis) -> formatXAxisTime((int) value, useHours));
		return 1f;
	}

	public static String formatXAxisTime(int seconds, boolean useHours) {
		if (useHours) {
			return OsmAndFormatter.getFormattedDurationShort(seconds);
		} else {
			int minutes = (seconds / 60) % 60;
			int sec = seconds % 60;
			return (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
		}
	}

	private static float setupXAxisTimeOfDay(@NonNull XAxis xAxis, long startTime) {
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((seconds, axis) -> {
			long time = startTime + (long) (seconds * 1000);
			return OsmAndFormatter.getFormattedFullTime(time);
		});
		return 1f;
	}

	private static List<Entry> calculateElevationArray(GPXTrackAnalysis analysis, GPXDataSetAxisType axisType,
													   float divX, float convEle, boolean useGeneralTrackPoints, boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		List<Elevation> elevationData = analysis.elevationData;
		float nextX = 0;
		float nextY;
		float elev;
		float prevElevOrig = -80000;
		float prevElev = 0;
		int i = -1;
		int lastIndex = elevationData.size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x = 0f;
		for (Elevation e : elevationData) {
			i++;
			if (axisType == GPXDataSetAxisType.TIME || axisType == GPXDataSetAxisType.TIMEOFDAY) {
				x = e.time;
			} else {
				x = e.distance;
			}
			if (x >= 0) {
				if (!(calcWithoutGaps && e.firstPoint && lastEntry != null)) {
					nextX += x / divX;
				}
				if (!Float.isNaN(e.elevation)) {
					elev = e.elevation;
					if (prevElevOrig != -80000) {
						if (elev > prevElevOrig) {
							//elev -= 1f;
						} else if (prevElevOrig == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (prevElev == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (hasSameY) {
							values.add(new Entry(lastXSameY, lastEntry.getY()));
						}
						hasSameY = false;
					}
					if (useGeneralTrackPoints && e.firstPoint && lastEntry != null) {
						values.add(new Entry(nextX, lastEntry.getY()));
					}
					prevElevOrig = e.elevation;
					prevElev = elev;
					nextY = elev * convEle;
					lastEntry = new Entry(nextX, nextY);
					values.add(lastEntry);
				}
			}
		}
		return values;
	}

	public static void setupHorizontalGPXChart(OsmandApplication app, HorizontalBarChart chart, int yLabelsCount,
	                                           float topOffset, float bottomOffset, boolean useGesturesAndScale, boolean nightMode) {
		chart.setHardwareAccelerationEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
		chart.setTouchEnabled(useGesturesAndScale);
		chart.setDragEnabled(useGesturesAndScale);
		chart.setScaleYEnabled(false);
		chart.setAutoScaleMinMaxEnabled(true);
		chart.setDrawBorders(true);
		chart.getDescription().setEnabled(false);
		chart.setDragDecelerationEnabled(false);

		chart.setExtraTopOffset(topOffset);
		chart.setExtraBottomOffset(bottomOffset);

		XAxis xl = chart.getXAxis();
		xl.setDrawLabels(false);
		xl.setEnabled(false);
		xl.setDrawAxisLine(false);
		xl.setDrawGridLines(false);

		YAxis yl = chart.getAxisLeft();
		yl.setLabelCount(yLabelsCount);
		yl.setDrawLabels(false);
		yl.setEnabled(false);
		yl.setDrawAxisLine(false);
		yl.setDrawGridLines(false);
		yl.setAxisMinimum(0f);

		YAxis yr = chart.getAxisRight();
		yr.setLabelCount(yLabelsCount);
		yr.setDrawAxisLine(false);
		yr.setDrawGridLines(false);
		yr.setAxisMinimum(0f);
		chart.setMinOffset(0);

		int mainFontColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		yl.setTextColor(mainFontColor);
		yr.setTextColor(mainFontColor);

		chart.setFitBars(true);
		chart.setBorderColor(ColorUtilities.getDividerColor(app, nightMode));

		Legend l = chart.getLegend();
		l.setEnabled(false);
	}

	public static <E> BarData buildStatisticChart(@NonNull OsmandApplication app,
	                                              @NonNull HorizontalBarChart mChart,
	                                              @NonNull RouteStatisticsHelper.RouteStatistics routeStatistics,
	                                              @NonNull GPXTrackAnalysis analysis,
	                                              boolean useRightAxis,
	                                              boolean nightMode) {

		XAxis xAxis = mChart.getXAxis();
		xAxis.setEnabled(false);

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		float divX = setupAxisDistance(app, yAxis, analysis.totalDistance);

		List<RouteSegmentAttribute> segments = routeStatistics.elements;
		List<BarEntry> entries = new ArrayList<>();
		float[] stacks = new float[segments.size()];
		int[] colors = new int[segments.size()];
		for (int i = 0; i < stacks.length; i++) {
			RouteSegmentAttribute segment = segments.get(i);
			stacks[i] = segment.getDistance() / divX;
			colors[i] = segment.getColor();
		}
		entries.add(new BarEntry(0, stacks));
		BarDataSet barDataSet = new BarDataSet(entries, "");
		barDataSet.setColors(colors);
		barDataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		BarData dataSet = new BarData(barDataSet);
		dataSet.setDrawValues(false);
		dataSet.setBarWidth(1);
		mChart.getAxisRight().setAxisMaximum(dataSet.getYMax());
		mChart.getAxisLeft().setAxisMaximum(dataSet.getYMax());

		return dataSet;
	}

	public static OrderedLineDataSet createGPXElevationDataSet(@NonNull OsmandApplication ctx,
															   @NonNull LineChart mChart,
															   @NonNull GPXTrackAnalysis analysis,
															   @NonNull GPXDataSetAxisType axisType,
															   boolean useRightAxis,
															   boolean drawFilled,
															   boolean calcWithoutGaps) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS);
		boolean light = settings.isLightContent();
		final float convEle = useFeet ? 3.28084f : 1.0f;

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == GPXDataSetAxisType.TIMEOFDAY && analysis.isTimeSpecified()) {
			divX = setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}

		final String mainUnitY = useFeet ? ctx.getString(R.string.foot) : ctx.getString(R.string.m);

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue_label));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) value, mainUnitY, ctx));

		List<Entry> values = calculateElevationArray(analysis, axisType, divX, convEle, true, calcWithoutGaps);

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.ALTITUDE,
				axisType, !useRightAxis);
		dataSet.priority = (float) (analysis.avgElevation - analysis.minElevation) * convEle;
		dataSet.divX = divX;
		dataSet.mulY = convEle;
		dataSet.divY = Float.NaN;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		dataSet.setFillFormatter((ds, dataProvider) -> dataProvider.getYChartMin());
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSpeedDataSet(@NonNull OsmandApplication ctx,
	                                                       @NonNull LineChart mChart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == GPXDataSetAxisType.TIMEOFDAY && analysis.isTimeSpecified()) {
			divX = setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}

		SpeedConstants sps = settings.SPEED_SYSTEM.get();
		float mulSpeed = Float.NaN;
		float divSpeed = Float.NaN;
		final String mainUnitY = sps.toShortString(ctx);
		if (sps == SpeedConstants.KILOMETERS_PER_HOUR) {
			mulSpeed = 3.6f;
		} else if (sps == SpeedConstants.MILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
		} else if (sps == SpeedConstants.NAUTICALMILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
		} else if (sps == SpeedConstants.MINUTES_PER_KILOMETER) {
			divSpeed = METERS_IN_KILOMETER / 60;
		} else if (sps == SpeedConstants.MINUTES_PER_MILE) {
			divSpeed = METERS_IN_ONE_MILE / 60;
		} else {
			mulSpeed = 1f;
		}

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		if (analysis.hasSpeedInTrack()) {
			yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange_label));
		} else {
			yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_red_label));
		}

		yAxis.setAxisMinimum(0f);

		ArrayList<Entry> values = new ArrayList<>();
		List<Speed> speedData = analysis.speedData;
		float nextX = 0;
		float nextY;
		float x;
		for (Speed s : speedData) {
			switch (axisType) {
				case TIMEOFDAY:
				case TIME:
					x = s.time;
					break;
				default:
					x = s.distance;
					break;
			}

			if (x > 0) {
				if (axisType == GPXDataSetAxisType.TIME && x > 60 ||
					axisType == GPXDataSetAxisType.TIMEOFDAY && x > 60) {
					values.add(new Entry(nextX + 1, 0));
					values.add(new Entry(nextX + x - 1, 0));
				}
				if (!(calcWithoutGaps && s.firstPoint)) {
					nextX += x / divX;
				}
				if (Float.isNaN(divSpeed)) {
					nextY = s.speed * mulSpeed;
				} else {
					nextY = divSpeed / s.speed;
				}
				if (nextY < 0 || Float.isInfinite(nextY)) {
					nextY = 0;
				}
				if (s.firstPoint) {
					values.add(new Entry(nextX, 0));
				}
				values.add(new Entry(nextX, nextY));
				if (s.lastPoint) {
					values.add(new Entry(nextX, 0));
				}
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.SPEED,
				axisType, !useRightAxis);

		String format = null;
		if (dataSet.getYMax() < 3) {
			format = "{0,number,0.#} ";
		}
		final String formatY = format;
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) value, mainUnitY, ctx);
			}
		});

		if (Float.isNaN(divSpeed)) {
			dataSet.priority = analysis.avgSpeed * mulSpeed;
		} else {
			dataSet.priority = divSpeed / analysis.avgSpeed;
		}
		dataSet.divX = divX;
		if (Float.isNaN(divSpeed)) {
			dataSet.mulY = mulSpeed;
			dataSet.divY = Float.NaN;
		} else {
			dataSet.divY = divSpeed;
			dataSet.mulY = Float.NaN;
		}
		dataSet.units = mainUnitY;

		if (analysis.hasSpeedInTrack()) {
			dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
		} else {
			dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_red));
		}
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			if (analysis.hasSpeedInTrack()) {
				dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
			} else {
				dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_red));
			}
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}
		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(@NonNull OsmandApplication ctx,
														   @NonNull LineChart mChart,
														   @NonNull GPXTrackAnalysis analysis,
														   @NonNull GPXDataSetAxisType axisType,
														   @Nullable List<Entry> eleValues,
														   boolean useRightAxis,
														   boolean drawFilled,
														   boolean calcWithoutGaps) {
		if (axisType == GPXDataSetAxisType.TIME || axisType == GPXDataSetAxisType.TIMEOFDAY) {
			return null;
		}
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS);
		final float convEle = useFeet ? 3.28084f : 1.0f;
		final float totalDistance = calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;

		XAxis xAxis = mChart.getXAxis();
		float divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);

		final String mainUnitY = "%";

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_green_label));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) value, mainUnitY, ctx));

		List<Entry> values;
		if (eleValues == null) {
			values = calculateElevationArray(analysis, GPXDataSetAxisType.DISTANCE, 1f, 1f, false, calcWithoutGaps);
		} else {
			values = new ArrayList<>(eleValues.size());
			for (Entry e : eleValues) {
				values.add(new Entry(e.getX() * divX, e.getY() / convEle));
			}
		}

		if (Algorithms.isEmpty(values)) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		int lastIndex = values.size() - 1;

		double STEP = 5;
		int l = 10;
		while (l > 0 && totalDistance / STEP > MAX_CHART_DATA_ITEMS) {
			STEP = Math.max(STEP, totalDistance / (values.size() * l--));
		}

		double[] calculatedDist = new double[(int) (totalDistance / STEP) + 1];
		double[] calculatedH = new double[(int) (totalDistance / STEP) + 1];
		int nextW = 0;
		for (int k = 0; k < calculatedDist.length; k++) {
			if (k > 0) {
				calculatedDist[k] = calculatedDist[k - 1] + STEP;
			}
			while (nextW < lastIndex && calculatedDist[k] > values.get(nextW).getX()) {
				nextW++;
			}
			double pd = nextW == 0 ? 0 : values.get(nextW - 1).getX();
			double ph = nextW == 0 ? values.get(0).getY() : values.get(nextW - 1).getY();
			calculatedH[k] = ph + (values.get(nextW).getY() - ph) / (values.get(nextW).getX() - pd) * (calculatedDist[k] - pd);
		}

		double SLOPE_PROXIMITY = Math.max(100, STEP * 2);

		if (totalDistance - SLOPE_PROXIMITY < 0) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		double[] calculatedSlopeDist = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];
		double[] calculatedSlope = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];

		int index = (int) ((SLOPE_PROXIMITY / STEP) / 2);
		for (int k = 0; k < calculatedSlopeDist.length; k++) {
			calculatedSlopeDist[k] = calculatedDist[index + k];
			calculatedSlope[k] = (calculatedH[2 * index + k] - calculatedH[k]) * 100 / SLOPE_PROXIMITY;
			if (Double.isNaN(calculatedSlope[k])) {
				calculatedSlope[k] = 0;
			}
		}

		List<Entry> slopeValues = new ArrayList<>(calculatedSlopeDist.length);
		float prevSlope = -80000;
		float slope;
		float x;
		float lastXSameY = 0;
		boolean hasSameY = false;
		Entry lastEntry = null;
		lastIndex = calculatedSlopeDist.length - 1;
		for (int i = 0; i < calculatedSlopeDist.length; i++) {
			x = (float) calculatedSlopeDist[i] / divX;
			slope = (float) calculatedSlope[i];
			if (prevSlope != -80000) {
				if (prevSlope == slope && i < lastIndex) {
					hasSameY = true;
					lastXSameY = x;
					continue;
				}
				if (hasSameY) {
					slopeValues.add(new Entry(lastXSameY, lastEntry.getY()));
				}
				hasSameY = false;
			}
			prevSlope = slope;
			lastEntry = new Entry(x, slope);
			slopeValues.add(lastEntry);
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(slopeValues, "", GPXDataSetType.SLOPE,
				axisType, !useRightAxis);
		dataSet.divX = divX;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		/*
		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		*/
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public enum GPXDataSetType {
		ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
		SPEED(R.string.map_widget_current_speed, R.drawable.ic_action_speed),
		SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

		private final int stringId;
		private final int imageId;

		GPXDataSetType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(@NonNull Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(@NonNull OsmandApplication app) {
			return app.getUIUtilities().getThemedIcon(imageId);
		}

		public static String getName(@NonNull Context ctx, @NonNull GPXDataSetType[] types) {
			List<String> list = new ArrayList<>();
			for (GPXDataSetType type : types) {
				list.add(type.getName(ctx));
			}
			Collections.sort(list);
			StringBuilder sb = new StringBuilder();
			for (String s : list) {
				if (sb.length() > 0) {
					sb.append("/");
				}
				sb.append(s);
			}
			return sb.toString();
		}

		public static Drawable getImageDrawable(@NonNull OsmandApplication app, @NonNull GPXDataSetType[] types) {
			if (types.length > 0) {
				return types[0].getImageDrawable(app);
			} else {
				return null;
			}
		}
	}

	public enum GPXDataSetAxisType {
		DISTANCE(R.string.distance, R.drawable.ic_action_marker_dark),
		TIME(R.string.shared_string_time, R.drawable.ic_action_time),
		TIMEOFDAY(R.string.time_of_day, R.drawable.ic_action_time_span);

		private final int stringId;
		private final int imageId;

		GPXDataSetAxisType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(OsmandApplication app) {
			return app.getUIUtilities().getThemedIcon(imageId);
		}
	}

	public static class OrderedLineDataSet extends LineDataSet {

		private final GPXDataSetType dataSetType;
		private final GPXDataSetAxisType dataSetAxisType;

		private final boolean leftAxis;

		float priority;
		String units;
		float divX = 1f;
		float divY = 1f;
		float mulY = 1f;

		OrderedLineDataSet(List<Entry> yVals, String label, GPXDataSetType dataSetType,
		                   GPXDataSetAxisType dataSetAxisType, boolean leftAxis) {
			super(yVals, label);
			this.dataSetType = dataSetType;
			this.dataSetAxisType = dataSetAxisType;
			this.leftAxis = leftAxis;
		}

		public GPXDataSetType getDataSetType() {
			return dataSetType;
		}

		public GPXDataSetAxisType getDataSetAxisType() {
			return dataSetAxisType;
		}

		public float getPriority() {
			return priority;
		}

		public float getDivX() {
			return divX;
		}

		public float getDivY() {
			return divY;
		}

		public float getMulY() {
			return mulY;
		}

		public String getUnits() {
			return units;
		}

		public boolean isLeftAxis() {
			return leftAxis;
		}
	}

	public static class GPXHighlight extends Highlight {

		private final boolean showIcon;

		public GPXHighlight(float x, int dataSetIndex, boolean showIcon) {
			super(x, Float.NaN, dataSetIndex);
			this.showIcon = showIcon;
		}

		public GPXHighlight(float x, float y, int dataSetIndex, boolean showIcon) {
			super(x, y, dataSetIndex);
			this.showIcon = showIcon;
		}

		public boolean shouldShowIcon() {
			return showIcon;
		}
	}

	@NonNull
	public static GPXFile makeGpxFromRoute(RouteCalculationResult route, OsmandApplication app) {
		return makeGpxFromLocations(route.getRouteLocations(), app);
	}

	@NonNull
	public static GPXFile makeGpxFromLocations(List<Location> locations, OsmandApplication app) {
		double lastHeight = HEIGHT_UNDEFINED;
		double lastValidHeight = Double.NaN;
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		if (locations != null) {
			Track track = new Track();
			TrkSegment seg = new TrkSegment();
			List<WptPt> pts = seg.points;
			for (Location l : locations) {
				WptPt point = new WptPt();
				point.lat = l.getLatitude();
				point.lon = l.getLongitude();
				if (l.hasAltitude()) {
					gpx.hasAltitude = true;
					float h = (float) l.getAltitude();
					point.ele = h;
					lastValidHeight = h;
					if (lastHeight == HEIGHT_UNDEFINED && pts.size() > 0) {
						for (WptPt pt : pts) {
							if (Double.isNaN(pt.ele)) {
								pt.ele = h;
							}
						}
					}
					lastHeight = h;
				} else {
					lastHeight = HEIGHT_UNDEFINED;
				}
				if (pts.size() == 0) {
					point.time = System.currentTimeMillis();
				} else {
					GPXUtilities.WptPt prevPoint = pts.get(pts.size() - 1);
					if (l.hasSpeed() && l.getSpeed() != 0) {
						point.speed = l.getSpeed();
						double dist = MapUtils.getDistance(prevPoint.lat, prevPoint.lon, point.lat, point.lon);
						point.time = prevPoint.time + (long) (dist / point.speed) * SECOND_IN_MILLIS;
					} else {
						point.time = prevPoint.time;
					}
				}
				pts.add(point);
			}
			if (!Double.isNaN(lastValidHeight) && lastHeight == HEIGHT_UNDEFINED) {
				for (ListIterator<WptPt> iterator = pts.listIterator(pts.size()); iterator.hasPrevious(); ) {
					WptPt point = iterator.previous();
					if (!Double.isNaN(point.ele)) {
						break;
					}
					point.ele = lastValidHeight;
				}
			}
			track.segments.add(seg);
			gpx.tracks.add(track);
		}
		return gpx;
	}

	public enum LineGraphType {
		ALTITUDE,
		SLOPE,
		SPEED
	}

	public static List<ILineDataSet> getDataSets(LineChart chart,
	                                             OsmandApplication app,
	                                             GPXTrackAnalysis analysis,
	                                             @NonNull LineGraphType firstType,
	                                             @Nullable LineGraphType secondType,
	                                             boolean calcWithoutGaps) {
		if (app == null || chart == null || analysis == null) {
			return new ArrayList<>();
		}
		List<ILineDataSet> result = new ArrayList<>();
		if (secondType == null) {
			ILineDataSet dataSet = getDataSet(chart, app, analysis, calcWithoutGaps, false, firstType);
			if (dataSet != null) {
				result.add(dataSet);
			}
		} else {
			OrderedLineDataSet dataSet1 = getDataSet(chart, app, analysis, calcWithoutGaps, false, firstType);
			OrderedLineDataSet dataSet2 = getDataSet(chart, app, analysis, calcWithoutGaps, true, secondType);
			if (dataSet1 == null && dataSet2 == null) {
				return new ArrayList<>();
			} else if (dataSet1 == null) {
				result.add(dataSet2);
			} else if (dataSet2 == null) {
				result.add(dataSet1);
			} else if (dataSet1.getPriority() < dataSet2.getPriority()) {
				result.add(dataSet2);
				result.add(dataSet1);
			} else {
				result.add(dataSet1);
				result.add(dataSet2);
			}
		}
		return result;
	}

	private static OrderedLineDataSet getDataSet(@NonNull LineChart chart,
	                                             @NonNull OsmandApplication app,
	                                             @NonNull GPXTrackAnalysis analysis,
	                                             boolean calcWithoutGaps,
	                                             boolean useRightAxis,
	                                             @NonNull LineGraphType type) {
		OrderedLineDataSet dataSet = null;
		switch (type) {
			case ALTITUDE: {
				if (analysis.hasElevationData) {
					dataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
				break;
			}
			case SLOPE:
				if (analysis.hasElevationData) {
					dataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, null, useRightAxis, true, calcWithoutGaps);
				}
				break;
			case SPEED: {
				if (analysis.hasSpeedData) {
					dataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
				break;
			}
		}
		return dataSet;
	}

	public static GpxDisplayItem makeGpxDisplayItem(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                                @NonNull ChartPointLayer chartPointLayer) {
		GpxDisplayGroup group = null;
		if (!Algorithms.isEmpty(gpxFile.tracks)) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			String groupName = helper.getGroupName(gpxFile);
			group = helper.buildGpxDisplayGroup(gpxFile, 0, groupName);
		}
		if (group != null && group.getModifiableList().size() > 0) {
			GpxDisplayItem gpxItem = group.getModifiableList().get(0);
			if (gpxItem != null) {
				gpxItem.chartPointLayer = chartPointLayer;
			}
			return gpxItem;
		}
		return null;
	}

	public static void saveAndShareGpx(@NonNull final Context context, @NonNull final GPXFile gpxFile) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		File tempDir = FileUtils.getTempDir(app);
		String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);
		final File file = new File(tempDir, fileName);
		SaveGpxListener listener = new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage == null) {
					shareGpx(context, file);
				}
			}
		};
		new SaveGpxAsyncTask(file, gpxFile, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void saveAndShareCurrentGpx(@NonNull final OsmandApplication app, @NonNull final GPXFile gpxFile) {
		SaveGpxListener saveGpxListener = new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage == null) {
					shareGpx(app, new File(gpxFile.path));
				}
			}
		};
		new SaveCurrentTrackTask(app, gpxFile, saveGpxListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void saveAndShareGpxWithAppearance(@NonNull final Context context, @NonNull final GPXFile gpxFile) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		GpxDataItem dataItem = getDataItem(app, gpxFile);
		if (dataItem != null) {
			addAppearanceToGpx(gpxFile, dataItem);
			saveAndShareGpx(app, gpxFile);
		}
	}

	public static void saveGpx(GPXFile gpxFile, SaveGpxListener listener) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void saveAndOpenGpx(@NonNull MapActivity mapActivity,
	                                  @NonNull File file,
	                                  @NonNull GPXFile gpxFile,
	                                  @NonNull WptPt selectedPoint,
	                                  @Nullable GPXTrackAnalysis analyses,
	                                  @Nullable Drawable trackIcon) {
		new SaveGpxAsyncTask(file, gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage == null) {
					OsmandApplication app = mapActivity.getMyApplication();
					SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, true, false);
					GPXTrackAnalysis trackAnalysis = analyses != null ? analyses : selectedGpxFile.getTrackAnalysis(app);
					SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(selectedGpxFile, selectedPoint);
					TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint,
							null, null, null, false, trackAnalysis, trackIcon);
				} else {
					LOG.error(errorMessage);
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static GpxDataItem getDataItem(@NonNull final OsmandApplication app, @NonNull final GPXFile gpxFile) {
		GpxDataItemCallback itemCallback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				addAppearanceToGpx(gpxFile, item);
				saveAndShareGpx(app, gpxFile);
			}
		};
		return app.getGpxDbHelper().getItem(new File(gpxFile.path), itemCallback);
	}

	private static void addAppearanceToGpx(@NonNull GPXFile gpxFile, @NonNull GpxDataItem dataItem) {
		gpxFile.setShowArrows(dataItem.isShowArrows());
		gpxFile.setShowStartFinish(dataItem.isShowStartFinish());
		gpxFile.setSplitInterval(dataItem.getSplitInterval());
		gpxFile.setSplitType(GpxSplitType.getSplitTypeByTypeId(dataItem.getSplitType()).getTypeName());
		if (dataItem.getColor() != 0) {
			gpxFile.setColor(dataItem.getColor());
		}
		if (dataItem.getWidth() != null) {
			gpxFile.setWidth(dataItem.getWidth());
		}
		if (dataItem.getColoringType() != null) {
			gpxFile.setColoringType(dataItem.getColoringType());
		}
		GpsFilter.writeValidFilterValuesToExtensions(gpxFile.getExtensionsToWrite(), dataItem);
	}

	public static void shareGpx(@NonNull Context context, @NonNull File file) {
		Uri fileUri = AndroidUtils.getUriForFile(context, file);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, fileUri);
		intent.setType("application/gpx+xml");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (context instanceof OsmandApplication) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		Intent chooserIntent = Intent.createChooser(intent, context.getString(R.string.shared_string_share));
		AndroidUtils.startActivityIfSafe(context, chooserIntent);
	}

	@NonNull
	public static String getGpxFileRelativePath(@NonNull OsmandApplication app, @NonNull String fullPath) {
		String rootGpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath() + '/';
		return fullPath.replace(rootGpxDir, "");
	}

	@Nullable
	public static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, @NonNull GPXFile gpxFile,
	                                          float time, boolean preciseLocation, boolean joinSegments) {
		if (!segment.generalSegment || joinSegments) {
			return getSegmentPointByTime(segment, time, 0, preciseLocation);
		}

		long passedSegmentsTime = 0;
		for (Track track : gpxFile.tracks) {
			if (track.generalTrack) {
				continue;
			}

			for (TrkSegment seg : track.segments) {
				WptPt point = getSegmentPointByTime(seg, time, passedSegmentsTime, preciseLocation);
				if (point != null) {
					return point;
				}

				long segmentStartTime = Algorithms.isEmpty(seg.points) ? 0 : seg.points.get(0).time;
				long segmentEndTime = Algorithms.isEmpty(seg.points) ?
						0 : seg.points.get(seg.points.size() - 1).time;
				passedSegmentsTime += segmentEndTime - segmentStartTime;
			}
		}

		return null;
	}

	@Nullable
	private static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, float timeToPoint,
	                                           long passedSegmentsTime, boolean preciseLocation) {
		WptPt previousPoint = null;
		long segmentStartTime = segment.points.get(0).time;
		for (WptPt currentPoint : segment.points) {
			long totalPassedTime = passedSegmentsTime + currentPoint.time - segmentStartTime;
			if (totalPassedTime >= timeToPoint) {
				return preciseLocation && previousPoint != null
						? getIntermediatePointByTime(totalPassedTime, timeToPoint, previousPoint, currentPoint)
						: currentPoint;
			}
			previousPoint = currentPoint;
		}
		return null;
	}

	@NonNull
	private static WptPt getIntermediatePointByTime(double passedTime, double timeToPoint,
	                                                WptPt prevPoint, WptPt currPoint) {
		double percent = 1 - (passedTime - timeToPoint) / (currPoint.time - prevPoint.time);
		double dLat = (currPoint.lat - prevPoint.lat) * percent;
		double dLon = (currPoint.lon - prevPoint.lon) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.lat = prevPoint.lat + dLat;
		intermediatePoint.lon = prevPoint.lon + dLon;
		return intermediatePoint;
	}

	@Nullable
	public static WptPt getSegmentPointByDistance(@NonNull TrkSegment segment, @NonNull GPXFile gpxFile,
	                                              float distanceToPoint, boolean preciseLocation,
	                                              boolean joinSegments) {
		double passedDistance = 0;

		if (!segment.generalSegment || joinSegments) {
			WptPt prevPoint = null;
			for (int i = 0; i < segment.points.size(); i++) {
				WptPt currPoint = segment.points.get(i);
				if (prevPoint != null) {
					passedDistance += MapUtils.getDistance(prevPoint.lat, prevPoint.lon, currPoint.lat, currPoint.lon);
				}
				if (currPoint.distance >= distanceToPoint || Math.abs(passedDistance - distanceToPoint) < 0.1) {
					return preciseLocation && prevPoint != null && currPoint.distance >= distanceToPoint
							? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
							: currPoint;
				}
				prevPoint = currPoint;
			}
		}

		double passedSegmentsPointsDistance = 0;
		WptPt prevPoint = null;
		for (Track track : gpxFile.tracks) {
			if (track.generalTrack) {
				continue;
			}

			for (TrkSegment seg : track.segments) {
				if (Algorithms.isEmpty(seg.points)) {
					continue;
				}
				for (WptPt currPoint : seg.points) {
					if (prevPoint != null) {
						passedDistance += MapUtils.getDistance(prevPoint.lat, prevPoint.lon,
								currPoint.lat, currPoint.lon);
					}

					if (passedSegmentsPointsDistance + currPoint.distance >= distanceToPoint
							|| Math.abs(passedDistance - distanceToPoint) < 0.1) {
						return preciseLocation && prevPoint != null
								&& currPoint.distance + passedSegmentsPointsDistance >= distanceToPoint
								? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
								: currPoint;
					}
					prevPoint = currPoint;
				}
				prevPoint = null;
				passedSegmentsPointsDistance += seg.points.get(seg.points.size() - 1).distance;
			}
		}
		return null;
	}

	@NonNull
	private static WptPt getIntermediatePointByDistance(double passedDistance, double distanceToPoint,
	                                                    WptPt currPoint, WptPt prevPoint) {
		double percent = 1 - (passedDistance - distanceToPoint) / (currPoint.distance - prevPoint.distance);
		double dLat = (currPoint.lat - prevPoint.lat) * percent;
		double dLon = (currPoint.lon - prevPoint.lon) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.lat = prevPoint.lat + dLat;
		intermediatePoint.lon = prevPoint.lon + dLon;
		return intermediatePoint;
	}

	public static class GPXInfo {
		private final String fileName;
		private final long lastModified;
		private final long fileSize;
		private boolean selected;

		public GPXInfo(String fileName, long lastModified, long fileSize) {
			this.fileName = fileName;
			this.lastModified = lastModified;
			this.fileSize = fileSize;
		}

		public String getFileName() {
			return fileName;
		}

		public long getLastModified() {
			return lastModified;
		}

		public long getFileSize() {
			return fileSize;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}
}
