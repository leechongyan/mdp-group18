package ntu.mdp.grp18;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import java.lang.reflect.Field;

import ntu.mdp.grp18.fragments.ControlFragment;

public class MapCanvasView extends View {

    final String TAG = "MapCanvasView";

    Paint mapPaintSpace, mapPaintWall, mapPaintUnknown, mapPaintWP, mapPaintStartingZone;

    public int[][] map;
    public int[] robotPos;
    public int[] wpPos;
    public int[][] imagePos;

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
    Bitmap[] imageBitmaps;

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
    public static final int STARTING_ZONE = 5;
    public static final int GOAL_ZONE = 6;

    public boolean mapTouchable = false;
    int touchMode = ControlFragment.MODE_DEFAULT;

    final int offset = 5;

    public MapCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initMap();
        initWp();
        initRobot();
        initImage();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawMap(canvas);
        drawWp(canvas);
        drawImage(canvas);
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

//        //test
//        map[4][15] = OBSTACLE;

        mapPaintSpace = new Paint();
        mapPaintSpace.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintSpace.setColor(0xffcccccc);
        mapPaintSpace.setStrokeWidth(4);

        mapPaintWall = new Paint();
        mapPaintWall.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintWall.setColor(Color.RED);
        mapPaintWall.setStrokeWidth(4);

        mapPaintUnknown = new Paint();
        mapPaintUnknown.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintUnknown.setColor(Color.GRAY);
        mapPaintUnknown.setStrokeWidth(4);

        mapPaintStartingZone = new Paint();
        mapPaintStartingZone.setStyle(Paint.Style.FILL_AND_STROKE);
        mapPaintStartingZone.setColor(Color.BLUE);
        mapPaintStartingZone.setStrokeWidth(4);
    }

    private void initWp(){
        //init wp
        wpPos = new int[2];
        setWpPos(-1, -1);

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

    private void initImage(){
        imagePos = new int[15][2];
        for(int i=0; i<15; i++){
            setImagePos(i, -1, -1);
        }

        imageBitmaps = new Bitmap[15];
        for(int i=0; i<15; i++){
            int resId = getResId("img_" + i, R.drawable.class);
            Drawable drawable = getResources().getDrawable(resId, null);
            imageBitmaps[i] = ((BitmapDrawable) drawable).getBitmap();
        }
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
        switch (touchMode){
            case ControlFragment.MODE_SET_WP:
                if(map[unit[0]][unit[1]] == OBSTACLE){
                    break;
                }
                if(unit[0] == wpPos[0] && unit[1] == wpPos[1]){
                    setWpPos(-1, -1);
                }
                else{
                    setWpPos(unit[0], unit[1]);
                }
                break;
            case ControlFragment.MODE_SET_ROBOT:
                setRobotPos(unit[0]-1, unit[1]-1);
                break;
        }
        reDraw();
    }

    private void drawMap(Canvas canvas){
        Paint unitPaint;
        unitEdge = getMeasuredWidth() / NUMBER_OF_UNIT_ON_X;

        Log.d(TAG, "drawMap: unit edge = " + unitEdge + " measuredWidth = " + getMeasuredWidth());

        //set waypoint
        setWpPos(wpPos[0], wpPos[1]);

        //set starting zone and goal zone
        for(int i=0; i<3; i++){
            for(int j=0; j<3; j++){
                map[i][NUMBER_OF_UNIT_ON_Y - 1 - j] = STARTING_ZONE;
                map[NUMBER_OF_UNIT_ON_X - 1 - i][j] = GOAL_ZONE;
            }
        }

        for(int i=0; i<NUMBER_OF_UNIT_ON_X; i++){
            for(int j=0; j<NUMBER_OF_UNIT_ON_Y; j++){
                switch (map[i][j]){
                    case OBSTACLE:
                        unitPaint = mapPaintWall;
                        break;
                    case BLANK:
                        unitPaint = mapPaintSpace;
                        break;
                    case STARTING_ZONE:
                    case GOAL_ZONE:
                        unitPaint = mapPaintStartingZone;
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

    private void drawImage(Canvas canvas){
        for(int i=0; i<imagePos.length; i++){
            if(imagePos[i][0] != -1){
                Matrix matrix = new Matrix();
                matrix.postScale(1f * (unitEdge - 2 * offset) / imageBitmaps[0].getWidth(), 1f * (unitEdge - 2 * offset) / imageBitmaps[0].getHeight());
                matrix.postTranslate(unitEdge * imagePos[i][0] + offset, unitEdge * imagePos[i][1] + offset);
                canvas.drawBitmap(imageBitmaps[i], matrix, null);
            }
        }
    }

    private void drawWp(Canvas canvas){
        int x = wpPos[0] * unitEdge + offset;
        int y = wpPos[1] * unitEdge + offset;
        canvas.drawRect(x, y, x + unitEdge - 2 * offset, y + unitEdge - 2 * offset, mapPaintWP);
    }

    public void reDraw() {
        this.invalidate();
    }

    public static Bitmap getRobotBitmapFromDrawable(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xaaffff00);
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

    public void setWpPos(int x, int y){
        wpPos[0] = x;
        wpPos[1] = y;
    }

    public void setImagePos(int index, int x, int y){
        imagePos[index][0] = x;
        imagePos[index][1] = y;
    }

    public void setImagePos(int[][] imagePos){
        if(imagePos.length == this.imagePos.length && imagePos[0].length == this.imagePos[0].length){
            this.imagePos = imagePos;
        }
    }

    public void setMapTouchable(boolean touchable){
        this.mapTouchable = touchable;
    }

    public void setTouchMode(int mode){
        this.touchMode = mode;
    }

    public void fetchMap(MapCanvasView mapCanvasView){
        if(mapCanvasView != null){
            setMap(mapCanvasView.map);
            setRobotDirection(mapCanvasView.robotDirection);
            setRobotPos(mapCanvasView.robotPos[0], mapCanvasView.robotPos[1]);
            setWpPos(mapCanvasView.wpPos[0], mapCanvasView.wpPos[1]);
            setImagePos(mapCanvasView.imagePos);
        }
    }

    public void clear(){
        clearMap();
        clearImagePos();
        reDraw();
    }

    private void clearMap(){
        for(int i=0; i<NUMBER_OF_UNIT_ON_X; i++){
            for(int j=0; j<NUMBER_OF_UNIT_ON_Y; j++){
                map[i][j] = UNEXPLORED;
            }
        }
    }

    private void clearWpPos(){
        setWpPos(-1, -1);
    }

    private void clearRobotPos(){
        setRobotPos(0, NUMBER_OF_UNIT_ON_Y - 3);
    }

    private void clearImagePos(){
        for(int i=0; i<imagePos.length; i++){
            setImagePos(i, -1, -1);
        }
    }

    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean hasImage(int x, int y){
        for(int i=0; i<15; i++){
            if(imagePos[i][0] == x && imagePos[i][1] == y){
                return true;
            }
        }
        return false;
    }

}
