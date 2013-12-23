package com.nexes.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;



class DBHelper extends SQLiteOpenHelper {  
	  
    private static final String DATABASE_NAME = "white_list.db";  
    private static final int DATABASE_VERSION = 1;  
      
    public DBHelper(Context context) {  
        super(context, DATABASE_NAME, null, DATABASE_VERSION);  
    }  
  
    //数据库第一次被创建时onCreate会被调用  
    @Override
    public void onCreate(SQLiteDatabase db) {  
        db.execSQL("CREATE TABLE IF NOT EXISTS white_list" +  
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT, fileID VARCHAR )");  
    }  
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {  
        db.execSQL("ALTER TABLE white_list ADD COLUMN other STRING");
    }
}


public class DatabaseUtil {  
    private DBHelper helper;  
    private SQLiteDatabase db;  
      
    public DatabaseUtil(Context context) {  
        helper = new DBHelper(context);  
        //因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0, mFactory);  
        //所以要确保context已初始化,我们可以把实例化放在Activity的onCreate里  
        db = helper.getWritableDatabase();  
    }  
      

    public void add(String fileid)
    {  
        db.beginTransaction();  //开始事务  
        try {  
        		//保证表中的数据不重复
        		Cursor result = db.rawQuery("select * from white_list where fileID = ? ", new String[]{fileid});
        		if(result.getCount() > 0)
        			return;	      			
                db.execSQL("INSERT INTO white_list VALUES(null, ?)", new String[]{fileid});  
                db.setTransactionSuccessful();  //设置事务成功完成  
        } finally {  
            db.endTransaction();    //结束事务 
        }  
    }  
      

    public void update(String fileid)
    {  
        ContentValues cv = new ContentValues();  
        cv.put("fileID", fileid);  
        db.update("white_list", cv, "fileID = ?", new String[]{fileid});  
    }  
    
    public String find(String fileid){
    	Cursor result = db.rawQuery("select * from white_list where fileID = ? ", new String[]{fileid});
    	   result.moveToFirst(); 
    	   String findstring = "";
    	    while (!result.isAfterLast()) { 
    	        findstring += result.getString(1) + "\n";
    	        result.moveToNext(); 
    	      } 
    	     result.close(); 
    	     return findstring;
    }
    
    public Boolean IsExist(String fileid)
    {
    	Cursor result = db.rawQuery("select * from white_list where fileID = ? ", new String[]{fileid});
		if(result.getCount() > 0)
			return  false;
		else 
			return  true;
    	
    }
    
    public String listall(){
    	Cursor result = db.rawQuery("select * from white_list", null);
    	   result.moveToFirst(); 
    	   String findstring = "";
    	    while (!result.isAfterLast()) { 
    	        findstring += result.getString(0)+ "-" + result.getString(1) + "\n";
    	        result.moveToNext(); 
    	      } 
    	     result.close(); 
    	     return findstring;
    }
      

    public void delete(String fileid) {  
        db.delete("white_list", "fileID = ?", new String[]{fileid});  
    }

    
    public void closeDB() {  
        db.close();  
    } 
}  




