package com.viktor.royalcalendar;

import android.content.Context;
import android.graphics.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;

/** Local adaptation of Workshop scene 3336208279, using its font, textures and 4K coordinates. */
final class ReloadRenderer {
  private static final int NAVY = 0xff181a27;
  private static final HashMap<String, Bitmap> TEXTURES = new HashMap<>();
  private static Typeface font;
  private final Context context;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  // Origins are composed from the scene's Days parent and individual day groups; Y points up there.
  private static final float[][] DAYS = {
    {995.85895f, 101.76346f}, {1335.86926f, 341.14862f}, {1671.71906f, 586.02844f},
    {2211.07251f, 949.99576f}, {2756.69898f, 1314.76797f}, {3090.99219f, 1553.60934f},
    {3427.60266f, 1796.23679f}
  };
  private static final float[][] MOONS = {
    {1802.93615f, 124.43548f}, {2139.20825f, 364.09891f}, {2783.88403f, 783.97418f},
    {3218.62207f, 1090.52038f}, {3554.93286f, 1331.35339f}, {3893.02368f, 1569.84704f}
  };

  ReloadRenderer(Context context) {
    this.context = context;
    if (font == null) font = Typeface.createFromAsset(context.getAssets(), "reload/calendar.otf");
  }

  Bitmap render(LocalDate today, int width, boolean opaque, float cornerRadius) {
    // Supersample the complete scene so diagonal geometry and small text share smooth edges.
    int height = Math.round(width * 9 / 16f);
    Bitmap high = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(high);
    c.scale(high.getWidth() / 3840f, high.getHeight() / 2160f);
    if (opaque) c.drawColor(NAVY);
    Path band =
        rectangle(
            1886.01685f,
            2160 - 1034.18066f,
            2048 * 2.48701f,
            2048 * 0.24992f,
            (float) Math.toDegrees(0.28484));
    paint.setColor(0xff00489f);
    c.drawPath(band, paint);
    c.save();
    c.clipPath(band);
    text(
        c,
        Integer.toString(today.getMonthValue()),
        1453.85071f,
        1307.82605f,
        170,
        1,
        1,
        Paint.Align.RIGHT);
    text(
        c,
        Integer.toString(today.getYear()),
        487.34180f,
        1475.48962f,
        34,
        1,
        1,
        Paint.Align.CENTER);
    text(
        c,
        today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
        340.27960f,
        1369.79358f,
        20,
        1.02106f,
        0.83450f,
        Paint.Align.LEFT);
    // These are the original four stacked circle layers, clipped to the blue ribbon.
    image(c, "circle_b", 2201.72607f, 947.25452f, 2048 * 0.35850f, 2048 * 0.35850f, false, false);
    image(c, "circle_a", 2201.72607f, 947.25452f, 2048 * 0.31927f, 2048 * 0.31927f, false, false);
    image(c, "circle_b", 2201.72607f, 947.25452f, 2048 * 0.16450f, 2048 * 0.16450f, false, false);
    image(c, "circle_a", 2201.72607f, 947.25452f, 2048 * 0.09667f, 2048 * 0.09667f, false, false);
    c.restore();

    paint.setColor(Color.WHITE);
    c.drawPath(
        rectangle(
            2528.47192f,
            2160 - 851.06561f,
            2048 * 1.77618f,
            256 * 0.06167f,
            (float) -Math.toDegrees(0.59976)),
        paint);
    for (int i = 0; i < DAYS.length; i++) {
      LocalDate date = today.plusDays(i - 3);
      float x = DAYS[i][0], y = DAYS[i][1];
      text(
          c,
          Integer.toString(date.getDayOfMonth()),
          x + 60,
          y,
          i == 3 ? 60 : 65,
          1,
          0.8f,
          Paint.Align.RIGHT);
      boolean weekend = date.getDayOfWeek().getValue() >= 6;
      text(
          c,
          date.getDayOfWeek()
              .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
              .toUpperCase(Locale.ENGLISH),
          x + 190,
          y - (weekend ? 10 : 45),
          i >= 5 ? 22 : 21,
          1.2f,
          0.8f,
          Paint.Align.CENTER);
      if (weekend) {
        paint.setColor(date.getDayOfWeek().getValue() == 6 ? 0xff29abe1 : 0xffe83f24);
        float lineY = 2160 - (y - 10 - 70);
        c.drawRect(
            x + 190 - 512 * 0.30763f / 2,
            lineY - 512 * 0.05275f / 2,
            x + 190 + 512 * 0.30763f / 2,
            lineY + 512 * 0.05275f / 2,
            paint);
      }
    }
    // The source scene deliberately has six moons: -2 through +3; the last is partly off-canvas.
    image(c, "circle_c", 2783.88403f, 783.97418f, 2048 * 0.11447f, 2048 * 0.11447f, false, false);
    image(c, "circle_a", 2783.88403f, 783.97418f, 2048 * 0.10184f, 2048 * 0.10184f, false, false);
    for (int i = 0; i < MOONS.length; i++) {
      double phase = phase(today.plusDays(i - 2));
      int step = (int) Math.round((phase <= 0.5 ? phase : 1 - phase) * 28);
      String texture;
      if (step == 0) texture = "moon_new";
      else if (step < 7) texture = "moon_waning_" + (7 - step);
      else if (step == 7) texture = "moon_half";
      else if (step < 14) texture = "moon_waxing_" + (step - 7);
      else texture = "moon_full";
      if (i != 2) texture += "_bw";
      boolean flip = phase < 0.5 ? step > 0 && step < 7 : step >= 7;
      image(
          c,
          texture,
          MOONS[i][0],
          MOONS[i][1],
          2048 * 0.09581f,
          2048 * 0.09581f * (i < 2 ? 1.05574f : 1),
          flip,
          !opaque);
    }
    Path corners = new Path();
    corners.setFillType(Path.FillType.EVEN_ODD);
    corners.addRect(0, 0, 3840, 2160, Path.Direction.CW);
    float radius = Math.max(0, Math.min(160, cornerRadius)) * 3.2f;
    corners.addRoundRect(new RectF(0, 0, 3840, 2160), radius, radius, Path.Direction.CW);
    paint.setColor(Color.BLACK);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    c.drawPath(corners, paint);
    paint.setXfermode(null);
    Bitmap out = Bitmap.createScaledBitmap(high, width, height, true);
    high.recycle();
    return out;
  }

