package org.tennis2d.d3;


import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;


import java.net.InetSocketAddress;
import java.util.Date;

import androidx.constraintlayout.widget.ConstraintLayout;

public class Pedals
{
    public static int level, throttle, break_;
    public static float levelPercent;
    public static byte buttons;
    public static byte[] buttonsCancelQueue=new byte[8];
    public static byte gearsDelay=6;
    public static int buttonsChangeCounter=0;

    private static Pedal pedalRight, pedalLeft;
    private static ConstraintLayout pedalsCombinedLayout;
    public static int scale=20;
    public static float mmInPx;

    private static int[] pointerIdToPedal=new int[20];
    private static Pedal[] pedals=new Pedal[20];

    private static int[] actionPrefClick=new int[2];
    private static int[][] actionsPref=new int[2][8];
    private static int clickSwipeLimit;
    private static int actionPrefVolumeUp, actionPrefVolumeDown, actionPrefBack;
    private static int clickTimeout;
    private static boolean resumed=false;




    public static void init()
    {
        //
    }


    public static void resume(final ConstraintLayout pedalsCombinedLayout)
    {
        if(resumed)
        {
            Pedals.pause();
        }

        resumed=true;
        Pedals.pedalsCombinedLayout=pedalsCombinedLayout;
        //Pedals.levelChanged=levelChanged;
        mmInPx=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, MainActivity.o.getResources().getDisplayMetrics());
        scale=20;

        clickSwipeLimit=500;
        clickTimeout=0;

        actionPrefClick[0]=2;

        actionsPref[0][0]=0;
        actionsPref[0][1]=0;
        actionsPref[0][2]=-2;
        actionsPref[0][3]=0;
        actionsPref[0][4]=0;
        actionsPref[0][5]=0;
        actionsPref[0][6]=-1;
        actionsPref[0][7]=0;

        actionPrefClick[1]=1;
        actionsPref[1]=new int[8];
        actionsPref[1][0]=0;
        actionsPref[1][1]=0;
        actionsPref[1][2]=-2;
        actionsPref[1][3]=0;
        actionsPref[1][4]=0;
        actionsPref[1][5]=0;
        actionsPref[1][6]=-1;
        actionsPref[1][7]=0;

        actionPrefVolumeUp=0;
        actionPrefVolumeDown=0;
        actionPrefBack=0;

