package com.lanjiyin.emptydemo.highlight_text;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ContextMenu;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * @ClassName: MyTextView
 * @Description: java类作用描述
 * @Author: 底超龙
 * @CreateDate: 2020/10/22 15:58
 */
public class HighLightTextView extends AppCompatTextView {
    private SelectableTextHelper mSelectableTextHelper;

    public HighLightTextView(Context context) {
        super(context);
    }

    public HighLightTextView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public HighLightTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setMovementMethod(SmoothLinkMovementMethod.getInstance());
        mSelectableTextHelper = new SelectableTextHelper.Builder(this)
                .setSelectedColor(Color.parseColor("#663982f7"))
                .setCursorHandleSizeInDp(13f)
                .setCursorHandleColor(Color.parseColor("#3982f7"))
                .build();
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {

    }

    @Override
    protected boolean getDefaultEditable() {
        return false;
    }


    public SelectableTextHelper getmSelectableTextHelper() {
        return mSelectableTextHelper;
    }

    public void addHighLight(int start,int end){
        mSelectableTextHelper.highLightSelect(start,end);
    }

    public void clearHighLight(){
        mSelectableTextHelper.clearHighLight();
    }

    public void deleteHighLight(int start,int end){
        mSelectableTextHelper.unHightLight(start,end);
    }
}
