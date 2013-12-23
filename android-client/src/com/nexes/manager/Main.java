// liulu 2013.7.5
package com.nexes.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.Environment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;


public final class Main extends ListActivity {
	public static final String ACTION_WIDGET = "com.nexes.manager.Main.ACTION_WIDGET";
	
	private static final String PREFS_NAME = "ManagerPrefsFile";	//user preference file name
	private static final String PREFS_HIDDEN = "hidden";
	private static final String PREFS_COLOR = "color";
	private static final String PREFS_THUMBNAIL = "thumbnail";
	private static final String PREFS_SORT = "sort";
	private static final String PREFS_STORAGE = "sdcard space";
	
	private static final int MENU_MKDIR =   0x00;			//option menu id
	private static final int MENU_SETTING = 0x01;			//option menu id
	private static final int MENU_SEARCH =  0x02;			//option menu id
	private static final int MENU_SPACE =   0x03;			//option menu id
	private static final int MENU_QUIT = 	0x04;			//option menu id
	private static final int SEARCH_B = 	0x09;
	
	private static final int D_MENU_DELETE = 0x05;			//context menu id
	private static final int D_MENU_RENAME = 0x06;			//context menu id
	private static final int D_MENU_COPY =   0x07;			//context menu id
	private static final int D_MENU_PASTE =  0x08;			//context menu id
	private static final int D_MENU_ZIP = 	 0x0e;			//context menu id
	private static final int D_MENU_UNZIP =  0x0f;			//context menu id
	private static final int D_MENU_MOVE = 	 0x30;			//context menu id
	private static final int F_MENU_MOVE = 	 0x20;			//context menu id
	private static final int F_MENU_DELETE = 0x0a;			//context menu id
	private static final int F_MENU_RENAME = 0x0b;			//context menu id
	private static final int F_MENU_ATTACH = 0x0c;			//context menu id
	private static final int F_MENU_COPY =   0x0d;			//context menu id
	private static final int SETTING_REQ = 	 0x10;			//request code for intent

	private FileManager mFileMag;
	private EventHandler mHandler;
	private EventHandler.TableRow mTable;
	
