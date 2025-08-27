package com.artifex.mupdf.viewer

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.mupdfviewer.R

class DrawingToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var pageView: PageView? = null
    private var onDrawingModeChanged: ((Boolean) -> Unit)? = null
    
    // Tool buttons
    private lateinit var penButton: ImageButton
    private lateinit var highlighterButton: ImageButton
    private lateinit var eraserButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var redoButton: ImageButton
    private lateinit var clearButton: ImageButton
    private lateinit var closeButton: ImageButton
    
    // Color buttons
    private lateinit var redButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var greenButton: ImageButton
    private lateinit var yellowButton: ImageButton
    private lateinit var blackButton: ImageButton
    
    // Stroke width control
    private lateinit var strokeWidthSeekBar: SeekBar
    private lateinit var strokeWidthText: TextView
    
    private val colors = mutableMapOf<Int, ImageButton>()
    
    init {
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        orientation = VERTICAL
        setBackgroundColor(Color.WHITE)
        elevation = 8f
        
        // Main toolbar
        val mainToolbar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        
        // Tool buttons
        penButton = createToolButton(R.drawable.ic_pen, "Pen")
        highlighterButton = createToolButton(R.drawable.ic_pen, "Highlighter")
        eraserButton = createToolButton(R.drawable.ic_pen, "Eraser")
        
        // Action buttons
        undoButton = createToolButton(R.drawable.ic_pen, "Undo")
        redoButton = createToolButton(R.drawable.ic_pen, "Redo")
        clearButton = createToolButton(R.drawable.ic_pen, "Clear")
        closeButton = createToolButton(R.drawable.ic_close_white_24dp, "Close")
        
        // Add tool buttons to main toolbar
        mainToolbar.addView(penButton)
        mainToolbar.addView(highlighterButton)
        mainToolbar.addView(eraserButton)
        mainToolbar.addView(createSeparator())
        mainToolbar.addView(undoButton)
        mainToolbar.addView(redoButton)
        mainToolbar.addView(clearButton)
        mainToolbar.addView(createSeparator())
        mainToolbar.addView(closeButton)
        
        // Color toolbar
        val colorToolbar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        
        // Color buttons
        redButton = createColorButton(Color.RED)
        blueButton = createColorButton(Color.BLUE)
        greenButton = createColorButton(Color.GREEN)
        yellowButton = createColorButton(Color.YELLOW)
        blackButton = createColorButton(Color.BLACK)
        
        // Add to colors map
        colors[Color.RED] = redButton
        colors[Color.BLUE] = blueButton
        colors[Color.GREEN] = greenButton
        colors[Color.YELLOW] = yellowButton
        colors[Color.BLACK] = blackButton
        
        colorToolbar.addView(redButton)
        colorToolbar.addView(blueButton)
        colorToolbar.addView(greenButton)
        colorToolbar.addView(yellowButton)
        colorToolbar.addView(blackButton)
        
        // Stroke width control
        val strokeWidthLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        
        strokeWidthText = TextView(context).apply {
            text = "Width: 3"
            setPadding(0, 0, 16, 0)
        }
        
        strokeWidthSeekBar = SeekBar(context).apply {
            max = 20
            progress = 3
        }
        
        strokeWidthLayout.addView(strokeWidthText)
        strokeWidthLayout.addView(strokeWidthSeekBar)
        
        // Add all components
        addView(mainToolbar)
        addView(createSeparator())
        addView(colorToolbar)
        addView(createSeparator())
        addView(strokeWidthLayout)
    }
    
    private fun createToolButton(iconRes: Int, contentDescription: String): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            this.contentDescription = contentDescription
            setBackgroundResource(android.R.drawable.btn_default_small)
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            }
        }
    }
    
    private fun createColorButton(color: Int): ImageButton {
        return ImageButton(context).apply {
            setBackgroundColor(color)
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                marginEnd = 8
            }
        }
    }
    
    private fun createSeparator(): View {
        return View(context).apply {
            setBackgroundColor(Color.LTGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(16, 4, 16, 4)
            }
        }
    }
    
    private fun setupListeners() {
        // Tool buttons
        penButton.setOnClickListener {
            pageView?.setDrawingTool(DrawingTool.PEN)
            updateToolSelection(DrawingTool.PEN)
        }
        
        highlighterButton.setOnClickListener {
            pageView?.setDrawingTool(DrawingTool.HIGHLIGHTER)
            updateToolSelection(DrawingTool.HIGHLIGHTER)
        }
        
        eraserButton.setOnClickListener {
            pageView?.setDrawingTool(DrawingTool.ERASER)
            updateToolSelection(DrawingTool.ERASER)
        }
        
        // Action buttons
        undoButton.setOnClickListener {
            pageView?.undoDrawing()
            updateActionButtons()
        }
        
        redoButton.setOnClickListener {
            pageView?.redoDrawing()
            updateActionButtons()
        }
        
        clearButton.setOnClickListener {
            pageView?.clearDrawing()
            updateActionButtons()
        }
        
        closeButton.setOnClickListener {
            setDrawingMode(false)
        }
        
        // Color buttons
        redButton.setOnClickListener { setColor(Color.RED) }
        blueButton.setOnClickListener { setColor(Color.BLUE) }
        greenButton.setOnClickListener { setColor(Color.GREEN) }
        yellowButton.setOnClickListener { setColor(Color.YELLOW) }
        blackButton.setOnClickListener { setColor(Color.BLACK) }
        
        // Stroke width
        strokeWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pageView?.setDrawingStrokeWidth(progress.toFloat())
                    strokeWidthText.text = "Width: $progress"
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun updateToolSelection(selectedTool: DrawingTool) {
        // Reset all tool button backgrounds
        penButton.setBackgroundResource(android.R.drawable.btn_default_small)
        highlighterButton.setBackgroundResource(android.R.drawable.btn_default_small)
        eraserButton.setBackgroundResource(android.R.drawable.btn_default_small)
        
        // Highlight selected tool
        when (selectedTool) {
            DrawingTool.PEN -> penButton.setBackgroundResource(android.R.drawable.btn_default_small)
            DrawingTool.HIGHLIGHTER -> highlighterButton.setBackgroundResource(android.R.drawable.btn_default_small)
            DrawingTool.ERASER -> eraserButton.setBackgroundResource(android.R.drawable.btn_default_small)
            else -> {}
        }
    }
    
    private fun updateActionButtons() {
        pageView?.let { view ->
            undoButton.isEnabled = view.canUndoDrawing()
            redoButton.isEnabled = view.canRedoDrawing()
            clearButton.isEnabled = view.hasDrawing()
        }
    }
    
    private fun setColor(color: Int) {
        pageView?.setDrawingColor(color)
        updateColorSelection(color)
    }
    
    private fun updateColorSelection(selectedColor: Int) {
        // Reset all color button borders
        colors.values.forEach { button ->
            button.setPadding(12, 12, 12, 12)
        }
        
        // Highlight selected color
        colors[selectedColor]?.setPadding(8, 8, 8, 8)
    }
    
    fun setPageView(view: PageView) {
        pageView = view
        updateActionButtons()
    }
    
    fun setDrawingMode(enabled: Boolean) {
        pageView?.enableDrawingMode(enabled)
        onDrawingModeChanged?.invoke(enabled)
    }
    
    fun setOnDrawingModeChangedListener(listener: (Boolean) -> Unit) {
        onDrawingModeChanged = listener
    }
    
    fun updateState() {
        pageView?.let { view ->
            // Note: PageView doesn't expose currentTool and currentColor directly
            // We'll need to track these separately if needed
            updateActionButtons()
        }
    }
}
