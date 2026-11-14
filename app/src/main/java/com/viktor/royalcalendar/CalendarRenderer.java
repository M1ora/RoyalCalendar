package com.viktor.royalcalendar;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;

public final class CalendarRenderer {
  public static final int TOP_PADDING = 200;
  public static final int DESIGN_HEIGHT = 900;
  private static final HashMap<String, Bitmap> CACHE = new HashMap<>();
  private static final HashMap<String, PointF> ANCHORS = new HashMap<>();
  private static final HashMap<String, float[]> BASES = new HashMap<>();
  private static final HashMap<String, float[]> WORDS = new HashMap<>();
  private static final HashMap<String, Bitmap[]> LAYERS = new HashMap<>();
  private static final float[] CAPTION_ANGLES = {
    0, 7, -7, -10, 3, 0, -8, 4, -4, 0, -5, 5, -12, -9, 4, -2, 3, -14, -12, 2, 8, -3, 6, -7, 0, 5,
    -10, -4, 5, -6, 4
  };
  private static final String[] EN = {
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
  };
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Context context;

  public CalendarRenderer(Context context) {
    this.context = context;
    upgradePlacement(context);
  }

  public static void upgradePlacement(Context context) {
    var prefs = context.getSharedPreferences("placement", 0);
    if (prefs.getInt("caption_vertical_version", 0) >= 3) return;
    var editor = prefs.edit();
    for (var entry : prefs.getAll().entrySet())
      if (entry.getKey().startsWith("caption_y_") && entry.getValue() instanceof Number) {
        float value = ((Number) entry.getValue()).floatValue();
        if (value == -36 || value == -41) editor.putFloat(entry.getKey(), -64);
      }
    for (var entry : prefs.getAll().entrySet())
      if (entry.getKey().startsWith("dagger_size_")
          && entry.getValue() instanceof Number
          && ((Number) entry.getValue()).floatValue() == 150) editor.putFloat(entry.getKey(), 135);
    editor.putInt("caption_vertical_version", 3).apply();
  }

  static boolean firstDigitIsLower(int day) {
    float[] b = BASES.get(Integer.toString(day));
    return b != null && b[1] > b[3];
  }

  static boolean weekdayLettersAreOpaque(String day) {
    Bitmap[] layers = LAYERS.get("word:" + day + ":" + Color.WHITE);
    if (layers == null) return false;
    Bitmap glyph = layers[2];
    int[] pixels = new int[glyph.getWidth() * glyph.getHeight()];
    glyph.getPixels(pixels, 0, glyph.getWidth(), 0, 0, glyph.getWidth(), glyph.getHeight());
    for (int p : pixels)
      if (Color.alpha(p) > 0
          && Color.red(p) > 240
          && Color.green(p) > 240
          && Color.blue(p) > 240
          && Color.alpha(p) < 255) return false;
    return true;
  }

  public static String language(Context c) {
    return "en";
  }

  private Bitmap raw(String path) {
    try (var stream = context.getAssets().open(path)) {
      Bitmap b = BitmapFactory.decodeStream(stream);
      if (b == null) throw new IllegalStateException("Invalid artwork: " + path);
      return b;
    } catch (IOException e) {
      throw new IllegalStateException("Missing artwork: " + path, e);
    }
  }

  private void tint(int color) {
    paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
  }

  private Bitmap croppedMask(Bitmap source, int left, int top, int width, int height, int color) {
    Bitmap layer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    tint(color);
    new Canvas(layer)
        .drawBitmap(
            source,
            new Rect(left, top, left + width, top + height),
            new Rect(0, 0, width, height),
            paint);
    paint.setColorFilter(null);
    return layer;
  }

  private PointF lowerEdge(int[] mask, int width, int height, int from, int to) {
    int bottom = 0;
    for (int y = height - 1; y >= 0; y--) {
      boolean found = false;
      for (int x = from; x <= to; x++)
        if ((mask[y * width + x] >>> 24) > 128) {
          found = true;
          break;
        }
      if (found) {
        bottom = y;
        break;
      }
    }
    long sum = 0;
    int count = 0;
    for (int y = Math.max(0, bottom - 4); y <= bottom; y++)
      for (int x = from; x <= to; x++)
        if ((mask[y * width + x] >>> 24) > 128) {
          sum += x;
          count++;
        }
    return new PointF(count == 0 ? (from + to) / 2f : sum / (float) count, bottom);
  }

