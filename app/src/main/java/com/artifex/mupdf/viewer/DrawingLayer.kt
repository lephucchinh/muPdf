package com.artifex.mupdf.viewer

import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

data class DrawingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DrawingStroke(
    val points: MutableList<DrawingPoint> = mutableListOf(),
    val color: Int = Color.RED,
    val strokeWidth: Float = 3.0f,
    val strokeStyle: StrokeStyle = StrokeStyle.SOLID
)

enum class StrokeStyle {
    SOLID, DASHED, DOTTED
}

enum class DrawingTool {
    PEN, HIGHLIGHTER, ERASER, SHAPE, TEXT
}

class DrawingLayer(private val view: View) {
    
    private val strokes = mutableListOf<DrawingStroke>()
    private val redoStrokes = mutableListOf<DrawingStroke>()
    
    private var currentStroke: DrawingStroke? = null
    var currentTool = DrawingTool.PEN
    var currentColor = Color.RED
    var currentStrokeWidth = 3.0f
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    private val path = Path()
    private val tempPath = Path()
    
    // Drawing settings
    var isDrawingEnabled = true
    var isVisible = true
    
    fun setTool(tool: DrawingTool) {
        currentTool = tool
        when (tool) {
            DrawingTool.PEN -> {
                currentStrokeWidth = 3.0f
                paint.strokeWidth = currentStrokeWidth
                paint.style = Paint.Style.STROKE
                paint.alpha = 255
            }
            DrawingTool.HIGHLIGHTER -> {
                currentStrokeWidth = 15.0f
                paint.strokeWidth = currentStrokeWidth
                paint.alpha = 128
                paint.style = Paint.Style.STROKE
            }
            DrawingTool.ERASER -> {
                currentStrokeWidth = 20.0f
                paint.strokeWidth = currentStrokeWidth
                paint.style = Paint.Style.STROKE
                paint.alpha = 255
            }
            DrawingTool.SHAPE -> {
                currentStrokeWidth = 3.0f
                paint.strokeWidth = currentStrokeWidth
                paint.style = Paint.Style.STROKE
                paint.alpha = 255
            }
            DrawingTool.TEXT -> {
                paint.style = Paint.Style.FILL
                paint.alpha = 255
            }
        }
    }
    
    fun setColor(color: Int) {
        currentColor = color
        paint.color = color
    }
    
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        paint.strokeWidth = width
    }
    
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled || !isVisible) return false
        
        val x = event.x
        val y = event.y
        val pressure = event.pressure
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startStroke(x, y, pressure)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                addPointToStroke(x, y, pressure)
                return true
            }
            MotionEvent.ACTION_UP -> {
                endStroke()
                return true
            }
        }
        return false
    }
    
    private fun startStroke(x: Float, y: Float, pressure: Float) {
        currentStroke = DrawingStroke(
            color = currentColor,
            strokeWidth = currentStrokeWidth,
            strokeStyle = when (currentTool) {
                DrawingTool.PEN -> StrokeStyle.SOLID
                DrawingTool.HIGHLIGHTER -> StrokeStyle.SOLID
                DrawingTool.ERASER -> StrokeStyle.SOLID
                DrawingTool.SHAPE -> StrokeStyle.SOLID
                DrawingTool.TEXT -> StrokeStyle.SOLID
            }
        )
        
        currentStroke?.points?.add(DrawingPoint(x, y, pressure))
        path.reset()
        path.moveTo(x, y)
        
        // Clear redo stack when new stroke starts
        redoStrokes.clear()
    }
    
    private fun addPointToStroke(x: Float, y: Float, pressure: Float) {
        currentStroke?.let { stroke ->
            stroke.points.add(DrawingPoint(x, y, pressure))
            
            // Smooth the path for better drawing
            if (stroke.points.size >= 3) {
                val lastIndex = stroke.points.size - 1
                val prevPoint = stroke.points[lastIndex - 2]
                val currentPoint = stroke.points[lastIndex - 1]
                val nextPoint = stroke.points[lastIndex]
                
                // Use quadratic bezier for smooth curves
                val controlX = (currentPoint.x + nextPoint.x) / 2
                val controlY = (currentPoint.y + nextPoint.y) / 2
                path.quadTo(currentPoint.x, currentPoint.y, controlX, controlY)
            } else {
                path.lineTo(x, y)
            }
        }
        view.invalidate()
    }
    
    private fun endStroke() {
        currentStroke?.let { stroke ->
            if (stroke.points.size > 1) {
                strokes.add(stroke)
                view.invalidate()
            }
        }
        currentStroke = null
        path.reset()
    }
    
    fun draw(canvas: Canvas) {
        if (!isVisible) return
        
        // Draw completed strokes
        for (stroke in strokes) {
            drawStroke(canvas, stroke)
        }
        
        // Draw current stroke
        currentStroke?.let { stroke ->
            drawStroke(canvas, stroke, path)
        }
    }
    
    private fun drawStroke(canvas: Canvas, stroke: DrawingStroke, customPath: Path? = null) {
        paint.color = stroke.color
        paint.strokeWidth = stroke.strokeWidth
        
        when (stroke.strokeStyle) {
            StrokeStyle.SOLID -> {
                paint.pathEffect = null
            }
            StrokeStyle.DASHED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
            }
            StrokeStyle.DOTTED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
        }
        
        if (customPath != null) {
            canvas.drawPath(customPath, paint)
        } else {
            // Reconstruct path from points
            tempPath.reset()
            if (stroke.points.isNotEmpty()) {
                tempPath.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    val point = stroke.points[i]
                    tempPath.lineTo(point.x, point.y)
                }
            }
            canvas.drawPath(tempPath, paint)
        }
    }
    
    fun undo(): Boolean {
        if (strokes.isNotEmpty()) {
            val lastStroke = strokes.removeAt(strokes.size - 1)
            redoStrokes.add(lastStroke)
            view.invalidate()
            return true
        }
        return false
    }
    
    fun redo(): Boolean {
        if (redoStrokes.isNotEmpty()) {
            val stroke = redoStrokes.removeAt(redoStrokes.size - 1)
            strokes.add(stroke)
            view.invalidate()
            return true
        }
        return false
    }
    
    fun clear() {
        strokes.clear()
        redoStrokes.clear()
        currentStroke = null
        path.reset()
        view.invalidate()
    }
    
    fun getStrokes(): List<DrawingStroke> = strokes.toList()
    
    fun setStrokes(newStrokes: List<DrawingStroke>) {
        strokes.clear()
        strokes.addAll(newStrokes)
        redoStrokes.clear()
        view.invalidate()
    }
    
    fun hasStrokes(): Boolean = strokes.isNotEmpty() || currentStroke != null
    
    fun canUndo(): Boolean = strokes.isNotEmpty()
    
    fun canRedo(): Boolean = redoStrokes.isNotEmpty()
}
