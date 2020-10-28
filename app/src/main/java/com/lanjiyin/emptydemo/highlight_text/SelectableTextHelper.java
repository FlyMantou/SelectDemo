package com.lanjiyin.emptydemo.highlight_text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.*;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.lanjiyin.emptydemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: SelectableTextHelper
 * @Description: java类作用描述
 * @Author: 底超龙
 * @CreateDate: 2020/10/23 11:02
 */
public class SelectableTextHelper {

    private final static int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION = 100;

    public final static int MENU_HIGHLIGHT_SELECT = 1;
    public final static int MENU_HIGHLIGHT_UN_SELECT = 2;

    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    private OperateWindow mOperateWindow;
    private SelectionInfo mSelectionInfo = new SelectionInfo();
//    private OnSelectListener mSelectListener;

    private Context mContext;
    private TextView mTextView;
    private Spannable mSpannable;

    private int mTouchX;
    private int mTouchY;

    private int downX;
    private int downY;

    private boolean isInitTouch = false;//是否是长按直接拖拽状态

    private int mSelectedColor;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    private BackgroundColorSpan mSpan;
    private boolean isHideWhenScroll;
    private boolean isHide = true;

    private List<SelectData> selectDataList = new ArrayList<>();

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    public SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        init();
    }

    private void init() {
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);
        mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.e("huanghai", "onLongClick");
                isInitTouch = true;
                showSelectView(mTouchX, mTouchY);
                MotionEvent motionEvent = MotionEvent.obtain(10,100,MotionEvent.ACTION_DOWN,0,0,0);
                getCursorHandle(false).onTouchEvent(motionEvent);
                return true;
            }
        });

        mTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                mTouchX = (int) event.getX();
                mTouchY = (int) (event.getY());
                if (mStartHandle == null) {
                    mStartHandle = new CursorHandle(true, mTextView.getLineHeight());
                }
                if (mEndHandle == null) {
                    mEndHandle = new CursorHandle(false, mTextView.getLineHeight());
                }
                CursorHandle right = getCursorHandle(false);
                CursorHandle left = getCursorHandle(true);
                if (!isHide&&isInitTouch){
                    if (event.getRawY() > right.bottom&&event.getRawX()>right.left) {
                        MotionEvent motionEvent = MotionEvent.obtain(event);
                        right.onTouchEvent(motionEvent);
                    }
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isInitTouch = false;
                        downX = (int)event.getRawX();
                        downY = (int)event.getRawY();
                        Log.e("huanghai", "onTouch  ACTION_DOWN");
                        break;
                    case MotionEvent.ACTION_UP:
                        //如果移动距离不超过5，取消选择
                        Log.e("huanghai", "onTouch  ACTION_UP-->downY:"+downY+",event.getRawY():"+event.getRawY());
                        if (!isHide){
                            if (event.getRawY()-downY<=5){
                                resetSelectionInfo();
                                hideSelectView();
                            }
                        }

                        Log.e("huanghai", "onTouch  ACTION_UP");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.e("huanghai", "onTouch  ACTION_MOVE");

                        if (!isHide&&isInitTouch){
                            return true;
                        }

                        break;

                }
                return false;
            }
        });

        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //    resetSelectionInfo();
            //    hideSelectView();
            }
        });
        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                destroy();
            }
        });

        mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isHideWhenScroll) {
                    isHideWhenScroll = false;
                    postShowSelectView(DEFAULT_SHOW_DURATION);
                }
                return true;
            }
        };
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (!isHideWhenScroll && !isHide) {
                    isHideWhenScroll = true;
                    if (mOperateWindow != null) {
                        mOperateWindow.dismiss();
                    }
                    if (mStartHandle != null) {
                        mStartHandle.dismiss();
                    }
                    if (mEndHandle != null) {
                        mEndHandle.dismiss();
                    }
                }
            }
        };
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

        mOperateWindow = new OperateWindow(mContext);
    }

    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) {

                return;
            }
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
        }
    };

    private void hideSelectView() {
        isHide = true;
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
        if (mOperateWindow != null) {
            mOperateWindow.dismiss();
        }
    }

    private void resetSelectionInfo() {
        Log.e("huanghai","resetSelectionInfo");
        mSelectionInfo.mSelectionContent = null;
        if (mSpannable != null && mSpan != null) {
            mSpannable.removeSpan(mSpan);
            mSpan = null;
        }
    }

    private void showSelectView(int x, int y) {
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        if (mStartHandle == null) {
            mStartHandle = new CursorHandle(true, mTextView.getLineHeight());
        }
        if (mEndHandle == null) {
            mEndHandle = new CursorHandle(false, mTextView.getLineHeight());
        }

        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, mTextView.getScrollY() + y);
        int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;
        int startOffsetNoScroll = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffsetNoScroll = startOffsetNoScroll + DEFAULT_SELECTION_LENGTH;
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null || startOffset >= mTextView.getText().length()) {
            return;
        }
        selectText(startOffset, endOffset);
        /*if (startOffsetNoScroll != -1) {
            mSelectionInfo.mStartNoScoll = startOffsetNoScroll;
        }
        if (endOffsetNoScroll != -1) {
            mSelectionInfo.mEndNoScoll = endOffsetNoScroll;
        }*/
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        mOperateWindow.show();
    }

    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset), cursorHandle.isLeft ? layout.getLineTop(layout.getLineForOffset(offset)) - cursorHandle.mCircleRadius * 2 - mTextView.getScrollY() : layout.getLineTop(layout.getLineForOffset(offset)) - mTextView.getScrollY());
    }

    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.mStart = startPos;
        }
        if (endPos != -1) {
            mSelectionInfo.mEnd = endPos;
        }
        if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
            int temp = mSelectionInfo.mStart;
            mSelectionInfo.mStart = mSelectionInfo.mEnd;
            mSelectionInfo.mEnd = temp;
        }

        if (mSpannable != null) {
            if (mSpan == null) {
                mSpan = new BackgroundColorSpan(mSelectedColor);
            }
            mSelectionInfo.mSelectionContent = mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd).toString();
            mSpannable.setSpan(mSpan, mSelectionInfo.mStart, mSelectionInfo.mEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//            if (mSelectListener != null) {
//                mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
//            }
        }
    }

    OnMenuClickListener mNoteBookClickListener;



    public void setOnNotesClickListener(OnMenuClickListener notesClickListener) {
        mNoteBookClickListener = notesClickListener;
    }

    public void destroy() {
        mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        resetSelectionInfo();
        hideSelectView();
        mStartHandle = null;
        mEndHandle = null;
        mOperateWindow = null;
    }

    public void dismiss() {
        SelectableTextHelper.this.resetSelectionInfo();
        SelectableTextHelper.this.hideSelectView();
    }

    public void clearHighLight() {
        String text = mTextView.getText().toString();
        mTextView.setText(text);
    }


    /**
     * Operate windows : copy, select all
     */
    private class OperateWindow {

        private PopupWindow mWindow;
        private int[] mTempCoors = new int[2];

        private int mWidth;
        private int mHeight;

        public OperateWindow(final Context context) {
            View contentView = LayoutInflater.from(context).inflate(R.layout.layout_operate_windows, null);
            contentView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWidth = contentView.getMeasuredWidth();
            mHeight = contentView.getMeasuredHeight();
            mWindow =
                    new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            mWindow.setClippingEnabled(false);

//            contentView.findViewById(R.id.tv_copy).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
//                    clip.setPrimaryClip(
//                            ClipData.newPlainText(mSelectionInfo.mSelectionContent, mSelectionInfo.mSelectionContent));
//                    if (mNoteBookClickListener != null) {
//                        mNoteBookClickListener.onTextSelect(MENU_COPY,mSelectionInfo.mStart,mSelectionInfo.mEnd,mSelectionInfo.mSelectionContent);
//                    }
//                    SelectableTextHelper.this.resetSelectionInfo();
//                    SelectableTextHelper.this.hideSelectView();
//                }
//            });

            contentView.findViewById(R.id.tv_note).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    SelectableTextHelper.this.resetSelectionInfo();
                    SelectableTextHelper.this.hideSelectView();
                    SelectableTextHelper.this.highLightSelect(mSelectionInfo.mStart, mSelectionInfo.mEnd);
                }
            });
        }

        public void show() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) + mTempCoors[0];
            int posY = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mStart)) + mTempCoors[1] - mHeight - 16;

            if (posX <= 0) {
                posX = 16;
            }
            if (posY < 0) {
                posY = 16;
            }
            if (posX + mWidth > TextLayoutUtil.getScreenWidth(mContext)) {
                posX = TextLayoutUtil.getScreenWidth(mContext) - mWidth - 16;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWindow.setElevation(8f);
            }
            Log.e("huanghai","posY:"+posY);
            Log.e("huanghai","=mTextView.getY():"+mTextView.getY());
            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
        }


        public void dismiss() {
            mWindow.dismiss();
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }
    }


    private CancelWindow cancelWindow = null;

    public interface CancelCallback {
        void onCancel();
    }

    /**
     * 取消高亮弹窗
     */
    private class CancelWindow {

        private PopupWindow mWindow;
        private int[] mTempCoors = new int[2];

        private int mWidth;
        private int mHeight;

        private CancelCallback cancelCallback;

        public void setCancelCallback(CancelCallback cancelCallback) {
            this.cancelCallback = cancelCallback;
        }


        public CancelWindow(final Context context) {
            View contentView = LayoutInflater.from(context).inflate(R.layout.layout_cancel_windows, null);
            contentView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWidth = contentView.getMeasuredWidth();
            mHeight = contentView.getMeasuredHeight();
            mWindow =
                    new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            mWindow.setClippingEnabled(false);
            mWindow.setOutsideTouchable(true);

            contentView.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (cancelCallback != null) {
                        cancelCallback.onCancel();
                    }
                    cancelWindow.dismiss();
                }
            });

        }

        public void showAtStart(int start) {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            int posX = (int) layout.getPrimaryHorizontal(start) + mTempCoors[0];
            int posY = layout.getLineTop(layout.getLineForOffset(start)) + mTempCoors[1] - mHeight - 16 - mTextView.getScrollY();
            if (posX <= 0) {
                posX = 16;
            }
            if (posY < 0) {
                posY = 16;
            }
            if (posX + mWidth > TextLayoutUtil.getScreenWidth(mContext)) {
                posX = TextLayoutUtil.getScreenWidth(mContext) - mWidth - 16;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWindow.setElevation(8f);
            }
            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
        }

        public void dismiss() {
            mWindow.dismiss();
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }
    }


    public boolean isInSelect(int index){
        for (int x = 0; x < selectDataList.size(); x++) {
            SelectData selectData = selectDataList.get(x);
            if (index>=selectData.start&&index<=selectData.end){
                return true;
            }
        }
        return false;
    }
    /**
     * 高亮选中
     *
     * @param mStart
     * @param mEnd
     */
    public void highLightSelect(final int mStart, final int mEnd) {
        //计算重叠
        //设置clickspan


        String text = mTextView.getText().toString();
        //遍历选中集合，计算重叠后，添加span
        List<SelectData> list = new ArrayList<>();
        List<Integer> integerList = new ArrayList<>();
        selectDataList.add(new SelectData(mStart,mEnd));
        for (int x=0;x<text.length();x++){
            if (isInSelect(x)){
                integerList.add(x);
            }
        }

        int lastIndex = -2; //上一个位置
        SelectData selectData = null;
        for (int x=0;x<integerList.size();x++){
            //如果当前位置，紧挨着上一个位置
            if (lastIndex+1==integerList.get(x)){
                if (x==integerList.size()-1){
                    //最后一个
                    if (selectData!=null){
                        selectData.end = integerList.get(x);
                        list.add(new SelectData(selectData.start,selectData.end));
                        selectData = null;
                    }
                }
            }else {
                if (selectData!=null){
                    selectData.end = lastIndex;
                    list.add(new SelectData(selectData.start,selectData.end));
                    selectData = null;
                }
                selectData = new SelectData();
                selectData.start = integerList.get(x);
            }
            lastIndex = integerList.get(x);
        }

        Log.e("huanghai","size-->"+list.size());
        selectDataList.clear();
        selectDataList.addAll(list);



        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        int lastEnd = 0;
        for (int x=0;x<selectDataList.size();x++){
            final SelectData select = selectDataList.get(x);
            Log.e("huanghai","start-->"+select.start);
            Log.e("huanghai","end-->"+select.end);
            spannableStringBuilder.append(text, lastEnd, select.start);
            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    if (cancelWindow == null) {
                        cancelWindow = new CancelWindow(mContext);
                    }
                    cancelWindow.setCancelCallback(new CancelCallback() {
                        @Override
                        public void onCancel() {
                            unHightLight(select.start, select.end);
                        }
                    });
                    cancelWindow.showAtStart(select.start);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.bgColor = Color.parseColor("#663982f7");
                    ds.linkColor = mTextView.getTextColors().getDefaultColor();
                    super.updateDrawState(ds);
                }
            };
            spannableStringBuilder.append(text, select.start, select.end).setSpan(clickableSpan, select.start, select.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            lastEnd = select.end;
        }
        spannableStringBuilder.append(text, lastEnd, mTextView.getText().length() - 1);
        mTextView.setText(spannableStringBuilder);
        if (mNoteBookClickListener != null) {
            mNoteBookClickListener.onHighLight(MENU_HIGHLIGHT_SELECT, selectDataList);
        }
    }

    public void unHightLight(int start, int end) {
        int removeIndex = -1;
        for (int x=0;x<selectDataList.size();x++){
            if (selectDataList.get(x).start==start&&selectDataList.get(x).end==end){
                removeIndex = x;
            }
        }
        if (removeIndex!=-1){
            selectDataList.remove(removeIndex);
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(mTextView.getText(), 0, start);
        spannableStringBuilder.append(new SpannableString(mTextView.getText().toString().subSequence(start, end)));
        spannableStringBuilder.append(mTextView.getText(), end, mTextView.getText().length() - 1);
        mTextView.setText(spannableStringBuilder);
        if (mNoteBookClickListener != null) {
            mNoteBookClickListener.onCancelHighLight(MENU_HIGHLIGHT_UN_SELECT,selectDataList);
        }
    }

    private class CursorHandle extends View {

        private PopupWindow mPopupWindow;
        private Paint mPaint;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth = mCircleRadius * 2;
        private int mHeight = 50;
        private int mPadding = 0;
        private boolean isLeft;
        private int lineHeight = 0;
        private int offsetY = 15;

        public CursorHandle(boolean isLeft, int lineHeight) {
            super(mContext);
            this.isLeft = isLeft;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);

            mPopupWindow = new PopupWindow(this);
            mPopupWindow.setClippingEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPopupWindow.setElevation(2f);
            }
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            this.lineHeight = lineHeight;
            mHeight = lineHeight + mCircleRadius * 2 + offsetY;
            mPopupWindow.setHeight(mHeight + mPadding / 2);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (isLeft) {
                canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius, mCircleRadius, mPaint);
                canvas.drawRect(mCircleRadius + mPadding - 2, mWidth, mCircleRadius + mPadding + 2, lineHeight + mWidth + offsetY, mPaint);
            } else {
                canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius + lineHeight + offsetY, mCircleRadius, mPaint);
                canvas.drawRect(mCircleRadius + mPadding - 2, 0, mCircleRadius + mPadding + 2, lineHeight + offsetY, mPaint);
            }
        }

        private int mAdjustX;
        private int mAdjustY;

        private int mBeforeDragStart;
        private int mBeforeDragEnd;

        public int top = 0;
        public int bottom = 0;
        public int left = 0;
        public int right = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.mStart;
                    mBeforeDragEnd = mSelectionInfo.mEnd;
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) (event.getY() + mTextView.getScrollY());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.dismiss();
                    int rawX = (int) event.getRawX();
                    int rawY = (int) event.getRawY();
                    update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
                    break;
            }
            return true;
        }

        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }

        private int[] mTempCoors = new int[2];

        public void update(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int oldOffset;
            if (isLeft) {
                oldOffset = mSelectionInfo.mStart;
            } else {
                oldOffset = mSelectionInfo.mEnd;
            }

            y -= mTempCoors[1];

            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);

            if (offset != oldOffset) {
                resetSelectionInfo();
                if (isLeft) {
                    if (offset > mBeforeDragEnd) {
                        CursorHandle handle = getCursorHandle(false);
                        changeDirection();
                        handle.changeDirection();
                        mBeforeDragStart = mBeforeDragEnd;
                        selectText(mBeforeDragEnd, offset);
                        handle.updateCursorHandle();
                    } else {
                        selectText(offset, -1);
                    }
                    updateCursorHandle();
                } else {
                    if (offset < mBeforeDragStart) {
                        CursorHandle handle = getCursorHandle(true);
                        handle.changeDirection();
                        changeDirection();
                        mBeforeDragEnd = mBeforeDragStart;
                        selectText(offset, mBeforeDragStart);
                        handle.updateCursorHandle();
                    } else {
                        selectText(mBeforeDragStart, offset);
                    }
                    updateCursorHandle();
                }
            }
        }

        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
