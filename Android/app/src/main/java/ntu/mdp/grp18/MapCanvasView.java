package ntu.mdp.grp18;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class MapCanvasView extends View {

    final String TAG = "MapCanvasView";

    Paint mapPaintDefault, mapPaintWall;

    int[][] map;
    int[] robotPos;

    int robotDirection;
    final int DIRECTION_FRONT = 0;
    final int DIRECTION_RIGHT = 1;
    final int DIRECTION_BACK = 2;
    final int DIRECTION_LEFT = 3;
    final int DIRECTION_DEFAULT = DIRECTION_RIGHT;

    final int NUMBER_OF_UNIT_ON_Y = 20;
    final int NUMBER_OF_UNIT_ON_X = 15;

    Bitmap robotBitmap;
    Drawable robotDrawable;

    int unitEdge;

    public static final int ACTION_ROTATE_LEFT = -1;
    public static final int ACTION_ROTATE_RIGHT = 1;

    public MapCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initMap();

        initRobot();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawMap(canvas);
        drawRobot(canvas);
    }

    private void initMap(){
        map = new int[NUMBER_OF_UNIT_ON_X][NUMBER_OF_UNIT_ON_Y];

        for(int i=0; i<NUMBER_OF_UNIT_ON_X; i++){
            for(int j=0; j<NUMBER_OF_UNIT_ON_Y; j++){
                map[i][j] = (int) (3 * Math.random());
            }
        }

        mapPaintDefault = new Paint();
        mapPaintDefault.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintDefault.setColor(Color.BLUE);
        mapPaintDefault.setStrokeWidth(4);

        mapPaintWall = new Paint();
        mapPaintWall.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintWall.setColor(Color.RED);
        mapPaintWall.setStrokeWidth(4);
    }

    private void initRobot(){
        robotPos = new int[2];

        robotDrawable = getResources().getDrawable(R.drawable.ic_forward_black_24dp, null);
        robotBitmap = getRobotBitmapFromDrawable(robotDrawable);
        robotDirection = DIRECTION_FRONT;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            int[] unit = getUnitCoordinateFromXY(event.getX(), event.getY());
//            Toast.makeText(getContext(), "x: " + unit[0] + " y: " + unit[1], Toast.LENGTH_SHORT).show();
            onUnitSelect(unit);
        }
        return super.onTouchEvent(event);
    }

    private int[] getUnitCoordinateFromXY(float x, float y){
        int unitX, unitY;
        unitX = (int)((x - getX()) / (getMeasuredWidth() - getX()) * NUMBER_OF_UNIT_ON_X);
        unitY = (int)((y - getY()) / (getMeasuredHeight() - getY()) * NUMBER_OF_UNIT_ON_Y);
        return new int[]{unitX, unitY};
    }

    private void onUnitSelect(int[] unit){
        if(map[unit[0]][unit[1]] != 1){
            map[unit[0]][unit[1]] = 1;
        }
        else{
            map[unit[0]][unit[1]] = 0;
        }

        reDraw();
    }

    private void drawMap(Canvas canvas){
        int offset = 5;
        Paint unitPaint;
        unitEdge = getMeasuredWidth() / NUMBER_OF_UNIT_ON_X;

        for(int i=0; i<NUMBER_OF_UNIT_ON_X; i++){
            for(int j=0; j<NUMBER_OF_UNIT_ON_Y; j++){
                if(map[i][j] == 1){
                    unitPaint = mapPaintWall;
                }
                else{
                    unitPaint = mapPaintDefault;
                }
                int x = i * unitEdge + offset;
                int y = j * unitEdge + offset;
                canvas.drawRect(x, y, x + unitEdge - 2 * offset, y + unitEdge - 2 * offset, unitPaint);
            }
        }
    }

    private void drawRobot(Canvas canvas){
        Matrix matrix = new Matrix();
        int rotation = (robotDirection - DIRECTION_DEFAULT) * 90;
        matrix.postRotate(rotation, robotBitmap.getWidth()/2, robotBitmap.getHeight()/2);
        matrix.postScale(3*unitEdge / robotBitmap.getWidth(), 3*unitEdge / robotBitmap.getHeight());
        matrix.postTranslate(unitEdge * robotPos[0], unitEdge * robotPos[1]);
        canvas.drawBitmap(robotBitmap, matrix, null);
    }

    protected void reDraw() {
        this.invalidate();
    }

    public static Bitmap getRobotBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xddffff00);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void robotMoveForward(){
        switch (robotDirection){
            case DIRECTION_FRONT:
                setRobotPos(robotPos[0], robotPos[1]-1);
                break;
            case DIRECTION_BACK:
                setRobotPos(robotPos[0], robotPos[1]+1);
                break;
            case DIRECTION_LEFT:
                setRobotPos(robotPos[0]-1, robotPos[1]);
                break;
            case DIRECTION_RIGHT:
                setRobotPos(robotPos[0]+1, robotPos[1]);
                break;
        }
        reDraw();
    }

    private void setRobotPos(int x, int y){
        robotPos[0] = x;
        robotPos[1] = y;
        if(robotPos[0] < 0){
            robotPos[0] = 0;
        }
        else if(robotPos[0] > NUMBER_OF_UNIT_ON_X - 3){
            robotPos[0] = NUMBER_OF_UNIT_ON_X - 3;
        }
        if(robotPos[1] < 0){
            robotPos[1] = 0;
        }
        else if(robotPos[1] > NUMBER_OF_UNIT_ON_Y - 3){
            robotPos[1] = NUMBER_OF_UNIT_ON_Y - 3;
        }
//        Log.d(TAG, "setRobotPos: robot coordinate: " + robotPos[0] + ", " + robotPos[1]);
    }

    public void robotRotate(int action){
        robotDirection += action;
        for(;robotDirection < 0;robotDirection+=4);
        robotDirection = robotDirection % 4;
//        Log.d(TAG, "robotRotate: direction: " + robotDirection);
        reDraw();
    }
}
