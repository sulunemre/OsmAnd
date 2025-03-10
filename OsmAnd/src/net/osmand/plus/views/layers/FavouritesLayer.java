package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.ArrayList;
import java.util.List;

public class FavouritesLayer extends OsmandMapLayer implements IContextMenuProvider,
		IMoveObjectProvider, MapTextProvider<FavouritePoint> {

	private static final int START_ZOOM = 6;

	protected OsmandMapTileView view;
	private FavouritesHelper favouritesHelper;
	private MapMarkersHelper mapMarkersHelper;
	protected List<FavouritePoint> cache = new ArrayList<>();
	private MapTextLayer textLayer;
	@ColorInt
	private int defaultColor;
	@ColorInt
	private int grayColor;
	private OsmandSettings settings;
	private ContextMenuLayer contextMenuLayer;

	public FavouritesLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		settings = view.getApplication().getSettings();
		favouritesHelper = view.getApplication().getFavoritesHelper();
		mapMarkersHelper = view.getApplication().getMapMarkersHelper();
		textLayer = view.getLayerByClass(MapTextLayer.class);
		defaultColor = ContextCompat.getColor(view.getContext(), R.color.color_favorite);
		grayColor = ContextCompat.getColor(view.getContext(), R.color.color_favorite_gray);
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 1.5 && Math.abs(objy - ey) <= radius * 1.5);
//		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
		//return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof FavouritePoint) {
			FavouritePoint objectInMotion = (FavouritePoint) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			MapMarker mapMarker = mapMarkersHelper.getMapMarker(objectInMotion);
			float textScale = getTextScale();
			drawBigPoint(canvas, objectInMotion, pf.x, pf.y, mapMarker, textScale);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		cache.clear();
		if (this.settings.SHOW_FAVORITES.get() && favouritesHelper.isFavoritesLoaded()) {
			if (tileBox.getZoom() >= START_ZOOM) {
				float textScale = getTextScale();
				float iconSize = getIconSize(view.getApplication());
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				for (FavoriteGroup group : favouritesHelper.getFavoriteGroups()) {
					List<Pair<FavouritePoint, MapMarker>> fullObjects = new ArrayList<>();
					boolean synced = isSynced(group);
					for (FavouritePoint favoritePoint : group.getPoints()) {
						double lat = favoritePoint.getLatitude();
						double lon = favoritePoint.getLongitude();
						if (favoritePoint.isVisible() && favoritePoint != contextMenuLayer.getMoveableObject()
								&& lat >= latLonBounds.bottom && lat <= latLonBounds.top
								&& lon >= latLonBounds.left && lon <= latLonBounds.right) {
							MapMarker marker = null;
							if (synced) {
								marker = mapMarkersHelper.getMapMarker(favoritePoint);
								if (marker == null || marker.history && !view.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
									continue;
								}
							}
							cache.add(favoritePoint);
							float x = tileBox.getPixXFromLatLon(lat, lon);
							float y = tileBox.getPixYFromLatLon(lat, lon);

							if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
								@ColorInt
								int color;
								if (marker != null && marker.history) {
									color = grayColor;
								} else {
									color = favouritesHelper.getColorWithCategory(favoritePoint, defaultColor);
								}
								PointImageDrawable pointImageDrawable = PointImageDrawable.getFromFavorite(
										view.getContext(), color, true, favoritePoint);
								pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
								smallObjectsLatLon.add(new LatLon(lat, lon));
							} else {
								fullObjects.add(new Pair<>(favoritePoint, marker));
								fullObjectsLatLon.add(new LatLon(lat, lon));
							}
						}
					}
					for (Pair<FavouritePoint, MapMarker> pair : fullObjects) {
						FavouritePoint favoritePoint = pair.first;
						float x = tileBox.getPixXFromLatLon(favoritePoint.getLatitude(), favoritePoint.getLongitude());
						float y = tileBox.getPixYFromLatLon(favoritePoint.getLatitude(), favoritePoint.getLongitude());
						drawBigPoint(canvas, favoritePoint, x, y, pair.second, textScale);
					}
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		if (isTextVisible()) {
			textLayer.putData(this, cache);
		}

	}

	private boolean isSynced(@NonNull FavoriteGroup group) {
		MapMarkersGroup markersGroup = mapMarkersHelper.getMarkersGroup(group);
		return markersGroup != null && !markersGroup.isDisabled();
	}

	private void drawBigPoint(Canvas canvas, FavouritePoint favoritePoint, float x, float y, @Nullable MapMarker marker,
							  float textScale) {
		PointImageDrawable pointImageDrawable;
		boolean history = false;
		if (marker != null) {
			pointImageDrawable = PointImageDrawable.getOrCreateSyncedIcon(view.getContext(),
					favouritesHelper.getColorWithCategory(favoritePoint, defaultColor), favoritePoint);
			history = marker.history;
		} else {
			pointImageDrawable = PointImageDrawable.getFromFavorite(view.getContext(),
					favouritesHelper.getColorWithCategory(favoritePoint, defaultColor), true, favoritePoint);
		}
		pointImageDrawable.drawPoint(canvas, x, y, textScale, history);
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	private void getFavoriteFromPoint(RotatedTileBox tb, PointF point, List<? super FavouritePoint> res) {
		int r = getScaledTouchRadius(view.getApplication(), getDefaultRadiusPoi(tb));
		int ex = (int) point.x;
		int ey = (int) point.y;
		List<FavouritePoint> favouritePoints = new ArrayList<>(favouritesHelper.getFavouritePoints());
		for (FavouritePoint n : favouritePoints) {
			getFavFromPoint(tb, res, r, ex, ey, n);
		}
	}

	private void getFavFromPoint(RotatedTileBox tb, List<? super FavouritePoint> res, int r, int ex, int ey,
								 FavouritePoint n) {
		if (n.isVisible()) {
			int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, r)) {
				res.add(n);
			}
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof FavouritePoint) {
			return ((FavouritePoint) o).getPointDescription(view.getContext());
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof FavouritePoint && o != contextMenuLayer.getMoveableObject();
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if (this.settings.SHOW_FAVORITES.get() && tileBox.getZoom() >= START_ZOOM) {
			getFavoriteFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof FavouritePoint) {
			return new LatLon(((FavouritePoint) o).getLatitude(), ((FavouritePoint) o).getLongitude());
		}
		return null;
	}

	@Override
	public LatLon getTextLocation(FavouritePoint o) {
		return new LatLon(o.getLatitude(), o.getLongitude());
	}

	@Override
	public int getTextShift(FavouritePoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(FavouritePoint o) {
		return PointDescription.getSimpleName(o, view.getContext());
	}

	@Override
	public boolean isTextVisible() {
		return settings.SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof FavouritePoint;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		if (o instanceof FavouritePoint) {
			favouritesHelper.editFavourite((FavouritePoint) o, position.getLatitude(), position.getLongitude());
			favouritesHelper.lookupAddress((FavouritePoint) o);
			result = true;
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, o);
		}
	}
}


