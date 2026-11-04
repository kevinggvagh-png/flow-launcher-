package com.kevin.glasslauncher;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
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
    boolean allApps = false, night = false, tintedGlass = false;
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
        accent=getPreferences(0).getInt("accent",0xffffbeb1);
        tintedGlass=getPreferences(0).getBoolean("tintedGlass",false);
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
    int alpha(int color, int opacity) { return (color & 0x00ffffff) | (opacity << 24); }
    Drawable glass() { return new GlassSurface(); }
    class GlassSurface extends Drawable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void draw(Canvas c) {
            Rect bounds=getBounds(); RectF r=new RectF(bounds); r.inset(dp(1),dp(1));
            float radius=dp(28);
            int tint=tintedGlass?accent:0xffd4ecff;
            p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,
                new int[]{0x99ffffff,alpha(tint,tintedGlass?115:40),0x55102032,alpha(tint,65)},
                new float[]{0,.25f,.7f,1},Shader.TileMode.CLAMP));
            c.drawRoundRect(r,radius,radius,p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.5f));
            p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,
                new int[]{0xeeffffff,alpha(tint,80),0x99ffffff},null,Shader.TileMode.CLAMP));
            c.drawRoundRect(r,radius,radius,p); p.setShader(null);
            r.inset(dp(3),dp(3)); p.setStrokeWidth(dp(1)); p.setColor(0x30ffffff);
            c.drawRoundRect(r,radius-dp(3),radius-dp(3),p); p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int value) { }
        @Override public void setColorFilter(ColorFilter filter) { p.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
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
        listParams.leftMargin=dp(36); listParams.rightMargin=dp(36); body.addView(scroll,listParams);
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
                GradientDrawable iconSurface=new GradientDrawable(); iconSurface.setShape(GradientDrawable.OVAL);
                iconSurface.setColor(alpha(accent,90)); icon.setBackground(iconSurface);
                icon.setClipToOutline(true); icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                icon.setOutlineProvider(new ViewOutlineProvider(){
                    @Override public void getOutline(View v,Outline outline){outline.setOval(0,0,v.getWidth(),v.getHeight());}
                });
                cell.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
                TextView title=text(label(a),18); title.setPadding(dp(14),0,0,0); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END);
                cell.addView(title,new LinearLayout.LayoutParams(0,-2,1));
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(76),1); cp.setMargins(0,0,dp(10),dp(10)); line.addView(cell,cp);
                cell.setContentDescription(label(a)+". Long press to change favorites.");
                cell.setOnClickListener(v->launch(a));
                cell.setOnLongClickListener(v->{
                    wheel(label(a),new String[]{"Open app",favorites.contains(key(a))?"Unpin favorite":"Pin favorite","App info"},null,n->{
                        if(n==0)launch(a);
                        else if(n==1){if(!favorites.add(key(a)))favorites.remove(key(a));saveFavorites();renderApps();}
                        else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.activityInfo.packageName)));
                    });return true;
                });
            }
            rows.addView(line);
        }
        if(filtered.isEmpty()) rows.addView(text(browsingLetter!=0?"No apps under "+browsingLetter:q.isEmpty()?"No favorites yet. Open All apps and hold an app to pin it.":"No matching apps",18));
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
        boolean dragging=false, leftEdge=false;
        float touchY, pull;
        AlphabetWave(Context context) {
            super(context); setFocusable(true);
            setContentDescription("Alphabet navigation. Drag up or down along either edge. Star shows favorites.");
        }
        float step(){return Math.max(1,(getHeight()-dp(24))/(float)letters.length());}
        int index(float y){return Math.max(0,Math.min(letters.length()-1,(int)((y-dp(12))/step())));}
        void select(float x,float y) {
            touchY=Math.max(dp(12),Math.min(getHeight()-dp(12),y));
            pull=Math.max(dp(54),Math.min(dp(130),(leftEdge?x:getWidth()-x)));
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
                    if(event.getX()>dp(36)&&event.getX()<getWidth()-dp(36))return false;
                    leftEdge=event.getX()<getWidth()/2f;
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
            float edge=leftEdge?dp(16):getWidth()-dp(16);
            float direction=leftEdge?1:-1;
            if(!dragging){
                paint.setColor(0x88ffffff);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(dp(12),step()*.85f));
                float other=leftEdge?getWidth()-dp(16):dp(16);
                for(int i=0;i<letters.length();i++)canvas.drawText(String.valueOf(letters.charAt(i)),other,dp(12)+step()*(i+.5f)-(paint.ascent()+paint.descent())/2,paint);
            }
            if(dragging){paint.setColor(0x20000000);canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
            paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            for(int i=0;i<letters.length();i++){
                float y=dp(12)+step()*(i+.5f);
                float distance=(y-touchY)/dp(96);
                float wave=dragging?(float)Math.exp(-distance*distance)*pull:0;
                paint.setColor(i==selected?accent:0xddffffff);
                paint.setTextSize(Math.min(dp(12),step()*.85f));
                canvas.drawText(String.valueOf(letters.charAt(i)),edge+direction*wave,y-(paint.ascent()+paint.descent())/2,paint);
            }
            if(dragging&&selected>=0){
                float x=Math.max(dp(30),Math.min(getWidth()-dp(30),edge+direction*(pull+dp(36))));
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
    interface Choice { void select(int index); }
    void wheel(String title,String[] options,int[] colors,Choice action) {
        Dialog dialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18),dp(20),dp(18),dp(16));panel.setBackground(glass());
        TextView heading=text(title,22);heading.setGravity(Gravity.CENTER);panel.addView(heading);
        TextView hint=text("Swipe around the dial · tap an option",13);hint.setGravity(Gravity.CENTER);panel.addView(hint);
        Dial dial=new Dial(options,colors);panel.addView(dial,new LinearLayout.LayoutParams(-1,dp(280)));
        TextView selected=text(options[0],20);selected.setGravity(Gravity.CENTER);selected.setTextColor(accent);
        selected.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);panel.addView(selected);
        dial.changed=()->selected.setText(options[dial.selected()]);
        LinearLayout controls=new LinearLayout(this);
        controls.addView(button("Previous",()->dial.move(-1)),new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(button("Next",()->dial.move(1)),new LinearLayout.LayoutParams(0,-2,1));panel.addView(controls);
        panel.addView(button("Choose",()->{int n=dial.selected();dialog.dismiss();action.select(n);}));
        panel.addView(button("Close",dialog::dismiss));
        dialog.setContentView(panel);
        Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=.55f;window.setAttributes(lp);}
        dialog.show();
        if(window!=null)window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(430)),WindowManager.LayoutParams.WRAP_CONTENT);
    }
    class Dial extends View {
        final String[] options;final int[] colors;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        float position=0,lastAngle,downX,downY;boolean moved;Runnable changed;
        Dial(String[] values,int[] swatches){super(MainActivity.this);options=values;colors=swatches;setContentDescription("Circular options dial. Use Previous, Next and Choose buttons.");}
        int selected(){return ((Math.round(position)%options.length)+options.length)%options.length;}
        void move(int amount){position=Math.round(position)+amount;notifySelection();}
        void notifySelection(){if(changed!=null)changed.run();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);invalidate();}
        float angle(float x,float y){return (float)Math.atan2(y-getHeight()/2f,x-getWidth()/2f);}
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY();
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;lastAngle=angle(x,y);return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(Math.hypot(x-downX,y-downY)>dp(8))moved=true;
                float next=angle(x,y),delta=next-lastAngle;
                if(delta>Math.PI)delta-=2*Math.PI;if(delta<-Math.PI)delta+=2*Math.PI;
                int old=selected();position-=delta*options.length/(2*(float)Math.PI);lastAngle=next;
                if(old!=selected())notifySelection();else invalidate();return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                if(!moved){
                    float tapped=(angle(x,y)+(float)Math.PI/2)*options.length/(2*(float)Math.PI);
                    position=Math.round(position+tapped);
                }else position=Math.round(position);
                notifySelection();performClick();return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){position=Math.round(position);notifySelection();return true;}
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        @Override protected void onDraw(Canvas c){
            float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx-dp(36),cy-dp(34));
            p.setShader(new RadialGradient(cx-r*.3f,cy-r*.4f,r*1.6f,new int[]{0x66ffffff,alpha(accent,55),0x33102136},null,Shader.TileMode.CLAMP));
            c.drawCircle(cx,cy,r+dp(25),p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.5f));p.setColor(0xaaffffff);c.drawCircle(cx,cy,r+dp(25),p);
            p.setColor(alpha(accent,160));c.drawCircle(cx,cy,r-dp(22),p);p.setStyle(Paint.Style.FILL);
            for(int i=0;i<60;i++){
                double a=i*Math.PI/30;float inner=r+dp(i%5==0?15:20);p.setColor(0x66ffffff);p.setStrokeWidth(dp(1));
                c.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*(r+dp(24)),cy+(float)Math.sin(a)*(r+dp(24)),p);
            }
            p.setTextAlign(Paint.Align.CENTER);
            for(int i=0;i<options.length;i++){
                double a=(i-position)*2*Math.PI/options.length-Math.PI/2;
                float x=cx+(float)Math.cos(a)*r,y=cy+(float)Math.sin(a)*r;
                boolean active=i==selected();float size=dp(active?23:18);
                p.setColor(colors!=null?colors[i]:active?accent:0xff3f6376);c.drawCircle(x,y,size,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(active?2:1));p.setColor(active?Color.WHITE:0x77ffffff);c.drawCircle(x,y,size,p);p.setStyle(Paint.Style.FILL);
                p.setColor(0xff102337);p.setTextSize(dp(active?17:14));p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(String.valueOf(i+1),x,y-(p.ascent()+p.descent())/2,p);
            }
            p.setTypeface(Typeface.DEFAULT);p.setColor(Color.WHITE);p.setTextSize(dp(16));
            c.drawText("ROTATE",cx,cy-dp(4),p);p.setTextSize(dp(12));p.setColor(accent);c.drawText((selected()+1)+" / "+options.length,cx,cy+dp(18),p);
        }
    }
    void accents() {
        String[] names={"Peach","Ice","Lavender","Rose","Mint","Amber","Ocean","Pearl"};
        int[] colors={0xffffbeb1,0xffa6eced,0xffd6baff,0xffff8eb5,0xff8fe0b5,0xffffce70,0xff83bbff,0xffe6edf7};
        wheel("Accent colour",names,colors,n->{accent=colors[n];getPreferences(0).edit().putInt("accent",accent).apply();home();});
    }
    void settings() {
        wheel("Glass Launcher",new String[]{"Set as home screen","Accent colour","Tinted glass: "+(tintedGlass?"On":"Off"),"Bedside clock"},null,which->{
            if(which==0){
                RoleManager rm=getSystemService(RoleManager.class);
                if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_HOME)&&!rm.isRoleHeld(RoleManager.ROLE_HOME))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),10);
                else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            }else if(which==1)accents();
            else if(which==2){tintedGlass=!tintedGlass;getPreferences(0).edit().putBoolean("tintedGlass",tintedGlass).apply();home();}
            else wheel("Dim clock · keeps screen awake",new String[]{"Open bedside clock","Return home"},null,n->{if(n==0)bedside();});
        });
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
