package ru.thelv.lib;

public class V
{
    public static float[] s(float[] a, float[] b)
    {
        float[] r={a[0]+b[0], a[1]+b[1], a[2]+b[2]};
        return r;
    }

    public static float[] o(float[] a)
    {
        float[] r={-a[0], -a[1], -a[2]};
        return r;
    }

    public static float[] ps(float k, float[] a)
    {
        float[] r={k*a[0], k*a[1], k*a[2]};
        return r;
    }

    public static float absSquare(float[] a)
    {
        return a[0]*a[0]+a[1]*a[1]+a[2]*a[2];
    }

    public static float abs(float[] a)
    {
        return (float)Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
    }
}
