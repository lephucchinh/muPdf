package com.artifex.mupdf.viewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/**
 * Demo class để test tính năng vẽ
 * Sử dụng để kiểm tra các chức năng vẽ cơ bản
 */
class DrawingDemo(context: Context) : View(context) {
    
    private val drawingLayer = DrawingLayer(this)
    
    init {
        // Thiết lập mặc định
        drawingLayer.setTool(DrawingTool.PEN)
        drawingLayer.setColor(Color.RED)
        drawingLayer.setStrokeWidth(5.0f)
        drawingLayer.isDrawingEnabled = true
        drawingLayer.isVisible = true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Vẽ background
        canvas.drawColor(Color.WHITE)
        
        // Vẽ các nét vẽ
        drawingLayer.draw(canvas)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return drawingLayer.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    // API để test
    fun setTool(tool: DrawingTool) {
        drawingLayer.setTool(tool)
    }
    
    fun setColor(color: Int) {
        drawingLayer.setColor(color)
    }
    
    fun setStrokeWidth(width: Float) {
        drawingLayer.setStrokeWidth(width)
    }
    
    fun undo() = drawingLayer.undo()
    
    fun redo() = drawingLayer.redo()
    
    fun clear() {
        drawingLayer.clear()
    }
    
    fun hasStrokes() = drawingLayer.hasStrokes()
    
    fun canUndo() = drawingLayer.canUndo()
    
    fun canRedo() = drawingLayer.canRedo()
    
    fun getStrokes() = drawingLayer.getStrokes()
    
    fun setStrokes(strokes: List<DrawingStroke>) {
        drawingLayer.setStrokes(strokes)
    }
    
    fun enableDrawing(enable: Boolean) {
        drawingLayer.isDrawingEnabled = enable
    }
    
    fun showDrawing(show: Boolean) {
        drawingLayer.isVisible = show
        invalidate()
    }
}

/**
 * Test class để kiểm tra các chức năng vẽ
 */
object DrawingTest {
    
    fun testBasicDrawing(demo: DrawingDemo) {
        println("Testing basic drawing...")
        
        // Test vẽ với pen
        demo.setTool(DrawingTool.PEN)
        demo.setColor(Color.RED)
        demo.setStrokeWidth(3.0f)
        
        // Test vẽ với highlighter
        demo.setTool(DrawingTool.HIGHLIGHTER)
        demo.setColor(Color.YELLOW)
        demo.setStrokeWidth(15.0f)
        
        // Test vẽ với eraser
        demo.setTool(DrawingTool.ERASER)
        demo.setStrokeWidth(20.0f)
        
        println("Basic drawing test completed")
    }
    
    fun testUndoRedo(demo: DrawingDemo) {
        println("Testing undo/redo...")
        
        // Test undo
        if (demo.canUndo()) {
            demo.undo()
            println("Undo performed")
        }
        
        // Test redo
        if (demo.canRedo()) {
            demo.redo()
            println("Redo performed")
        }
        
        println("Undo/redo test completed")
    }
    
    fun testColorSelection(demo: DrawingDemo) {
        println("Testing color selection...")
        
        val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.BLACK)
        
        colors.forEach { color ->
            demo.setColor(color)
            println("Color set to: $color")
        }
        
        println("Color selection test completed")
    }
    
    fun testStrokeWidth(demo: DrawingDemo) {
        println("Testing stroke width...")
        
        val widths = listOf(1.0f, 3.0f, 5.0f, 10.0f, 15.0f, 20.0f)
        
        widths.forEach { width ->
            demo.setStrokeWidth(width)
            println("Stroke width set to: $width")
        }
        
        println("Stroke width test completed")
    }
    
    fun testToolSelection(demo: DrawingDemo) {
        println("Testing tool selection...")
        
        val tools = listOf(DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.ERASER)
        
        tools.forEach { tool ->
            demo.setTool(tool)
            println("Tool set to: $tool")
        }
        
        println("Tool selection test completed")
    }
    
    fun runAllTests(demo: DrawingDemo) {
        println("Starting all drawing tests...")
        
        testBasicDrawing(demo)
        testColorSelection(demo)
        testStrokeWidth(demo)
        testToolSelection(demo)
        testUndoRedo(demo)
        
        println("All tests completed!")
    }
}