        final PedalsCombined pc=new PedalsCombined(pedalsCombinedLayout.getWidth());
        pedalsCombinedLayout.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                pc.handle(event);
                return false;
            }
        });
    }

    public static boolean buttonBack()
    {
        /*if(actionPrefBack!=0)
        {
            Pedals.buttons=(byte)(Pedals.buttons | (1 << (actionPrefBack-1)));
            int i=((Connection.packetId+Pedals.gearsDelay) & 15) >> 1;
            Pedals.buttonsChangeCounter++;
            Pedals.buttonsCancelQueue[i]=(byte)(Pedals.buttonsCancelQueue[i] | (1 << (actionPrefBack-1)));
            send();
            //Pedal.vibrator.vibrate(100);//50
            return true;
        }*/

        return false;
    }

    public static boolean buttonDown(int keyCode)
    {
        if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if(actionPrefVolumeDown!=0)
            {
                Pedals.buttons=(byte)(Pedals.buttons | (1 << (actionPrefVolumeDown-1)));
                send();
                return true;
            }
        }
        else if(keyCode==KeyEvent.KEYCODE_VOLUME_UP)
        {
            if(actionPrefVolumeUp!=0)
            {
                Pedals.buttons=(byte)(Pedals.buttons | (1 << (actionPrefVolumeUp-1)));
                send();
                return true;
            }
        }

        return false;
    }

    public static boolean buttonUp(int keyCode)
    {
        if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if(actionPrefVolumeDown!=0)
            {
                Pedals.buttons=(byte)(Pedals.buttons & (254 << (actionPrefVolumeDown-1)));
                send();
                return true;
            }
        }
        else if(keyCode==KeyEvent.KEYCODE_VOLUME_UP)
        {
            if(actionPrefVolumeUp!=0)
            {
                Pedals.buttons=(byte)(Pedals.buttons & (254 << (actionPrefVolumeUp-1)));
                send();
                return true;
            }
        }

        return false;
    }

    private static class PedalsCombined
    {
        private int layoutWidthHalf;
        private boolean upped=false;

        public PedalsCombined(int layoutWidth)
        {
            this.layoutWidthHalf=layoutWidth/2;
        }

        public void handle(MotionEvent e)
        {
            int action=e.getAction();
            int actionIndex=e.getActionIndex();
            int actionId=e.getPointerId(actionIndex);
            //Log.d("dddddddd",Integer.toString(action)+' '+Integer.toString(actionId)+e.getX());
            if(action==MotionEvent.ACTION_MOVE)
            {
                if(upped) return;
                Log.d("asdf", Integer.toString(level));
                int pointerCount=e.getPointerCount();
                for(int i=0; i!=pointerCount; i++)
                {
                    Pedal pedal=pedals[e.getPointerId(i)];
                    pedal.pointerIndex=i;
                    pedal.onTouch(null, e);
                }
            }
            else if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN || action==MotionEvent.ACTION_POINTER_2_DOWN)
            {
                upped=false;
                byte side=e.getX(actionIndex)>layoutWidthHalf ? (byte)0 : (byte)1;
                pedals[actionId]=new Pedal(null, scale, actionsPref[side], actionPrefClick[side], clickSwipeLimit, clickTimeout, side);
                pedals[actionId].pointerIndex=actionIndex;
                pedals[actionId].onTouch(null, e);

            }
            else if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_2_UP || action==MotionEvent.ACTION_POINTER_UP)
            {
                upped=true;
                pedals[actionId].pointerIndex=actionIndex;
                pedals[actionId].onTouch(null, e);
            }
        }
    }

	/*public static void pauseMain()
	{
		if(pedalsCombinedLayout!=null)
		{
			pedalsCombinedLayout.setHandler(null);
			pedalsCombinedLayout=null;
		}
	}*/

    public static void pause()
    {
        resumed=false;
        if(pedalsCombinedLayout!=null)
        {
     //       pedalsCombinedLayout.setHandler(null);
            pedalsCombinedLayout=null;
        }
        if(pedalLeft!=null)
        {
            pedalLeft.stop();
            pedalLeft=null;
            pedalRight.stop();
            pedalRight=null;
        }
    }

    public static void calc()
    {
        if(pedalLeft.throttle==0) levelPercent=1/mmInPx/scale*pedalRight.throttle; else
        if(pedalRight.throttle==0) levelPercent=1/mmInPx/scale*pedalLeft.throttle; else
            levelPercent=1/mmInPx/scale*Math.min(pedalLeft.throttle, pedalRight.throttle)
                    ;
        level=Math.max(-16384, Math.min(16384, Math.round(16384*levelPercent)));
        throttle=(level>0 ? level : 0);
        break_=(level>0 ? 0 : level);
     //   levelChanged.drawNeeded();

        send();
    }

    public static void calcPedalsCombined(float throttleInPx)
    {
        levelPercent=1/mmInPx/scale*throttleInPx;
        level=Math.max(-16384, Math.min(16384, Math.round(16384*levelPercent)));
        throttle=(level>0 ? level : 0);
        break_=(level>0 ? 0 : level);
      //  levelChanged.drawNeeded();

        send();
    }

    static void send()
    {
        //Connection.sendUnlock();
        try
        {
           /* synchronized(Connection.thread.runnable.lock)
            {
                Connection.thread.runnable.lock.notify();
                Connection.sendTimePedal=(int)System.currentTimeMillis();
            }*/
        }
        catch(Exception e)
        {
            //
        }
    }
}

class Pedal implements OnTouchListener
{
    public float throttle=0;
    public float level=0;
    //public static Vibrator vibrator;

    private float startX=0;
    private float startY=0;
    private float startDX;
    private float startDY;
    private int action;
    private float startLevel=0;
    private boolean directionDeterminated=false;
    private int[] actions;
    private int actionPrefClick;
    private View view;
    byte side;
    private int clickSwipeLimitPx=0, clickSwipeLimitPxSquare=0;
    private int clickTimeout;
    private int startTime;
    public int pointerIndex=0;

