package com.kevin.glasslauncher;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.*;
import java.util.*;
import org.json.*;

/** Owns widget IDs, setup transactions and saved home-screen placement. */
final class WidgetBoard {
    static final int BIND=701, CONFIGURE=702, HOST=4101;
    final MainActivity activity;
    final AppWidgetManager manager;
    final AppWidgetHost host;
    final SharedPreferences prefs;
    final ArrayList<Item> items=new ArrayList<>();
    int pending;
    static class Item {
        int id,width,height;
        Item(int id,int width,int height){this.id=id;this.width=width;this.height=height;}
    }
    WidgetBoard(MainActivity a){
        activity=a;prefs=a.getPreferences(0);manager=AppWidgetManager.getInstance(a);host=new AppWidgetHost(a,HOST);
        pending=prefs.getInt("pendingWidget",-1);
        try{JSONArray array=new JSONArray(prefs.getString("widgets","[]"));
            for(int i=0;i<array.length();i++){JSONObject o=array.getJSONObject(i);items.add(new Item(o.getInt("id"),o.optInt("width",320),o.optInt("height",180)));}
        }catch(JSONException ignored){items.clear();}
        // Reclaim IDs from additions interrupted before they could be persisted.
        for(int id:host.getAppWidgetIds()){
            boolean keep=id==pending;for(Item item:items)if(item.id==id)keep=true;
            if(!keep)host.deleteAppWidgetId(id);
        }
    }
    void start(){host.startListening();}
    void stop(){host.stopListening();}
    void save(){
        JSONArray array=new JSONArray();
        for(Item item:items){JSONObject o=new JSONObject();try{o.put("id",item.id);o.put("width",item.width);o.put("height",item.height);}catch(JSONException ignored){}array.put(o);}
        prefs.edit().putString("widgets",array.toString()).apply();
    }
    void pending(int id){pending=id;prefs.edit().putInt("pendingWidget",id).apply();}
    void cancel(){if(pending!=-1)host.deleteAppWidgetId(pending);pending(-1);}
    void message(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    void pick(){
        if(pending!=-1)cancel();
        ArrayList<AppWidgetProviderInfo> providers=new ArrayList<>(manager.getInstalledProviders());
        providers.removeIf(p->(p.widgetCategory&AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)==0);
        providers.sort((a,b)->a.loadLabel(activity.getPackageManager()).compareToIgnoreCase(b.loadLabel(activity.getPackageManager())));
        if(providers.isEmpty()){message("No widgets available. Install an app that provides Android widgets.");return;}
        Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(activity.dp(20),activity.dp(20),activity.dp(20),activity.dp(20));panel.setBackgroundColor(0xff102333);
        panel.addView(activity.text("Add a widget",24));
        EditText search=new EditText(activity);search.setSingleLine(true);search.setTextColor(-1);search.setHintTextColor(0xffb4c9d4);search.setHint("Search widgets");panel.addView(search);
        ListView list=new ListView(activity);panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        ArrayList<AppWidgetProviderInfo> visible=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);
        Runnable filter=()->{visible.clear();labels.clear();String q=search.getText().toString().toLowerCase(Locale.ROOT);
            for(AppWidgetProviderInfo p:providers){String label=p.loadLabel(activity.getPackageManager())+" · "+p.provider.getPackageName();if(label.toLowerCase(Locale.ROOT).contains(q)){visible.add(p);labels.add(label);}}adapter.notifyDataSetChanged();};
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){filter.run();}public void afterTextChanged(Editable e){}});
        filter.run();list.setOnItemClickListener((parent,view,position,id)->{AppWidgetProviderInfo info=visible.get(position);dialog.dismiss();bind(info);});
        panel.addView(activity.button("Close",dialog::dismiss));dialog.setContentView(panel);dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
    }
    void bind(AppWidgetProviderInfo info){
        pending(host.allocateAppWidgetId());
        Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        try{
            if(manager.bindAppWidgetIdIfAllowed(pending,info.provider,options))configure();
            else activity.startActivityForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pending)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options),BIND);
        }catch(RuntimeException error){cancel();message("This widget could not be added. Try another widget.");}
    }
    void configure(){
        AppWidgetProviderInfo info=manager.getAppWidgetInfo(pending);
        if(info==null){cancel();message("Widget permission was not granted.");return;}
        if(info.configure==null){finish();return;}
        try{host.startAppWidgetConfigureActivityForResult(activity,pending,0,CONFIGURE,null);}
        catch(RuntimeException error){cancel();message("The widget's setup screen could not open.");}
    }
    void result(int request,int result){
        if(request!=BIND&&request!=CONFIGURE)return;
        if(pending==-1)return;
        if(result!=Activity.RESULT_OK){cancel();return;}
        if(request==BIND)configure();else finish();
    }
    void finish(){
        AppWidgetProviderInfo info=manager.getAppWidgetInfo(pending);
        if(info==null){cancel();return;}
        int id=pending;
        items.add(new Item(id,Math.max(280,dip(info.minWidth)),Math.max(120,dip(info.minHeight))));
        save();pending(-1);activity.allApps=false;activity.home();
    }
    int dip(int pixels){return Math.round(pixels/activity.getResources().getDisplayMetrics().density);}
    int minWidth(AppWidgetProviderInfo p){return (p.resizeMode&AppWidgetProviderInfo.RESIZE_HORIZONTAL)!=0&&p.minResizeWidth>0?dip(p.minResizeWidth):dip(p.minWidth);}
    int minHeight(AppWidgetProviderInfo p){return (p.resizeMode&AppWidgetProviderInfo.RESIZE_VERTICAL)!=0&&p.minResizeHeight>0?dip(p.minResizeHeight):dip(p.minHeight);}
    void resize(Item item,int dx,int dy){
        AppWidgetProviderInfo p=manager.getAppWidgetInfo(item.id);if(p==null)return;
        if(dx!=0&&(p.resizeMode&AppWidgetProviderInfo.RESIZE_HORIZONTAL)==0){message("This widget has a fixed width.");return;}
        if(dy!=0&&(p.resizeMode&AppWidgetProviderInfo.RESIZE_VERTICAL)==0){message("This widget has a fixed height.");return;}
        int available=Math.max(80,activity.getResources().getConfiguration().screenWidthDp-144);
        item.width=Math.max(minWidth(p),Math.min(available,item.width+dx));
        item.height=Math.max(minHeight(p),Math.min(900,item.height+dy));
        if(android.os.Build.VERSION.SDK_INT>=31){if(p.maxResizeWidth>0)item.width=Math.min(item.width,Math.max(minWidth(p),dip(p.maxResizeWidth)));if(p.maxResizeHeight>0)item.height=Math.min(item.height,Math.max(minHeight(p),dip(p.maxResizeHeight)));}
        save();activity.renderApps();
    }
    void options(Item item){
        activity.wheel("Widget controls",new String[]{"Move up","Move down","Wider","Narrower","Taller","Shorter","Remove widget"},null,n->{
            int index=items.indexOf(item);if(index<0)return;
            if(n<2){int next=index+(n==0?-1:1);if(next>=0&&next<items.size()){Collections.swap(items,index,next);save();activity.renderApps();}}
            else if(n==6){host.deleteAppWidgetId(item.id);items.remove(item);save();activity.renderApps();}
            else resize(item,n==2?64:n==3?-64:0,n==4?64:n==5?-64:0);
        });
    }
    void render(LinearLayout rows){
        rows.addView(activity.button("＋ Add widgets",this::pick));
        for(Item item:new ArrayList<>(items)){
            AppWidgetProviderInfo info=manager.getAppWidgetInfo(item.id);
            LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(activity.dp(8),activity.dp(8),activity.dp(8),activity.dp(8));card.setBackground(activity.glass());
            card.addView(activity.button((info==null?"Unavailable widget":info.loadLabel(activity.getPackageManager()))+"  ·  Edit",()->options(item)));
            if(info!=null){
                try{
                    AppWidgetHostView view=host.createView(activity,item.id,info);
                    int height=Math.max(minHeight(info),item.height);
                    LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(activity.dp(item.width),activity.dp(height));vp.gravity=Gravity.CENTER_HORIZONTAL;card.addView(view,vp);
                    card.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
                        int available=card.getWidth()-card.getPaddingLeft()-card.getPaddingRight();if(available<=0)return;
                        int width=Math.min(activity.dp(item.width),available);
                        if(view.getLayoutParams().width!=width){view.getLayoutParams().width=width;view.requestLayout();}
                        int widthDp=Math.round(width/activity.getResources().getDisplayMetrics().density);
                        // HostView subtracts its own default padding when notifying the provider.
                        view.updateAppWidgetSize(null,widthDp,height,widthDp,height);
                    });
                }catch(RuntimeException error){card.addView(activity.text("Widget could not load. Use Edit to remove it.",14));}
            }else card.addView(activity.text("The app may be removed or temporarily unavailable.",14));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,activity.dp(8),0,activity.dp(12));rows.addView(card,cp);
        }
    }
}