  private void measureWord(String value, Bitmap word) {
    int w = word.getWidth(), h = word.getHeight();
    int[] mask = new int[w * h];
    word.getPixels(mask, 0, w, 0, 0, w, h);
    double sx = 0, sy = 0, sxx = 0, sxy = 0;
    int n = 0;
    double topSum = 0;
    for (int x = w / 5; x < w * 4 / 5; x++) {
      int top = 0, bottom = h - 1;
      while (top < h && (mask[top * w + x] >>> 24) < 128) top++;
      while (bottom >= 0 && (mask[bottom * w + x] >>> 24) < 128) bottom--;
      if (top >= h || bottom < 0) continue;
      sx += x;
      sy += bottom;
      sxx += (double) x * x;
      sxy += (double) x * bottom;
      topSum += top;
      n++;
    }
    double slope = (n * sxy - sx * sy) / (n * sxx - sx * sx);
    float topAtMiddle = (float) (topSum / n + slope * (w / 2f - sx / n));
    WORDS.put(value, new float[] {(float) Math.toDegrees(Math.atan(slope)), topAtMiddle});
  }

  // The source layers share a canvas. Compose before trimming to preserve their
  // deliberately uneven, angular registration instead of generating a round stroke.
  private Bitmap sprite(String value, boolean word, int color) {
    String key = (word ? "word:" : "number:") + value + ":" + color;
    Bitmap hit = CACHE.get(key);
    if (hit != null) return hit;
    Bitmap top = raw((word ? "week/" : "days/") + value + ".png");
    Bitmap back = raw((word ? "week-back/" : "day-back/") + value + ".png");
    Bitmap outer = raw((word ? "week-outer/" : "day-outer/") + value + ".png");
    int anchorX = 0, anchorY = 0;
    PointF leftBase = null, rightBase = null;
    if (!word) {
      int tw = top.getWidth(), th = top.getHeight();
      int[] mask = new int[tw * th];
      top.getPixels(mask, 0, tw, 0, 0, tw, th);
      int min = tw, max = 0;
      for (int y = 0; y < th; y++)
        for (int x = 0; x < tw; x++)
          if ((mask[y * tw + x] >>> 24) > 128) {
            min = Math.min(min, x);
            max = Math.max(max, x);
          }
      anchorX = Math.round(min + (max - min) * (value.length() == 1 ? 0.58f : 0.77f));
      for (int y = 0; y < th; y++)
        if ((mask[y * tw + anchorX] >>> 24) > 128) {
          anchorY = y + 3;
          break;
        }
      int middle = (min + max) / 2;
      back.getPixels(mask, 0, tw, 0, 0, tw, th);
      leftBase = lowerEdge(mask, tw, th, min, middle);
      rightBase = lowerEdge(mask, tw, th, middle + 1, max);
    }
    Bitmap full = Bitmap.createBitmap(top.getWidth(), top.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(full);
    tint(Color.BLACK);
    canvas.drawBitmap(outer, 0, 0, paint);
    tint(Color.WHITE);
    canvas.drawBitmap(back, 0, 0, paint);
    Bitmap normalized = null;
    if (word) {
      // Top word art contains colored weekend outlines; all weekday text in
      // the game's day-transition screen is white with a black outline.
      normalized = top.copy(Bitmap.Config.ARGB_8888, true);
      int w = top.getWidth(), h = top.getHeight();
      int[] pixels = new int[w * h];
      top.getPixels(pixels, 0, w, 0, 0, w, h);
      for (int i = 0; i < pixels.length; i++) {
        int p = pixels[i];
        int shade = 255 - Math.max(Color.red(p), Math.max(Color.green(p), Color.blue(p)));
        int alpha = Color.alpha(p);
        if (shade > 240 && alpha > 0) alpha = 255;
        pixels[i] = Color.argb(alpha, shade, shade, shade);
      }
      normalized.setPixels(pixels, 0, w, 0, 0, w, h);
      paint.setColorFilter(null);
      canvas.drawBitmap(normalized, 0, 0, paint);
    } else {
      tint(color);
      canvas.drawBitmap(top, 0, 0, paint);
    }
    paint.setColorFilter(null);
    int w = full.getWidth(), h = full.getHeight(), left = w, right = 0, first = h, last = 0;
    int[] pixels = new int[w * h];
    full.getPixels(pixels, 0, w, 0, 0, w, h);
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++)
        if ((pixels[y * w + x] >>> 24) > 32) {
          left = Math.min(left, x);
          right = Math.max(right, x);
          first = Math.min(first, y);
          last = Math.max(last, y);
        }
    Bitmap cropped = Bitmap.createBitmap(full, left, first, right - left + 1, last - first + 1);
    int cw = cropped.getWidth(), ch = cropped.getHeight();
    Bitmap foreground =
        word
            ? Bitmap.createBitmap(normalized, left, first, cw, ch)
            : croppedMask(top, left, first, cw, ch, color);
    LAYERS.put(
        key,
        new Bitmap[] {
          croppedMask(outer, left, first, cw, ch, Color.BLACK),
          croppedMask(back, left, first, cw, ch, Color.WHITE),
          foreground
        });
    if (normalized != null && normalized != foreground) normalized.recycle();
    top.recycle();
    back.recycle();
    outer.recycle();
    if (!word) {
      ANCHORS.put(
          value,
          new PointF(
              (anchorX - left) / (float) cropped.getWidth(),
              (anchorY - first) / (float) cropped.getHeight()));
      BASES.put(
          value,
          new float[] {
            (leftBase.x - left) / cropped.getWidth(),
            (leftBase.y - first) / cropped.getHeight(),
            (rightBase.x - left) / cropped.getWidth(),
            (rightBase.y - first) / cropped.getHeight()
          });
    } else measureWord(value, cropped);
    if (cropped != full) full.recycle();
    CACHE.put(key, cropped);
    return cropped;
  }

