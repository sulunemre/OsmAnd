package net.osmand.plus.views.layers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.RenderingContext;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AmenitySymbolsProvider.AmenitySymbolsGroup;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IBillboardMapSymbol;
import net.osmand.core.jni.IMapRenderer.MapSymbolInformation;
import net.osmand.core.jni.MapObject;
import net.osmand.core.jni.MapObjectsSymbolsProvider.MapObjectSymbolsGroup;
import net.osmand.core.jni.MapSymbolInformationList;
import net.osmand.core.jni.MapSymbolsGroup.AdditionalBillboardSymbolInstanceParameters;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.Utilities;
import net.osmand.data.Amenity;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.MoveMarkerBottomSheetHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;

public class ContextMenuLayer extends OsmandMapLayer {

	//private static final Log LOG = PlatformUtil.getLog(ContextMenuLayer.class);
	public static final int VIBRATE_SHORT = 100;
	private static final int AMENITY_SEARCH_RADIUS = 50;
	private static final int ROUTE_SEARCH_RADIUS_PX = 10;

	private OsmandMapTileView view;

	private MapContextMenu menu;
	private MapMultiSelectionMenu multiSelectionMenu;
	private CallbackWithObject<LatLon> selectOnMap = null;
	private MapQuickActionLayer mapQuickActionLayer;

	private ImageView contextMarker;
	private Paint paint;
	private Paint outlinePaint;
	private Map<LatLon, BackgroundType> pressedLatLonFull = new HashMap<>();
	private Map<LatLon, BackgroundType> pressedLatLonSmall = new HashMap<>();

	private GestureDetector movementListener;

	private MoveMarkerBottomSheetHelper mMoveMarkerBottomSheetHelper;
	private AddGpxPointBottomSheetHelper mAddGpxPointBottomSheetHelper;
	private boolean mInChangeMarkerPositionMode;
	private IContextMenuProvider selectedObjectContextMenuProvider;
	private boolean cancelApplyingNewMarkerPosition;
	private LatLon applyingMarkerLatLon;
	private boolean mInGpxDetailsMode;
	private boolean mInAddGpxPointMode;

	private List<String> publicTransportTypes;
	private Object selectedObject;

