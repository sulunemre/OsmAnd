package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.Location;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapMarkersLayer extends OsmandMapLayer implements IContextMenuProvider,
		IContextMenuProviderSelection, ContextMenuLayer.IMoveObjectProvider {

	private static final long USE_FINGER_LOCATION_DELAY = 1000;
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 6;
	protected static final int DIST_TO_SHOW = 80;

	private OsmandMapTileView view;

	private MarkersWidgetsHelper markersWidgetsHelper;

	private Paint bitmapPaint;
	private Bitmap markerBitmapBlue;
	private Bitmap markerBitmapGreen;
	private Bitmap markerBitmapOrange;
	private Bitmap markerBitmapRed;
	private Bitmap markerBitmapYellow;
	private Bitmap markerBitmapTeal;
	private Bitmap markerBitmapPurple;

	private Paint bitmapPaintDestBlue;
	private Paint bitmapPaintDestGreen;
	private Paint bitmapPaintDestOrange;
	private Paint bitmapPaintDestRed;
	private Paint bitmapPaintDestYellow;
	private Paint bitmapPaintDestTeal;
	private Paint bitmapPaintDestPurple;
	private Bitmap arrowLight;
	private Bitmap arrowToDestination;
	private Bitmap arrowShadow;
	private final float[] calculations = new float[2];

	private final TextPaint textPaint = new TextPaint();
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private final RenderingLineAttributes textAttrs = new RenderingLineAttributes("rulerLineFont");
	private final RenderingLineAttributes planRouteAttrs = new RenderingLineAttributes("markerPlanRouteline");
	private TrkSegment route;

	private float textSize;
	private float verticalOffset;

	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private final Path linePath = new Path();

	private LatLon fingerLocation;
	private boolean hasMoved;
	private boolean moving;
	private boolean useFingerLocation;
	private GestureDetector longTapDetector;
	private Handler handler;

	private ContextMenuLayer contextMenuLayer;

	private boolean inPlanRouteMode;
	private boolean defaultAppMode = true;
	private boolean carView;
	private float textScale = 1f;
	private double markerSizePx;

	private final List<Amenity> amenities = new ArrayList<>();

	public MapMarkersLayer(@NonNull Context context) {
		super(context);
	}

	public MarkersWidgetsHelper getMarkersWidgetsHelper() {
		return markersWidgetsHelper;
	}

	public boolean isInPlanRouteMode() {
		return inPlanRouteMode;
	}

	public void setInPlanRouteMode(boolean inPlanRouteMode) {
		this.inPlanRouteMode = inPlanRouteMode;
	}

	public void setDefaultAppMode(boolean defaultAppMode) {
		this.defaultAppMode = defaultAppMode;
	}

	private void initUI() {
		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);

		updateBitmaps(true);

		bitmapPaintDestBlue = createPaintDest(R.color.marker_blue);
		bitmapPaintDestGreen = createPaintDest(R.color.marker_green);
		bitmapPaintDestOrange = createPaintDest(R.color.marker_orange);
		bitmapPaintDestRed = createPaintDest(R.color.marker_red);
		bitmapPaintDestYellow = createPaintDest(R.color.marker_yellow);
		bitmapPaintDestTeal = createPaintDest(R.color.marker_teal);
		bitmapPaintDestPurple = createPaintDest(R.color.marker_purple);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			markersWidgetsHelper = new MarkersWidgetsHelper(mapActivity);
			longTapDetector = new GestureDetector(mapActivity, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public void onLongPress(MotionEvent e) {
					cancelFingerAction();
				}
			});
		} else {
			markersWidgetsHelper = null;
			longTapDetector = null;
		}
	}

	private Paint createPaintDest(int colorId) {
		Paint paint = new Paint();
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		int color = ContextCompat.getColor(getContext(), colorId);
		paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
		return paint;
	}

	private Paint getMarkerDestPaint(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return bitmapPaintDestBlue;
			case 1:
				return bitmapPaintDestGreen;
			case 2:
				return bitmapPaintDestOrange;
			case 3:
				return bitmapPaintDestRed;
			case 4:
				return bitmapPaintDestYellow;
			case 5:
				return bitmapPaintDestTeal;
			case 6:
				return bitmapPaintDestPurple;
			default:
				return bitmapPaintDestBlue;
		}
	}

	private Bitmap getMapMarkerBitmap(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return markerBitmapBlue;
			case 1:
				return markerBitmapGreen;
			case 2:
				return markerBitmapOrange;
			case 3:
				return markerBitmapRed;
			case 4:
				return markerBitmapYellow;
			case 5:
				return markerBitmapTeal;
			case 6:
				return markerBitmapPurple;
			default:
				return markerBitmapBlue;
		}
	}

	public void setRoute(TrkSegment route) {
		this.route = route;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		handler = new Handler();
		initUI();
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();
		if (!settings.SHOW_MAP_MARKERS.get()) {
			return;
		}

		Location myLoc;
		if (useFingerLocation && fingerLocation != null) {
			myLoc = new Location("");
			myLoc.setLatitude(fingerLocation.getLatitude());
			myLoc.setLongitude(fingerLocation.getLongitude());
		} else {
			myLoc = app.getLocationProvider().getLastStaleKnownLocation();
		}
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> activeMapMarkers = markersHelper.getMapMarkers();
		int displayedWidgets = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();

		if (route != null && route.points.size() > 0) {
			planRouteAttrs.updatePaints(app, nightMode, tileBox);
			new Renderable.StandardTrack(new ArrayList<>(route.points), 17.2).
					drawSegment(view.getZoom(), defaultAppMode ? planRouteAttrs.paint : planRouteAttrs.paint2, canvas, tileBox);
		}

		if (settings.SHOW_LINES_TO_FIRST_MARKERS.get() && myLoc != null) {
			textAttrs.paint.setTextSize(textSize);
			textAttrs.paint2.setTextSize(textSize);

			lineAttrs.updatePaints(app, nightMode, tileBox);
			textAttrs.updatePaints(app, nightMode, tileBox);
			textAttrs.paint.setStyle(Paint.Style.FILL);

			textPaint.set(textAttrs.paint);

			boolean drawMarkerName = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1;

			float locX;
			float locY;
			MapViewTrackingUtilities mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
			if (mapViewTrackingUtilities.isMapLinkedToLocation()
					&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(myLoc)
					&& !mapViewTrackingUtilities.isMovingToMyLocation()) {
				locX = tileBox.getPixXFromLatLon(tileBox.getLatitude(), tileBox.getLongitude());
				locY = tileBox.getPixYFromLatLon(tileBox.getLatitude(), tileBox.getLongitude());
			} else {
				locX = tileBox.getPixXFromLatLon(myLoc.getLatitude(), myLoc.getLongitude());
				locY = tileBox.getPixYFromLatLon(myLoc.getLatitude(), myLoc.getLongitude());
			}
			int[] colors = MapMarker.getColors(getContext());
			for (int i = 0; i < activeMapMarkers.size() && i < displayedWidgets; i++) {
				MapMarker marker = activeMapMarkers.get(i);
				float markerX = tileBox.getPixXFromLatLon(marker.getLatitude(), marker.getLongitude());
				float markerY = tileBox.getPixYFromLatLon(marker.getLatitude(), marker.getLongitude());

				linePath.reset();
				tx.clear();
				ty.clear();

				tx.add(locX);
				ty.add(locY);
				tx.add(markerX);
				ty.add(markerY);

				GeometryWay.calculatePath(tileBox, tx, ty, linePath);
				PathMeasure pm = new PathMeasure(linePath, false);
				float[] pos = new float[2];
				pm.getPosTan(pm.getLength() / 2, pos, null);

				float dist = (float) MapUtils.getDistance(myLoc.getLatitude(), myLoc.getLongitude(), marker.getLatitude(), marker.getLongitude());
				String distSt = OsmAndFormatter.getFormattedDistance(dist, view.getApplication());
				String text = distSt + (drawMarkerName ? " • " + marker.getName(getContext()) : "");
				text = TextUtils.ellipsize(text, textPaint, pm.getLength(), TextUtils.TruncateAt.END).toString();
				Rect bounds = new Rect();
				textAttrs.paint.getTextBounds(text, 0, text.length(), bounds);
				float hOffset = pm.getLength() / 2 - bounds.width() / 2f;
				lineAttrs.paint.setColor(colors[marker.colorIndex]);

				canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
				canvas.drawPath(linePath, lineAttrs.paint);
				if (locX >= markerX) {
					canvas.rotate(180, pos[0], pos[1]);
					canvas.drawTextOnPath(text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint2);
					canvas.drawTextOnPath(text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint);
					canvas.rotate(-180, pos[0], pos[1]);
				} else {
					canvas.drawTextOnPath(text, linePath, hOffset, -verticalOffset, textAttrs.paint2);
					canvas.drawTextOnPath(text, linePath, hOffset, -verticalOffset, textAttrs.paint);
				}
				canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			}
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		if (markersWidgetsHelper != null) {
			markersWidgetsHelper.setCustomLatLon(useFingerLocation ? fingerLocation : null);
		}
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();

		if (tileBox.getZoom() < 3 || !settings.SHOW_MAP_MARKERS.get()) {
			return;
		}

		int displayedWidgets = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		updateBitmaps(false);

		for (MapMarker marker : markersHelper.getMapMarkers()) {
			if (isMarkerVisible(tileBox, marker) && !overlappedByWaypoint(marker)
					&& !isInMotion(marker) && !isSynced(marker)) {
				Bitmap bmp = getMapMarkerBitmap(marker.colorIndex);
				int marginX = bmp.getWidth() / 6;
				int marginY = bmp.getHeight();
				int locationX = tileBox.getPixXFromLonNoRot(marker.getLongitude());
				int locationY = tileBox.getPixYFromLatNoRot(marker.getLatitude());
				canvas.rotate(-tileBox.getRotate(), locationX, locationY);
				canvas.drawBitmap(bmp, locationX - marginX, locationY - marginY, bitmapPaint);
				canvas.rotate(tileBox.getRotate(), locationX, locationY);
			}
		}

		if (settings.SHOW_ARROWS_TO_FIRST_MARKERS.get()) {
			LatLon loc = tileBox.getCenterLatLon();
			int i = 0;
			for (MapMarker marker : markersHelper.getMapMarkers()) {
				if (!isLocationVisible(tileBox, marker) && !isInMotion(marker)) {
					canvas.save();
					net.osmand.Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
							marker.getLatitude(), marker.getLongitude(), calculations);
					float bearing = calculations[1] - 90;
					float radiusBearing = DIST_TO_SHOW * tileBox.getDensity();
					final QuadPoint cp = tileBox.getCenterPixelPoint();
					canvas.rotate(bearing, cp.x, cp.y);
					canvas.translate(-24 * tileBox.getDensity() + radiusBearing, -22 * tileBox.getDensity());
					canvas.drawBitmap(arrowShadow, cp.x, cp.y, bitmapPaint);
					canvas.drawBitmap(arrowToDestination, cp.x, cp.y, getMarkerDestPaint(marker.colorIndex));
					canvas.drawBitmap(arrowLight, cp.x, cp.y, bitmapPaint);
					canvas.restore();
				}
				i++;
				if (i > displayedWidgets - 1) {
					break;
				}
			}
		}

		if (contextMenuLayer.getMoveableObject() instanceof MapMarker) {
			MapMarker objectInMotion = (MapMarker) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			Bitmap bitmap = getMapMarkerBitmap(objectInMotion.colorIndex);
			int marginX = bitmap.getWidth() / 6;
			int marginY = bitmap.getHeight();
			float locationX = pf.x;
			float locationY = pf.y;
			canvas.rotate(-tileBox.getRotate(), locationX, locationY);
			canvas.drawBitmap(bitmap, locationX - marginX, locationY - marginY, bitmapPaint);

		}
	}

	private void updateBitmaps(boolean forceUpdate) {
		OsmandApplication app = getApplication();
		float textScale = getTextScale();
		boolean carView = app.getOsmandMap().getMapView().isCarView();
		if (this.textScale != textScale || this.carView != carView || forceUpdate) {
			this.textScale = textScale;
			this.carView = carView;
			recreateBitmaps();
			textSize = app.getResources().getDimensionPixelSize(R.dimen.guide_line_text_size) * textScale;
			verticalOffset = app.getResources().getDimensionPixelSize(R.dimen.guide_line_vertical_offset) * textScale;
		}
	}

	private void recreateBitmaps() {
		markerBitmapBlue = getScaledBitmap(R.drawable.map_marker_blue);
		markerBitmapGreen = getScaledBitmap(R.drawable.map_marker_green);
		markerBitmapOrange = getScaledBitmap(R.drawable.map_marker_orange);
		markerBitmapRed = getScaledBitmap(R.drawable.map_marker_red);
		markerBitmapYellow = getScaledBitmap(R.drawable.map_marker_yellow);
		markerBitmapTeal = getScaledBitmap(R.drawable.map_marker_teal);
		markerBitmapPurple = getScaledBitmap(R.drawable.map_marker_purple);

		markerSizePx = Math.sqrt(markerBitmapBlue.getWidth() * markerBitmapBlue.getWidth()
				+ markerBitmapBlue.getHeight() * markerBitmapBlue.getHeight());

		arrowLight = getScaledBitmap(R.drawable.map_marker_direction_arrow_p1_light);
		arrowToDestination = getScaledBitmap(R.drawable.map_marker_direction_arrow_p2_color);
		arrowShadow = getScaledBitmap(R.drawable.map_marker_direction_arrow_p3_shadow);
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return getScaledBitmap(drawableId, textScale);
	}

	private boolean isSynced(@NonNull MapMarker marker) {
		return marker.wptPt != null || marker.favouritePoint != null;
	}

	private boolean isInMotion(@NonNull MapMarker marker) {
		return marker.equals(contextMenuLayer.getMoveableObject());
	}

	public boolean isLocationVisible(RotatedTileBox tb, MapMarker marker) {
		//noinspection SimplifiableIfStatement
		if (marker == null || tb == null) {
			return false;
		}
		return containsLatLon(tb, marker.getLatitude(), marker.getLongitude(), 0, 0);
	}

	public boolean isMarkerVisible(RotatedTileBox tb, MapMarker marker) {
		//noinspection SimplifiableIfStatement
		if (marker == null || tb == null) {
			return false;
		}
		return containsLatLon(tb, marker.getLatitude(), marker.getLongitude(), markerSizePx, markerSizePx);
	}

	public boolean containsLatLon(RotatedTileBox tb, double lat, double lon, double w, double h) {
		double widgetHeight = 0;
		if (markersWidgetsHelper != null
				&& markersWidgetsHelper.isMapMarkersBarWidgetVisible()
				&& !getApplication().getOsmandMap().getMapView().isCarView()) {
			widgetHeight = markersWidgetsHelper.getMapMarkersBarWidgetHeight();
		}
		double tx = tb.getPixXFromLatLon(lat, lon);
		double ty = tb.getPixYFromLatLon(lat, lon);
		return tx >= -w && tx <= tb.getPixWidth() + w && ty >= widgetHeight - h && ty <= tb.getPixHeight() + h;
	}

	public boolean overlappedByWaypoint(MapMarker marker) {
		List<TargetPoint> targetPoints = getApplication().getTargetPointsHelper().getAllPoints();
		for (TargetPoint t : targetPoints) {
			if (t.point.equals(marker.point)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		if (!longTapDetector.onTouchEvent(event)) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					float x = event.getX();
					float y = event.getY();
					fingerLocation = tileBox.getLatLonFromPixel(x, y);
					hasMoved = false;
					moving = true;
					break;

				case MotionEvent.ACTION_MOVE:
					if (!hasMoved) {
						if (!handler.hasMessages(MAP_REFRESH_MESSAGE)) {
							Message msg = Message.obtain(handler, () -> {
								handler.removeMessages(MAP_REFRESH_MESSAGE);
								if (moving) {
									if (!useFingerLocation) {
										useFingerLocation = true;
										getApplication().getOsmandMap().refreshMap();
									}
								}
							});
							msg.what = MAP_REFRESH_MESSAGE;
							handler.sendMessageDelayed(msg, USE_FINGER_LOCATION_DELAY);
						}
						hasMoved = true;
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					cancelFingerAction();
					break;
			}
		}
		return super.onTouchEvent(event, tileBox);
	}

	private void cancelFingerAction() {
		handler.removeMessages(MAP_REFRESH_MESSAGE);
		useFingerLocation = false;
		moving = false;
		fingerLocation = null;
		getApplication().getOsmandMap().refreshMap();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean disableSingleTap() {
		return inPlanRouteMode;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return inPlanRouteMode;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		MapActivity mapActivity = getMapActivity();
		OsmandSettings settings = getApplication().getSettings();
		if (unknownLocation
				|| mapActivity == null
				|| !(o instanceof MapMarker)
				|| !settings.SELECT_MARKER_ON_SINGLE_TAP.get()
				|| !settings.SHOW_MAP_MARKERS.get()) {
			return false;
		}
		final MapMarkersHelper helper = getApplication().getMapMarkersHelper();
		final MapMarker old = helper.getMapMarkers().get(0);
		helper.moveMarkerToTop((MapMarker) o);
		String title = getContext().getString(R.string.marker_activated, helper.getMapMarkers().get(0).getName(getContext()));
		Snackbar.make(mapActivity.findViewById(R.id.bottomFragmentContainer), title, Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_cancel, v -> helper.moveMarkerToTop(old))
				.show();
		return true;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (tileBox.getZoom() < 3 || !getApplication().getSettings().SHOW_MAP_MARKERS.get()) {
			return;
		}
		amenities.clear();
		OsmandApplication app = getApplication();
		int r = getDefaultRadiusPoi(tileBox);
		boolean selectMarkerOnSingleTap = app.getSettings().SELECT_MARKER_ON_SINGLE_TAP.get();

		for (MapMarker marker : app.getMapMarkersHelper().getMapMarkers()) {
			if ((!unknownLocation && selectMarkerOnSingleTap) || !isSynced(marker)) {
				LatLon latLon = marker.point;
				if (latLon != null) {
					int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());

					if (calculateBelongs((int) point.x, (int) point.y, x, y, r)) {
						if (!unknownLocation && selectMarkerOnSingleTap) {
							o.add(marker);
						} else {
							if (isMarkerOnFavorite(marker) && app.getSettings().SHOW_FAVORITES.get()
									|| isMarkerOnWaypoint(marker) && app.getSettings().SHOW_WPT.get()) {
								continue;
							}
							Amenity mapObj = getMapObjectByMarker(marker);
							if (mapObj != null) {
								amenities.add(mapObj);
								o.add(mapObj);
							} else {
								o.add(marker);
							}
						}
					}
				}
			}
		}
	}

	private boolean isMarkerOnWaypoint(@NonNull MapMarker marker) {
		return marker.point != null && getApplication().getSelectedGpxHelper().getVisibleWayPointByLatLon(marker.point) != null;
	}

	private boolean isMarkerOnFavorite(@NonNull MapMarker marker) {
		return marker.point != null && getApplication().getFavoritesHelper().getVisibleFavByLatLon(marker.point) != null;
	}

	@Nullable
	public Amenity getMapObjectByMarker(@NonNull MapMarker marker) {
		if (marker.mapObjectName != null && marker.point != null) {
			String mapObjName = marker.mapObjectName.split("_")[0];
			return findAmenity(getApplication(), -1, Collections.singletonList(mapObjName), marker.point, 15);
		}
		return null;
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius * 1.5 && (ey - objy) <= radius * 1.5 && (objy - ey) <= 2.5 * radius;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).point;
		} else if (o instanceof Amenity && amenities.contains(o)) {
			return ((Amenity) o).getLocation();
		}
		return null;
	}


	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).getPointDescription(view.getContext());
		}
		return null;
	}

	@Override
	public int getOrder(Object o) {
		return 0;
	}

	@Override
	public void setSelectedObject(Object o) {
	}

	@Override
	public void clearSelectedObject() {
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof MapMarker;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
									   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		MapMarker newObject = null;
		if (o instanceof MapMarker) {
			MapMarkersHelper markersHelper = getApplication().getMapMarkersHelper();
			MapMarker marker = (MapMarker) o;

			PointDescription originalDescription = marker.getOriginalPointDescription();
			if (originalDescription.isLocation()) {
				originalDescription.setName(PointDescription.getSearchAddressStr(getContext()));
			}
			markersHelper.moveMapMarker(marker, position);
			int index = markersHelper.getMapMarkers().indexOf(marker);
			if (index != -1) {
				newObject = markersHelper.getMapMarkers().get(index);
			}
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, newObject == null ? o : newObject);
		}
	}
}