    public Pedal(View view, int scale, int[] actionsPref, int actionPrefClick, int clickSwipeLimit, int clickTimeout, byte side)
    {
        this.side=side;
        this.view=view;
        if(actionPrefClick!=0)
        {
            this.clickSwipeLimitPx=(int)(((float)clickSwipeLimit/(float)1000)*Pedals.mmInPx);
            this.clickSwipeLimitPxSquare=clickSwipeLimitPx*clickSwipeLimitPx;
        }
        this.clickTimeout=clickTimeout;
        this.actionPrefClick=actionPrefClick;
        this.actions=new int[17];
        for(int i=1;i<=16;i++)
        {
            int min=100;
            int minSide=-1;
            for(int j=0; j<=7; j++)
            {
                if(actionsPref[j]!=0)
                {
                    int m=Math.abs(j*4-(i*2-1));
                    if(m>16) m=32-m;
                    if(m<min)
                    {
                        min=m;
                        minSide=j;
                    }
                }
            }

            if(minSide!=-1)
            {
                this.actions[i-1]=actionsPref[minSide];
            }
            else
            {
                this.actions[i-1]=0;
            }
        }

        if(view!=null)
        {
            view.setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent e)
    {
        int eAction=e.getAction();
        //if(eAction!=MotionEvent.ACTION_MOVE) Log.d("actionnnn "+Byte.toString(side), Integer.toString(eAction));
        if(eAction==MotionEvent.ACTION_MOVE)
        {
            if(!directionDeterminated)
            {
                startDX=e.getX(pointerIndex)-startX;
                startDY=startY-e.getY(pointerIndex);
                if(startDX*startDX+startDY*startDY>clickSwipeLimitPxSquare)
                {
                    directionDeterminated=true;
                    double angle=-(Math.atan2(startDY, startDX)-Math.PI/2);
                    if(angle<0) angle+=2*Math.PI;
                    angle=angle/Math.PI*8;
                    action=actions[(int) angle];
                    if(action>0)
                    {
                        Pedals.buttons=(byte)(Pedals.buttons | (1 << (action-1)));
                    }
                }
                else
                {
                    return true;
                }
                startLevel=-clickSwipeLimitPx;
            }

            if(action<0)
            {

                float dLevel
                        =((int) Math.signum(
                        (e.getX(pointerIndex)-startX)*startDX+(startY-e.getY(pointerIndex))*startDY
                ))
                        *(float) Math.sqrt
                        (
                                (float) Math.pow(e.getX(pointerIndex)-startX, 2)+
                                        (float) Math.pow(startY-e.getY(pointerIndex), 2)
                        );
                if(dLevel<0 && 1<1/Pedals.mmInPx/Pedals.scale*level)
                {
                    if(dLevel>-Pedals.mmInPx) return true;
                    startLevel=Pedals.mmInPx*Pedals.scale;
                }
                level=startLevel+dLevel;
                //if(level<0) level=0;
                startX=e.getX(pointerIndex);
                startY=e.getY(pointerIndex);
                startLevel=level;
            }

        }
        else if(eAction==MotionEvent.ACTION_DOWN || eAction==MotionEvent.ACTION_POINTER_DOWN || eAction==MotionEvent.ACTION_POINTER_2_DOWN)
        {
            startTime=(int) new Date().getTime();
            startX=e.getX(pointerIndex);
            startY=e.getY(pointerIndex);
            startDX=0;
            startDY=0;
            startLevel=0;
            directionDeterminated=false;
            throttle=0;
        }
        else if((eAction==MotionEvent.ACTION_UP) || (eAction==MotionEvent.ACTION_CANCEL))
        {
			/*if(action<0)
			{
				throttle=0;
				level=0;
			}*/
            throttle=0;
            level=0;
            if(action>0)
            {
                Pedals.buttons=(byte)(Pedals.buttons & (254 << (action-1)));
            }
            else if(! directionDeterminated && actionPrefClick!=0 && (int)new Date().getTime()-startTime<clickTimeout)
            {
                Pedals.buttons=(byte)(Pedals.buttons | (1 << (actionPrefClick-1)));
                //int i=((Connection.packetId+Pedals.gearsDelay) & 15) >> 1;
                //Pedals.buttonsChangeCounter++;
                //Pedals.buttonsCancelQueue[i]=(byte)(Pedals.buttonsCancelQueue[i] | (1 << (actionPrefClick-1)));
				/*if(Pedal.vibrator!=null)
				{
					/-new UISync(null)
					{
						@Override
						public void run()
						{
							Pedal.vibrator.vibrate(100);//50
						}
					};-/
					Pedal.vibrator.vibrate(100);//50
				}*/
            }
        }

        if(action==-1)
        {
            throttle=level;
            if(view==null) Pedals.calcPedalsCombined(throttle);
            else Pedals.calc();
        }
        else if(action==-2)
        {
            throttle=-level;
            if(view==null) Pedals.calcPedalsCombined(throttle);
            else Pedals.calc();
        }

        return true;
    }

    public void stop()
    {
        if(view!=null)
        {
            view.setOnTouchListener(null);
        }
    }
}