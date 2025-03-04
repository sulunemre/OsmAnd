package net.osmand.plus.utils;


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StatFs;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static android.content.Context.POWER_SERVICE;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.COMPLEX_UNIT_SP;

public class AndroidUtils {
	private static final Log LOG = PlatformUtil.getLog(AndroidUtils.class);

	public static final String STRING_PLACEHOLDER = "%s";
	public static final MessageFormat formatKb = new MessageFormat("{0, number,##.#}", Locale.US);
	public static final MessageFormat formatGb = new MessageFormat("{0, number,#.##}", Locale.US);
	public static final MessageFormat formatMb = new MessageFormat("{0, number,##.#}", Locale.US);

	/**
	 * @param context
	 * @return true if Hardware keyboard is available
	 */

	public static boolean isHardwareKeyboardAvailable(Context context) {
		return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}

	public static void softKeyboardDelayed(@NonNull Activity activity, @NonNull View view) {
		view.post(() -> {
			if (!isHardwareKeyboardAvailable(view.getContext())) {
				showSoftKeyboard(activity, view);
			}
		});
	}

	public static void showSoftKeyboard(@NonNull Activity activity, @NonNull View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				KeyguardManager keyguardManager = (KeyguardManager) view.getContext().getSystemService(Context.KEYGUARD_SERVICE);
				keyguardManager.requestDismissKeyguard(activity, null);
			}
			imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	public static void hideSoftKeyboard(@NonNull Activity activity, @Nullable View input) {
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			if (input != null) {
				IBinder windowToken = input.getWindowToken();
				if (windowToken != null) {
					inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
				}
			}
		}

	}

	public static Bitmap scaleBitmap(Bitmap bm, int newWidth, int newHeight, boolean keepOriginalBitmap) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		if (!keepOriginalBitmap) {
			bm.recycle();
		}
		return resizedBitmap;
	}

	public static byte[] getByteArrayFromBitmap(@NonNull Bitmap bitmap) {
		int size = bitmap.getRowBytes() * bitmap.getHeight();
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		bitmap.copyPixelsToBuffer(byteBuffer);
		return byteBuffer.array();
	}

	public static Bitmap createScaledBitmapWithTint(final Context ctx, @DrawableRes int drawableId, float scale, int tint) {
		Drawable drawableIcon = AppCompatResources.getDrawable(ctx, drawableId);
		if (drawableIcon != null) {
			DrawableCompat.setTint(DrawableCompat.wrap(drawableIcon), tint);
		}
		Bitmap bitmap = drawableToBitmap(drawableIcon, true);
		if (bitmap != null && scale != 1f && scale > 0.0f) {
			bitmap = scaleBitmap(bitmap,
					(int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), false);
		}

		return bitmap;
	}

	public static Bitmap createScaledBitmap(Drawable drawable, float scale) {
		int width = (int) (drawable.getIntrinsicWidth() * scale);
		int height = (int) (drawable.getIntrinsicHeight() * scale);
		width += width % 2 == 1 ? 1 : 0;
		height += height % 2 == 1 ? 1 : 0;

		return createScaledBitmap(drawable, width, height);
	}

	public static Bitmap createScaledBitmap(Drawable drawable, int width, int height) {
		return scaleBitmap(drawableToBitmap(drawable), width, height, false);
	}

	public static ColorStateList createBottomNavColorStateList(Context ctx, boolean nightMode) {
		return AndroidUtils.createCheckedColorStateList(ctx, nightMode,
				R.color.icon_color_default_light, R.color.wikivoyage_active_light,
				R.color.icon_color_default_light, R.color.wikivoyage_active_dark);
	}

	public static String addColon(OsmandApplication app, @StringRes int stringRes) {
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, app.getString(stringRes), "").trim();
	}

	public static Uri getUriForFile(Context context, File file) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			return Uri.fromFile(file);
		} else {
			return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
		}
	}

	public static boolean startActivityIfSafe(@NonNull Context context, @NonNull Intent intent) {
		return startActivityIfSafe(context, intent, null);
	}

	public static boolean startActivityIfSafe(@NonNull Context context, @NonNull Intent intent, @Nullable Intent chooserIntent) {
		try {
			if (!(context instanceof Activity)) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}
			context.startActivity(chooserIntent != null ? chooserIntent : intent);
			return true;
		} catch (ActivityNotFoundException e) {
			LOG.error(e);
			Toast.makeText(context, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static boolean startActivityForResultIfSafe(@NonNull Activity activity, @NonNull Intent intent, int requestCode) {
		try {
			activity.startActivityForResult(intent, requestCode);
			return true;
		} catch (ActivityNotFoundException e) {
			LOG.error(e);
			Toast.makeText(activity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public static boolean isActivityNotDestroyed(@Nullable Activity activity) {
		return activity != null && !activity.isFinishing() && !activity.isDestroyed();
	}

	public static boolean isFragmentCanBeAdded(@NonNull FragmentManager manager, @Nullable String tag) {
		return !manager.isStateSaved();
	}

	public static Spannable replaceCharsWithIcon(String text, Drawable icon, String[] chars) {
		Spannable spannable = new SpannableString(text);
		for (String entry : chars) {
			int i = text.indexOf(entry, 0);
			while (i < text.length() && i != -1) {
				ImageSpan span = new ImageSpan(icon) {
					public void draw(Canvas canvas, CharSequence text, int start, int end,
					                 float x, int top, int y, int bottom, Paint paint) {
						Drawable drawable = getDrawable();
						canvas.save();
						int transY = bottom - drawable.getBounds().bottom;
						transY -= paint.getFontMetricsInt().descent / 2;
						canvas.translate(x, transY);
						drawable.draw(canvas);
						canvas.restore();
					}
				};
				spannable.setSpan(span, i, i + entry.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				i = text.indexOf(entry, i + entry.length());
			}
		}
		return spannable;
	}

	public static void removeLinkUnderline(TextView textView) {
		Spannable s = new SpannableString(textView.getText());
		for (URLSpan span : s.getSpans(0, s.length(), URLSpan.class)) {
			int start = s.getSpanStart(span);
			int end = s.getSpanEnd(span);
			s.removeSpan(span);
			span = new URLSpan(span.getURL()) {
				@Override
				public void updateDrawState(TextPaint ds) {
					super.updateDrawState(ds);
					ds.setUnderlineText(false);
				}
			};
			s.setSpan(span, start, end, 0);
		}
		textView.setText(s);
	}

	public static String formatDate(Context ctx, long time) {
		return DateFormat.getDateFormat(ctx).format(new Date(time));
	}

	public static String formatDateTime(Context ctx, long time) {
		Date d = new Date(time);
		return DateFormat.getDateFormat(ctx).format(d) +
				" " + DateFormat.getTimeFormat(ctx).format(d);
	}

	public static String formatTime(Context ctx, long time) {
		return DateFormat.getTimeFormat(ctx).format(new Date(time));
	}

	public static String formatSize(Context ctx, long sizeBytes) {
		if (sizeBytes > 0) {
			int sizeKb = (int) ((sizeBytes + 512) >> 10);
			String size = "";
			String numSuffix = "MB";
			if (sizeKb > 1 << 20) {
				size = formatGb.format(new Object[] {(float) sizeKb / (1 << 20)});
				numSuffix = "GB";
			} else if (sizeBytes > (100 * (1 << 10))) {
				size = formatMb.format(new Object[] {(float) sizeBytes / (1 << 20)});
			} else {
				size = formatKb.format(new Object[] {(float) sizeBytes / (1 << 10)});
				numSuffix = "kB";
			}
			if (ctx == null) {
				return size + " " + numSuffix;
			}
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, size, numSuffix);
		}
		return "";
	}

	public static String getFreeSpace(Context ctx, File dir) {
		long size = AndroidUtils.getAvailableSpace(dir);
		return AndroidUtils.formatSize(ctx, size);
	}

	public static View findParentViewById(View view, int id) {
		ViewParent viewParent = view.getParent();

		while (viewParent instanceof View) {
			View parentView = (View) viewParent;
			if (parentView.getId() == id) {
				return parentView;
			}
			viewParent = parentView.getParent();
		}

		return null;
	}

	public static ColorStateList createCheckedColorStateList(Context ctx, @ColorRes int normal, @ColorRes int checked) {
		return createCheckedColorStateList(ctx, false, normal, checked, 0, 0);
	}

	public static ColorStateList createCheckedColorStateList(Context ctx, boolean night,
															 @ColorRes int lightNormal, @ColorRes int lightChecked,
															 @ColorRes int darkNormal, @ColorRes int darkChecked) {
		return createColorStateList(ctx, night, android.R.attr.state_checked,
				lightNormal, lightChecked, darkNormal, darkChecked);
	}

	public static ColorStateList createEnabledColorStateList(Context ctx, @ColorRes int normal, @ColorRes int pressed) {
		return createEnabledColorStateList(ctx, false, normal, pressed, 0, 0);
	}

	public static ColorStateList createEnabledColorStateList(Context ctx, boolean night,
															 @ColorRes int lightNormal, @ColorRes int lightPressed,
															 @ColorRes int darkNormal, @ColorRes int darkPressed) {
		return createColorStateList(ctx, night, android.R.attr.state_enabled,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	public static ColorStateList createPressedColorStateList(Context ctx, @ColorRes int normal, @ColorRes int pressed) {
		return createPressedColorStateList(ctx, false, normal, pressed, 0, 0);
	}

	public static ColorStateList createPressedColorStateList(Context ctx, boolean night,
															 @ColorRes int lightNormal, @ColorRes int lightPressed,
															 @ColorRes int darkNormal, @ColorRes int darkPressed) {
		return createColorStateList(ctx, night, android.R.attr.state_pressed,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	private static ColorStateList createColorStateList(Context ctx, boolean night, int state,
													   @ColorRes int lightNormal, @ColorRes int lightState,
													   @ColorRes int darkNormal, @ColorRes int darkState) {
		return new ColorStateList(
				new int[][] {
						new int[] {state},
						new int[] {}
				},
				new int[] {
						ContextCompat.getColor(ctx, night ? darkState : lightState),
						ContextCompat.getColor(ctx, night ? darkNormal : lightNormal)
				}
		);
	}

	public static ColorStateList createColorStateList(Context ctx, boolean night) {
		return new ColorStateList(
				new int[][] {
						new int[] {-android.R.attr.state_enabled}, // disabled
						new int[] {android.R.attr.state_checked},
						new int[] {}
				},
				new int[] {
						ColorUtilities.getSecondaryTextColor(ctx, night),
						ColorUtilities.getActiveColor(ctx, night),
						ColorUtilities.getSecondaryTextColor(ctx, night)}
		);
	}

	public static ColorStateList createCheckedColorIntStateList(@ColorInt int normal, @ColorInt int checked) {
		return createCheckedColorIntStateList(false, normal, checked, 0, 0);
	}

	public static ColorStateList createCheckedColorIntStateList(boolean night,
																@ColorInt int lightNormal, @ColorInt int lightChecked,
																@ColorInt int darkNormal, @ColorInt int darkChecked) {
		return createColorIntStateList(night, android.R.attr.state_checked,
				lightNormal, lightChecked, darkNormal, darkChecked);
	}

	public static ColorStateList createEnabledColorIntStateList(@ColorInt int normal, @ColorInt int pressed) {
		return createEnabledColorIntStateList(false, normal, pressed, 0, 0);
	}

	public static ColorStateList createEnabledColorIntStateList(boolean night,
																@ColorInt int lightNormal, @ColorInt int lightPressed,
																@ColorInt int darkNormal, @ColorInt int darkPressed) {
		return createColorIntStateList(night, android.R.attr.state_enabled,
				lightNormal, lightPressed, darkNormal, darkPressed);
	}

	private static ColorStateList createColorIntStateList(boolean night, int state,
														  @ColorInt int lightNormal, @ColorInt int lightState,
														  @ColorInt int darkNormal, @ColorInt int darkState) {
		return new ColorStateList(
				new int[][] {
						new int[] {state},
						new int[] {}
				},
				new int[] {
						night ? darkState : lightState,
						night ? darkNormal : lightNormal
				}
		);
	}

	public static StateListDrawable createCheckedStateListDrawable(Drawable normal, Drawable checked) {
		return createStateListDrawable(normal, checked, android.R.attr.state_checked);
	}

	public static StateListDrawable createPressedStateListDrawable(Drawable normal, Drawable pressed) {
		return createStateListDrawable(normal, pressed, android.R.attr.state_pressed);
	}

	public static StateListDrawable createEnabledStateListDrawable(Drawable disabled, Drawable enabled) {
		return createStateListDrawable(disabled, enabled, android.R.attr.state_enabled);
	}

	private static StateListDrawable createStateListDrawable(Drawable normal, Drawable stateDrawable, int state) {
		StateListDrawable res = new StateListDrawable();
		res.addState(new int[] {state}, stateDrawable);
		res.addState(new int[] {}, normal);
		return res;
	}

	public static LayerDrawable createProgressDrawable(@ColorInt int bgColor, @ColorInt int progressColor) {
		ShapeDrawable bg = new ShapeDrawable();
		bg.getPaint().setColor(bgColor);

		ShapeDrawable progress = new ShapeDrawable();
		progress.getPaint().setColor(progressColor);

		LayerDrawable res = new LayerDrawable(new Drawable[] {
				bg,
				new ClipDrawable(progress, Gravity.START, ClipDrawable.HORIZONTAL)
		});

		res.setId(0, android.R.id.background);
		res.setId(1, android.R.id.progress);

		return res;
	}

	public static void setBackground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		setBackground(ctx, view, night ? darkResId : lightResId);
	}

	public static void setBackground(Context ctx, View view, int resId) {
		setBackground(view, AppCompatResources.getDrawable(ctx, resId));
	}

	public static void setBackground(View view, Drawable drawable) {
		view.setBackground(drawable);
	}

	public static void setForeground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
			view.setForeground(AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
		} else if (view instanceof FrameLayout) {
			((FrameLayout) view).setForeground(AppCompatResources.getDrawable(ctx, night ? darkResId : lightResId));
		}
	}

	public static void setDashButtonBackground(Context ctx, View view, boolean night) {
		setBackground(ctx, view, night, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
	}

	public static void setBackgroundColor(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		setBackgroundColor(ctx, view, night ? darkResId : lightResId);
	}

	public static void setBackgroundColor(Context ctx, View view, @ColorRes int colorId) {
		view.setBackgroundColor(ContextCompat.getColor(ctx, colorId));
	}

	public static void setListItemBackground(Context ctx, View view, boolean night) {
		setBackgroundColor(ctx, view, ColorUtilities.getListBgColorId(night));
	}

	public static void setTextPrimaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(ctx, night));
	}

	public static void setTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(ColorUtilities.getSecondaryTextColor(ctx, night));
	}

	public static void setHintTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setHintTextColor(ColorUtilities.getSecondaryTextColor(ctx, night));
	}

	public static int getTextMaxWidth(float textSize, List<String> titles) {
		int width = 0;
		for (String title : titles) {
			int titleWidth = getTextWidth(textSize, title);
			if (titleWidth > width) {
				width = titleWidth;
			}
		}
		return width;
	}

	public static int getTextWidth(float textSize, String text) {
		Paint paint = new Paint();
		paint.setTextSize(textSize);
		return (int) paint.measureText(text);
	}

	public static int getTextHeight(Paint paint) {
		Paint.FontMetrics fm = paint.getFontMetrics();
		float height = fm.bottom - fm.top;
		return (int) height;
	}

	public static int dpToPx(Context ctx, float dp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	public static int spToPx(Context ctx, float sp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_SP,
				sp,
				r.getDisplayMetrics()
		);
	}

	@ColorInt
	public static int getColorFromAttr(@NonNull Context ctx, @AttrRes int colorAttribute) {
		TypedValue typedValue = new TypedValue();
		ctx.getTheme().resolveAttribute(colorAttribute, typedValue, true);
		return typedValue.data;
	}

	public static int resolveAttribute(Context ctx, int attribute) {
		TypedValue outValue = new TypedValue();
		ctx.getTheme().resolveAttribute(attribute, outValue, true);
		return outValue.resourceId;
	}

	public static float getFloatValueFromRes(Context ctx, int resId) {
		TypedValue outValue = new TypedValue();
		ctx.getResources().getValue(resId, outValue, true);
		return outValue.getFloat();
	}

	public static int getDrawableId(OsmandApplication app, String id) {
		if (!Algorithms.isEmpty(id)) {
			return app.getResources().getIdentifier(id, "drawable", app.getPackageName());
		}
		return 0;
	}

	public static int getStatusBarHeight(Context ctx) {
		int result = 0;
		int resourceId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = ctx.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static void addStatusBarPadding21v(Context ctx, View view) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		if (Build.VERSION.SDK_INT >= 21
				&& (!OsmandPlugin.isDevelopment()
				|| app.getSettings().TRANSPARENT_STATUS_BAR.get())) {
			int paddingLeft = view.getPaddingLeft();
			int paddingTop = view.getPaddingTop();
			int paddingRight = view.getPaddingRight();
			int paddingBottom = view.getPaddingBottom();
			view.setPadding(paddingLeft, paddingTop + getStatusBarHeight(ctx), paddingRight, paddingBottom);
		}
	}

	public static int getNavBarHeight(Context ctx) {
		if (!hasNavBar(ctx)) {
			return 0;
		}
		boolean landscape = ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		boolean isSmartphone = ctx.getResources().getConfiguration().smallestScreenWidthDp < 600;
		if (isSmartphone && landscape) {
			return 0;
		}
		int id = ctx.getResources().getIdentifier(landscape ? "navigation_bar_height_landscape" : "navigation_bar_height", "dimen", "android");
		if (id > 0) {
			return ctx.getResources().getDimensionPixelSize(id);
		}
		return 0;
	}

	public static boolean hasNavBar(Context ctx) {
		int id = ctx.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
		return id > 0 && ctx.getResources().getBoolean(id);
	}

	public static int getScreenHeight(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	public static int getScreenWidth(Activity activity) {
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	public static boolean isValidEmail(CharSequence target) {
		return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
	}

	public static PointF centroidForPoly(PointF[] points) {
		float centroidX = 0, centroidY = 0;

		for (PointF point : points) {
			centroidX += point.x / points.length;
			centroidY += point.y / points.length;
		}
		return new PointF(centroidX, centroidY);
	}

	public static void showNavBar(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19 && !isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static void hideNavBar(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19 && isNavBarVisible(activity)) {
			switchNavBarVisibility(activity);
		}
	}

	public static boolean isNavBarVisible(Activity activity) {
		if (Build.VERSION.SDK_INT >= 19) {
			int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
			return !((uiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == uiOptions);
		}
		return true;
	}

	public static void switchNavBarVisibility(Activity activity) {
		if (Build.VERSION.SDK_INT < 19) {
			return;
		}
		View decorView = activity.getWindow().getDecorView();
		int uiOptions = decorView.getSystemUiVisibility();
		uiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		uiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}

	public static int[] getCenterViewCoordinates(View view) {
		int[] coordinates = new int[2];
		view.getLocationOnScreen(coordinates);
		coordinates[0] += view.getWidth() / 2;
		coordinates[1] += view.getHeight() / 2;
		return coordinates;
	}

	public static int getViewOnScreenX(@NonNull View view) {
		return getLocationOnScreen(view)[0];
	}

	public static int getViewOnScreenY(@NonNull View view) {
		return getLocationOnScreen(view)[1];
	}

	public static int[] getLocationOnScreen(@NonNull View view) {
		int[] locationOnScreen = new int[2];
		view.getLocationOnScreen(locationOnScreen);
		return locationOnScreen;
	}

	public static void enterToFullScreen(Activity activity, View view) {
		if (Build.VERSION.SDK_INT >= 21) {
			requestLayout(view);
			activity.getWindow().getDecorView()
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		}
	}

	public static void exitFromFullScreen(Activity activity, View view) {
		if (Build.VERSION.SDK_INT >= 21) {
			requestLayout(view);
			activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
		}
	}

	private static void requestLayout(final View view) {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					ViewTreeObserver obs = view.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeOnGlobalLayoutListener(this);
					} else {
						obs.removeGlobalOnLayoutListener(this);
					}
					view.requestLayout();
				}
			});
		}
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static boolean isScreenOn(Context context) {
		PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && powerManager.isInteractive()
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && powerManager.isScreenOn();
	}

	public static boolean isScreenLocked(Context context) {
		KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return keyguardManager.inKeyguardRestrictedInputMode();
	}

	public static CharSequence getStyledString(CharSequence baseString, CharSequence stringToInsertAndStyle, int typefaceStyle) {

		if (typefaceStyle == Typeface.NORMAL || typefaceStyle == Typeface.BOLD
				|| typefaceStyle == Typeface.ITALIC || typefaceStyle == Typeface.BOLD_ITALIC
				|| baseString.toString().contains(STRING_PLACEHOLDER)) {

			return getStyledString(baseString, stringToInsertAndStyle, null, new StyleSpan(typefaceStyle));
		} else {
			return baseString;
		}
	}

	public static void setCompoundDrawablesWithIntrinsicBounds(@NonNull TextView tv, Drawable start, Drawable top, Drawable end, Drawable bottom) {
		if (isSupportRTL()) {
			tv.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
		} else {
			tv.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
		}
	}

	public static Drawable[] getCompoundDrawables(@NonNull TextView tv) {
		if (isSupportRTL()) {
			return tv.getCompoundDrawablesRelative();
		}
		return tv.getCompoundDrawables();
	}

	public static void setPadding(View view, int start, int top, int end, int bottom) {
		if (isSupportRTL()) {
			view.setPaddingRelative(start, top, end, bottom);
		} else {
			view.setPadding(start, top, end, bottom);
		}
	}

	public static void setMargins(ViewGroup.MarginLayoutParams layoutParams, int start, int top, int end, int bottom) {
		layoutParams.setMargins(start, top, end, bottom);
		if (isSupportRTL()) {
			layoutParams.setMarginStart(start);
			layoutParams.setMarginEnd(end);
		}
	}

	public static void setTextDirection(@NonNull TextView tv, boolean rtl) {
		if (isSupportRTL()) {
			int textDirection = rtl ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
			tv.setTextDirection(textDirection);
		}
	}

	public static int getLayoutDirection(@NonNull Context ctx) {
		Locale currentLocale = ctx.getResources().getConfiguration().locale;
		return TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale);
	}

	public static int getNavigationIconResId(@NonNull Context ctx) {
		return isLayoutRtl(ctx) ? R.drawable.ic_arrow_forward : R.drawable.ic_arrow_back;
	}

	public static Drawable getDrawableForDirection(@NonNull Context ctx, @NonNull Drawable drawable) {
		return isLayoutRtl(ctx) ? getMirroredDrawable(drawable) : drawable;
	}

	public static Drawable getMirroredDrawable(@NonNull Drawable drawable) {
		drawable.setAutoMirrored(true);
		return drawable;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		return drawableToBitmap(drawable, false);
	}

	public static Bitmap drawableToBitmap(Drawable drawable, boolean noOptimization) {
		if (drawable instanceof BitmapDrawable && !noOptimization) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if (bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		Bitmap bitmap = null;
		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap flipBitmapHorizontally(Bitmap source) {
		Matrix matrix = new Matrix();
		matrix.preScale(-1.0f, 1.0f, source.getWidth() / 2f, source.getHeight() / 2f);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	public static void setTextHorizontalGravity(@NonNull TextView tv, int hGravity) {
		if (tv.getContext() != null) {
			boolean isLayoutRtl = AndroidUtils.isLayoutRtl(tv.getContext());
			int gravity = Gravity.LEFT;
			if (isLayoutRtl && (hGravity == Gravity.START)
					|| !isLayoutRtl && hGravity == Gravity.END) {
				gravity = Gravity.RIGHT;
			}
			tv.setGravity(gravity);
		}
	}

	public static boolean isSupportRTL() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	public static boolean isLayoutRtl(Context ctx) {
		return isSupportRTL() && getLayoutDirection(ctx) == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	public static ArrayList<View> getChildrenViews(ViewGroup vg) {
		ArrayList<View> result = new ArrayList<>();
		for (int i = 0; i < vg.getChildCount(); i++) {
			View child = vg.getChildAt(i);
			result.add(child);
		}
		return result;
	}

	public static long getAvailableSpace(@NonNull OsmandApplication app) {
		return getAvailableSpace(app.getAppPath(null));
	}

	public static long getTotalSpace(@NonNull OsmandApplication app) {
		return getTotalSpace(app.getAppPath(null));
	}

	public static long getAvailableSpace(@Nullable File dir) {
		if (dir != null && dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
					return fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
				} else {
					return (long) (fs.getAvailableBlocks()) * fs.getBlockSize();
				}
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static long getTotalSpace(@Nullable File dir) {
		if (dir != null && dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
					return fs.getBlockCountLong() * fs.getBlockSizeLong();
				} else {
					return (long) (fs.getBlockCount()) * fs.getBlockSize();
				}
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static float getFreeSpaceGb(File dir) {
		if (dir.canRead()) {
			try {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				return (float) (fs.getBlockSize()) * fs.getAvailableBlocks() / (1 << 30);
			} catch (IllegalArgumentException e) {
				LOG.error(e);
			}
		}
		return -1;
	}

	public static float getTotalSpaceGb(File dir) {
		if (dir.canRead()) {
			return (float) (dir.getTotalSpace()) / (1 << 30);
		}
		return -1;
	}

	public static CharSequence getStyledString(CharSequence baseString, CharSequence stringToInsertAndStyle,
	                                           CharacterStyle baseStyle, CharacterStyle replaceStyle) {
		int indexOfPlaceholder = baseString.toString().indexOf(STRING_PLACEHOLDER);
		if (replaceStyle != null || baseStyle != null || indexOfPlaceholder != -1) {
			String nStr = baseString.toString().replace(STRING_PLACEHOLDER, stringToInsertAndStyle);
			SpannableStringBuilder ssb = new SpannableStringBuilder(nStr);
			if (baseStyle != null) {
				if (indexOfPlaceholder > 0) {
					ssb.setSpan(baseStyle, 0, indexOfPlaceholder, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (indexOfPlaceholder + stringToInsertAndStyle.length() < nStr.length()) {
					ssb.setSpan(baseStyle,
							indexOfPlaceholder + stringToInsertAndStyle.length(),
							nStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			if (replaceStyle != null) {
				ssb.setSpan(replaceStyle, indexOfPlaceholder,
						indexOfPlaceholder + stringToInsertAndStyle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return ssb;
		} else {
			return baseString;
		}
	}

	public static boolean isRTL() {
		return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	public static String createNewFileName(String oldName) {
		int firstDotIndex = oldName.indexOf('.');
		String nameWithoutExt = oldName.substring(0, firstDotIndex);
		String ext = oldName.substring(firstDotIndex);

		StringBuilder numberSection = new StringBuilder();
		int i = nameWithoutExt.length() - 1;
		boolean hasNameNumberSection = false;
		do {
			char c = nameWithoutExt.charAt(i);
			if (Character.isDigit(c)) {
				numberSection.insert(0, c);
			} else if (Character.isSpaceChar(c) && numberSection.length() > 0) {
				hasNameNumberSection = true;
				break;
			} else {
				break;
			}
			i--;
		} while (i >= 0);
		int newNumberValue = Integer.parseInt(hasNameNumberSection ? numberSection.toString() : "0") + 1;

		String newName;
		if (newNumberValue == 1) {
			newName = nameWithoutExt + " " + newNumberValue + ext;
		} else {
			newName = nameWithoutExt.substring(0, i) + " " + newNumberValue + ext;
		}

		return newName;
	}

	public static StringBuilder formatWarnings(List<String> warnings) {
		StringBuilder builder = new StringBuilder();
		boolean f = true;
		for (String w : warnings) {
			if (f) {
				f = false;
			} else {
				builder.append('\n');
			}
			builder.append(w);
		}
		return builder;
	}

	@NonNull
	public static String checkEmoticons(@NonNull String name) {
		char[] chars = name.toCharArray();
		char ch1;
		char ch2;

		int index = 0;
		StringBuilder builder = new StringBuilder();
		while (index < chars.length) {
			ch1 = chars[index];
			if ((int) ch1 == 0xD83C) {
				ch2 = chars[index + 1];
				if ((int) ch2 >= 0xDF00 && (int) ch2 <= 0xDFFF) {
					index += 2;
					continue;
				}
			} else if ((int) ch1 == 0xD83D) {
				ch2 = chars[index + 1];
				if ((int) ch2 >= 0xDC00 && (int) ch2 <= 0xDDFF) {
					index += 2;
					continue;
				}
			}
			builder.append(ch1);
			++index;
		}
		builder.trimToSize(); // remove trailing null characters
		return builder.toString();
	}

	@NonNull
	public static String createDbInsertQuery(@NonNull String tableName, @NonNull Set<String> rowKeys) {
		StringBuilder keys = new StringBuilder();
		StringBuilder values = new StringBuilder();
		String split = ", ";
		Iterator<String> iterator = rowKeys.iterator();
		while (iterator.hasNext()) {
			keys.append(iterator.next());
			values.append("?");
			if (iterator.hasNext()) {
				keys.append(split);
				values.append(split);
			}
		}
		return "INSERT INTO " + tableName + " (" + keys.toString() + ") VALUES (" + values.toString() + ")";
	}

	public static String getRoutingStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "routing_attr_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	public static String getRoutingStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "routing_attr_" + propertyName + "_description");
		return value != null ? value : defValue;
	}

	public static String getRenderingStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "rendering_attr_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	public static String getRenderingStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "rendering_attr_" + propertyName + "_description");
		return value != null ? value : defValue;
	}

	public static String getIconStringPropertyName(Context ctx, String propertyName) {
		String value = getStringByProperty(ctx, "icon_group_" + propertyName);
		return value != null ? value : propertyName;
	}

	@NonNull
	public static String getRenderingStringPropertyValue(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "rendering_value_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getRenderingStringPropertyDescription(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "rendering_value_" + propertyValueReplaced + "_description");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getStringRouteInfoPropertyValue(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "routeInfo_" + propertyValueReplaced + "_name");
		return value != null ? value : propertyValue;
	}

	@NonNull
	public static String getStringRouteInfoPropertyDescription(Context ctx, String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		String propertyValueReplaced = propertyValue.replaceAll("\\s+", "_");
		String value = getStringByProperty(ctx, "routeInfo_" + propertyValueReplaced + "_description");
		return value != null ? value : propertyValue;
	}


	public static String getActivityTypeStringPropertyName(Context ctx, String propertyName, String defValue) {
		String value = getStringByProperty(ctx, "activity_type_" + propertyName + "_name");
		return value != null ? value : defValue;
	}

	@Nullable
	public static String getStringByProperty(@NonNull Context ctx, @NonNull String property) {
		try {
			Field field = R.string.class.getField(property);
			return getStringForField(ctx, field);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	@Nullable
	private static String getStringForField(@NonNull Context ctx, @Nullable Field field) throws IllegalAccessException {
		if (field != null) {
			Integer in = (Integer) field.get(null);
			return ctx.getString(in);
		}
		return null;
	}

	public static void openUrl(@NonNull Context context, @NonNull Uri uri, boolean nightMode) {
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
				.setToolbarColor(ColorUtilities.getAppBarColor(context, nightMode))
				.build();
		customTabsIntent.intent.setData(uri);
		try {
			customTabsIntent.launchUrl(context, uri);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
		}
	}



}
