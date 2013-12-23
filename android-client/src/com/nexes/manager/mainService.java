package com.nexes.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.nexes.manager.EventHandler.scan_thread;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class mainService extends Service {
	private static final String TAG = "mainService";
	private DatabaseUtil mydatabase; 
	
	 @Override
	 public IBinder onBind(Intent intent) {
		 	Log.e(TAG, "start IBinder~~~");
	        return null;
	    }
	 
	 @Override
	 public void onCreate() {
	        //Toast.makeText(this, "My Service created", Toast.LENGTH_LONG).show();
	        Log.i(TAG, "创建onCreate");
	        mydatabase = new DatabaseUtil(this); //数据库访问对象

	    }

	 @Override
	 public void onDestroy() {
	        //Toast.makeText(this, "服务销毁...", Toast.LENGTH_LONG).show();
	        Log.i(TAG, "销毁onDestroy");
	        
	    }

	 @Override
	 public void onStart(Intent intent, int startid) {
	        //Toast.makeText(this, "启动监控服务...", Toast.LENGTH_LONG).show();
	        Log.i(TAG, "后台服务启动");
	        
	        //开启文件事件监控
	        //TODO :递归对每个目录做下面的事情
            setlistener_thread t1 = new setlistener_thread();
	        new Thread(t1).start();
	 }
	
	
    private Handler showhandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {
    	    super.handleMessage(msg);
    	    
    	    String show = msg.getData().getString("show");

    	    switch (msg.what) {
    	        case 1: //自定义的一种消息回显方式
    	            //Toast.makeText(getApplicationContext(), show, Toast.LENGTH_LONG).show();
    	        	Toast.makeText(mainService.this, show, Toast.LENGTH_LONG).show();
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

    
	//交给线程来做
	class setlistener_thread implements Runnable {

	    public void run() {
	    	SetListener("/sdcard"); //TODO :先用sd卡目录作为测试
	        }
	 }
    
	//递归查找整个文件系统
	public void SetListener(String path)
	{
		if(isDirectory(path)) 
		{
			Log.d("设置文件监听:",path);
	        FileListener listener = new FileListener(path);  
	        listener.startWatching();
			
			String[] list = list_file(path);
			if (list != null)
			{
				for(int i=0; i<list.length; i++)
				{
					SetListener(list[i]);
				}
			}
		}
	}
	
	//列出某一目录path内所有的文件
	public String[] list_file(String path) {
		
		File file = new File(path);
		if(file.canRead())
		{
			String[] list = file.list();
			for(int i=0;i<list.length;i++)
				list[i] = path + "/" +list[i];
			
			return list;
		}
		else
		{
			Log.w("文件不可读：",path); //注意，如何文件由于权限问题不可读，则函数返回null
		}
		return null;
	}
	
	//判断一个path是文件还是目录
	private boolean isDirectory(String name) {
		return new File(name).isDirectory();
	}
	    
	
	
	//本内部类用于监控文件
	class FileListener extends FileObserver{
		private String mAbsolutePath; //保存绝对路径
	    	
	    	public FileListener(String path){
	    		super(path); //监听所有事件，都要回调onEvent
	    		mAbsolutePath = path;
	    		Log.d("开始文件监听",path);
	    	}
	    	
	    	@Override
	    	public void onEvent(int event, String path){
	    		//如果这里要做的事情比较多，最好用线程去做
	    		switch(event){
	    			case FileObserver.CREATE:
	    				String newfile = mAbsolutePath + "/" + path;
	    				
	    				Log.d("监听到-文件创建-事件",newfile);
	    				auto_scan(newfile);
	    				
	    				break;
	    			case FileObserver.ALL_EVENTS:
	    				Log.d("other_file_event",path);
	    				break;
	    		}
	    	}
	}
	    
	//自动处理文件
	public void auto_scan(String filepath){
			
			ShowResult("发现新文件:"+filepath);
			Log.d("扫描新文件",filepath);
			
	    	File file = new File(filepath);
			
			String requestURL = getResources().getString(R.string.ip_address) + "md5handle/";
			String s = file.toString();
			String md5 = GetDigest.getMD5EncryptedString(s);
			
			if(mydatabase.IsExist(md5) == false) //找到了，返回安全
			{
				Toast.makeText(this, "文件安全", Toast.LENGTH_LONG).show();
				return;
	        }
			else
			{
					File tempfile;
					//tempfile = new File("/sdcard/LOST.DIR/" + file.getName() + "_md5.txt");
				try {
					File outputDir = getCacheDir();
					tempfile = File.createTempFile(file.getName(), null, outputDir);
					Log.d("创建临时文件:",tempfile.getAbsolutePath());
					
					FileOutputStream out = new FileOutputStream(tempfile);
		            PrintStream p= new PrintStream(out);
		            p.println(md5);
		            p.close();
		            out.close();

				
		            if(file!=null){
					
						//上传MD5没有使用线程，因为信息量很短
						String result = UploadUtil.uploadFile(tempfile, requestURL);
				        tempfile.delete(); //删除临时文件
				        
				        if(result.equals("new_file"))
				        {	//如果是云端没有的文件，则把整个文件上传
				        	requestURL = getResources().getString(R.string.ip_address) + "filehandle/";
			                //upload_thr upthread = new upload_thr();
			    	        //upthread.file = file;
			    	        //upthread.requestURL = requestURL;
			    	        //new Thread(upthread).start();
				        	
				        	String result2 = "what?";
				        	result2 = UploadUtil.uploadFile(file, requestURL);
				        	ShowResult("文件扫描结果:" + result2);
				        	if(result2=="safe")
				        	{
				        		mydatabase.add(md5);
				        		ShowResult("文件安全");
				        	}
				        	else
				        	{
				        		file.delete();
				        		ShowResult("文件扫描结果:" + result2 + " 已删除");
				        	}
				        }
				        else
				        {
				        	ShowResult("摘要已上传:" + result);
				        }
			        }
				} catch (FileNotFoundException e) {
					Log.w("error","文件操作错误：FileNotFoundException");
					e.printStackTrace();
				} catch (IOException e) {
					Log.w("error","文件操作错误：IOException");
					e.printStackTrace();
				}
			}
	}

}
