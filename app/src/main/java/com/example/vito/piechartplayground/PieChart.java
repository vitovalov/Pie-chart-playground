package com.example.vito.piechartplayground;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.*;

/**
 * Created by vito on 15/12/2017.
 */

public class PieChart extends ViewGroup {
  private List<Item> mData = new ArrayList<>();

  private float mTotal = 0.0f;

  private Paint mPiePaint;

  private int mPieRotation;

  private PieView mPieView;

  /**
   * Class constructor taking only a context. Use this constructor to create
   * {@link PieChart} objects from your own code.
   *
   * @param context
   */
  public PieChart(Context context) {
    super(context);
    init();
  }

  /**
   * Class constructor taking a context and an attribute set. This constructor
   * is used by the layout engine to construct a {@link PieChart} from a set of
   * XML attributes.
   *
   * @param context
   * @param attrs   An attribute set which can contain attributes from
   *                {@link PieChart} as well as attributes inherited
   *                from {@link View}.
   */
  public PieChart(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray a = context.getTheme().obtainStyledAttributes(
        attrs,
        R.styleable.PieChart,
        0, 0
    );

    try {
      mPieRotation = a.getInt(R.styleable.PieChart_pieRotation, 0);
    } finally {
      a.recycle();
    }

    init();
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {

  }



  //
  // Measurement functions. This example uses a simple heuristic: it assumes that
  // the pie chart should be at least as wide as its label.
  //
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Try for a width based on our minimum
    int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();

    int w = Math.max(minw, View.MeasureSpec.getSize(widthMeasureSpec));

    // Whatever the width ends up being, ask for a height that would let the pie
    // get as big as it can
    int minh = (w) + getPaddingBottom() + getPaddingTop();
    int h = Math.min(View.MeasureSpec.getSize(heightMeasureSpec), minh);

    setMeasuredDimension(w, h);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    //
    // Set dimensions for text, pie chart, etc
    //
    // Account for padding
    float xpad = (float) (getPaddingLeft() + getPaddingRight());
    float ypad = (float) (getPaddingTop() + getPaddingBottom());

    float ww = (float) w - xpad;
    float hh = (float) h - ypad;

    // Figure out how big we can make the pie.
    float diameter = Math.min(ww, hh);
    RectF mPieBounds = new RectF(
        0.0f,
        0.0f,
        diameter,
        diameter);
    mPieBounds.offsetTo(getPaddingLeft(), getPaddingTop());

    // Lay out the child view that actually draws the pie.
    mPieView.layout((int) mPieBounds.left,
        (int) mPieBounds.top,
        (int) mPieBounds.right,
        (int) mPieBounds.bottom);
    //mPieView.setPivot(mPieBounds.width() / 2, mPieBounds.height() / 2);

    onDataChanged();
  }

  /**
   * Initialize the control. This code is in a separate method so that it can be
   * called from both constructors.
   */
  private void init() {
    // Force the background to software rendering because otherwise the Blur
    // filter won't work.
    setLayerToSW(this);

    // Set up the paint for the pie slices
    mPiePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPiePaint.setStyle(Paint.Style.FILL);

    // Add a child view to draw the pie. Putting this in a child view
    // makes it possible to draw it on a separate hardware layer that rotates
    // independently
    mPieView = new PieView(getContext());
    addView(mPieView);
    //mPieView.rotateTo(mPieRotation);

    // In edit mode it's nice to have some demo data, so add that here.
    if (this.isInEditMode()) {
      Resources res = getResources();
      addItem(25, res.getColor(R.color.colorPrimary));
      addItem(75, res.getColor(R.color.colorAccent));
    }

  }

  private void setLayerToSW(View v) {
    if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
  }

  private void setLayerToHW(View v) {
    if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
      setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
  }

  /**
   * Add a new data item to this view. Adding an item adds a slice to the pie whose
   * size is proportional to the item's value. As new items are added, the size of each
   * existing slice is recalculated so that the proportions remain correct.
   *
   * @param value The value of this item.
   * @param color The ARGB color of the pie slice associated with this item.
   * @return The index of the newly added item.
   */
  public int addItem(float value, int color) {
    Item it = new Item();
    it.mColor = color;
    it.mValue = value;

    mTotal += value;

    mData.add(it);

    onDataChanged();

    return mData.size() - 1;
  }
  /**
   * Do all of the recalculations needed when the data array changes.
   */
  private void onDataChanged() {
    // When the data changes, we have to recalculate
    // all of the angles.
    int currentAngle = 0;
    for (Item it : mData) {
      it.mStartAngle = currentAngle;
      it.mEndAngle = (int) ((float) currentAngle + it.mValue * 360.0f / mTotal);
      currentAngle = it.mEndAngle;
    }
  }


  /**
   * Internal child class that draws the pie chart onto a separate hardware layer
   * when necessary.
   */
  private class PieView extends View {

    // Used for SDK < 11
    private float mRotation = 0;
    private Matrix mTransform = new Matrix();
    private PointF mPivot = new PointF();

    /**
     * Construct a PieView
     *
     * @param context
     */
    public PieView(Context context) {
      super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      if (Build.VERSION.SDK_INT < 11) {
        //mTransform.set(canvas.getMatrix());
        //mTransform.preRotate(mRotation, mPivot.x, mPivot.y);
        //canvas.setMatrix(mTransform);
      }

      for (Item it : mData) {
        mPiePaint.setColor(it.mColor);
        canvas.drawArc(mBounds,
            360 - it.mEndAngle,
            it.mEndAngle - it.mStartAngle,
            true, mPiePaint);
      }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      mBounds = new RectF(0, 0, w, h);
    }

    RectF mBounds;

    //public void rotateTo(float pieRotation) {
    //  mRotation = pieRotation;
    //  if (Build.VERSION.SDK_INT >= 11) {
    //    setRotation(pieRotation);
    //  } else {
    //    invalidate();
    //  }
    //}
    //
    //public void setPivot(float x, float y) {
    //  mPivot.x = x;
    //  mPivot.y = y;
    //  if (Build.VERSION.SDK_INT >= 11) {
    //    setPivotX(x);
    //    setPivotY(y);
    //  } else {
    //    invalidate();
    //  }
    //}
  }

  /**
   * Maintains the state for a data item.
   */
  private class Item {

    public float mValue;
    public int mColor;

    // computed values
    public int mStartAngle;
    public int mEndAngle;
  }

}
