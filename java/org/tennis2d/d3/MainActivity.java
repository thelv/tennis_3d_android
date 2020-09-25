package org.tennis2d.d3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;

import androidx.constraintlayout.widget.ConstraintLayout;
import ru.thelv.lib.V;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.net.wifi.WifiManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import ru.thelv.lib.Preferences;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity o;
    public Pedals pedals;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        o=this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Pedals.resume((ConstraintLayout) findViewById(R.id.all));

        if(! Vars.inited)
        {
            Vars.inited=true;

            Preferences.init(this);

            Vars.vibrator=(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            findViewById(R.id.all).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    //Rotation.reset(true);
                }
            });

            findViewById(R.id.calibrate).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Accelerometer.calibrate();
                }
            });


            Vars.sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);

            ((TextView) findViewById(R.id.text)).setText(getLocalWifiIpAddress());
            final SimpleServer server=(new SimpleServer(new InetSocketAddress((int) 41789)));
            Vars.server=server;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    server.run();
                }
            }).start();

            Timer timer=new Timer();
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if(!Vars.connected) return;
                        ;
                       /* JSONArray o=new JSONArray();
                        o.put(Rotation.n[0]);
                        o.put(Rotation.n[1]);
                        o.put(Rotation.n[2]);

                        JSONArray p=new JSONArray();
                        p.put(Accelerometer.v_[0]);
                        p.put(Accelerometer.v_[1]);
                        p.put(Accelerometer.v_[2]);

                        JSONArray r=new JSONArray();
                        r.put(o);
                        r.put(p);
                        r.

                        server.broadcast(r.toString());*/
                        server.send();
                    }
                    catch(Exception e)
                    {
                        //
                    }
                }
            }, 0, 20);

            new Rotation();
            new Accelerometer();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN || keyCode==KeyEvent.KEYCODE_VOLUME_UP)
        {
            Rotation.reset(true);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private String getLocalWifiIpAddress()
    {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }

        return ipAddressString;
    }
}

class Vars
{
    public static SensorManager sensorManager;
    public static float[] a=new float[3];
    public static float[] orientationAngles=new float[3];
    public static float[] rotationMatrix=new float[16];
    public static boolean connected=false;
    public static Vibrator vibrator;
    public static WebSocketServer server;
    public static boolean inited=false;
}

