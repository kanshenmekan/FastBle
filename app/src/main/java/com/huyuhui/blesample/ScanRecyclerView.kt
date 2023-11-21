package com.huyuhui.blesample

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.huyuhui.blesample.adapter.ScanFilterAdapter


class ScanRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    companion object {
        @JvmStatic
        private val FILTER_STATUS_HIDDEN = 1

        @JvmStatic
        private val FILTER_STATUS_SHOWN = 2

        @JvmStatic
        private val FILTER_STATUS_SHOWING = 3
    }

    private var mCurrentFilterStatus = FILTER_STATUS_SHOWN

    private var hasRecord = false
    private var mStartEventY: Float = 0f

    private val scanFilterView
        get() = ((adapter as ConcatAdapter).adapters[0] as ScanFilterAdapter).scanFilterBinding?.root

    private val filterViewHeight
        get() = scanFilterView?.measuredHeight ?: 0


    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        if (mCurrentFilterStatus == FILTER_STATUS_SHOWN) {
            if ((layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 1) {
                scanFilterView?.let {
                    it.updateLayoutParams<MarginLayoutParams> {
                        topMargin = -filterViewHeight + 1
                    }
                }
                mCurrentFilterStatus = FILTER_STATUS_HIDDEN
                closeSoftInput()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mCurrentFilterStatus == FILTER_STATUS_SHOWN) {
            return super.dispatchTouchEvent(ev)
        }
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                record(ev)
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (mCurrentFilterStatus == FILTER_STATUS_SHOWING) {
                    restoreFilterView()
                }
                hasRecord = false
            }

        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (mCurrentFilterStatus == FILTER_STATUS_SHOWN || !canShowFilter()) {
            return super.onTouchEvent(ev)
        }
        when (ev?.action) {
            MotionEvent.ACTION_MOVE -> {
                record(ev)
                val distance = (ev.rawY - mStartEventY) * 0.3
                if (distance > 0) {
                    scrollToPosition(0)
                    mCurrentFilterStatus = FILTER_STATUS_SHOWING
                    val marginTop = (distance - filterViewHeight).let {
                        if (it > 0) {
                            -1
                        } else
                            it
                    }
                    scanFilterView?.let {
                        it.updateLayoutParams<MarginLayoutParams> {
                            topMargin = marginTop.toInt()
                        }
                    }
                    if (distance > filterViewHeight - 1) {
                        mCurrentFilterStatus = FILTER_STATUS_SHOWN
                        hasRecord = false
                    }
                    return false
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    // 是否滚动到表头了
    private fun canShowFilter(): Boolean {
        return !canScrollVertically(-1) && scrollY <= 0
    }

    private fun restoreFilterView() {
        val currentTopMargin = (scanFilterView?.layoutParams as MarginLayoutParams).topMargin
        if ((currentTopMargin + filterViewHeight.toFloat()) / filterViewHeight < 0.3) {
            val animator: ValueAnimator =
                ObjectAnimator.ofFloat(
                    currentTopMargin.toFloat(),
                    -filterViewHeight + 1f
                )
                    .setDuration(300)
            animator.addUpdateListener {
                scanFilterView?.let { v ->
                    v.updateLayoutParams<MarginLayoutParams> {
                        topMargin = (it.animatedValue as Float).toInt()
                    }
                }
            }
            mCurrentFilterStatus = FILTER_STATUS_HIDDEN
            closeSoftInput()
            animator.start()
        } else {
            val animator: ValueAnimator =
                ObjectAnimator.ofFloat(
                    currentTopMargin.toFloat(),
                    1f
                )
                    .setDuration(300)
            animator.addUpdateListener {
                scanFilterView?.let { v ->
                    v.updateLayoutParams<MarginLayoutParams> {
                        topMargin = (it.animatedValue as Float).toInt()
                    }
                    scrollToPosition(0)
                }
            }
            mCurrentFilterStatus = FILTER_STATUS_SHOWN
            animator.start()
        }
    }

    private fun closeSoftInput() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun record(ev: MotionEvent) {
        if (!hasRecord && canShowFilter()) {
            mStartEventY = ev.rawY
            hasRecord = true
        }
    }
}