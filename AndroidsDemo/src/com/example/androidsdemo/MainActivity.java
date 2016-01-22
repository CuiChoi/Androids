package com.example.androidsdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import cn.forward.androids.utils.LogUtil;

/**
 * @author huangziwei
 * 
 */
public class MainActivity extends ListActivity {

	public static final String TITLE = "title";
	public static final String SUBTITLE = "subtitle";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new SimpleAdapter(this, createData(),
                android.R.layout.simple_list_item_2, new String[]{TITLE,
                SUBTITLE}, new int[]{android.R.id.text1,
                android.R.id.text2}));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0) {
			startActivity(new Intent(getApplicationContext(),
					ShapeImageViewDemo.class));
		}else if(position==1){
            startActivity(new Intent(getApplicationContext(),
                    MaskImageViewDemo.class));
        }else if(position==2){
            startActivity(new Intent(getApplicationContext(),
                    RatioImageViewDemo.class));
        }
	}

	private List<Map<String, String>> createData() {
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		data.add(createItem("ShapeImageview", "可设置形状(圆形、圆角矩形)的ImageView，抗锯齿"));
        data.add(createItem("MaskImageview", "test MaskImageview"));
        data.add(createItem("RatioImageview", "test RatioImageview"));
		return data;
	}

	private Map<String, String> createItem(String title, String subtitle) {
		Map<String, String> item = new HashMap<String, String>();

		item.put(TITLE, title);
		item.put(SUBTITLE, subtitle);

		return item;
	}
}