	private SharedPreferences mSettings;
	private boolean mReturnIntent = false;
	private boolean mHoldingFile = false;
	private boolean mHoldingZip = false;
	private boolean mUseBackKey = true;
	private String mCopiedTarget;
	private String mZippedTarget;
	private String mSelectedListItem;				//item from context menu
	private TextView  mPathLabel, mDetailLabel, mStorageLabel;
	//数据库成员
	private DatabaseUtil mydatabase; 
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        /*read settings*/
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = mSettings.getBoolean(PREFS_HIDDEN, false);
        boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);
        int space = mSettings.getInt(PREFS_STORAGE, View.VISIBLE);
        int color = mSettings.getInt(PREFS_COLOR, -1);
        int sort = mSettings.getInt(PREFS_SORT, 3);
        
        mFileMag = new FileManager();
        mFileMag.setShowHiddenFiles(hide);
        mFileMag.setSortType(sort);
        
        if (savedInstanceState != null)
        	mHandler = new EventHandler(Main.this, mFileMag, savedInstanceState.getString("location"));
        else
        	mHandler = new EventHandler(Main.this, mFileMag);
        
        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        
        /* register context menu for our list view */
        registerForContextMenu(getListView());
        
        mStorageLabel = (TextView)findViewById(R.id.storage_label);
        mDetailLabel = (TextView)findViewById(R.id.detail_label);
        mPathLabel = (TextView)findViewById(R.id.path_label);
        mPathLabel.setText("path: /sdcard");
        
        updateStorageLabel();
        mStorageLabel.setVisibility(space);
        
        mHandler.setUpdateLabels(mPathLabel, mDetailLabel);
        
        /* setup buttons 全盘扫描按钮*/
        int[] img_button_id = {R.id.all_scan,R.id.help_button, R.id.home_button, 
        					   R.id.back_button, 
        					   R.id.multiselect_button};
        
        int[] button_id = {R.id.hidden_copy, R.id.hidden_attach,
        				   R.id.hidden_delete, R.id.hidden_move};
        
        ImageButton[] bimg = new ImageButton[img_button_id.length];
        Button[] bt = new Button[button_id.length];
        
        for(int i = 0; i < img_button_id.length; i++) {
        	bimg[i] = (ImageButton)findViewById(img_button_id[i]);
        	bimg[i].setOnClickListener(mHandler);

        	if(i < 4) {
        		bt[i] = (Button)findViewById(button_id[i]);
        		bt[i].setOnClickListener(mHandler);
        	}
        }
    
        Intent intent = getIntent();
        
        if(intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
        	bimg[5].setVisibility(View.GONE);
        	mReturnIntent = true;
        
        } else if (intent.getAction().equals(ACTION_WIDGET)) {
        	Log.e("MAIN", "Widget action, string = " + intent.getExtras().getString("folder"));
        	mHandler.updateDirectory(mFileMag.getNextDir(intent.getExtras().getString("folder"), true));
        	
        }
        
        
        //初始化数据库
        mydatabase = new DatabaseUtil(this);
        //TODO
        
        //开启后台mainService
        startService(new Intent(Main.this, mainService.class));
        
        /*
        Button Btn_all_scan = (Button)findViewById(R.id.all_scan); //获取按钮资源  
        Btn_all_scan.setOnClickListener(new Button.OnClickListener(){ //创建监听  
            @Override  
            public void onClick(View v) {
            	//fun("/");
            }  
        });  
        */
        
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("location", mFileMag.getCurrentDir());
	}
	
	/*(non Java-Doc)
	 * Returns the file that was selected to the intent that
	 * called this activity. usually from the caller is another application.
	 */
	private void returnIntentResults(File data) {
		mReturnIntent = false;
		
		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		
		finish();
	}
	
	private void updateStorageLabel() {
		long total, aval;
		int kb = 1024;
		
		StatFs fs = new StatFs(Environment.
								getExternalStorageDirectory().getPath());
		
		total = fs.getBlockCount() * (fs.getBlockSize() / kb);
		aval = fs.getAvailableBlocks() * (fs.getBlockSize() / kb);
		
		mStorageLabel.setText(String.format("SD存储卡: 总容量 %.2f GB " +
							  "\t\t可用量 %.2f GB", 
							  (double)total / (kb * kb), (double)aval / (kb * kb)));
	}
	
	/**
	 *  To add more functionality and let the user interact with more
	 *  file types, this is the function to add the ability. 
	 *  
	 *  (note): this method can be done more efficiently 
	 */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
    	final String item = mHandler.getData(position);
    	boolean multiSelect = mHandler.isMultiSelected();
    	File file = new File(mFileMag.getCurrentDir() + "/" + item);
    	String item_ext = null;
    	
    	try {
    		item_ext = item.substring(item.lastIndexOf("."), item.length());
    		
    	} catch(IndexOutOfBoundsException e) {	
    		item_ext = ""; 
    	}
    	
    	/*
    	 * If the user has multi-select on, we just need to record the file
    	 * not make an intent for it.
    	 */
    	if(multiSelect) {
    		mTable.addMultiPosition(position, file.getPath());
    		
    	} else {
	    	if (file.isDirectory()) {
				if(file.canRead()) {
					mHandler.stopThumbnailThread();
		    		mHandler.updateDirectory(mFileMag.getNextDir(item, false));
		    		mPathLabel.setText(mFileMag.getCurrentDir());
		    		
		    		/*set back button switch to true 
		    		 * (this will be better implemented later)
		    		 */
		    		if(!mUseBackKey)
		    			mUseBackKey = true;
		    		
	    		} else {
	    			Toast.makeText(this, "Can't read folder due to permissions", 
	    							Toast.LENGTH_SHORT).show();
	    		}
	    	}
	    	
	    	/*music file selected--add more audio formats*/
	    	else if (item_ext.equalsIgnoreCase(".mp3") || 
	    			 item_ext.equalsIgnoreCase(".m4a")||
	    			 item_ext.equalsIgnoreCase(".mp4")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    		} else {
	    			Intent i = new Intent();
    				i.setAction(android.content.Intent.ACTION_VIEW);
    				i.setDataAndType(Uri.fromFile(file), "audio/*");
    				startActivity(i);
	    		}
	    	}
	    	
	    	/*photo file selected*/
	    	else if(item_ext.equalsIgnoreCase(".jpeg") || 
	    			item_ext.equalsIgnoreCase(".jpg")  ||
	    			item_ext.equalsIgnoreCase(".png")  ||
	    			item_ext.equalsIgnoreCase(".gif")  || 
	    			item_ext.equalsIgnoreCase(".tiff")) {
	 			    		
	    		if (file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent picIntent = new Intent();
			    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		picIntent.setDataAndType(Uri.fromFile(file), "image/*");
			    		startActivity(picIntent);
	    			}
	    		}
	    	}
	    	
	    	/*video file selected--add more video formats*/
	    	else if(item_ext.equalsIgnoreCase(".m4v") || 
	    			item_ext.equalsIgnoreCase(".3gp") ||
	    			item_ext.equalsIgnoreCase(".wmv") || 
	    			item_ext.equalsIgnoreCase(".mp4") || 
	    			item_ext.equalsIgnoreCase(".ogg") ||
	    			item_ext.equalsIgnoreCase(".wav")) {
	    		
	    		if (file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
	    				Intent movieIntent = new Intent();
			    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
			    		startActivity(movieIntent);
	    			}
	    		}
	    	}
	    	
	    	/*zip file */
	    	else if(item_ext.equalsIgnoreCase(".zip")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    			
	    		} else {
		    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    		AlertDialog alert;
		    		mZippedTarget = mFileMag.getCurrentDir() + "/" + item;
		    		CharSequence[] option = {"Extract here", "Extract to..."};
		    		
		    		builder.setTitle("Extract");
		    		builder.setItems(option, new DialogInterface.OnClickListener() {
		
						public void onClick(DialogInterface dialog, int which) {
							switch(which) {
								case 0:
									String dir = mFileMag.getCurrentDir();
									mHandler.unZipFile(item, dir + "/");
									break;
									
								case 1:
									mDetailLabel.setText("Holding " + item + 
														 " to extract");
									mHoldingZip = true;
									break;
							}
						}
		    		});
		    		
		    		alert = builder.create();
		    		alert.show();
	    		}
	    	}
	    	
	    	/* gzip files, this will be implemented later */
	    	else if(item_ext.equalsIgnoreCase(".gzip") ||
	    			item_ext.equalsIgnoreCase(".gz")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    			
	    		} else {
	    			//TODO:
	    		}
	    	}
	    	
	    	/* pdf file selected*/
	    	else if(item_ext.equalsIgnoreCase(".pdf")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent pdfIntent = new Intent();
			    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		pdfIntent.setDataAndType(Uri.fromFile(file), 
			    								 "application/pdf");
			    		
			    		try {
			    			startActivity(pdfIntent);
			    		} catch (ActivityNotFoundException e) {
			    			Toast.makeText(this, "Sorry, couldn't find a pdf viewer", 
									Toast.LENGTH_SHORT).show();
			    		}
		    		}
	    		}
	    	}
	    	
	    	/*Android application file*/
	    	else if(item_ext.equalsIgnoreCase(".apk")){
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent apkIntent = new Intent();
		    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		    			startActivity(apkIntent);
	    			}
	    		}
	    	}
	    	
	    	/* HTML file */
	    	else if(item_ext.equalsIgnoreCase(".html")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent htmlIntent = new Intent();
		    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");
		    			
		    			try {
		    				startActivity(htmlIntent);
		    			} catch(ActivityNotFoundException e) {
		    				Toast.makeText(this, "Sorry, couldn't find a HTML viewer", 
		    									Toast.LENGTH_SHORT).show();
		    			}
	    			}
	    		}
	    	}
	    	
	    	/* text file*/
	    	else if(item_ext.equalsIgnoreCase(".txt")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent txtIntent = new Intent();
		    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");
		    			
		    			try {
		    				startActivity(txtIntent);
		    			} catch(ActivityNotFoundException e) {
		    				txtIntent.setType("text/*");
		    				startActivity(txtIntent);
		    			}
	    			}
	    		}
	    	}
	    	
	    	/* generic intent */
	    	else {
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent generic = new Intent();
			    		generic.setAction(android.content.Intent.ACTION_VIEW);
			    		generic.setDataAndType(Uri.fromFile(file), "text/plain");
			    		
			    		try {
			    			startActivity(generic);
			    		} catch(ActivityNotFoundException e) {
			    			Toast.makeText(this, "Sorry, couldn't find anything " +
			    						   "to open " + file.getName(), 
			    						   Toast.LENGTH_SHORT).show();
			    		}
	    			}
	    		}
	    	}
    	}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	SharedPreferences.Editor editor = mSettings.edit();
    	boolean check;
    	boolean thumbnail;
    	int color, sort, space;
    	
    	/* resultCode must equal RESULT_CANCELED because the only way
    	 * out of that activity is pressing the back button on the phone
    	 * this publishes a canceled result code not an ok result code
    	 */
    	if(requestCode == SETTING_REQ && resultCode == RESULT_CANCELED) {
    		//save the information we get from settings activity
    		check = data.getBooleanExtra("HIDDEN", false);
    		thumbnail = data.getBooleanExtra("THUMBNAIL", true);
    		color = data.getIntExtra("COLOR", -1);
    		sort = data.getIntExtra("SORT", 0);
    		space = data.getIntExtra("SPACE", View.VISIBLE);
    		
    		editor.putBoolean(PREFS_HIDDEN, check);
    		editor.putBoolean(PREFS_THUMBNAIL, thumbnail);
    		editor.putInt(PREFS_COLOR, color);
    		editor.putInt(PREFS_SORT, sort);
    		editor.putInt(PREFS_STORAGE, space);
    		editor.commit();
    		  		
    		mFileMag.setShowHiddenFiles(check);
    		mFileMag.setSortType(sort);
    		mHandler.setTextColor(color);
    		mHandler.setShowThumbnails(thumbnail);
    		mStorageLabel.setVisibility(space);
    		mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
    	}
    }
    
    /* ================Menus, options menu and context menu start here=================*/
    //点击手机菜单键后弹出的菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_MKDIR, 0, "新建目录").setIcon(R.drawable.newfolder);
    	menu.add(0, MENU_SEARCH, 0, "搜索").setIcon(R.drawable.search);

    	menu.add(0, MENU_SPACE, 0, "本地数据库").setIcon(R.drawable.space);
    	menu.add(0, MENU_SETTING, 0, "设置").setIcon(R.drawable.setting);
    	menu.add(0, MENU_QUIT, 0, "退出").setIcon(R.drawable.logout);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case MENU_MKDIR:
    			showDialog(MENU_MKDIR);
    			return true;
    			
    		case MENU_SEARCH:
    			showDialog(MENU_SEARCH);
    			return true;
    			
    		case MENU_SPACE:
    		{
    			//展示本地数据库的内容
    			String show = mydatabase.listall();
    			Toast.makeText(getApplicationContext(), show, Toast.LENGTH_LONG).show();
    			return true;
    		}
    			
    		case MENU_SETTING:
    			Intent settings_int = new Intent(this, Settings.class);
    			settings_int.putExtra("HIDDEN", mSettings.getBoolean(PREFS_HIDDEN, false));
    			settings_int.putExtra("THUMBNAIL", mSettings.getBoolean(PREFS_THUMBNAIL, true));
    			settings_int.putExtra("COLOR", mSettings.getInt(PREFS_COLOR, -1));
    			settings_int.putExtra("SORT", mSettings.getInt(PREFS_SORT, 0));
    			settings_int.putExtra("SPACE", mSettings.getInt(PREFS_STORAGE, View.VISIBLE));
    			
    			startActivityForResult(settings_int, SETTING_REQ);
    			return true;
    			
    		case MENU_QUIT:
    			//关闭程序前，先关闭数据库。
    			mydatabase.closeDB();
    			finish();
    			return true;
    	}
    	return false;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	super.onCreateContextMenu(menu, v, info);
    	
    	boolean multi_data = mHandler.hasMultiSelectData();
    	AdapterContextMenuInfo _info = (AdapterContextMenuInfo)info;
    	mSelectedListItem = mHandler.getData(_info.position);

    	/*对目录的操作 is it a directory and is multi-select turned off */
    	if(mFileMag.isDirectory(mSelectedListItem) && !mHandler.isMultiSelected()) {
    		menu.setHeaderTitle("文件夹操作");
        	menu.add(0, D_MENU_DELETE, 0, "Delete Folder");
        	menu.add(0, D_MENU_RENAME, 0, "Rename Folder");
        	menu.add(0, D_MENU_COPY, 0, "Copy Folder");
        	menu.add(0, D_MENU_MOVE, 0, "Move(Cut) Folder");
        	menu.add(0, D_MENU_ZIP, 0, "Zip Folder");
        	menu.add(0, D_MENU_PASTE, 0, "Paste into folder").setEnabled(mHoldingFile || 
        																 multi_data);
        	menu.add(0, D_MENU_UNZIP, 0, "Extract here").setEnabled(mHoldingZip);
    		
        /*对文件的操作 is it a file and is multi-select turned off */
    	} else if(!mFileMag.isDirectory(mSelectedListItem) && !mHandler.isMultiSelected()) {
        	menu.setHeaderTitle("文件操作");
    		//menu.add(0, F_MENU_RENAME, 0, "Rename File");
    		
        	
        	//menu.add(0, F_MENU_COPY, 0, "本地数据库测试");
    		
        	menu.add(0, F_MENU_MOVE, 0, "扫描该文件");
    		//menu.add(0, F_MENU_ATTACH, 0, "上传文件");
    		//menu.add(0, F_MENU_DELETE, 0, "删除文件");
    	}	
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {

    	switch(item.getItemId()) {
    		case D_MENU_DELETE:
    			
    		case F_MENU_DELETE:
    			//删除文件的操作
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle("Warning ");
    			builder.setIcon(R.drawable.warning);
    			builder.setMessage("确定要删除 " + mSelectedListItem +
    							" 吗？");
    			builder.setCancelable(false);
    			
    			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
    			});
    			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mHandler.deleteFile(mFileMag.getCurrentDir() + "/" + mSelectedListItem);
					}
    			});
    			AlertDialog alert_d = builder.create();
    			alert_d.show();
    			return true;
    			
    		case D_MENU_RENAME:
    			showDialog(D_MENU_RENAME);
    			return true;
    			
    		case F_MENU_RENAME:
    			showDialog(F_MENU_RENAME);
    			return true;
    			
    		case F_MENU_ATTACH:
    		{
    			//手动上传文件操作测试
    			File file = new File(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    			String requestURL = getResources().getString(R.string.ip_address) + "filehandle/";
    			if(file!=null)
                {
                    upload_thr upthread = new upload_thr();
        	        upthread.file = file;
        	        upthread.requestURL = requestURL;
        	        new Thread(upthread).start();
                }
    			return true;
    		}
    		
    		case F_MENU_MOVE:
    		{
    			//手动扫描指定文件
    			//TODO
    			mHandler.manual_scan(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    			return true;
    		}

    		case D_MENU_MOVE:
    			
    		case F_MENU_COPY:
    		{
    			//本地数据库测试。
    			File file = new File(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    			String md5 = GetDigest.getMD5EncryptedString(file.toString());
    			mydatabase.add(md5);
    			Toast.makeText(getApplicationContext(), mydatabase.find(md5), Toast.LENGTH_LONG).show();
    			return true;
    			
    		}
    		
    		case D_MENU_COPY:
    			if(item.getItemId() == F_MENU_MOVE || item.getItemId() == D_MENU_MOVE)
    				mHandler.setDeleteAfterCopy(true);
    			
    			mHoldingFile = true;
    			
    			mCopiedTarget = mFileMag.getCurrentDir() +"/"+ mSelectedListItem;
    			mDetailLabel.setText("Holding " + mSelectedListItem);
    			return true;
    			
    		
    		case D_MENU_PASTE:
    			boolean multi_select = mHandler.hasMultiSelectData();
    			
    			if(multi_select) {
    				mHandler.copyFileMultiSelect(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    				
    			} else if(mHoldingFile && mCopiedTarget.length() > 1) {
    				
    				mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    				mDetailLabel.setText("");
    			}
    			    			   			
    			mHoldingFile = false;
    			return true;
    			
    		case D_MENU_ZIP:
    			String dir = mFileMag.getCurrentDir();
    			
    			mHandler.zipFile(dir + "/" + mSelectedListItem);
    			return true;
    			
    		case D_MENU_UNZIP:
    			if(mHoldingZip && mZippedTarget.length() > 1) {
    				String current_dir = mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/";
    				String old_dir = mZippedTarget.substring(0, mZippedTarget.lastIndexOf("/"));
    				String name = mZippedTarget.substring(mZippedTarget.lastIndexOf("/") + 1, mZippedTarget.length());
    				
    				if(new File(mZippedTarget).canRead() && new File(current_dir).canWrite()) {
	    				mHandler.unZipFileToDir(name, current_dir, old_dir);				
	    				mPathLabel.setText(current_dir);
	    				
    				} else {
    					Toast.makeText(this, "You do not have permission to unzip " + name, 
    							Toast.LENGTH_SHORT).show();
    				}
    			}
    			
    			mHoldingZip = false;
    			mDetailLabel.setText("");
    			mZippedTarget = "";
    			return true;
    	}
    	return false;
    }
    
    /* ================Menus, options menu and context menu end here=================*/

    @Override
    protected Dialog onCreateDialog(int id) {
    	final Dialog dialog = new Dialog(Main.this);
    	
    	switch(id) {
    		case MENU_MKDIR:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Create New Directory");
    			dialog.setCancelable(false);
    			
    			ImageView icon = (ImageView)dialog.findViewById(R.id.input_icon);
    			icon.setImageResource(R.drawable.newfolder);
    			
    			TextView label = (TextView)dialog.findViewById(R.id.input_label);
    			label.setText(mFileMag.getCurrentDir());
    			final EditText input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
    			Button create = (Button)dialog.findViewById(R.id.input_create_b);
    			
    			create.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {
    					if (input.getText().length() > 1) {
    						if (mFileMag.createDir(mFileMag.getCurrentDir() + "/", input.getText().toString()) == 0)
    							Toast.makeText(Main.this, 
    										   "Folder " + input.getText().toString() + " created", 
    										   Toast.LENGTH_LONG).show();
    						else
    							Toast.makeText(Main.this, "New folder was not created", Toast.LENGTH_SHORT).show();
    					}
    					
    					dialog.dismiss();
    					String temp = mFileMag.getCurrentDir();
    					mHandler.updateDirectory(mFileMag.getNextDir(temp, true));
    				}
    			});
    			cancel.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {	dialog.dismiss(); }
    			});
    		break; 
    		case D_MENU_RENAME:
    		case F_MENU_RENAME:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Rename " + mSelectedListItem);
    			dialog.setCancelable(false);
    			
    			ImageView rename_icon = (ImageView)dialog.findViewById(R.id.input_icon);
    			rename_icon.setImageResource(R.drawable.rename);
    			
    			TextView rename_label = (TextView)dialog.findViewById(R.id.input_label);
    			rename_label.setText(mFileMag.getCurrentDir());
    			final EditText rename_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button rename_cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
    			Button rename_create = (Button)dialog.findViewById(R.id.input_create_b);
    			rename_create.setText("Rename");
    			
    			rename_create.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {
    					if(rename_input.getText().length() < 1)
    						dialog.dismiss();
    					
    					if(mFileMag.renameTarget(mFileMag.getCurrentDir() +"/"+ mSelectedListItem, rename_input.getText().toString()) == 0) {
    						Toast.makeText(Main.this, mSelectedListItem + " was renamed to " +rename_input.getText().toString(),
    								Toast.LENGTH_LONG).show();
    					}else
    						Toast.makeText(Main.this, mSelectedListItem + " was not renamed", Toast.LENGTH_LONG).show();
    						
    					dialog.dismiss();
    					String temp = mFileMag.getCurrentDir();
    					mHandler.updateDirectory(mFileMag.getNextDir(temp, true));
    				}
    			});
    			rename_cancel.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {	dialog.dismiss(); }
    			});
    		break;
    		
    		case SEARCH_B:
    		case MENU_SEARCH:
    			dialog.setContentView(R.layout.input_layout);
    			dialog.setTitle("Search");
    			dialog.setCancelable(false);
    			
    			ImageView searchIcon = (ImageView)dialog.findViewById(R.id.input_icon);
    			searchIcon.setImageResource(R.drawable.search);
    			
    			TextView search_label = (TextView)dialog.findViewById(R.id.input_label);
    			search_label.setText("Search for a file");
    			final EditText search_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button search_button = (Button)dialog.findViewById(R.id.input_create_b);
    			Button cancel_button = (Button)dialog.findViewById(R.id.input_cancel_b);
    			search_button.setText("Search");
    			
    			search_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) {
    					String temp = search_input.getText().toString();
    					
    					if (temp.length() > 0)
    						mHandler.searchForFile(temp);
    					dialog.dismiss();
    				}
    			});
    			
    			cancel_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) { dialog.dismiss(); }
    			});

    		break;
    	}
    	return dialog;
    }
    
    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
   public boolean onKeyDown(int keycode, KeyEvent event) {
    	String current = mFileMag.getCurrentDir();
    	
    	if(keycode == KeyEvent.KEYCODE_SEARCH) {
    		showDialog(SEARCH_B);
    		
    		return true;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && !current.equals("/")) {
    		if(mHandler.isMultiSelected()) {
    			mTable.killMultiSelect(true);
    			Toast.makeText(Main.this, "Multi-select is now off", Toast.LENGTH_SHORT).show();
    		
    		} else {
    			//stop updating thumbnail icons if its running
    			mHandler.stopThumbnailThread();
	    		mHandler.updateDirectory(mFileMag.getPreviousDir());
	    		mPathLabel.setText(mFileMag.getCurrentDir());
    		}
    		return true;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && current.equals("/")) {
    		Toast.makeText(Main.this, "Press back again to quit.", Toast.LENGTH_SHORT).show();
    		
    		if(mHandler.isMultiSelected()) {
    			mTable.killMultiSelect(true);
    			Toast.makeText(Main.this, "Multi-select is now off", Toast.LENGTH_SHORT).show();
    		}
    		
    		mUseBackKey = false;
    		mPathLabel.setText(mFileMag.getCurrentDir());
    		
    		return false;
    		
    	} else if(keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey && current.equals("/")) {
    		finish();
    		
    		return false;
    	}
    	return false;
    }
    
    
    
    
    //本内部类创建用于上传文件的线程
    class upload_thr implements Runnable {
    	public File file;
    	public String requestURL;
        public void run() {
    		String request = "what?";
            request = UploadUtil.uploadFile(file, requestURL);
            //Toast.makeText(getApplicationContext(), "Response: " + request, Toast.LENGTH_LONG).show();
            }
     }
    
   
    
    //处理线程中回调函数的成员变量
    private Handler showhandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {
    	    super.handleMessage(msg);
    	    
    	    String show = msg.getData().getString("show");

    	    switch (msg.what) {
    	        case 1: //自定义的一种消息回显方式
    	            Toast.makeText(getApplicationContext(), show, Toast.LENGTH_LONG).show();
    	        break;
    	    }
    	}
    	}; 
    
    //将操作结果回显到主线程
    public void ShowResult(String str){
    	
    	Message msg = new Message();
    	msg.what = 1; //这里只定义了一种方式
    	Bundle bundle = new Bundle(); //利用bundle传输参数
    	
	    bundle.putString("show", str);
	    msg.setData(bundle);
	    showhandler.sendMessage(msg);
	    //bundle.clear();
    	
    }
    
   
    
}