//                popY = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY()-mWidth-mTextView.getScrollY();
                //如果top小于textview的top，不显示
                int y = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY() - mWidth - mTextView.getScrollY();
                if (y<mTextView.getY()){
                    return;
                }
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) - mWidth / 2 + getExtraX(),
                        y, -1, -1);
            } else {
                int y = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mEnd)) + getExtraY() - mTextView.getScrollY();
                if (y<mTextView.getY()){
                    return;
                }
//                popY = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mEnd)) + getExtraY()-mTextView.getScrollY();
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd) + getExtraX() - mWidth / 2,
                        y, -1, -1);
            }
        }

        public void show(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int offset = isLeft ? mWidth / 2 : mWidth / 2;
            top = y + getExtraY();
            bottom = top+mHeight;
            left = x - offset + getExtraX();
            right = x - offset + getExtraX()+mWidth;
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mTempCoors[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mTempCoors[1] + mTextView.getPaddingTop();
        }
    }

    private CursorHandle getCursorHandle(boolean isLeft) {
        if (mStartHandle.isLeft == isLeft) {
            return mStartHandle;
        } else {
            return mEndHandle;
        }
    }

    public static class Builder {
        private TextView mTextView;
        private int mCursorHandleColor = 0xFF1379D6;
        private int mSelectedColor = 0xFFAFE1F4;
        private float mCursorHandleSizeInDp = 24;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }
}
