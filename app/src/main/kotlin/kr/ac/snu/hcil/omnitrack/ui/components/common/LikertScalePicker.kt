package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class LikertScalePicker : View {

    var leftMost: Int by Delegates.observable(1) {
        prop, old, new ->
        if (old != new) {
            refreshVariableSizes()
            invalidate()
        }
    }

    var rightMost: Int by Delegates.observable(5) {
        prop, old, new ->
        if (old != new) {
            refreshVariableSizes()
            invalidate()
        }
    }


    var leftLabel: String by Delegates.observable(OTApplication.app.resources.getString(R.string.msg_rating_options_leftmost_label_example)) {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var middleLabel: String by Delegates.observable("") {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var rightLabel: String by Delegates.observable(OTApplication.app.resources.getString(R.string.msg_rating_options_rightmost_label_example)) {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var allowIntermediate: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (old != new) {
            invalidate()
        }
    }

    var value: Float by Delegates.observable(((rightMost + leftMost) shr 1).toFloat()) {
        prop, old, new ->
        if (old != new) {
            invalidate()
            valueChanged.invoke(this, new)
        }
    }

    val valueChanged = Event<Float>()

    val numPoints: Int get() = Math.abs(rightMost - leftMost) + 1

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0

    private var intrinsicWidth: Int = 0
    private var intrinsicHeight: Int = 0

    private var _pointDistance: Float = 0f
    private var _lineY: Float = 0f
    private var _valueY: Float = 0f
    private var _numberY: Float = 0f
    private var _labelY: Float = 0f
    private var _lineLeft: Float = 0f
    private var _lineRight: Float = 0f


    private val valueBoxVerticalPadding: Float
    private val valueBoxHorizontalPadding: Float
    private val valueBoxSpacing: Float
    private val valueIndicatorRadius: Float
    private val valueTextSize: Float
    private val pointRadius: Float
    private val numberSpacing: Float
    private val numberTextSize: Float
    private val labelTextSize: Float
    private val labelSpacing: Float

    private val labelTextPaint: Paint
    private val numberTextPaint: Paint
    private val valueTextPaint: Paint
    private val valueBoxPaint: Paint
    private val pointPaint: Paint
    private val linePaint: Paint
    private val valueIndicatorPaint: Paint

    private val boundRect = Rect()
    private val boxRect = RectF()

    init {
        valueBoxHorizontalPadding = resources.getDimension(R.dimen.likert_scale_value_box_padding_horizontal)
        valueBoxVerticalPadding = resources.getDimension(R.dimen.likert_scale_value_box_padding_vertical)
        valueBoxSpacing = resources.getDimension(R.dimen.likert_scale_value_box_spacing)
        valueIndicatorRadius = resources.getDimension(R.dimen.likert_scale_value_indicator_radius)
        valueTextSize = resources.getDimension(R.dimen.likert_scale_value_textSize)
        pointRadius = resources.getDimension(R.dimen.likert_scale_point_radius)
        numberSpacing = resources.getDimension(R.dimen.likert_scale_number_spacing)
        numberTextSize = resources.getDimension(R.dimen.likert_scale_number_textSize)
        labelTextSize = resources.getDimension(R.dimen.likert_scale_label_textSize)
        labelSpacing = resources.getDimension(R.dimen.likert_scale_label_spacing)

        labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        valueBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        valueIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = resources.getDimension(R.dimen.likert_scale_line_stroke_width)
        linePaint.color = resources.getColor(R.color.textColorLight, null)
        linePaint.alpha = 200

        pointPaint.style = Paint.Style.FILL
        pointPaint.color = resources.getColor(R.color.textColorLight, null)

        numberTextPaint.style = Paint.Style.FILL
        numberTextPaint.color = resources.getColor(R.color.textColorMidLight, null)
        numberTextPaint.textAlign = Paint.Align.CENTER
        numberTextPaint.textSize = numberTextSize
        numberTextPaint.isFakeBoldText = true

        valueIndicatorPaint.style = Paint.Style.FILL
        valueIndicatorPaint.color = resources.getColor(R.color.colorSecondary, null)

        valueTextPaint.style = Paint.Style.FILL
        valueTextPaint.textSize = valueTextSize
        valueTextPaint.color = Color.WHITE
        valueTextPaint.textAlign = Paint.Align.CENTER

        valueBoxPaint.style = Paint.Style.FILL
        valueBoxPaint.color = resources.getColor(R.color.colorSecondary, null)

        calcIntrinsicSize()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)


    private fun calcIntrinsicSize() {
        intrinsicHeight = (2 * valueBoxVerticalPadding + valueTextSize + valueBoxSpacing + numberSpacing + numberTextSize + labelSpacing +
                Math.max(Math.max(textHeight(leftLabel, labelTextPaint), textHeight(rightLabel, labelTextPaint)), textHeight(middleLabel, labelTextPaint)) + 0.5f).toInt()

        intrinsicWidth = contentWidth
    }

    private fun textHeight(text: String, paint: Paint): Int {
        paint.getTextBounds(text, 0, text.length, boundRect)
        return boundRect.height()
    }

    private fun textWidth(text: String, paint: Paint): Int {
        paint.getTextBounds(text, 0, text.length, boundRect)
        return boundRect.width()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(_lineLeft, _lineY, _lineRight, _lineY, linePaint)

        for (i in 0..numPoints - 1) {
            val centerX = _lineLeft + _pointDistance * i
            val numberText = (i + leftMost).toString()

            canvas.drawCircle(centerX, _lineY, pointRadius, pointPaint)

            val numberX: Float = getWrappedCenterPoint(centerX, textWidth(numberText, numberTextPaint) / 2f)

            canvas.drawText(numberText, numberX, _lineY + numberTextSize + numberSpacing, numberTextPaint)
        }

        val valuePosition = convertValueToCoordinate(value)
        //draw value
        canvas.drawCircle(valuePosition, _lineY, valueIndicatorRadius, valueIndicatorPaint)

        val valueText = value.toString()
        valueTextPaint.getTextBounds(valueText, 0, valueText.length, boundRect)
        val valueCenter = getWrappedCenterPoint(valuePosition, boundRect.width() / 2 + valueBoxHorizontalPadding)
        boxRect.set(valueCenter - boundRect.width() / 2 - valueBoxHorizontalPadding, 0f, valueCenter + boundRect.width() / 2 + valueBoxHorizontalPadding, valueTextSize + 2 * valueBoxVerticalPadding)
        canvas.drawRoundRect(boxRect, 15f, 15f, valueBoxPaint)

        canvas.drawText(valueText, valueCenter, (boxRect.top + boxRect.bottom) / 2 + boundRect.height() / 2, valueTextPaint)
    }

    fun getWrappedCenterPoint(desired: Float, contentHalfWidth: Float): Float {
        val left = paddingLeft
        val right = paddingLeft + contentWidth

        return Math.min(Math.max(
                desired,
                contentHalfWidth + left
        ),
                right - contentHalfWidth
        )
    }

    fun convertValueToCoordinate(value: Float): Float {
        return (_lineRight - _lineLeft) * ((value - leftMost) / (rightMost - leftMost)) + _lineLeft
    }

    fun convertCoordinateToValue(x: Float): Float {
        return (rightMost - leftMost) * ((x - _lineLeft) / (_lineRight - _lineLeft)) + leftMost
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
        } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            handleTouchEvent(event)

            return true
        } else if (event.action == MotionEvent.ACTION_MOVE) {

            handleTouchEvent(event)
            return true
        } else if (event.action == MotionEvent.ACTION_UP) {
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun handleTouchEvent(event: MotionEvent) {
        if (event.x < _lineLeft) {
            value = leftMost.toFloat()
        } else if (event.x > _lineRight) {
            value = rightMost.toFloat()
        } else {
            value = (convertCoordinateToValue(event.x) * 10 + .5f).toInt() / 10f
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        var measuredWidth: Int
        var measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(intrinsicWidth + paddingLeft + paddingRight, widthSize).toInt()
        } else {
            measuredWidth = intrinsicWidth.toInt() + paddingLeft + paddingRight
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(intrinsicHeight + paddingTop + paddingBottom, heightSize).toInt()
        } else {
            measuredHeight = intrinsicHeight.toInt() + paddingTop + paddingBottom
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun refreshVariableSizes() {

        _valueY = valueBoxVerticalPadding + valueTextSize
        _lineY = _valueY + valueBoxVerticalPadding + valueBoxSpacing
        _numberY = _lineY + numberSpacing
        _labelY = _numberY + labelSpacing

        _lineLeft = paddingLeft + Math.max(pointRadius, valueIndicatorRadius)
        _lineRight = paddingLeft + contentWidth - Math.max(pointRadius, valueIndicatorRadius)


        _pointDistance = if (numPoints > 1) {
            (_lineRight - _lineLeft) / (numPoints - 1)
        } else {
            0f
        }

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            contentWidth = measuredWidth - paddingStart - paddingEnd
            contentHeight = measuredHeight - paddingTop - paddingBottom

            refreshVariableSizes()
        }
    }
}