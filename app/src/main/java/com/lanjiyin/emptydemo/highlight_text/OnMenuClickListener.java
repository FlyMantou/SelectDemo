package com.lanjiyin.emptydemo.highlight_text;

import java.util.List;

/**
 * @ClassName: OnNoteBookClickListener
 * @Description: java类作用描述
 * @Author: 底超龙
 * @CreateDate: 2020/10/23 11:04
 */
public interface OnMenuClickListener {
    /**
     *  回调
     * @param type {@link SelectableTextHelper.MENU_HIGHLIGHT_SELECT} {@link SelectableTextHelper.MENU_HIGHLIGHT_UN_SELECT}
     * @param selectDataList 所有选中的起始位置坐标
     */
    void onHighLight(int type,List<SelectData> selectDataList);
    void onCancelHighLight(int type,List<SelectData> selectDataList);
}