	public ContextMenuLayer(@NonNull Context context) {
		super(context);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			menu = mapActivity.getContextMenu();
			multiSelectionMenu = menu.getMultiSelectionMenu();
			movementListener = new GestureDetector(mapActivity, new MenuLayerOnGestureListener());
			mMoveMarkerBottomSheetHelper = new MoveMarkerBottomSheetHelper(mapActivity, this);
			mAddGpxPointBottomSheetHelper = new AddGpxPointBottomSheetHelper(mapActivity, this);
		} else {
			menu = null;
			multiSelectionMenu = null;
			movementListener = null;
			mMoveMarkerBottomSheetHelper = null;
			mAddGpxPointBottomSheetHelper = null;
		}
	}

	public AddGpxPointBottomSheetHelper getAddGpxPointBottomSheetHelper() {
		return mAddGpxPointBottomSheetHelper;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;

		Context context = view.getContext();
		contextMarker = new ImageView(context);
		contextMarker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		contextMarker.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.map_pin_context_menu));
		contextMarker.setClickable(true);
		int minw = contextMarker.getDrawable().getMinimumWidth();
		int minh = contextMarker.getDrawable().getMinimumHeight();
		contextMarker.layout(0, 0, minw, minh);

		paint = new Paint();
		paint.setColor(0x7f000000);

		outlinePaint = new Paint();
		outlinePaint.setStyle(Paint.Style.STROKE);
		outlinePaint.setAntiAlias(true);
		outlinePaint.setStrokeWidth(AndroidUtils.dpToPx(getContext(), 2f));
		outlinePaint.setStrokeCap(Paint.Cap.ROUND);
		outlinePaint.setColor(getContext().getResources().getColor(R.color.osmand_orange));
	}

	public Object getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

	@Nullable
	private List<String> getPublicTransportTypes() {
		OsmandApplication app = getApplication();
		if (publicTransportTypes == null && !app.isApplicationInitializing()) {
			PoiCategory category = app.getPoiTypes().getPoiCategoryByName("transportation");
			if (category != null) {
				List<PoiFilter> filters = category.getPoiFilters();
				publicTransportTypes = new ArrayList<>();
				for (PoiFilter poiFilter : filters) {
					if (poiFilter.getKeyName().equals("public_transport")) {
						for (PoiType poiType : poiFilter.getPoiTypes()) {
							publicTransportTypes.add(poiType.getKeyName());
							for (PoiType poiAdditionalType : poiType.getPoiAdditionals()) {
								publicTransportTypes.add(poiAdditionalType.getKeyName());
							}
						}
					}
				}
			}
		}
		return publicTransportTypes;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean markerCustomized = false;
		if (selectedObject != null) {
			TIntArrayList x = null;
			TIntArrayList y = null;
			if (selectedObject instanceof Amenity) {
				Amenity a = (Amenity) selectedObject;
				x = a.getX();
				y = a.getY();
			} else if (selectedObject instanceof RenderedObject) {
				RenderedObject r = (RenderedObject) selectedObject;
				x = r.getX();
				y = r.getY();
			} else if (selectedObject instanceof AidlMapPointWrapper) {
				markerCustomized = true;
			}
			if (x != null && y != null && x.size() > 2) {
				float px, py, prevX, prevY;
				prevX = box.getPixXFrom31(x.get(0), y.get(0));
				prevY = box.getPixYFrom31(x.get(0), y.get(0));
				for (int i = 1; i < x.size(); i++) {
					px = box.getPixXFrom31(x.get(i), y.get(i));
					py = box.getPixYFrom31(x.get(i), y.get(i));
					canvas.drawLine(prevX, prevY, px, py, outlinePaint);
					prevX = px;
					prevY = py;
				}
			}
		}
		float textScale = 1f;
		if (!pressedLatLonSmall.isEmpty() || !pressedLatLonFull.isEmpty()) {
			textScale = getTextScale();
		}
		for (Entry<LatLon, BackgroundType> entry : pressedLatLonSmall.entrySet()) {
			LatLon latLon = entry.getKey();
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			BackgroundType background = entry.getValue();
			Bitmap pressedBitmapSmall = background.getTouchBackground(mapActivity, true);
			Rect destRect = getIconDestinationRect(
					x, y, pressedBitmapSmall.getWidth(), pressedBitmapSmall.getHeight(), textScale);
			canvas.drawBitmap(pressedBitmapSmall, null, destRect, paint);
		}
		for (Entry<LatLon, BackgroundType> entry : pressedLatLonFull.entrySet()) {
			LatLon latLon = entry.getKey();
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());

			BackgroundType background = entry.getValue();
			Bitmap pressedBitmap = background.getTouchBackground(mapActivity, false);
			int offsetY = background.getOffsetY(mapActivity, textScale);
			Rect destRect = getIconDestinationRect(
					x, y - offsetY, pressedBitmap.getWidth(), pressedBitmap.getHeight(), textScale);
			canvas.drawBitmap(pressedBitmap, null, destRect, paint);
		}

		boolean movingMarker = mapQuickActionLayer != null && mapQuickActionLayer.isInMovingMarkerMode();
		boolean downloadingTiles = mapActivity.getDownloadTilesFragment() != null;
		if (movingMarker || downloadingTiles) {
			return;
		}

		if (mInChangeMarkerPositionMode) {
			if (menu != null && menu.getObject() == null) {
				canvas.translate(box.getPixWidth() / 2 - contextMarker.getWidth() / 2, box.getPixHeight() / 2 - contextMarker.getHeight());
				contextMarker.draw(canvas);
			}
			if (mMoveMarkerBottomSheetHelper != null) {
				mMoveMarkerBottomSheetHelper.onDraw(box);
			}
		} else if (mInAddGpxPointMode) {
			canvas.translate(box.getPixWidth() / 2 - contextMarker.getWidth() / 2, box.getPixHeight() / 2 - contextMarker.getHeight());
			contextMarker.draw(canvas);
			if (mAddGpxPointBottomSheetHelper != null) {
				mAddGpxPointBottomSheetHelper.onDraw(box);
			}
		} else if (!markerCustomized) {
			LatLon latLon = null;
			if (menu != null && menu.isActive()) {
				latLon = menu.getLatLon();
			} else if (mapActivity.getTrackMenuFragment() != null) {
				latLon = mapActivity.getTrackMenuFragment().getLatLon();
			}
			if (latLon != null) {
				int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				canvas.translate(x - contextMarker.getWidth() / 2, y - contextMarker.getHeight());
				contextMarker.draw(canvas);
			}
		}
	}

	public void setSelectOnMap(CallbackWithObject<LatLon> selectOnMap) {
		this.selectOnMap = selectOnMap;
	}

	public void updateContextMenu() {
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IMoveObjectProvider && ((IMoveObjectProvider) layer).isObjectMovable(selectedObject)) {
				selectedObjectContextMenuProvider = (IContextMenuProvider) layer;
				break;
			}
		}
	}

	@Override
	public void populateObjectContextMenu(@NonNull LatLon latLon, @Nullable Object o, @NonNull ContextMenuAdapter adapter) {
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
			enterMovingMode(tileBox);
			return true;
		};
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION)
				.setTitleId(R.string.change_markers_position, getContext())
				.setIcon(R.drawable.ic_show_on_map)
				.setOrder(MapActivityActions.CHANGE_POSITION_ITEM_ORDER)
				.setClickable(isObjectMoveable(o))
				.setListener(listener));
	}

	@Override
	@RequiresPermission(Manifest.permission.VIBRATE)
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (menu == null || disableLongPressOnMap(point, tileBox)) {
			return false;
		}
		if (pressedContextMarker(tileBox, point.x, point.y)) {
			Object obj = menu.getObject();
			if (isObjectMoveable(obj)) {
				enterMovingMode(tileBox);
				return true;
			}
			return false;
		}
		hideVisibleMenues();
		LatLon pointLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
		menu.show(pointLatLon, null, null);

		view.refreshMap();
		return true;
	}


	public PointF getMovableCenterPoint(RotatedTileBox tb) {
		if (applyingMarkerLatLon != null) {
			float x = tb.getPixXFromLatLon(applyingMarkerLatLon.getLatitude(), applyingMarkerLatLon.getLongitude());
			float y = tb.getPixYFromLatLon(applyingMarkerLatLon.getLatitude(), applyingMarkerLatLon.getLongitude());
			return new PointF(x, y);
		} else {
			return new PointF(tb.getPixWidth() / 2, tb.getPixHeight() / 2);
		}
	}

	public Object getMoveableObject() {
		return mInChangeMarkerPositionMode ? menu.getObject() : null;
	}

	public boolean isInChangeMarkerPositionMode() {
		return mInChangeMarkerPositionMode;
	}

	public boolean isInGpxDetailsMode() {
		return mInGpxDetailsMode;
	}

	public boolean isInAddGpxPointMode() {
		return mInAddGpxPointMode;
	}

	public boolean isObjectMoveable(Object o) {
		if (o != null && selectedObjectContextMenuProvider != null
				&& selectedObjectContextMenuProvider instanceof IMoveObjectProvider) {
			final IMoveObjectProvider l = (IMoveObjectProvider) selectedObjectContextMenuProvider;
			return l.isObjectMovable(o);
		}
		return false;
	}

	public void applyMovedObject(Object o, LatLon position, ApplyMovedObjectCallback callback) {
		if (selectedObjectContextMenuProvider != null && !isInAddGpxPointMode()) {
			if (selectedObjectContextMenuProvider instanceof IMoveObjectProvider) {
				final IMoveObjectProvider l = (IMoveObjectProvider) selectedObjectContextMenuProvider;
				if (l.isObjectMovable(o)) {
					l.applyNewObjectPosition(o, position, callback);
				}
			}
		} else if (mInChangeMarkerPositionMode || mInAddGpxPointMode) {
			callback.onApplyMovedObject(true, null);
		}
	}

	public void applyNewMarkerPosition() {
		if (!mInChangeMarkerPositionMode) {
			throw new IllegalStateException("Not in change marker position mode");
		}
		if (mMoveMarkerBottomSheetHelper == null) {
			return;
		}

		RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
		PointF newMarkerPosition = getMovableCenterPoint(tileBox);
		final LatLon ll = tileBox.getLatLonFromPixel(newMarkerPosition.x, newMarkerPosition.y);
		applyingMarkerLatLon = ll;

		Object obj = getMoveableObject();
		cancelApplyingNewMarkerPosition = false;
		mMoveMarkerBottomSheetHelper.enterApplyPositionMode();
		applyMovedObject(obj, ll, new ApplyMovedObjectCallback() {
			@Override
			public void onApplyMovedObject(boolean success, @Nullable Object newObject) {
				mMoveMarkerBottomSheetHelper.exitApplyPositionMode();
				if (success && !cancelApplyingNewMarkerPosition) {
					mMoveMarkerBottomSheetHelper.hide();
					quitMovingMarker();
					menu.close();

					view.refreshMap();
				}
				selectedObjectContextMenuProvider = null;
				applyingMarkerLatLon = null;
			}

			@Override
			public boolean isCancelled() {
				return cancelApplyingNewMarkerPosition;
			}
		});
	}

	public void createGpxPoint() {
		if (!mInAddGpxPointMode) {
			throw new IllegalStateException("Not in add gpx point mode");
		}
		if (mAddGpxPointBottomSheetHelper == null) {
			return;
		}

		RotatedTileBox tileBox = getMapView().getCurrentRotatedTileBox();
		PointF newMarkerPosition = getMovableCenterPoint(tileBox);
		final LatLon ll = tileBox.getLatLonFromPixel(newMarkerPosition.x, newMarkerPosition.y);
		applyingMarkerLatLon = ll;

		Object obj = getMoveableObject();
		cancelApplyingNewMarkerPosition = false;
		mAddGpxPointBottomSheetHelper.enterApplyPositionMode();
		applyMovedObject(obj, ll, new ApplyMovedObjectCallback() {
			@Override
			public void onApplyMovedObject(boolean success, @Nullable Object newObject) {
				mAddGpxPointBottomSheetHelper.exitApplyPositionMode();
				if (success && !cancelApplyingNewMarkerPosition) {
					mAddGpxPointBottomSheetHelper.hide();
					quitAddGpxPoint();
					view.refreshMap();
				}
				selectedObjectContextMenuProvider = null;
				applyingMarkerLatLon = null;
			}

			@Override
			public boolean isCancelled() {
				return cancelApplyingNewMarkerPosition;
			}
		});
	}

	public void enterGpxDetailsMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		menu.updateMapCenter(null);
		menu.hide();

		mInGpxDetailsMode = true;
		mapActivity.disableDrawer();
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	public void exitGpxDetailsMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInGpxDetailsMode = false;
		mapActivity.enableDrawer();
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	private void quitMovingMarker() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInChangeMarkerPositionMode = false;
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	public void quitAddGpxPoint() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mInAddGpxPointMode = false;
		AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);
	}

	public void enterAddGpxPointMode(NewGpxPoint newGpxPoint) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mAddGpxPointBottomSheetHelper == null) {
			return;
		}

		menu.updateMapCenter(null);
		menu.hide();

		mapActivity.disableDrawer();

		mInAddGpxPointMode = true;
		mAddGpxPointBottomSheetHelper.show(newGpxPoint);
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		view.refreshMap();
	}

	private void enterMovingMode(RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mMoveMarkerBottomSheetHelper == null) {
			return;
		}

		Vibrator vibrator = (Vibrator) mapActivity.getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null) {
			vibrator.vibrate(VIBRATE_SHORT);
		}

		menu.updateMapCenter(null);
		menu.hide();

		LatLon ll = menu.getLatLon();
		RotatedTileBox rb = new RotatedTileBox(tileBox);
		rb.setCenterLocation(0.5f, 0.5f);
		rb.setLatLonCenter(ll.getLatitude(), ll.getLongitude());
		double lat = rb.getLatFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		double lon = rb.getLonFromPixel(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		view.setLatLon(lat, lon);

		mInChangeMarkerPositionMode = true;
		mMoveMarkerBottomSheetHelper.show(menu.getRightIcon());
		AndroidUiHelper.setVisibility(mapActivity, View.INVISIBLE, R.id.map_ruler_layout,
				R.id.map_left_widgets_panel, R.id.map_right_widgets_panel, R.id.map_center_info);

		view.refreshMap();
	}

	public void cancelMovingMarker() {
		if (menu != null) {
			cancelApplyingNewMarkerPosition = true;
			quitMovingMarker();
			menu.show();
			applyingMarkerLatLon = null;
		}
	}

	public void cancelAddGpxPoint() {
		cancelApplyingNewMarkerPosition = true;
		quitAddGpxPoint();
		applyingMarkerLatLon = null;
	}

	public boolean showContextMenuForMyLocation() {
		PointLocationLayer provider = view.getLayerByClass(PointLocationLayer.class);
		if (provider != null) {
			LatLon ll = provider.getObjectLocation(null);
			if (ll != null) {
				PointDescription pointDescription = provider.getObjectName(null);
				return showContextMenu(ll, pointDescription, ll, provider);
			}
		}
		return false;
	}

	public boolean showContextMenu(double latitude, double longitude, boolean showUnknownLocation) {
		return showContextMenu(getPointFromLatLon(latitude, longitude),
				getMapView().getCurrentRotatedTileBox(), showUnknownLocation);
	}

	public boolean showContextMenu(@NonNull LatLon latLon,
								   @Nullable PointDescription pointDescription,
								   @Nullable Object object,
								   @Nullable IContextMenuProvider provider) {
		if (mInAddGpxPointMode) {
			String title = pointDescription == null ? "" : pointDescription.getName();
			if (mAddGpxPointBottomSheetHelper != null) {
				mAddGpxPointBottomSheetHelper.setTitle(title);
			}
			view.getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), view.getZoom(), true);
		} else if (provider == null || !provider.showMenuAction(object)) {
			selectedObjectContextMenuProvider = provider;
			hideVisibleMenues();
			getApplication().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (!getMapView().getCurrentRotatedTileBox().containsLatLon(latLon)) {
				menu.setMapCenter(latLon);
				menu.setMapPosition(getMapView().getMapPosition());
				menu.setCenterMarker(true);
			}
			menu.show(latLon, pointDescription, object);
		}
		return true;
	}

	private boolean showContextMenu(PointF point, RotatedTileBox tileBox, boolean showUnknownLocation) {
		if (menu == null || mAddGpxPointBottomSheetHelper == null) {
			return false;
		}

		LatLon objectLatLon = null;
		Map<Object, IContextMenuProvider> selectedObjects = selectObjectsForContextMenu(tileBox, point, false, showUnknownLocation);
		NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
		LatLon pointLatLon = tileBox.getLatLonFromPixel(point.x, point.y);
		OsmandApplication app = getApplication();
		IContextMenuProvider poiMenuProvider = app.getOsmandMap().getMapLayers().getPoiMapLayer();
		IContextMenuProvider gpxMenuProvider = app.getOsmandMap().getMapLayers().getGpxLayer();

		if (app.getSettings().USE_OPENGL_RENDER.get() && NativeCoreContext.isInit()) {
			MapRendererView rendererView = view.getMapRenderer();
			if (rendererView != null) {
				int delta = 20;
				PointI tl = new PointI((int) point.x - delta, (int) point.y - delta);
				PointI br = new PointI((int) point.x + delta, (int) point.y + delta);
				MapSymbolInformationList symbols = rendererView.getSymbolsIn(new AreaI(tl, br), false);
				for (int i = 0; i < symbols.size(); i++) {
					MapSymbolInformation symbolInfo = symbols.get(i);
					IBillboardMapSymbol billboardMapSymbol;
					try {
						billboardMapSymbol = IBillboardMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
					} catch (Exception eBillboard) {
						billboardMapSymbol = null;
					}
					if (billboardMapSymbol != null) {
						double lat = Utilities.get31LatitudeY(billboardMapSymbol.getPosition31().getY());
						double lon = Utilities.get31LongitudeX(billboardMapSymbol.getPosition31().getX());
						objectLatLon = new LatLon(lat, lon);

						AdditionalBillboardSymbolInstanceParameters billboardAdditionalParams;
						try {
							billboardAdditionalParams = AdditionalBillboardSymbolInstanceParameters
									.dynamic_pointer_cast(symbolInfo.getInstanceParameters());
						} catch (Exception eBillboardParams) {
							billboardAdditionalParams = null;
						}
						if (billboardAdditionalParams != null && billboardAdditionalParams.getOverridesPosition31()) {
							lat = Utilities.get31LatitudeY(billboardAdditionalParams.getPosition31().getY());
							lon = Utilities.get31LongitudeX(billboardAdditionalParams.getPosition31().getX());
							objectLatLon = new LatLon(lat, lon);
						}

						Amenity amenity = null;
						net.osmand.core.jni.Amenity jniAmenity;
						try {
							jniAmenity = AmenitySymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getAmenity();
						} catch (Exception eAmenity) {
							jniAmenity = null;
						}
						if (jniAmenity != null) {
							List<String> names = getValues(jniAmenity.getLocalizedNames());
							names.add(jniAmenity.getNativeName());
							long id = jniAmenity.getId().getId().longValue() >> 7;
							amenity = findAmenity(app, id, names, objectLatLon, AMENITY_SEARCH_RADIUS);
						} else {
							MapObject mapObject;
							try {
								mapObject = MapObjectSymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getMapObject();
							} catch (Exception eMapObject) {
								mapObject = null;
							}
							if (mapObject != null) {
								ObfMapObject obfMapObject;
								try {
									obfMapObject = ObfMapObject.dynamic_pointer_cast(mapObject);
								} catch (Exception eObfMapObject) {
									obfMapObject = null;
								}
								if (obfMapObject != null) {
									List<String> names = getValues(obfMapObject.getCaptionsInAllLanguages());
									names.add(obfMapObject.getCaptionInNativeLanguage());
									long id = obfMapObject.getId().getId().longValue() >> 7;
									amenity = findAmenity(app, id, names, objectLatLon, AMENITY_SEARCH_RADIUS);
								}
							}
						}
						if (amenity != null && isUniqueAmenity(selectedObjects.keySet(), amenity)) {
							selectedObjects.put(amenity, poiMenuProvider);
						}
					}
				}
			}
		} else if (nativeLib != null) {
			MapRenderRepositories maps = app.getResourceManager().getRenderer();
			RenderingContext rc = maps.getVisibleRenderingContext();
			RenderedObject[] renderedObjects = null;
			if (rc != null && rc.zoom == tileBox.getZoom()) {
				double sinRotate = Math.sin(Math.toRadians(rc.rotate - tileBox.getRotate()));
				double cosRotate = Math.cos(Math.toRadians(rc.rotate - tileBox.getRotate()));
				float x = tileBox.getPixXFrom31((int) (rc.leftX * rc.tileDivisor), (int) (rc.topY * rc.tileDivisor));
				float y = tileBox.getPixYFrom31((int) (rc.leftX * rc.tileDivisor), (int) (rc.topY * rc.tileDivisor));
				float dx = point.x - x;
				float dy = point.y - y;
				int coordX = (int) (dx * cosRotate - dy * sinRotate);
				int coordY = (int) (dy * cosRotate + dx * sinRotate);

				renderedObjects = nativeLib.searchRenderedObjectsFromContext(rc, coordX, coordY, true);
			}
			if (renderedObjects != null) {
				int TILE_SIZE = 256;
				double cosRotateTileSize = Math.cos(Math.toRadians(rc.rotate)) * TILE_SIZE;
				double sinRotateTileSize = Math.sin(Math.toRadians(rc.rotate)) * TILE_SIZE;

				for (RenderedObject renderedObject : renderedObjects) {

					String routeID = renderedObject.getRouteID();
					String fileName = renderedObject.getGpxFileName();
					String filter = routeID != null ? routeID : fileName;
					List<RouteKey> routeKeys = RouteType.getRouteStringKeys(renderedObject);

					boolean isTravelGpx = !Algorithms.isEmpty(filter);
					boolean isRouteGpx = !Algorithms.isEmpty(routeKeys);
					if (!isTravelGpx && !isRouteGpx && (renderedObject.getId() == null
							|| !renderedObject.isVisible() || renderedObject.isDrawOnPath())) {
						continue;
					}

					if (renderedObject.getLabelX() != 0 && renderedObject.getLabelY() != 0) {
						double lat = MapUtils.get31LatitudeY(renderedObject.getLabelY());
						double lon = MapUtils.get31LongitudeX(renderedObject.getLabelX());
						renderedObject.setLabelLatLon(new LatLon(lat, lon));
					} else {
						double cx = renderedObject.getBbox().centerX();
						double cy = renderedObject.getBbox().centerY();
						double dTileX = (cx * cosRotateTileSize + cy * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
						double dTileY = (cy * cosRotateTileSize - cx * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
						int x31 = (int) ((dTileX + rc.leftX) * rc.tileDivisor);
						int y31 = (int) ((dTileY + rc.topY) * rc.tileDivisor);
						double lat = MapUtils.get31LatitudeY(y31);
						double lon = MapUtils.get31LongitudeX(x31);
						renderedObject.setLabelLatLon(new LatLon(lat, lon));
					}

					if (renderedObject.getX() != null && renderedObject.getX().size() == 1
							&& renderedObject.getY() != null && renderedObject.getY().size() == 1) {
						objectLatLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)),
								MapUtils.get31LongitudeX(renderedObject.getX().get(0)));
					} else if (renderedObject.getLabelLatLon() != null) {
						objectLatLon = renderedObject.getLabelLatLon();
					}
					LatLon searchLatLon = objectLatLon != null ? objectLatLon : pointLatLon;
					if (isTravelGpx) {
						TravelGpx travelGpx = app.getTravelHelper().searchGpx(pointLatLon, filter,
								renderedObject.getTagValue("ref"));
						if (travelGpx != null && isUniqueGpx(selectedObjects, travelGpx)) {
							WptPt selectedPoint = new WptPt();
							selectedPoint.lat = pointLatLon.getLatitude();
							selectedPoint.lon = pointLatLon.getLongitude();
							SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
							selectedObjects.put(new Pair<>(travelGpx, selectedGpxPoint), gpxMenuProvider);
						}
					} else if (isRouteGpx) {
						if (isUniqueRoute(selectedObjects.keySet(), renderedObject)) {
							LatLon minLatLon = tileBox.getLatLonFromPixel(point.x - ROUTE_SEARCH_RADIUS_PX, point.y - ROUTE_SEARCH_RADIUS_PX);
							LatLon maxLatLon = tileBox.getLatLonFromPixel(point.x + ROUTE_SEARCH_RADIUS_PX, point.y + ROUTE_SEARCH_RADIUS_PX);

							QuadRect rect = new QuadRect(minLatLon.getLongitude(), maxLatLon.getLatitude(),
									maxLatLon.getLongitude(), minLatLon.getLatitude());

							selectedObjects.put(new Pair<>(renderedObject, rect), gpxMenuProvider);
						}
					} else {
						Amenity amenity = findAmenity(app, renderedObject.getId() >> 7,
								renderedObject.getOriginalNames(), searchLatLon, AMENITY_SEARCH_RADIUS);
						if (amenity != null) {
							if (renderedObject.getX() != null && renderedObject.getX().size() > 1
									&& renderedObject.getY() != null && renderedObject.getY().size() > 1) {
								amenity.getX().addAll(renderedObject.getX());
								amenity.getY().addAll(renderedObject.getY());
							}
							if (isUniqueAmenity(selectedObjects.keySet(), amenity)) {
								selectedObjects.put(amenity, poiMenuProvider);
							}
						} else {
							selectedObjects.put(renderedObject, null);
						}
					}
				}
			}
		}
		for (Map.Entry<Object, IContextMenuProvider> entry : selectedObjects.entrySet()) {
			IContextMenuProvider provider = entry.getValue();
			if (provider != null && provider.runExclusiveAction(entry.getKey(), showUnknownLocation)) {
				return true;
			}
		}
		processTransportStops(selectedObjects);
		if (selectedObjects.size() == 1) {
			Object selectedObj = selectedObjects.keySet().iterator().next();
			LatLon latLon = objectLatLon;
			PointDescription pointDescription = null;
			final IContextMenuProvider provider = selectedObjects.get(selectedObj);
			if (provider != null) {
				if (latLon == null) {
					latLon = provider.getObjectLocation(selectedObj);
				}
				pointDescription = provider.getObjectName(selectedObj);
			}
			if (latLon == null) {
				latLon = pointLatLon;
			}
			if (mInAddGpxPointMode) {
				String title = pointDescription == null ? "" : pointDescription.getName();
				mAddGpxPointBottomSheetHelper.setTitle(title);
				view.getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), view.getZoom(), true);
			} else {
				showContextMenu(latLon, pointDescription, selectedObj, provider);
			}
			return true;
		} else if (selectedObjects.size() > 1) {
			showContextMenuForSelectedObjects(pointLatLon, selectedObjects);
			return true;
		} else if (showUnknownLocation) {
			hideVisibleMenues();
			selectedObjectContextMenuProvider = null;
			getApplication().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (mInAddGpxPointMode) {
				mAddGpxPointBottomSheetHelper.setTitle("");
				view.getAnimatedDraggingThread().startMoving(pointLatLon.getLatitude(), pointLatLon.getLongitude(), view.getZoom(), true);
			} else {
				menu.show(pointLatLon, null, null);
			}
			return true;
		}
		return false;
	}

	private PointF getPointFromLatLon(double latitude, double longitude) {
		RotatedTileBox cp = getMapView().getCurrentRotatedTileBox();
		float x = cp.getPixXFromLatLon(latitude, longitude);
		float y = cp.getPixYFromLatLon(latitude, longitude);
		return new PointF(x, y);
	}

	private List<String> getValues(@Nullable QStringStringHash set) {
		List<String> res = new ArrayList<>();
		if (set != null) {
			QStringList keys = set.keys();
			for (int i = 0; i < keys.size(); i++) {
				res.add(set.get(keys.get(i)));
			}
		}
		return res;
	}

	private boolean isUniqueGpx(Map<Object, IContextMenuProvider> selectedObjects, TravelGpx travelGpx) {
		String tracksDir = view.getApplication().getAppPath(IndexConstants.GPX_TRAVEL_DIR).getPath();
		File file = new File(tracksDir, travelGpx.getRouteId() + GPX_FILE_EXT);
		if (file.exists()) {
			return false;
		}
		for (Map.Entry<Object, IContextMenuProvider> entry : selectedObjects.entrySet()) {
			if (entry.getKey() instanceof Pair && entry.getValue() instanceof GPXLayer
					&& ((Pair<?, ?>) entry.getKey()).first instanceof TravelGpx) {
				TravelGpx object = (TravelGpx) ((Pair<?, ?>) entry.getKey()).first;
				if (travelGpx.equals(object)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isUniqueAmenity(@NonNull Set<Object> set, @NonNull Amenity amenity) {
		for (Object o : set) {
			if (o instanceof Amenity && ((Amenity) o).compareTo(amenity) == 0) {
				return false;
			} else if (o instanceof TransportStop && ((TransportStop) o).getName().startsWith(amenity.getName())) {
				return false;
			}
		}
		return true;
	}

	private boolean isUniqueRoute(@NonNull Set<Object> set, @NonNull RenderedObject renderedObject) {
		for (Object o : set) {
			if (o instanceof Pair) {
				Pair<?, ?> pair = (Pair<?, ?>) o;
				if (pair.first instanceof RenderedObject) {
					RenderedObject object = (RenderedObject) pair.first;
					if (Algorithms.objectEquals(object.getId(), renderedObject.getId())
							|| Algorithms.stringsEqual(object.getRouteName(), renderedObject.getRouteName())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean disableSingleTap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mapActivity.getMapRouteInfoMenu().isVisible()
				|| MapRouteInfoMenu.waypointsVisible || MapRouteInfoMenu.followTrackVisible) {
			return true;
		}
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableSingleTap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		if (mInChangeMarkerPositionMode || mInGpxDetailsMode || mInAddGpxPointMode
				|| mapActivity == null || mapActivity.getMapRouteInfoMenu().isVisible()
				|| MapRouteInfoMenu.waypointsVisible || MapRouteInfoMenu.followTrackVisible
				|| mapActivity.getGpsFilterFragment() != null
				|| mapActivity.getDownloadTilesFragment() != null) {
			return true;
		}
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableLongPressOnMap(point, tileBox)) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	private void processTransportStops(@NonNull Map<Object, IContextMenuProvider> selectedObjects) {
		List<String> publicTransportTypes = getPublicTransportTypes();
		if (publicTransportTypes != null) {
			List<Amenity> transportStopAmenities = new ArrayList<>();
			for (Object o : selectedObjects.keySet()) {
				if (o instanceof Amenity) {
					Amenity amenity = (Amenity) o;
					if (!TextUtils.isEmpty(amenity.getSubType()) && publicTransportTypes.contains(amenity.getSubType())) {
						transportStopAmenities.add(amenity);
					}
				}
			}
			if (transportStopAmenities.size() > 0) {
				for (Amenity amenity : transportStopAmenities) {
					TransportStop transportStop = TransportStopController.findBestTransportStopForAmenity(getApplication(), amenity);
					if (transportStop != null) {
						TransportStopsLayer transportStopsLayer = getApplication().getOsmandMap().getMapLayers().getTransportStopsLayer();
						if (transportStopsLayer != null) {
							selectedObjects.remove(amenity);
							selectedObjects.put(transportStop, transportStopsLayer);
						}
					}
				}
			}
		}
	}

	@NonNull
	private Map<Object, IContextMenuProvider> selectObjectsForContextMenu(RotatedTileBox tileBox,
																		  PointF point, boolean acquireObjLatLon,
																		  boolean unknownLocation) {
		Map<LatLon, BackgroundType> pressedLatLonFull = new HashMap<>();
		Map<LatLon, BackgroundType> pressedLatLonSmall = new HashMap<>();
		Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
		List<Object> s = new ArrayList<>();
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof IContextMenuProvider) {
				s.clear();
				final IContextMenuProvider l = (IContextMenuProvider) lt;
				l.collectObjectsFromPoint(point, tileBox, s, unknownLocation);
				for (Object o : s) {
					selectedObjects.put(o, l);
					if (acquireObjLatLon && l.isObjectClickable(o)) {
						LatLon latLon = l.getObjectLocation(o);
						BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;
						if (o instanceof OsmBugsLayer.OpenStreetNote) {
							backgroundType = BackgroundType.COMMENT;
						}
						if (o instanceof FavouritePoint) {
							backgroundType = ((FavouritePoint) o).getBackgroundType();
						}
						if (o instanceof GPXUtilities.WptPt) {
							backgroundType = BackgroundType.getByTypeName(
									((GPXUtilities.WptPt) o).getBackgroundType(), DEFAULT_BACKGROUND_TYPE);
						}
						if (lt.isPresentInFullObjects(latLon) && !pressedLatLonFull.containsKey(latLon)) {
							pressedLatLonFull.put(latLon, backgroundType);
						} else if (lt.isPresentInSmallObjects(latLon) && !pressedLatLonSmall.containsKey(latLon)) {
							pressedLatLonSmall.put(latLon, backgroundType);
						}
					}
				}
			}
		}
		if (acquireObjLatLon) {
			this.pressedLatLonFull = pressedLatLonFull;
			this.pressedLatLonSmall = pressedLatLonSmall;
		}
		return selectedObjects;
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public boolean pressedContextMarker(RotatedTileBox tb, float px, float py) {
		float markerX;
		float markerY;
		if (mInChangeMarkerPositionMode) {
			markerX = tb.getCenterPixelX();
			markerY = tb.getCenterPixelY();
		} else if (menu != null && menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			markerX = tb.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			markerY = tb.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		} else {
			return false;
		}
		Rect bs = contextMarker.getDrawable().getBounds();
		int dx = (int) (px - markerX);
		int dy = (int) (py - markerY);
		int bx = dx + bs.width() / 2;
		int by = dy + bs.height();
		return (bs.contains(bx, by));
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || menu == null || mInChangeMarkerPositionMode || mInGpxDetailsMode
				|| mapActivity.getGpsFilterFragment() != null
				|| mapActivity.getDownloadTilesFragment() != null) {
			return true;
		}

		if (pressedContextMarker(tileBox, point.x, point.y)) {
			hideVisibleMenues();
			menu.show();
			return true;
		}

		if (selectOnMap != null) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			menu.init(latlon, null, null);
			CallbackWithObject<LatLon> cb = selectOnMap;
			cb.processResult(latlon);
			selectOnMap = null;
			return true;
		}

		if (!disableSingleTap()) {
			boolean res = showContextMenu(point, tileBox, false);
			if (res) {
				return true;
			}
		}

		boolean processed = hideVisibleMenues();
		processed |= menu.onSingleTapOnMap();
		if (!processed && MapRouteInfoMenu.chooseRoutesVisible) {
			WeakReference<ChooseRouteFragment> chooseRouteFragmentRef = mapActivity.getMapRouteInfoMenu().findChooseRouteFragment();
			if (chooseRouteFragmentRef != null) {
				ChooseRouteFragment chooseRouteFragment = chooseRouteFragmentRef.get();
				if (chooseRouteFragment != null) {
					chooseRouteFragment.dismiss();
					processed = true;
				}
			}
		}
		if (!processed) {
			MapControlsLayer mapControlsLayer = getApplication().getOsmandMap().getMapLayers().getMapControlsLayer();
			if (mapControlsLayer != null && (!mapControlsLayer.isMapControlsVisible()
					|| getApplication().getSettings().MAP_EMPTY_STATE_ALLOWED.get())) {
				mapControlsLayer.switchMapControlsVisibility(true);
			}
		}
		return false;
	}

	private boolean hideVisibleMenues() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && mapActivity.getTrackMenuFragment() != null) {
			mapActivity.getTrackMenuFragment().dismiss();
			return true;
		}
		if (multiSelectionMenu != null && multiSelectionMenu.isVisible()) {
			multiSelectionMenu.hide();
			return true;
		}
		return false;
	}

	protected void showContextMenuForSelectedObjects(LatLon latLon, Map<Object, IContextMenuProvider> selectedObjects) {
		hideVisibleMenues();
		selectedObjectContextMenuProvider = null;
		if (multiSelectionMenu != null) {
			multiSelectionMenu.show(latLon, selectedObjects);
		}
	}

	public void setMapQuickActionLayer(MapQuickActionLayer mapQuickActionLayer) {
		this.mapQuickActionLayer = mapQuickActionLayer;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {

		if (movementListener != null && movementListener.onTouchEvent(event)) {
			if (multiSelectionMenu != null && multiSelectionMenu.isVisible()) {
				multiSelectionMenu.hide();
			}
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!mInChangeMarkerPositionMode && !mInGpxDetailsMode) {
					selectObjectsForContextMenu(tileBox, new PointF(event.getX(), event.getY()), true, true);
					if (pressedLatLonFull.size() > 0 || pressedLatLonSmall.size() > 0) {
						view.refreshMap();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pressedLatLonFull.clear();
				pressedLatLonSmall.clear();
				view.refreshMap();
				break;
		}

		return false;
	}

	public interface IContextMenuProvider {

		void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation);

		LatLon getObjectLocation(Object o);

		PointDescription getObjectName(Object o);

		boolean disableSingleTap();

		boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox);

		boolean isObjectClickable(Object o);

		boolean runExclusiveAction(@Nullable Object o, boolean unknownLocation);

		boolean showMenuAction(@Nullable Object o);
	}

	public interface IMoveObjectProvider {

		boolean isObjectMovable(Object o);

		void applyNewObjectPosition(@NonNull Object o,
									@NonNull LatLon position,
									@Nullable ApplyMovedObjectCallback callback);
	}

	public interface ApplyMovedObjectCallback {

		void onApplyMovedObject(boolean success, @Nullable Object newObject);

		boolean isCancelled();
	}

	public interface IContextMenuProviderSelection {

		int getOrder(Object o);

		void setSelectedObject(Object o);

		void clearSelectedObject();
	}

	private static class MenuLayerOnGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return true;
		}
	}
}
