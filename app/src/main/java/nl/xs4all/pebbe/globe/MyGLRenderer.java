package nl.xs4all.pebbe.globe;

import com.mhuss.AstroLib.DateOps;
import com.mhuss.AstroLib.Latitude;
import com.mhuss.AstroLib.LocationElements;
import com.mhuss.AstroLib.Longitude;
import com.mhuss.AstroLib.ObsInfo;
import com.mhuss.AstroLib.PlanetData;
import com.mhuss.AstroLib.Planets;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private final static String angelHState = "nl.xs4all.pebbe.globe.ANGLEH";
    private final static String angelVState = "nl.xs4all.pebbe.globe.ANGLEV";
    private final static String zoomState = "nl.xs4all.pebbe.globe.ZOOM";
    private final static String saveState = "nl.xs4all.pebbe.globe.STATESAVED";

    private Globe globe;
    private Context mContext;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mProjectionMatrixZoom = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float mAngleH = 0;
    private float mAngleV = 0;
    private boolean mZoom = false;
    private boolean mStateSaved = false;

    public void setContext(Context context) {
        mContext = context;
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mAngleH = savedInstanceState.getFloat(angelHState, mAngleH);
            mAngleV = savedInstanceState.getFloat(angelVState, mAngleV);
            mZoom = savedInstanceState.getBoolean(zoomState, mZoom);
            mStateSaved = savedInstanceState.getBoolean(saveState, false);
        }
    }

    public void saveInstanceState(Bundle outState) {
        outState.putFloat(angelHState, mAngleH);
        outState.putFloat(angelVState, mAngleV);
        outState.putBoolean(zoomState, mZoom);
        outState.putBoolean(saveState, true);
    }

    public void zoom() {
        mZoom = !mZoom;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig eglConfig) {
        // Set the background frame color
        //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        float c = 0.6f;
        GLES20.glClearColor(c * 0.27f, c * 0.35f, c * 0.39f, 1.0f);

        // enable face culling feature
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // nodig als objecten niet convex zijn
        //GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        float longitude = 0;
        float latitude = 0;

        Calendar cal = new GregorianCalendar();
        Date trialTime = new Date();
        cal.setTime(trialTime);

        ObsInfo oi = new ObsInfo(new Latitude(0), new Longitude(0), 0);
        double jd = DateOps.calendarToDoubleDay(cal);
        PlanetData pd = new PlanetData(Planets.SUN, jd, oi);
        LocationElements le;
        try {
            longitude = (float) -pd.hourAngle();
            latitude = (float) pd.getDeclination();
        } catch (Exception e) {
            Log.i("MYTAG", e.toString());
        }
        globe = new Globe(mContext);
        globe.Sun(longitude, latitude);

        if (!mStateSaved) {
            mAngleH = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000) / 4;
            mAngleV = 0;
            mZoom = false;
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)

        float h = mAngleH / 180.0f * (float) PI;
        float v = mAngleV / 180.0f * (float) PI;

        Matrix.setLookAtM(mViewMatrix, 0,
                100 * (float) (sin(h) * cos(v)), 100 * (float) sin(v), 100 * (float) (cos(h) * cos(v)),
                //(float)(sin(h)*cos(v)), (float)sin(v), (float)(cos(h)*cos(v)),
                0, 0, 0,
                0, 1, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0,
                mZoom ? mProjectionMatrix : mProjectionMatrixZoom,
                0, mViewMatrix, 0);

        globe.draw(mMVPMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = ((float) width) / (float) height;

        float xmul;
        float ymul;
        if (ratio > 1.0f) {
            xmul = 1.01f * ratio;
            ymul = 1.01f;
        } else {
            xmul = 1.01f;
            ymul = 1.01f / ratio;
        }

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -xmul, xmul, -ymul, ymul, 98, 102);
        Matrix.frustumM(mProjectionMatrixZoom, 0, 0.65f * -xmul, 0.65f * xmul, 0.65f * -ymul, 0.65f * ymul, 98, 102);
    }

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        checkGlError("glShaderSource");
        GLES20.glCompileShader(shader);
        checkGlError("glCompileShader");

        return shader;
    }

    public float getAngleH() {
        return mAngleH;
    }

    public float getAngleV() {
        return mAngleV;
    }

    public void setAngleH(float angle) {
        mAngleH = angle;
    }

    public void setAngleV(float angle) {
        mAngleV = angle;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("MyGLRenderer", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}
