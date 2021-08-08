package com.pujit.wallpaperdemo.interfaces;

import com.pujit.wallpaperdemo.api.response.HitsItem;

public interface AllClickListeners {


    interface OnImageClick{
        void onImageClick(int position, HitsItem item);
    }

}
