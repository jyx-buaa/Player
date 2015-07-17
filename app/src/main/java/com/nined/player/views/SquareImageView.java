package com.nined.player.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.nined.player.R;

public class SquareImageView extends ImageView {
    /*********************************/
    /**        Constant(s)          **/
    /*********************************/
    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;
    private static final int DEFAULT_SQUARE_SIZE = 300;
    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final int DEFAULT_BORDER_COLOR = Color.BLACK;
    private static final boolean DEFAULT_BORDER_OVERLAY = false;
    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private int squareSize = DEFAULT_SQUARE_SIZE;
    private int borderWidth = DEFAULT_BORDER_WIDTH;
    private int borderColor = DEFAULT_BORDER_COLOR;
    private boolean borderOverlay = DEFAULT_BORDER_OVERLAY;
    /*********************************/
    /**        Constructor(s)       **/
    /*********************************/
    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SquareImageView, defStyle, 0);
        squareSize = a.getDimensionPixelSize(R.styleable.SquareImageView_square_size, DEFAULT_SQUARE_SIZE);
        borderWidth = a.getDimensionPixelSize(R.styleable.SquareImageView_border_width, DEFAULT_BORDER_WIDTH);
        borderColor = a.getColor(R.styleable.SquareImageView_border_color, DEFAULT_BORDER_COLOR);
        borderOverlay = a.getBoolean(R.styleable.SquareImageView_border_overlay, DEFAULT_BORDER_OVERLAY);
        a.recycle();
    }
    /*********************************/
    /**      onMeasure Override     **/
    /*********************************/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
        setMeasuredDimension(squareSize, squareSize);
/*        getLayoutParams().width = SQUARE_SIZE;
        getLayoutParams().height = SQUARE_SIZE;
        setAdjustViewBounds(true);*/
    }
}