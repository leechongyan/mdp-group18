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
import android.widget.Switch;
import android.widget.Toast;

public class MapCanvasView extends View {

    final String TAG = "MapCanvasView";

    Paint mapPaintSpace, mapPaintWall, mapPaintUnknown, mapPaintWP;

    public static int[][] map;
    public static int[] robotPos;
    public static int[] wpPos;

    int robotDirection;
    public static final int DIRECTION_FRONT = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_BACK = 2;
    public static final int DIRECTION_LEFT = 3;
    public static final int DIRECTION_DEFAULT = DIRECTION_RIGHT;

    public static final int NUMBER_OF_UNIT_ON_Y = 20;
    public static final int NUMBER_OF_UNIT_ON_X = 15;

    Bitmap robotBitmap;
    Drawable robotDrawable;

    int unitEdge;

    public static final int ACTION_ROTATE_LEFT = -1;
    public static final int ACTION_ROTATE_RIGHT = 1;

    // 0: unexplored
    // 1: explored
    // 2: blank
    // 3: obstacle
    // state will not be in 1
    public static final int UNEXPLORED = 0;
    public static final int BLANK = 2;
    public static final int OBSTACLE = 3;
    public static final int WP = 4;

    public boolean mapTouchable = false;

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
//                map[i][j] = (int) (3 * Math.random());
                map[i][j] = UNEXPLORED;
            }
        }

        //init wp
        wpPos = new int[2];
        wpPos[0] = -1;
        wpPos[1] = -1;

        mapPaintSpace = new Paint();
        mapPaintSpace.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintSpace.setColor(Color.BLUE);
        mapPaintSpace.setStrokeWidth(4);

        mapPaintWall = new Paint();
        mapPaintWall.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintWall.setColor(Color.RED);
        mapPaintWall.setStrokeWidth(4);

        mapPaintUnknown = new Paint();
        mapPaintUnknown.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintUnknown.setColor(Color.GRAY);
        mapPaintUnknown.setStrokeWidth(4);

        mapPaintWP = new Paint();
        mapPaintWP.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintWP.setColor(Color.GREEN);
        mapPaintWP.setStrokeWidth(4);

    }

    private void initRobot(){
        robotPos = new int[2];
        robotPos[0] = 0;
        robotPos[1] = NUMBER_OF_UNIT_ON_Y - 3;

        robotDrawable = getResources().getDrawable(R.drawable.ic_forward_black_24dp, null);
        robotBitmap = getRobotBitmapFromDrawable(robotDrawable);
        robotDirection = DIRECTION_FRONT;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mapTouchable){
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                int[] unit = getUnitCoordinateFromXY(event.getX(), event.getY());
//                Toast.makeText(getContext(), "x: " + unit[0] + " y: " + unit[1], Toast.LENGTH_SHORT).show();
                onUnitSelect(unit);
            }
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
        if(map[unit[0]][unit[1]] == BLANK || map[unit[0]][unit[1]] == UNEXPLORED){
            map[unit[0]][unit[1]] = WP;
            if(wpPos[0] != -1){
                map[wpPos[0]][wpPos[1]] = UNEXPLORED;
            }
            wpPos[0] = unit[0];
            wpPos[1] = unit[1];
        }
        else if(map[unit[0]][unit[1]] == WP){
            map[wpPos[0]][wpPos[1]] = UNEXPLORED;
            wpPos[0] = -1;
            wpPos[1] = -1;
        }
        reDraw();
    }

    private void drawMap(Canvas canvas){
        int offset = 5;
        Paint unitPaint;
        unitEdge = getMeasuredWidth() / NUMBER_OF_UNIT_ON_X;

        Log.d(TAG, "drawMap: unit edge = " + unitEdge + " measuredWidth = " + getMeasuredWidth());

        for(int i=0; i<NUMBER_OF_UNIT_ON_X; i++){
            for(int j=0; j<NUMBER_OF_UNIT_ON_Y; j++){
                switch (map[i][j]){
                    case OBSTACLE:
                        unitPaint = mapPaintWall;
                        break;
                    case BLANK:
                        unitPaint = mapPaintSpace;
                        break;
                    case WP:
                        unitPaint = mapPaintWP;
                        break;
                    default:
                        unitPaint = mapPaintUnknown;
                        break;
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
        Log.d(TAG, "drawRobot: unitEdge = " + unitEdge + " robotBitmapWidth: " + robotBitmap.getWidth());
        matrix.postRotate(rotation, robotBitmap.getScaledWidth(canvas)/2f, robotBitmap.getScaledHeight(canvas)/2f);
        matrix.postScale(3f * unitEdge / robotBitmap.getWidth(), 3f * unitEdge / robotBitmap.getHeight());
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

    public void setRobotPos(int x, int y){
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

    public void setRobotDirection(int direction){
        robotDirection = direction;
    }

    public void setMap(int[][] mapMatrix){
        map = mapMatrix;
    }

    public void update(){
        reDraw();
    }

    public void setMapTouchable(boolean touchable){
        this.mapTouchable = touchable;
    }
}
