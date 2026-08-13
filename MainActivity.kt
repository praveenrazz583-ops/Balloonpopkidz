package com.example.balloonpopkids

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.*
import android.content.Context
import android.widget.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.*
import kotlin.random.Random

class MainActivity : Activity() {
    private var rewarded: RewardedAd? = null
    private lateinit var game: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        MobileAds.initialize(this) {}
        loadRewarded()

        val root = FrameLayout(this)
        game = GameView(this)
        root.addView(game, FrameLayout.LayoutParams(-1,-1))

        val rewardBtn = Button(this).apply {
            text = "🎁 Watch Ad = +1 Life"
            setOnClickListener { showRewarded() }
        }
        val lp = FrameLayout.LayoutParams(-1,60)
        lp.gravity = Gravity.BOTTOM
        root.addView(rewardBtn, lp)
        setContentView(root)
    }

    private fun loadRewarded() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,
            "ca-app-pub-6393455596255684/6053116454",
            adRequest,
            object: RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
            })
    }

    private fun showRewarded() {
        val ad = rewarded ?: run { Toast.makeText(this,"Ad अभी उपलब्ध नहीं है",Toast.LENGTH_SHORT).show(); loadRewarded(); return }
        ad.show(this) { game.addLife() }
        rewarded = null
        loadRewarded()
    }
}

class GameView(context: Context): View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val bs=mutableListOf<B>()
    private var score=0; private var lives=3; private var level=1; private var over=false; private var last=0L
    private val pref=context.getSharedPreferences("game",0)
    private var high=pref.getInt("high",0)
    data class B(var x:Float,var y:Float,var r:Float,var col:Int,var speed:Float)

    override fun onDraw(c:Canvas){
        val w=width.toFloat(); val h=height.toFloat()
        p.shader=LinearGradient(0f,0f,0f,h,Color.rgb(120,210,255),Color.rgb(245,190,255),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h,p); p.shader=null
        p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.DEFAULT_BOLD; p.color=Color.DKGRAY
        p.textSize=36f; c.drawText("🎈 Balloon Pop Kids",w/2,50f,p)
        p.textSize=25f; c.drawText("Score: $score   ❤️ $lives   Level: $level",w/2,90f,p)
        if(!over){
            if(System.currentTimeMillis()-last>maxOf(280L,900L-level*45L)){
                bs.add(B(Random.nextFloat()*w,h+70,35+Random.nextFloat()*12,
                    intArrayOf(Color.RED,Color.YELLOW,Color.GREEN,Color.BLUE,Color.MAGENTA,Color.CYAN).random(),2.5f+level*.35f))
                last=System.currentTimeMillis()
            }
            val it=bs.iterator()
            while(it.hasNext()){ val b=it.next(); b.y-=b.speed; drawB(c,b); if(b.y < -80){it.remove(); lives--; if(lives<=0)over=true} }
            postInvalidateDelayed(16)
        } else {
            p.color=Color.WHITE;p.textSize=48f;c.drawText("Game Over!",w/2,h/2-40,p)
            p.textSize=28f;c.drawText("Score: $score   Best: $high",w/2,h/2+5,p)
            p.color=Color.YELLOW;c.drawRoundRect(w/2-140,h/2+35,w/2+140,h/2+105,25f,25f,p)
            p.color=Color.DKGRAY;p.textSize=28f;c.drawText("TAP TO PLAY",w/2,h/2+80,p)
        }
    }
    private fun drawB(c:Canvas,b:B){p.color=b.col;c.drawCircle(b.x,b.y,b.r,p);p.color=Color.WHITE;p.alpha=160;c.drawCircle(b.x-b.r*.3f,b.y-b.r*.3f,b.r*.18f,p);p.alpha=255;p.color=Color.DKGRAY;p.strokeWidth=3f;c.drawLine(b.x,b.y+b.r,b.x,b.y+b.r+55,p)}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action!=MotionEvent.ACTION_DOWN)return true
        if(over){score=0;lives=3;level=1;over=false;last=0;invalidate();return true}
        val hit=bs.find{(e.x-it.x)*(e.x-it.x)+(e.y-it.y)*(e.y-it.y)<=it.r*it.r}
        if(hit!=null){bs.remove(hit);score+=10;if(score>high){high=score;pref.edit().putInt("high",high).apply()};level=1+score/100}
        return true
    }
    fun addLife(){lives=minOf(5,lives+1);over=false;invalidate()}
}
