package com.pujit.wallpaperdemo.api.response;

import java.util.List;
import com.google.gson.annotations.SerializedName;
import com.pujit.wallpaperdemo.api.response.HitsItem;

public class PhotoResponse {

	@SerializedName("hits")
	private List<HitsItem> hits;

	@SerializedName("total")
	private int total;

	@SerializedName("totalHits")
	private int totalHits;

	public List<HitsItem> getHits(){
		return hits;
	}

	public int getTotal(){
		return total;
	}

	public int getTotalHits(){
		return totalHits;
	}
}