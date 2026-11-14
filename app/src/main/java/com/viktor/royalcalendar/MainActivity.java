package com.viktor.royalcalendar;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;

public class MainActivity extends Activity {
  private LinearLayout content;
  private ImageView preview;
  private LinearLayout exactAlarmPanel;
  private boolean ru;
  private int selectedDay = LocalDate.now().getDayOfMonth();
  private int selectedPart = 0;
  private boolean binding;
  private int debugTaps;
  private Button debugToggle;
  private SeekBar xSlider, ySlider, angleSlider, sizeSlider;
  private TextView xLabel, yLabel, angleLabel, sizeLabel;
  private final android.os.Handler handler =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private final Runnable updateWidgets = () -> CalendarWidget.updateAll(this);

  private int dp(float v) {
    return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
  }

  private String t(String r, String e) {
    return ru ? r : e;
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    CalendarRenderer.upgradePlacement(this);
    build();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (preview != null) refresh();
    if (exactAlarmPanel != null)
      exactAlarmPanel.setVisibility(
          CalendarWidget.exactAlarmsAllowed(this) ? View.GONE : View.VISIBLE);
    CalendarWidget.updateAll(this);
  }

  private TextView text(String value, int size, int color) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(8), 0, dp(8));
    content.addView(v);
    return v;
  }

  private void build() {
    ru = getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ru");
    getWindow().setStatusBarColor(Color.rgb(16, 16, 18));
    getWindow().setNavigationBarColor(Color.rgb(16, 16, 18));
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(Color.rgb(16, 16, 18));
    scroll.setClipToPadding(false);
    scroll.setFillViewport(true);
    scroll.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
          return insets.consumeSystemWindowInsets();
        });
    content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(24), dp(16), dp(24), dp(24));
    scroll.addView(content);
    TextView title = text("Royal Calendar", 32, Color.WHITE);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    text(t("Каждый день — твой.", "Make every day yours."), 16, 0xffb9b9bd);
    FrameLayout stage = new FrameLayout(this);
    stage.setBackgroundColor(0xff343438);
    content.addView(stage, new LinearLayout.LayoutParams(-1, dp(270)));
    preview = new ImageView(this);
    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
    preview.setContentDescription(
        t("Предпросмотр календаря с сегодняшней датой", "Preview of today's calendar"));
    text(
        t(
            "Прозрачный фон · дата телефона · без интернета",
            "Transparent background · your phone's date · works offline"),
        14,
        0xffb9b9bd);
    Button pin = new Button(this);
    pin.setText(t("Добавить на главный экран", "Add to home screen"));
    pin.setMinHeight(dp(52));
    pin.setTextColor(Color.WHITE);
    pin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffd91532));
    content.addView(pin, new LinearLayout.LayoutParams(-1, -2));
    pin.setOnClickListener(
        v -> {
          AppWidgetManager m = getSystemService(AppWidgetManager.class);
          if (m.isRequestPinAppWidgetSupported())
            m.requestPinAppWidget(new ComponentName(this, CalendarWidget.class), null, null);
          else
            Toast.makeText(
                    this,
                    t(
                        "Удерживай пустое место на рабочем столе → Виджеты → Royal Calendar",
                        "Long-press the home screen → Widgets → Royal Calendar"),
                    Toast.LENGTH_LONG)
                .show();
        });
    addExactAlarmAccess();
    if (BuildConfig.DEBUG) {
      addEditor();
      title.setOnClickListener(
          v -> {
            if (++debugTaps == 7) {
              debugToggle.setVisibility(View.VISIBLE);
              debugToggle.performClick();
              Toast.makeText(
                      this,
                      t("Настройка для отладки включена", "Layout debug tools enabled"),
                      Toast.LENGTH_SHORT)
                  .show();
            }
          });
    }
    text(
        t(
            "Удерживай виджет, чтобы изменить размер. Нажми на него, чтобы открыть приложение.",
            "Hold the widget to resize it. Tap it to open the app."),
        15,
        0xffb9b9bd);
    text(
        t(
            "Спрайты календаря: BHDown. Неофициальный личный проект по мотивам Persona 5 / Royal."
                + " Графика игры: ATLUS / SEGA.",
            "Calendar artwork: BHDown. Unofficial personal project inspired by Persona 5 / Royal."
                + " Game artwork: ATLUS / SEGA."),
        12,
        0xff96969e);
    setContentView(scroll);
    refresh();
  }

  private LocalDate previewDate() {
    LocalDate d = LocalDate.now();
    if (selectedDay > d.lengthOfMonth()) d = d.plusMonths(1);
    return d.withDayOfMonth(selectedDay);
  }

  private void addExactAlarmAccess() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return;
    exactAlarmPanel = new LinearLayout(this);
    exactAlarmPanel.setOrientation(LinearLayout.VERTICAL);
    content.addView(exactAlarmPanel);
    TextView explanation = new TextView(this);
    explanation.setText(
        t(
            "Для смены даты в полночь разреши «Будильники и напоминания». Без этого Android может"
                + " задержать обновление на несколько минут.",
            "Allow Alarms & reminders to update the date at midnight. Without this permission,"
                + " Android may delay the update by several minutes."));
    explanation.setTextColor(0xffb9b9bd);
    explanation.setTextSize(15);
    explanation.setPadding(0, dp(12), 0, dp(8));
    exactAlarmPanel.addView(explanation);
    Button allow = new Button(this);
    allow.setText(t("Разрешить точное обновление", "Allow precise updates"));
    allow.setMinHeight(dp(48));
    exactAlarmPanel.addView(allow, new LinearLayout.LayoutParams(-1, -2));
    allow.setOnClickListener(
        v -> {
          Intent request =
              new Intent(
                  android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                  android.net.Uri.parse("package:" + getPackageName()));
          try {
            startActivity(request);
          } catch (ActivityNotFoundException unavailable) {
            Toast.makeText(
                    this,
                    t(
                        "Открой настройки приложения → Будильники и напоминания",
                        "Open app settings → Alarms & reminders"),
                    Toast.LENGTH_LONG)
                .show();
          }
        });
    exactAlarmPanel.setVisibility(
        CalendarWidget.exactAlarmsAllowed(this) ? View.GONE : View.VISIBLE);
  }

  private void refresh() {
    preview.setImageBitmap(
        new CalendarRenderer(this).render(previewDate(), CalendarRenderer.language(this), 1000));
  }

  private String key(String field) {
    return (selectedPart == 0 ? "dagger_" : "caption_") + field + "_" + selectedDay;
  }

  private android.content.SharedPreferences placement() {
    return getSharedPreferences("placement", 0);
  }

  private float standard(String field) {
    if (field.equals("size")) return selectedPart == 0 ? 135 : 100;
    if (selectedPart == 0 && field.equals("x")) return 15;
    if (selectedPart == 0 && field.equals("y")) return 9;
    if (selectedPart == 1 && field.equals("y")) return -64;
    return 0;
  }

  private void changed() {
    refresh();
    handler.removeCallbacks(updateWidgets);
    handler.postDelayed(updateWidgets, 180);
  }

  private void save(String field, float value) {
    placement().edit().putFloat(key(field), value).apply();
    changed();
  }

  private void addEditor() {
    Button toggle = new Button(this);
    debugToggle = toggle;
    toggle.setVisibility(View.GONE);
    toggle.setText(t("Отладка положения", "Layout debug tools"));
    toggle.setMinHeight(dp(48));
    content.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout editor = new LinearLayout(this);
    editor.setOrientation(LinearLayout.VERTICAL);
    editor.setVisibility(View.GONE);
    content.addView(editor);
    toggle.setOnClickListener(
        v -> {
          boolean show = editor.getVisibility() != View.VISIBLE;
          editor.setVisibility(show ? View.VISIBLE : View.GONE);
          toggle.setText(
              show
                  ? t("Скрыть настройку", "Hide adjustments")
                  : t("Настроить положение", "Adjust placement"));
        });
    LinearLayout original = content;
    content = editor;
    text(
        t(
            "Выбери число и элемент, затем перетащи его на предпросмотре или используй ползунки."
                + " Настройка сохраняется для этого числа во всех месяцах.",
            "Choose a date number and an element, then drag it in the preview or use the sliders."
                + " Adjustments apply to this date number every month."),
        15,
        0xffb9b9bd);
    text(t("Число для настройки", "Date number"), 16, Color.WHITE);
    Spinner dates = new Spinner(this);
    String[] days = new String[31];
    for (int i = 0; i < 31; i++) days[i] = Integer.toString(i + 1);
    android.widget.ArrayAdapter<String> datesAdapter =
        new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
    datesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    dates.setAdapter(datesAdapter);
    dates.setSelection(selectedDay - 1);
    editor.addView(dates, new LinearLayout.LayoutParams(-1, dp(52)));
    Spinner part = new Spinner(this);
    String[] parts = {t("Кинжал", "Dagger"), t("День недели", "Weekday")};
    android.widget.ArrayAdapter<String> partsAdapter =
        new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, parts);
    partsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    part.setAdapter(partsAdapter);
    editor.addView(part, new LinearLayout.LayoutParams(-1, dp(52)));
    xLabel = text("", 15, Color.WHITE);
    xSlider = slider(editor, "x", -150, 150);
    yLabel = text("", 15, Color.WHITE);
    ySlider = slider(editor, "y", -120, 120);
    angleLabel = text("", 15, Color.WHITE);
    angleSlider = slider(editor, "angle", -15, 15);
    sizeLabel = text("", 15, Color.WHITE);
    sizeSlider = slider(editor, "size", 60, 150);
    Button reset = new Button(this);
    reset.setText(t("Сбросить этот элемент", "Reset this element"));
    reset.setMinHeight(dp(48));
    editor.addView(reset, new LinearLayout.LayoutParams(-1, -2));
    reset.setOnClickListener(
        v -> {
          var e = placement().edit();
          for (String field : new String[] {"x", "y", "angle", "size"}) e.remove(key(field));
          e.apply();
          loadControls();
          changed();
        });
    dates.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            selectedDay = pos + 1;
            loadControls();
            refresh();
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
    part.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
            selectedPart = pos;
            loadControls();
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
    final float[] drag = new float[4];
    preview.setOnTouchListener(
        (v, event) -> {
          if (editor.getVisibility() != View.VISIBLE) return false;
          switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
              drag[0] = event.getX();
              drag[1] = event.getY();
              drag[2] = placement().getFloat(key("x"), standard("x"));
              drag[3] = placement().getFloat(key("y"), standard("y"));
              v.getParent().requestDisallowInterceptTouchEvent(true);
              return true;
            case android.view.MotionEvent.ACTION_MOVE:
              float factor =
                  Math.min(
                      preview.getWidth() / 1200f,
                      preview.getHeight() / (float) CalendarRenderer.DESIGN_HEIGHT);
              float x = Math.max(-150, Math.min(150, drag[2] + (event.getX() - drag[0]) / factor));
              float y = Math.max(-120, Math.min(120, drag[3] + (event.getY() - drag[1]) / factor));
              placement().edit().putFloat(key("x"), x).putFloat(key("y"), y).apply();
              loadControls();
              changed();
              return true;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
              v.getParent().requestDisallowInterceptTouchEvent(false);
              v.performClick();
              return true;
          }
          return false;
        });
    loadControls();
    content = original;
  }

  private SeekBar slider(LinearLayout editor, String field, int min, int max) {
    SeekBar bar = new SeekBar(this);
    bar.setMax(max - min);
    bar.setMinimumHeight(dp(48));
    bar.setContentDescription(field);
    editor.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
    bar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int p, boolean user) {
            if (user && !binding) {
              save(field, p + min);
              labels();
            }
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {
            handler.removeCallbacks(updateWidgets);
            CalendarWidget.updateAll(MainActivity.this);
          }
        });
    return bar;
  }

  private void loadControls() {
    if (xSlider == null) return;
    binding = true;
    xSlider.setProgress(Math.round(placement().getFloat(key("x"), standard("x"))) + 150);
    ySlider.setProgress(Math.round(placement().getFloat(key("y"), standard("y"))) + 120);
    angleSlider.setProgress(Math.round(placement().getFloat(key("angle"), 0)) + 15);
    sizeSlider.setProgress(Math.round(placement().getFloat(key("size"), standard("size"))) - 60);
    binding = false;
    labels();
  }

  private void labels() {
    xLabel.setText(t("По горизонтали: ", "Horizontal: ") + (xSlider.getProgress() - 150));
    yLabel.setText(t("По вертикали: ", "Vertical: ") + (ySlider.getProgress() - 120));
    angleLabel.setText(t("Поворот: ", "Rotation: ") + (angleSlider.getProgress() - 15) + "°");
    sizeLabel.setText(t("Размер: ", "Size: ") + (sizeSlider.getProgress() + 60) + "%");
  }

  @Override
  public void onPause() {
    super.onPause();
    handler.removeCallbacks(updateWidgets);
    CalendarWidget.updateAll(this);
  }
}
