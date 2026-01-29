package com.mdp26.mdp20.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.mdp26.mdp20.Facing;

public class CanvasView extends View {
    private final String TAG = "CanvasView";
    private int cellSize;  // Calculated dynamically
    private int offsetX, offsetY; // To center the grid
    private final Paint gridPaintRed = new Paint();
    private final Paint gridPaintBlue = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint obstacleSelectedPaint = new Paint();
    private final Paint obstaclePaint = new Paint();
    private final Paint idPaint = new Paint();
    private final Paint facingPaint = new Paint();
    private final Paint targetPaint = new Paint();
    private final Paint startRegionPaint = new Paint();
    private Grid grid;

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void init(Context context) {
        // Grid styling - Red
        gridPaintRed.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.grid_line_red));
        gridPaintRed.setStrokeWidth(2);
        gridPaintRed.setStyle(Paint.Style.STROKE);

        // Grid styling - Blue
        gridPaintBlue.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.grid_line_blue));
        gridPaintBlue.setStrokeWidth(2);
        gridPaintBlue.setStyle(Paint.Style.STROKE);

        // Label text styling
        textPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.text_primary));
        textPaint.setTextSize(20);  // Adjust for readability
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        textPaint.setFakeBoldText(true);

        // Obstacle styling
        obstaclePaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.obstacle_body));
        obstaclePaint.setStyle(Paint.Style.FILL);
        obstacleSelectedPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.obstacle_selected));
        obstacleSelectedPaint.setStyle(Paint.Style.FILL);

        // ID text styling (obstacle IDs)
        idPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.white));
        idPaint.setTextAlign(Paint.Align.CENTER);
        idPaint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        idPaint.setFakeBoldText(false);
        idPaint.setTextSize(16);

        // Target styling
        targetPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.status_success));
        targetPaint.setTextAlign(Paint.Align.CENTER);
        targetPaint.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.NORMAL));
        targetPaint.setFakeBoldText(true);
        targetPaint.setTextSize(21);

        // Facing indicator styling
        facingPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.secondary)); 
        facingPaint.setStyle(Paint.Style.FILL);

        // Initialize startRegionPaint
        startRegionPaint.setColor(androidx.core.content.ContextCompat.getColor(context, com.mdp26.mdp20.R.color.accent));
        startRegionPaint.setStrokeWidth(4);
        startRegionPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int gridSize = Grid.GRID_SIZE;

        // Compute cell size dynamically
        cellSize = Math.min(w, h) / (gridSize + 2); // +2 for axis labels

        // Offset to keep grid centered
        offsetX = (w - (gridSize * cellSize)) / 2;
        offsetY = (h - (gridSize * cellSize)) / 2;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        drawStartRegion(canvas);
        drawAxisLabels(canvas);
        drawObstacles(canvas);
    }

    private void drawGrid(Canvas canvas) {
        int gridSize = Grid.GRID_SIZE;
        int gridWidth = gridSize * cellSize;
        int gridHeight = gridSize * cellSize;

        // Draw vertical grid lines (alternating colors)
        for (int i = 0; i <= gridSize; i++) {
            Paint paintToUse = (i % 2 == 0) ? gridPaintRed : gridPaintBlue;
            canvas.drawLine(offsetX + i * cellSize, offsetY, offsetX + i * cellSize, offsetY + gridHeight, paintToUse);
        }

        // Draw horizontal grid lines (alternating colors)
        for (int i = 0; i <= gridSize; i++) {
            Paint paintToUse = (i % 2 == 0) ? gridPaintBlue : gridPaintRed;
            canvas.drawLine(offsetX, offsetY + i * cellSize, offsetX + gridWidth, offsetY + i * cellSize, paintToUse);
        }
    }

    private void drawStartRegion(Canvas canvas) {
        int left = offsetX;
        int right = offsetX + (4 * cellSize);
        int top = offsetY + ((Grid.GRID_SIZE - 4) * cellSize); // Flip y-axis
        int bottom = offsetY + (Grid.GRID_SIZE * cellSize);

        // Draw the green boundary lines
        canvas.drawLine(left, bottom, right, bottom, startRegionPaint); // Bottom line (0,0) to (4,0)
        canvas.drawLine(left, top, right, top, startRegionPaint);       // Top line (0,4) to (4,4)
        canvas.drawLine(left, top, left, bottom, startRegionPaint);     // Left line (0,0) to (0,4)
        canvas.drawLine(right, top, right, bottom, startRegionPaint);   // Right line (4,0) to (4,4)
    }

    private void drawAxisLabels(Canvas canvas) {
        int gridSize = Grid.GRID_SIZE;

        // Adjustments for positioning
        // Center text in the cell: Start of cell + half cell size
        float halfCell = cellSize / 2f;
        float yLabelOffsetY = cellSize / 4f; // Vertical adjustment for text centering

        // Draw X-axis labels (below the grid)
        for (int x = 0; x < gridSize; x++) {
            // "0" at x=0
            canvas.drawText(
                    String.valueOf(x),
                    offsetX + (x * cellSize) + halfCell, 
                    offsetY + (gridSize * cellSize) + 30,    
                    textPaint
            );
        }

        // Draw Y-axis labels (left of the grid)
        for (int y = 0; y < gridSize; y++) {
            // "0" at y=0, which is at the BOTTOM cell (index gridSize-1 visually)
            // But wait, our loop y=0 usually means the bottom-most coordinate?
            // Let's check drawObstacles: y=0 is at `offsetY + (Grid.GRID_SIZE - 1 - y) * cellSize`
            // So y=0 is the bottom cell.
            
            float yPos = offsetY + ((gridSize - 1 - y) * cellSize) + halfCell + yLabelOffsetY;
            
            canvas.drawText(
                    String.valueOf(y),
                    offsetX - 30,  
                    yPos,  
                    textPaint
            );
        }
    }

    private void drawObstacles(Canvas canvas) {
        int id = 1;
        for (GridObstacle gridObstacle : grid.getObstacleList()){
            int left = offsetX + gridObstacle.getPosition().getXInt() * cellSize;
            int top = offsetY + (Grid.GRID_SIZE - 1 - gridObstacle.getPosition().getYInt()) * cellSize;  // Flip y-axis
            int right = left + cellSize;
            int bottom = top + cellSize;

            boolean selected = gridObstacle.isSelected();
            canvas.drawRect(left, top, right, bottom, selected ? obstacleSelectedPaint : obstaclePaint);

            // Compute text position (center of cell)
            float textX = left + (cellSize / 2);
            float textY = top + (cellSize / 2) - ((idPaint.descent() + idPaint.ascent()) / 2);

            if(gridObstacle.getTarget() == null){
                // Draw ID text (no target yet)
                canvas.drawText(String.valueOf(gridObstacle.getId()), textX, textY, idPaint);
            }
            else {
                // Draw target text if avail
                canvas.drawText(gridObstacle.getTarget().getTargetStr(), textX, textY, targetPaint);
            }

            // Draw facing indicator
            drawFacingIndicator(canvas, gridObstacle.getFacing(), left, top, right, bottom);
        }
    }

    private void drawFacingIndicator(Canvas canvas, Facing facing, int left, int top, int right, int bottom) {
        int stripThickness = cellSize / 6; // Adjust strip size relative to the cell size

        switch (facing) {
            case NORTH:
                canvas.drawRect(left, top, right, top + stripThickness, facingPaint);
                break;
            case EAST:
                canvas.drawRect(right - stripThickness, top, right, bottom, facingPaint);
                break;
            case SOUTH:
                canvas.drawRect(left, bottom - stripThickness, right, bottom, facingPaint);
                break;
            case WEST:
                canvas.drawRect(left, top, left + stripThickness, bottom, facingPaint);
                break;
        }
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
        invalidate(); // Refresh the view
    }
}