  public Bitmap render(LocalDate today, String lang, int width) {
    return render(today, lang, width, selectedStyle(context));
  }

  public static String selectedStyle(Context context) {
    return context.getSharedPreferences("appearance", 0).getString("style", "royal");
  }

  /** Remove Royal's editing margins before fitting it into the wide home-screen widget. */
  public Bitmap renderWidget(LocalDate today, String lang, int width) {
    Bitmap source = render(today, lang, width);
    if ("reload".equals(selectedStyle(context))) return source;
    int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
    int[] row = new int[source.getWidth()];
    for (int y = 0; y < source.getHeight(); y++) {
      source.getPixels(row, 0, row.length, 0, y, row.length, 1);
      for (int x = 0; x < row.length; x++) {
        if (Color.alpha(row[x]) <= 8) continue;
        left = Math.min(left, x);
        right = Math.max(right, x);
        top = Math.min(top, y);
        bottom = Math.max(bottom, y);
      }
    }
    Bitmap out = Bitmap.createBitmap(width, Math.round(width * 9 / 16f), Bitmap.Config.ARGB_8888);
    if (right >= left) {
      float scale =
          Math.min(
              width / (float) (right - left + 1), out.getHeight() / (float) (bottom - top + 1));
      float w = (right - left + 1) * scale, h = (bottom - top + 1) * scale;
      new Canvas(out)
          .drawBitmap(
              source,
              new Rect(left, top, right + 1, bottom + 1),
              new RectF(
                  (width - w) / 2,
                  (out.getHeight() - h) / 2,
                  (width + w) / 2,
                  (out.getHeight() + h) / 2),
              paint);
    }
    source.recycle();
    return out;
  }

