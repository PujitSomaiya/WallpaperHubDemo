package com.pujit.wallpaperdemo.interfaces;

import android.widget.RadioButton;

import com.pujit.wallpaperdemo.api.response.HitsItem;

public interface AllClickListeners {


    interface OnImageClick{
        void onImageClick(int position, HitsItem item);
    }


    interface SetOnBottomDialogButtonClick {
        void setFilter(int number, RadioButton rbImageType, RadioButton rbOrientation);
    }

}
