package com.artifex.mupdf.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val drawingLayer = DrawingLayer(this)
    
    // Drawing toolbar state
    var isDrawingMode = false
        set(value) {
            field = value
            drawingLayer.isDrawingEnabled = value
            if (!value) {
                drawingLayer.clear()
            }
        }
    
    // Drawing settings
    var currentTool: DrawingTool
        get() = drawingLayer.currentTool
        set(value) = drawingLayer.setTool(value)
    
    var currentColor: Int
        get() = drawingLayer.currentColor
        set(value) = drawingLayer.setColor(value)
    
    var currentStrokeWidth: Float
        get() = drawingLayer.currentStrokeWidth
        set(value) = drawingLayer.setStrokeWidth(value)
    
    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the drawing layer on top of everything
        if (isDrawingMode) {
            drawingLayer.draw(canvas)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDrawingMode && drawingLayer.onTouchEvent(event)) {
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isDrawingMode && ev != null) {
            return drawingLayer.onTouchEvent(ev)
        }
        return super.onInterceptTouchEvent(ev)
    }
    
    // Drawing control methods
    fun enableDrawing(enable: Boolean) {
        isDrawingMode = enable
    }
    
    fun setDrawingTool(tool: DrawingTool) {
        currentTool = tool
    }
    
    fun setDrawingColor(color: Int) {
        currentColor = color
    }
    
    fun setDrawingStrokeWidth(width: Float) {
        currentStrokeWidth = width
    }
    
    fun undo() = drawingLayer.undo()
    
    fun redo() = drawingLayer.redo()
    
    fun clearDrawing() {
        drawingLayer.clear()
    }
    
    fun hasDrawing(): Boolean = drawingLayer.hasStrokes()
    
    fun canUndo(): Boolean = drawingLayer.canUndo()
    
    fun canRedo(): Boolean = drawingLayer.canRedo()
    
    fun getDrawingStrokes() = drawingLayer.getStrokes()
    
    fun setDrawingStrokes(strokes: List<DrawingStroke>) {
        drawingLayer.setStrokes(strokes)
    }
    
    fun showDrawing(show: Boolean) {
        drawingLayer.isVisible = show
        invalidate()
    }
}
