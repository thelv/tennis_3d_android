package org.tennis2d.d3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;

import ru.thelv.lib.V;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.TextView;

import java.net.InetAddress;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.all).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Rotation.reset();
            }
        });

        Vars.sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);

        ((TextView)findViewById(R.id.text)).setText(getLocalWifiIpAddress());
        final WebSocketServer server=(new SimpleServer(new InetSocketAddress((int)41789)));
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                server.run();
            }
        }).start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
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

                    server.broadcast(r.toString());
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
        send();
        //System.out.println("received message from "+conn.getRemoteSocketAddress()+": "+message);
    }

    private void send()
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
            r.put(1);

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

        WebSocketServer server=new SimpleServer(new InetSocketAddress(host, port));
        server.run();
    }
}

class Rotation implements SensorEventListener
{
    public boolean isAvailable;
    public static float[] n_={0, 0, 1};
    public static float[] n={0, 0, 1};
    public static float[] n0={0, 1};

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

    public static void reset()
    {
        float k=(float)Math.sqrt(1-n_[2]*n_[2]);
        n0[0]=n_[0]/k;
        n0[1]=n_[1]/k;
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

        float[] p={0, 1, 0};
        float[] p_=
                {
                        m[0]*p[0]+m[1]*p[1]+m[2]*p[2],
                        m[4]*p[0]+m[5]*p[1]+m[6]*p[2],
                        m[8]*p[0]+m[9]*p[1]+m[10]*p[2]
                };


        //this.n[0]=-n_[0]*n0[0]-n_[1]*n0[1];
        //this.n[1]=n_[0]*n0[1]-n_[1]*n0[0];
        if(n_[2]==1)
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
        }
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

    public Accelerometer()
    {
        Vars.sensorManager.registerListener
                (
                        this,
                        Vars.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_FASTEST
                );
    }

    public void stop()
    {
        Vars.sensorManager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event)
    {
        float[] a={event.values[0], event.values[1], event.values[2], 0};
        float[] m=Vars.rotationMatrix;
        float[] a_=
        {
                m[0]*a[0]+m[1]*a[1]+m[2]*a[2],
                m[4]*a[0]+m[5]*a[1]+m[6]*a[2],
                m[8]*a[0]+m[9]*a[1]+m[10]*a[2]
        };
        a_[2]-=9.81;
        float l=V.absSquare(a_);
        if(l<3.6)
        {
            a_=V.ps((float)-2, v);
        }
        long t_=new Date().getTime();
        v=V.s(v, V.ps((float)(t_-t)/1000, a_));
        t=t_;


        float[] n=Rotation.n_;
        float k=(float)Math.sqrt(1-n[2]*n[2]);
        float[] nxy={n[0]/k, n[1]/k};

        float[] v_=new float[2];
        v_[0]=-v[0]*nxy[0]-v[1]*nxy[1];
        v_[1]=v[0]*nxy[1]-v[1]*nxy[0];

        this.v_[0]=v_[0];
        this.v_[1]=v_[1];
        this.v_[2]=v[2];


/*        n=Rotation.n;
        k=(float)Math.sqrt(1-n[2]*n[2]);
        nxy[0]=n[0]/k; nxy[1]=n[1]/k;

        this.v_[0]=v_[0]*nxy[0]-v_[1]*nxy[1];
        this.v_[1]=v_[0]*nxy[1]+v_[1]*nxy[0];

        this.v_[2]=v[2];*/
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //
    }
}