class SimpleServer extends WebSocketServer
{
    public SimpleServer(InetSocketAddress address)
    {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake)
    {
       // conn.send("Welcome to the server!"); //This method sends a message to the new client
       // broadcast("new connection: "+handshake.getResourceDescriptor()); //This method sends a message to all clients connected
        Vars.connected=true;
        System.out.println("new connection to "+conn.getRemoteSocketAddress());
        send();

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote)
    {
        System.out.println("closed "+conn.getRemoteSocketAddress()+" with exit code "+code+" additional info: "+reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message)
    {
        //send();
        if(message.equals("echo"))
        {
            JSONArray o=new JSONArray();
            o.put("echo");
            o.put(new Date().getTime());
            broadcast(o.toString());
        }
        else if(message.startsWith("hit"))
        {
            int force=Integer.parseInt(message.substring(3));
            Accelerometer.rotationReseted=false;
// Vibrate for 500 milliseconds
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
            {
                Vars.vibrator.vibrate(VibrationEffect.createOneShot(force, 1));
            }
            else
            {
                //deprecated in API 26
                Vars.vibrator.vibrate(force);
            }
        }
        //System.out.println("received message from "+conn.getRemoteSocketAddress()+": "+message);
    }

    public void send()
    {
        try
        {
            if(! Vars.connected) return;;

            JSONArray o=new JSONArray();
            o.put(Rotation.n[0]);
            o.put(Rotation.n[1]);
            o.put(Rotation.n[2]);

            JSONArray p=new JSONArray();
            p.put(Accelerometer.v_[0]);
            p.put(Accelerometer.v_[1]);
            p.put(Accelerometer.v_[2]);

            JSONArray r=new JSONArray();
            r.put(o);
            r.put(p);
            r.put(Pedals.level);
            r.put(new Date().getTime());

            broadcast(r.toString());
        }
        catch(Exception e)
        {
            //
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message)
    {
        System.out.println("received ByteBuffer from "+conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex)
    {
      //  System.err.println("an error occurred on connection "+conn.getRemoteSocketAddress()+":"+ex);
    }

    @Override
    public void onStart()
    {
        System.out.println("server started successfully");
    }


    public static void main(String[] args)
    {
        String host="localhost";
        int port=8887;

        //SimpleServer server=new SimpleServer(new InetSocketAddress(host, port));
        //server.run();
    }
}

class Rotation implements SensorEventListener
{
    public boolean isAvailable;
    public static float[] n_={0, 0, 1};
    public static float[] n={0, 0, 1};
    public static float[] n0={0, 1};
    public static boolean resetFirst=true;

    public Rotation()
    {
        Sensor sensor=Vars.sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if(sensor==null)
        {
            isAvailable=false;
            return;
        }

        isAvailable=true;
        Vars.sensorManager.registerListener
                (
                        this,
                        sensor,
                        SensorManager.SENSOR_DELAY_FASTEST
                );
    }

    public void stop()
    {
        Vars.sensorManager.unregisterListener(this);
    }

    public static void reset(boolean touch)
    {
        float k=(float)Math.sqrt(1-n_[2]*n_[2]);
        float[] n0_=new float[2];
        n0_[0]=-n_[0]/k;
        n0_[1]=-n_[1]/k;

        if(resetFirst || touch)
        {
            n0_[0]=-n0_[0];
            n0_[1]=-n0_[1];
            resetFirst=false;
        }
        else if(n0[0]*n0_[0]+n0[1]*n0_[1]<0)
        {
            n0_[0]=-n0_[0];
            n0_[1]=-n0_[1];
        }

        n0[0]=n0_[0];
        n0[1]=n0_[1];

        Accelerometer.v[0]=0;
        Accelerometer.v[1]=0;
        Accelerometer.v[2]=0;
    }
    public void onSensorChanged(SensorEvent event)
    {
        float[] rotationMatrix=new float[16];
        SensorManager.getRotationMatrixFromVector(Vars.rotationMatrix, event.values);
        float[] n={0, 0, 1};
        float[] m=Vars.rotationMatrix;
        float[] n_=
        {
                m[0]*n[0]+m[1]*n[1]+m[2]*n[2],
                m[4]*n[0]+m[5]*n[1]+m[6]*n[2],
                m[8]*n[0]+m[9]*n[1]+m[10]*n[2]
        };
        this.n_=n_;

        float[] p={-1, 0, 0};
        float[] p_=
                {
                        m[0]*p[0]+m[1]*p[1]+m[2]*p[2],
                        m[4]*p[0]+m[5]*p[1]+m[6]*p[2],
                        m[8]*p[0]+m[9]*p[1]+m[10]*p[2]
                };


        this.n[0]=-n_[0]*n0[0]-n_[1]*n0[1];
        this.n[1]=n_[0]*n0[1]-n_[1]*n0[0];
       /* if(n_[2]==1)
        {
            this.n[1]=0;
            this.n[0]=1;
        }
        else
        {
   //         this.n[1]=p[2]/(float) Math.sqrt(1-n_[2]*n_[2]);
     //       this.n[0]=(float) Math.sqrt(1-this.n[1]*this.n[1]);
            this.n[1]=p_[2];
            this.n[0]=(float)Math.sqrt(1-this.n[1]*this.n[1]-n_[2]*n_[2]);
        }*/
        this.n[2]=n_[2];
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //
    }
}



class Accelerometer implements SensorEventListener
{
    public long t=new Date().getTime();
    public static float[] v={0, 0, 0}, v_={0, 0, 0};

    private float[] calibrationK={1, 1, 1};
    private float[] calibrationOffset={0, 0, 0};
    private static boolean calibrating=false;
    private static int calibratingI=0;
    private static float calibratingSum=0;

    private static int STATE_REST=0;
    private static int STATE_ACTIVE=1;

    private static int state=Accelerometer.STATE_REST;
    private static long startRestTime=0;

    public static boolean rotationReseted=false;

    public Accelerometer()
    {
        calibartionInit();
        Vars.sensorManager.registerListener
                (
                        this,
                        Vars.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_FASTEST
                );
    }

    private void calibartionInit()
    {
        for(int i=0; i<=2; i++)
        {
            float calibration1=Preferences.getFloat("accelerometer_g_"+Integer.toString(i)+"0", 0);
            float calibration0=Preferences.getFloat("accelerometer_g_"+Integer.toString(i)+"1", 0);
            if(calibration0!=0 && calibration1!=0)
            {
                calibrationOffset[i]=-(float)((calibration1+calibration0)/2.0);
                calibrationK[i]=(float)(9.81*2.0/(calibration1-calibration0));
            }
        }
    }

    public static void calibrate()
    {
        calibrating=true;
        calibratingI=0;
        calibratingSum=0;
    }

    public void stop()
    {
        Vars.sensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event)
    {
        float[] a__=event.values;
        float[] a={(event.values[0]+calibrationOffset[0])*calibrationK[0], (event.values[1]+calibrationOffset[1])*calibrationK[1], (event.values[2]+calibrationOffset[2])*calibrationK[2], 0};
        float[] m=Vars.rotationMatrix;
        float[] a_=
        {
                m[0]*a[0]+m[1]*a[1]+m[2]*a[2],
                m[4]*a[0]+m[5]*a[1]+m[6]*a[2],
                m[8]*a[0]+m[9]*a[1]+m[10]*a[2]
        };
        a_[2]-=9.81;
        float l=V.absSquare(a_);
        if(l<1.5)
        {
            long t=new Date().getTime();
            if(state==Accelerometer.STATE_REST || startRestTime!=0 && t-startRestTime>300)
            {
                state=Accelerometer.STATE_REST;
                v[0]=0;
                v[1]=0;
                v[2]=0;
                if(! Accelerometer.rotationReseted)
                {
                    Rotation.reset(false);
                    Vars.server.broadcast("reset");
                    Vars.vibrator.vibrate(50);
                    Accelerometer.rotationReseted=true;
                }

            }
            else if(startRestTime==0)
            {
                startRestTime=t;
            }
          //  a_=V.ps((float)-2, v);
        }
        else
        {
            state=Accelerometer.STATE_ACTIVE;
         //   Accelerometer.rotationReseted=false;
            startRestTime=0;
        }
        long t_=new Date().getTime();
        v=V.s(v, V.ps((float)(t_-t)/1000, a_));
        t=t_;


        float[] n=Rotation.n0;
      //  float k=(float)Math.sqrt(1-n[2]*n[2]);
       // float[] nxy={n[0], n[1]};

        float[] v_=new float[2];
        v_[0]=-v[0]*n[0]-v[1]*n[1];
        v_[1]=v[0]*n[1]-v[1]*n[0];

        this.v_[0]=v_[0];
        this.v_[1]=v_[1];
        this.v_[2]=v[2];


/*        n=Rotation.n;
        k=(float)Math.sqrt(1-n[2]*n[2]);
        nxy[0]=n[0]/k; nxy[1]=n[1]/k;

        this.v_[0]=v_[0]*nxy[0]-v_[1]*nxy[1];
        this.v_[1]=v_[0]*nxy[1]+v_[1]*nxy[0];

        this.v_[2]=v[2];*/

        if(calibrating)
        {
            if(calibratingI>100)
            {
                int calibratingSide=(Math.abs(a__[0])>Math.abs(1) ? 0 : Math.abs(a__[1])>Math.abs(a__[2]) ? 1 : 2);
                int calibratingSide_=a__[calibratingSide]>0 ? 0 : 1;
                Preferences.setFloat("accelerometer_g_"+Integer.toString(calibratingSide)+Integer.toString(calibratingSide_), calibratingSum/calibratingI);
                calibartionInit();
                calibrating=false;
            }
            else
            {
                calibratingI++;
                float aMax=Math.max(Math.max(a__[0], a__[1]), a__[2]);
                float aMin=Math.min(Math.min(a__[0], a__[1]), a__[2]);
                calibratingSum+=aMax>-aMin ? aMax : aMin;
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //
    }
}