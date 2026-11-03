package com.kevin.glasslauncher;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Offline home screen. No accessibility, network, or notification permissions. */
public class MainActivity extends Activity {
    final ArrayList<ResolveInfo> apps = new ArrayList<>();
    final Handler handler = new Handler(Looper.getMainLooper());
    final Set<String> favorites = new HashSet<>();
    LinearLayout page, rows;
    FrameLayout root;
    TextView clock, date, mode;
    EditText search;
    ScrollView scroll;
    boolean allApps = false, night = false;
    char browsingLetter = 0;
    AlphabetWave alphabetWave;
    int accent = Color.rgb(255, 190, 177);
    final Runnable tick = new Runnable() {
        public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            if (date != null) date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));
            if (night && clock != null) {
                long minute = System.currentTimeMillis() / 60000;
                clock.setTranslationX(dp((int)(minute % 7 - 3) * 5));
                clock.setTranslationY(dp((int)(minute % 5 - 2) * 5));
            }
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        favorites.addAll(getPreferences(0).getStringSet("favorites", Collections.emptySet()));
        loadApps(); home();
    }
    int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    String key(ResolveInfo a) { return a.activityInfo.packageName + "/" + a.activityInfo.name; }
    String label(ResolveInfo a) { return a.loadLabel(getPackageManager()).toString(); }
    void loadApps() {
        apps.clear();
        apps.addAll(getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        apps.removeIf(a -> a.activityInfo.packageName.equals(getPackageName()));
        apps.sort((a,b) -> label(a).compareToIgnoreCase(label(b)));
        if (!getPreferences(0).getBoolean("seeded", false)) {
            for (ResolveInfo a : apps) {
                String l = label(a).toLowerCase(Locale.ROOT);
                if (Arrays.asList("chatgpt","youtube","chrome","spotify","camera","gmail","photos","instagram").contains(l)) favorites.add(key(a));
            }
            if (favorites.isEmpty()) for (int i=0; i<Math.min(8, apps.size()); i++) favorites.add(key(apps.get(i)));
            saveFavorites();
        }
    }
    void saveFavorites() { getPreferences(0).edit().putStringSet("favorites", new HashSet<>(favorites)).putBoolean("seeded",true).apply(); }
    GradientDrawable glass() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x38ffffff,0x12ffffff});
        d.setCornerRadius(dp(28)); d.setStroke(dp(1),0x66ffffff); return d;
    }
    TextView text(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    TextView button(String label, Runnable action) {
        TextView b=text(label,14); b.setGravity(Gravity.CENTER); b.setPadding(dp(16),dp(12),dp(16),dp(12));
        b.setBackground(glass()); b.setOnClickListener(v->action.run()); b.setMinHeight(dp(48)); return b;
    }
    void home() {
        night=false; browsingLetter=0;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams w=getWindow().getAttributes(); w.screenBrightness=-1; getWindow().setAttributes(w);
        getWindow().getDecorView().setSystemUiVisibility(0);
        root=new FrameLayout(this); root.addView(new Aurora(this));
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(24),dp(16),dp(24),dp(16));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            page.setPadding(dp(24)+insets.getSystemWindowInsetLeft(),dp(16)+insets.getSystemWindowInsetTop(),
                dp(24)+insets.getSystemWindowInsetRight(),dp(16)+insets.getSystemWindowInsetBottom()); return insets;
        });
        root.requestApplyInsets();
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout time=new LinearLayout(this); time.setOrientation(LinearLayout.VERTICAL);
        clock=text("",64); clock.setTextColor(accent); clock.setTypeface(null,Typeface.BOLD);
        date=text("",16); time.addView(clock); time.addView(date);
        header.addView(time,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button("Settings",this::settings)); page.addView(header);
        search=new EditText(this); search.setSingleLine(true); search.setTextColor(Color.WHITE); search.setHintTextColor(0xffd2dce6);
        search.setTextSize(16); search.setHint("Search your apps"); search.setPadding(dp(20),dp(12),dp(20),dp(12)); search.setBackground(glass());
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52)); sp.setMargins(0,dp(20),0,dp(12)); page.addView(search,sp);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){browsingLetter=0;renderApps();}
            public void afterTextChanged(Editable e){}
        });
        FrameLayout body=new FrameLayout(this);
        scroll=new ScrollView(this); rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows);
        FrameLayout.LayoutParams listParams=new FrameLayout.LayoutParams(-1,-1);
        listParams.rightMargin=dp(48); body.addView(scroll,listParams);
        alphabetWave=new AlphabetWave(this);
        body.addView(alphabetWave,new FrameLayout.LayoutParams(-1,-1));
        page.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(this); footer.setGravity(Gravity.CENTER);
        mode=button("All apps",()->{allApps=!allApps;search.setText("");renderApps();});
        footer.addView(mode,new LinearLayout.LayoutParams(0,-2,1));
        footer.addView(button("Bedside clock",this::bedside),new LinearLayout.LayoutParams(0,-2,1));
        page.addView(footer); page.setFocusableInTouchMode(true); page.requestFocus(); renderApps();
    }
    void renderApps() {
        if (rows==null || night) return;
        rows.removeAllViews();
        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        ArrayList<ResolveInfo> filtered=new ArrayList<>();
        for (ResolveInfo a:apps) {
            String name=label(a).toLowerCase(Locale.ROOT);
            boolean matches=browsingLetter!=0?appLetter(a)==browsingLetter:name.contains(q);
            if (matches && (!q.isEmpty()||allApps||favorites.contains(key(a)))) filtered.add(a);
        }
        int columns=getResources().getConfiguration().screenWidthDp>=600?2:1;
        for(int i=0;i<filtered.size();i+=columns) {
            LinearLayout line=new LinearLayout(this);
            for(int j=0;j<columns;j++){
                if(i+j>=filtered.size()) { line.addView(new View(this),new LinearLayout.LayoutParams(0,dp(76),1)); continue; }
                ResolveInfo a=filtered.get(i+j);
                LinearLayout cell=new LinearLayout(this); cell.setGravity(Gravity.CENTER_VERTICAL); cell.setPadding(dp(12),dp(10),dp(12),dp(10)); cell.setBackground(glass());
                ImageView icon=new ImageView(this); icon.setImageDrawable(a.loadIcon(getPackageManager()));
                cell.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
                TextView title=text(label(a),18); title.setPadding(dp(14),0,0,0); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END);
                cell.addView(title,new LinearLayout.LayoutParams(0,-2,1));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(76),1); cp.setMargins(0,0,dp(10),dp(10)); line.addView(cell,cp);
                cell.setContentDescription(label(a)+". Long press to change favorites.");
                cell.setOnClickListener(v->launch(a));
                cell.setOnLongClickListener(v->{ if(!favorites.add(key(a)))favorites.remove(key(a));saveFavorites();renderApps();Toast.makeText(this,favorites.contains(key(a))?"Added to favorites":"Removed from favorites",Toast.LENGTH_SHORT).show();return true;});
            }
            rows.addView(line);
        }
        if(filtered.isEmpty()) rows.addView(text(q.isEmpty()?"No favorites yet. Open All apps and hold an app to pin it.":"No matching apps",18));
        if(mode!=null)mode.setText(allApps?"Favorites":"All apps");
        scroll.scrollTo(0,0);
    }
    char appLetter(ResolveInfo a) {
        String name=label(a).trim().toUpperCase(Locale.ROOT);
        char first=name.isEmpty()?'#':name.charAt(0);
        return first>='A'&&first<='Z'?first:'#';
    }
    void chooseLetter(char letter) {
        if(search.length()>0) search.setText("");
        allApps=letter!='★'; browsingLetter=letter=='★'?0:letter;
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);
        page.requestFocus(); renderApps();
    }
    void finishLetter(char letter) {
        if(letter=='★') { chooseLetter(letter); return; }
        browsingLetter=0; allApps=true; renderApps();
        int first=-1;
        for(int i=0;i<apps.size();i++) if(appLetter(apps.get(i))==letter){first=i;break;}
        final int row=first/(getResources().getConfiguration().screenWidthDp>=600?2:1);
        if(first>=0) scroll.post(()->{
            if(!night && row<rows.getChildCount()) scroll.scrollTo(0,rows.getChildAt(row).getTop());
        });
    }
    /** A continuous edge gesture; lifting never launches an app. */
    class AlphabetWave extends View {
        final String letters="★ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        int selected=-1;
        boolean dragging=false;
        float touchY, pull;
        AlphabetWave(Context context) {
            super(context); setFocusable(true);
            setContentDescription("Alphabet navigation. Drag up or down along the right edge. Star shows favorites.");
        }
        float step(){return Math.max(1,(getHeight()-dp(24))/(float)letters.length());}
        int index(float y){return Math.max(0,Math.min(letters.length()-1,(int)((y-dp(12))/step())));}
        void select(float x,float y) {
            touchY=Math.max(dp(12),Math.min(getHeight()-dp(12),y));
            pull=Math.max(dp(54),Math.min(dp(130),getWidth()-x));
            int next=index(touchY);
            if(next!=selected){
                selected=next; chooseLetter(letters.charAt(next));
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                setContentDescription(letters.charAt(next)=='★'?"Favorites":"Apps starting with "+letters.charAt(next));
            }
            invalidate();
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if(event.getX()<getWidth()-dp(48))return false;
                    dragging=true;selected=-1;getParent().requestDisallowInterceptTouchEvent(true);
                    select(event.getX(),event.getY());return true;
                case MotionEvent.ACTION_MOVE:
                    if(!dragging)return false;
                    select(event.getX(),event.getY());return true;
                case MotionEvent.ACTION_UP:
                    if(!dragging)return false;
                    dragging=false;getParent().requestDisallowInterceptTouchEvent(false);
                    if(selected>=0)finishLetter(letters.charAt(selected));
                    performClick();invalidate();return true;
                case MotionEvent.ACTION_CANCEL:
                    if(!dragging)return false;
                    dragging=false;getParent().requestDisallowInterceptTouchEvent(false);
                    if(selected>=0)finishLetter(letters.charAt(selected));
                    invalidate();return true;
                default:return dragging;
            }
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onKeyDown(int code,KeyEvent event){
            if(code==KeyEvent.KEYCODE_DPAD_DOWN||code==KeyEvent.KEYCODE_DPAD_UP){
                selected=Math.max(0,Math.min(letters.length()-1,selected+(code==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));
                chooseLetter(letters.charAt(selected));invalidate();return true;
            }
            return super.onKeyDown(code,event);
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){
            super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        @Override public boolean performAccessibilityAction(int action,Bundle args){
            if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){
                selected=Math.max(0,Math.min(letters.length()-1,selected+(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1)));
                chooseLetter(letters.charAt(selected));announceForAccessibility(getContentDescription());invalidate();return true;
            }
            return super.performAccessibilityAction(action,args);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float edge=getWidth()-dp(22);
            if(dragging){paint.setColor(0x20000000);canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            for(int i=0;i<letters.length();i++){
                float y=dp(12)+step()*(i+.5f);
                float distance=(y-touchY)/dp(96);
                float wave=dragging?(float)Math.exp(-distance*distance)*pull:0;
                paint.setColor(i==selected?accent:0xddffffff);
                paint.setTextSize(Math.min(dp(12),step()*.85f));
                canvas.drawText(String.valueOf(letters.charAt(i)),edge-wave,y-(paint.ascent()+paint.descent())/2,paint);
            }
            if(dragging&&selected>=0){
                float x=Math.max(dp(30),edge-pull-dp(36));
                float y=Math.max(dp(30),Math.min(getHeight()-dp(30),touchY));
                paint.setShader(new LinearGradient(x-dp(26),y-dp(26),x+dp(26),y+dp(26),new int[]{0xaaffffff,0x446f9fbb},null,Shader.TileMode.CLAMP));
                canvas.drawCircle(x,y,dp(26),paint);paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0xbbffffff);canvas.drawCircle(x,y,dp(26),paint);paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(dp(26));paint.setColor(Color.WHITE);
                canvas.drawText(String.valueOf(letters.charAt(selected)),x,y-(paint.ascent()+paint.descent())/2,paint);
            }
        }
    }
    void launch(ResolveInfo a) {
        try {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(a.activityInfo.packageName,a.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch(RuntimeException e) { Toast.makeText(this,"This app is unavailable. Refreshing the list.",Toast.LENGTH_SHORT).show();loadApps();renderApps(); }
    }
    void settings() {
        new AlertDialog.Builder(this).setTitle("Glass Launcher")
            .setItems(new String[]{"Set as home screen","Change accent","About bedside clock"},(dialog,which)->{
                if(which==0){
                    RoleManager rm=getSystemService(RoleManager.class);
                    if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)&&!rm.isRoleHeld(RoleManager.ROLE_HOME))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),10);
                    else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                }else if(which==1){
                    new AlertDialog.Builder(this).setTitle("Accent").setItems(new String[]{"Peach","Ice","Lavender"},(d,n)->{
                        accent=new int[]{0xffffbeb1,0xffa6eced,0xffd6baff}[n];getPreferences(0).edit().putInt("accent",accent).apply();clock.setTextColor(accent);
                    }).show();
                }else new AlertDialog.Builder(this).setTitle("Bedside clock")
                    .setMessage("An optional dim clock while this app is open. It keeps the screen on and uses battery. It is not a system always-on display and does not replace your lock screen. Tap anywhere or press Back to exit.")
                    .setPositiveButton("Open clock",(d,n)->bedside()).setNegativeButton("Close",null).show();
            }).setNegativeButton("Close",null).show();
    }
    void bedside() {
        night=true; ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setGravity(Gravity.CENTER);screen.setBackgroundColor(Color.BLACK);
        clock=text("",80);clock.setTextColor(0xff9ba2ad);date=text("",16);date.setTextColor(0xff747b85);
        screen.addView(clock);screen.addView(date);TextView hint=text("Tap anywhere to return",12);hint.setPadding(0,dp(40),0,0);hint.setTextColor(0xff606770);screen.addView(hint);
        setContentView(screen);screen.setOnClickListener(v->home());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams w=getWindow().getAttributes();w.screenBrightness=0.03f;getWindow().setAttributes(w);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    @Override protected void onResume(){super.onResume();accent=getPreferences(0).getInt("accent",0xffffbeb1);if(!night){loadApps();renderApps();if(clock!=null)clock.setTextColor(accent);}handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);if(night)home();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);allApps=false;home();}
    @Override public void onBackPressed(){allApps=false;home();}
    static class Aurora extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Aurora(Context c){super(c);}
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff0c2033,0xff175361,0xff192b43},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.8f,h*.3f,w*.65f,new int[]{0x8878ddd1,0x00173750},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.25f,h*.95f,w*.65f,new int[]{0x777e619e,0x00173750},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
            p.setShader(null);p.setColor(0x0cffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
            c.drawCircle(w*.8f,h*.25f,w*.6f,p);c.drawCircle(w*.8f,h*.25f,w*.63f,p);p.setStyle(Paint.Style.FILL);
        }
    }
}