  private Path rectangle(float x, float y, float w, float h, float degrees) {
    Path path = new Path();
    path.addRect(-w / 2, -h / 2, w / 2, h / 2, Path.Direction.CW);
    Matrix matrix = new Matrix();
    matrix.setRotate(degrees);
    matrix.postTranslate(x, y);
    path.transform(matrix);
    return path;
  }

  private void text(
      Canvas c,
      String value,
      float x,
      float y,
      float points,
      float scaleX,
      float scaleY,
      Paint.Align align) {
    paint.setTypeface(font);
    paint.setTextSize(points * 300 / 72f); // Wallpaper Engine's scene text uses 300 DPI.
    paint.setTextAlign(align);
    paint.setColor(Color.WHITE);
    Rect bounds = new Rect();
    paint.getTextBounds(value, 0, value.length(), bounds);
    c.save();
    c.translate(x, 2160 - y);
    c.scale(scaleX, scaleY);
    c.drawText(value, 0, -(bounds.top + bounds.bottom) / 2f, paint);
    c.restore();
  }

  private void image(
      Canvas c,
      String name,
      float x,
      float y,
      float width,
      float height,
      boolean flip,
      boolean transparent) {
    Bitmap bitmap = texture(name, transparent);
    c.save();
    c.translate(x, 2160 - y);
    if (flip) c.scale(-1, 1);
    c.drawBitmap(bitmap, null, new RectF(-width / 2, -height / 2, width / 2, height / 2), paint);
    c.restore();
  }

  private Bitmap texture(String name, boolean transparent) {
    String key = name + (transparent ? ":transparent" : "");
    Bitmap hit = TEXTURES.get(key);
    if (hit != null) return hit;
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = name.startsWith("circle") ? 4 : 8;
    options.inMutable = transparent;
    try (var stream = context.getAssets().open("reload/" + name + ".png")) {
      Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
      if (bitmap == null) throw new IllegalStateException("Invalid Reload texture: " + name);
      if (transparent) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
          int p = pixels[i];
          if (Color.red(p) < 65 && Color.green(p) < 65 && Color.blue(p) < 80) pixels[i] = 0;
        }
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
      }
      TEXTURES.put(key, bitmap);
      return bitmap;
    } catch (IOException e) {
      throw new IllegalStateException("Missing Reload texture: " + name, e);
    }
  }

  static double phase(LocalDate date) {
    double cycles =
        (date.toEpochDay() - LocalDate.of(2000, 1, 6).toEpochDay() - 0.25972) / 29.530588853;
    return cycles - Math.floor(cycles);
  }
}