  public Bitmap render(LocalDate today, String lang, int width, String style) {
    if ("reload".equals(style))
      return new ReloadRenderer(context)
          .render(
              today,
              width,
              context.getSharedPreferences("appearance", 0).getBoolean("reload_opaque", false),
              context.getSharedPreferences("appearance", 0).getInt("reload_radius", 48));
    Bitmap out =
        Bitmap.createBitmap(
            width, Math.round(width * DESIGN_HEIGHT / 1200f), Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(out);
    c.scale(width / 1200f, width / 1200f);
    c.translate(0, TOP_PADDING);
    LocalDate[] dates = DateModel.around(today);
    float[] centers = {110, 316, 586, 862, 1080};
    float[] scales = {0.53f, 0.72f, 1f, 0.69f, 0.55f};
    float[] heights = {300, 405, 510, 335, 225};
    float[] angles = {-2, 6, 0, -4, 7};
    for (int i = 0; i < 5; i++) {
      c.save();
      c.translate(centers[i], heights[i]);
      c.scale(scales[i], scales[i]);
      c.rotate(angles[i]);
      date(c, dates[i], angles[i]);
      c.restore();
    }
    // Anchor the lower-left blade tip to the top edge of the actual digit mask.
    // This follows the silhouette for 1, 8, 13, 31, etc., rather than empty bounds.
    String day = Integer.toString(today.getDayOfMonth());
    int color =
        today.getDayOfWeek().getValue() == 7
            ? Color.rgb(238, 0, 22)
            : today.getDayOfWeek().getValue() == 6 ? Color.rgb(69, 187, 241) : Color.BLACK;
    Bitmap current = sprite(day, false, color);
    PointF anchor = ANCHORS.get(day);
    float nh = 300, nw = nh * current.getWidth() / current.getHeight();
    float tipX = centers[2] - nw / 2 + anchor.x * nw, tipY = heights[2] - nh + anchor.y * nh;
    // User-supplied transparent original. Preserve its 458:487 aspect ratio.
    var placement = context.getSharedPreferences("placement", 0);
    float bladeSize = placement.getFloat("dagger_size_" + day, 135) / 100f;
    float bladeWidth = 218 * bladeSize, bladeHeight = 232 * bladeSize;
    c.save();
    c.translate(
        tipX + placement.getFloat("dagger_x_" + day, 15),
        tipY + 6 + placement.getFloat("dagger_y_" + day, 9));
    c.rotate(placement.getFloat("dagger_angle_" + day, 0));
    Drawable dagger = context.getDrawable(R.drawable.dagger_original);
    int dl = Math.round(-bladeWidth * 0.045f), dt = Math.round(-bladeHeight * 0.982f);
    dagger.setBounds(dl, dt, Math.round(dl + bladeWidth), Math.round(dt + bladeHeight));
    dagger.draw(c);
    c.restore();
    return out;
  }

  private void date(Canvas c, LocalDate date, float cardAngle) {
    int weekday = date.getDayOfWeek().getValue();
    int color =
        weekday == 7 ? Color.rgb(238, 0, 22) : weekday == 6 ? Color.rgb(69, 187, 241) : Color.BLACK;
    Bitmap number = sprite(Integer.toString(date.getDayOfMonth()), false, color);
    float h = 300, w = h * number.getWidth() / number.getHeight();
    Bitmap word = sprite(EN[weekday - 1], true, Color.WHITE);
    float[] base = BASES.get(Integer.toString(date.getDayOfMonth())),
        metrics = WORDS.get(EN[weekday - 1]);
    float lx = -w / 2 + base[0] * w,
        ly = -h + base[1] * h,
        rx = -w / 2 + base[2] * w,
        ry = -h + base[3] * h;
    float slope = (ry - ly) / Math.max(1, rx - lx);
    var placement = context.getSharedPreferences("placement", 0);
    String day = Integer.toString(date.getDayOfMonth());
    boolean single = date.getDayOfMonth() < 10;
    float labelX = (single ? 0 : 32) + placement.getFloat("caption_x_" + day, 0);
    float referenceAngle = CAPTION_ANGLES[date.getDayOfMonth() - 1] - cardAngle;
    // Caption tilt is its own stable composition choice, independent of the
    // two digit heights. Only the user rotation is added to that choice.
    float angle =
        Math.max(
                -15,
                Math.min(
                    15,
                    CAPTION_ANGLES[date.getDayOfMonth() - 1]
                        + placement.getFloat("caption_angle_" + day, 0)))
            - cardAngle;
    // All weekday sprites share one native pixel scale. Never stretch them to
    // fit a date: Wednesday remains wider than Friday, even next to one digit.
    float scale = 0.70f * placement.getFloat("caption_size_" + day, 100) / 100f,
        labelWidth = word.getWidth() * scale;
    float captionSlope = (float) Math.tan(Math.toRadians(referenceAngle));
    float from = Math.max(-w / 2, labelX - labelWidth / 2),
        to = Math.min(w / 2, labelX + labelWidth / 2);
    float atLeft = ly + (from - lx) * slope - captionSlope * (from - labelX);
    float atRight = ly + (to - lx) * slope - captionSlope * (to - labelX);
    float labelY = Math.max(atLeft, atRight) - 25 + placement.getFloat("caption_y_" + day, -64);
    // Keep the caption center stable when applying a manual rotation.
    float centerY = (word.getHeight() / 2f - metrics[1]) * scale;
    double oldRotation = Math.toRadians(referenceAngle - metrics[0]),
        newRotation = Math.toRadians(angle - metrics[0]);
    if (single) labelX += Math.sin(newRotation) * centerY;
    else labelX += (Math.sin(newRotation) - Math.sin(oldRotation)) * centerY;
    labelY += (Math.cos(oldRotation) - Math.cos(newRotation)) * centerY;
    Bitmap[] nl = LAYERS.get("number:" + date.getDayOfMonth() + ":" + color),
        wl = LAYERS.get("word:" + EN[weekday - 1] + ":" + Color.WHITE);
    RectF numberRect = new RectF(-w / 2, -h, w / 2, 0);
    // Bottom to top: weekday black, number black, number white, weekday white,
    // digits, weekday lettering. White outlines join at their shared boundary.
    wordLayer(c, wl[0], word, metrics, labelX, labelY, angle, scale);
    c.drawBitmap(nl[0], null, numberRect, paint);
    c.drawBitmap(nl[1], null, numberRect, paint);
    wordLayer(c, wl[1], word, metrics, labelX, labelY, angle, scale);
    c.drawBitmap(nl[2], null, numberRect, paint);
    wordLayer(c, wl[2], word, metrics, labelX, labelY, angle, scale);
  }

  private void wordLayer(
      Canvas c,
      Bitmap layer,
      Bitmap word,
      float[] metrics,
      float x,
      float y,
      float angle,
      float scale) {
    c.save();
    c.translate(x, y);
    c.rotate(angle - metrics[0]);
    c.scale(scale, scale);
    c.drawBitmap(layer, -word.getWidth() / 2f, -metrics[1], paint);
    c.restore();
  }
}
