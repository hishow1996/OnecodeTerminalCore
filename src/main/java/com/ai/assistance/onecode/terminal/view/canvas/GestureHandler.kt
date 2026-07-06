package com.ai.assistance.onecode.terminal.view.canvas

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.max
import kotlin.math.min

/**
 * 手势处理器
 * 支持缩放、滚动、文本选择
 */
class GestureHandler(
    context: Context,
    private val onScale: (Float) -> Unit,
    private val onScroll: (Float, Float) -> Unit,
    private val onFling: ((Float, Float) -> Unit)? = null,
    private val onDoubleTap: (Float, Float) -> Unit,
    private val onLongPress: (Float, Float) -> Unit,
    // 缩放手势结束时的一次性回调：用于把积累的字号变化一次性同步给终端（resize + SIGWINCH），
    // 避免在手势过程中每帧 resize/SIGWINCH 与 TUI 互相竞争产生错位。
    private val onScaleEnd: (() -> Unit)? = null
) {
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    
    var isScaling = false
        private set
    
    init {
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                onScale(scaleFactor)
                return true
            }
            
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                return true
            }
            
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                onScaleEnd?.invoke()
            }
        })
        
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isScaling) {
                    onScroll(distanceX, distanceY)
                    return true
                }
                return false
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap(e.x, e.y)
                return true
            }
            
            override fun onLongPress(e: MotionEvent) {
                onLongPress(e.x, e.y)
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (!isScaling) {
                    onFling?.invoke(velocityX, velocityY)
                    return true
                }
                return false
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
    }
    
    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled
    }
}

/**
 * 文本选择管理器
 */
class TextSelectionManager {
    enum class DragHandle { NONE, START, END }
    
    data class Selection(
        val startRow: Int,
        val startCol: Int,
        val endRow: Int,
        val endCol: Int
    ) {
        fun contains(row: Int, col: Int): Boolean {
            val (sr, sc, er, ec) = normalize()
            return when {
                row < sr || row > er -> false
                row == sr && row == er -> col in sc..ec
                row == sr -> col >= sc
                row == er -> col <= ec
                else -> true
            }
        }
        
        fun normalize(): Selection {
            return if (startRow < endRow || (startRow == endRow && startCol <= endCol)) {
                this
            } else {
                Selection(endRow, endCol, startRow, startCol)
            }
        }
    }
    
    var selection: Selection? = null
        private set
    
    var activeDragHandle = DragHandle.NONE
    
    private var selectionStart: Pair<Int, Int>? = null
    
    fun startSelection(row: Int, col: Int) {
        selectionStart = Pair(row, col)
        selection = Selection(row, col, row, col)
    }
    
    fun updateSelection(row: Int, col: Int) {
        selectionStart?.let { (startRow, startCol) ->
            selection = Selection(startRow, startCol, row, col)
        }
    }
    
    fun updateStartSelection(row: Int, col: Int) {
        selection?.let { sel ->
            selection = Selection(row, col, sel.endRow, sel.endCol)
        }
    }
    
    fun updateEndSelection(row: Int, col: Int) {
        selection?.let { sel ->
            selection = Selection(sel.startRow, sel.startCol, row, col)
        }
    }
    
    fun setSelection(startRow: Int, startCol: Int, endRow: Int, endCol: Int) {
        selection = Selection(startRow, startCol, endRow, endCol)
        selectionStart = Pair(startRow, startCol)
    }
    
    fun clearSelection() {
        selection = null
        selectionStart = null
        activeDragHandle = DragHandle.NONE
    }
    
    fun hasSelection(): Boolean = selection != null
}

