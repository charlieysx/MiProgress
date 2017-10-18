package cb.miprogress;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.Transformation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * description:
 * <p>
 * Created by CodeBear on 2017/10/17.
 */

public class MiProgress extends View {

    private class Circle {
        float x;
        float y;
        float r;

        public Circle(float x, float y, float r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }
    }

    /**
     * 默认宽度
     */
    private static final int DEFAULT_MI_WIDTH = 800;
    /**
     * 最小宽度
     */
    private static final int MIN_MI_WIDTH = 300;

    /**
     * 屏幕宽度
     */
    private int screenWidth;
    /**
     * 屏幕密度
     */
    private float screenScale;

    /**
     * 计算之后得出的宽度
     */
    private int miWidth;
    /**
     * 空心圆的个数
     */
    private int circleCount = 40;
    /**
     * 粒子个数
     */
    private int pointCount = 40;
    /**
     * 完成后外围旋转的圆的个数
     */
    private int completeSweepGradientCount = 4;

    /**
     * 存储连接时的空心圆的信息
     */
    private Circle[] circles;

    /**
     * 存储粒子信息
     */
    private PointF[] points;

    /**
     * 计算点是否在path中时用到
     */
    private Region region;

    /**
     * 画笔
     */
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 存储连接时的空心圆的渐变色
     */
    private SweepGradient[] firstSweepGradients;
    /**
     * 存储完成时圆的渐变色
     */
    private SweepGradient completeSweepGradient;
    /**
     * 存储完成时外围圆的渐变色
     */
    private SweepGradient[] completeSweepGradients;
    /**
     * 存储完成时外围圆的数据
     */
    private RectF[] completeRectFs;
    /**
     * 内部圆为虚线模式
     */
    private DashPathEffect progressEffect;
    /**
     * 绘制进度用到的
     */
    private RectF progressRectF;

    /**
     * 旋转角度
     */
    private int loadingAngle = 0;
    /**
     * 进度旋转角度
     */
    private int progressAngle = 0;

    /**
     * 旋转动画
     */
    private ValueAnimator loadingAnimator;
    /**
     * 获取粒子的动画
     */
    private ValueAnimator getPointsAnimator;
    /**
     * 显示进度的动画
     */
    private ValueAnimator progressAnimator;

    /**
     * 是否连接完成
     */
    private boolean loadingComplete = true;

    /**
     * 获取随机数
     */
    private Random random = new Random();

    public MiProgress(Context context) {
        this(context, null);
    }

    public MiProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!isInEditMode()) {
            screenWidth = getScreenWidth(context);
        }


        // 获取手机像素密度 （即dp与px的比例）
        screenScale = getContext().getResources().getDisplayMetrics().density;

        initAttrs(context, attrs);
        init();
        loadingComplete(0);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MiProgress);

        miWidth = array.getDimensionPixelOffset(R.styleable.MiProgress_miWidth, DEFAULT_MI_WIDTH);

        array.recycle();
    }

    private void init() {
        firstSweepGradients = new SweepGradient[circleCount];

        for (int i = 0; i < circleCount; ++i) {
            int startAlpha = random.nextInt(5);
            int endAlpha = random.nextInt(206) + 50;
            firstSweepGradients[i] = new SweepGradient(
                    miWidth / 2,
                    miWidth / 2,
                    Color.argb(startAlpha, 255, 255, 255),
                    Color.argb(endAlpha, 255, 255, 255));
        }

        circles = new Circle[circleCount];
        getCircles();

        points = new PointF[pointCount];
        for (int i = 0; i < pointCount; ++i) {
            points[i] = new PointF();
        }

        float cx = miWidth - 138;
        float cy = miWidth / 2;

        Path pointPath = new Path();
        pointPath.moveTo(cx, cy + 5);
        pointPath.rLineTo(-130, -200);
        pointPath.rLineTo(150, 0);
        pointPath.close();

        RectF rect = new RectF();
        region = new Region();
        pointPath.computeBounds(rect, true);
        region.setPath(pointPath, new Region((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom));
        getPoints();

        completeSweepGradient = new SweepGradient(miWidth / 2, miWidth / 2,
                new int[]{Color.argb(0, 255, 255, 255), Color.argb(255, 255, 255, 255), Color.argb(0, 255, 255, 255)}, null);

        completeSweepGradients = new SweepGradient[completeSweepGradientCount];
        completeRectFs = new RectF[completeSweepGradientCount];

        for (int i = 0; i < completeSweepGradientCount; ++i) {
            completeSweepGradients[i] = new SweepGradient(
                    miWidth / 2, miWidth / 2,
                    new int[]{Color.argb(0, 255, 255, 255), Color.argb(0, 255, 255, 255),
                            Color.argb(100 - i * 15, 255, 255, 255), Color.argb(0, 255, 255, 255),
                            Color.argb(0, 255, 255, 255)}, null);

            completeRectFs[i] = new RectF(
                    miWidth / 2 - (miWidth - 180) / 2 - 10 * i,
                    miWidth / 2 - (miWidth - 180) / 2,
                    miWidth / 2 + (miWidth - 180) / 2 + 10 * i,
                    miWidth / 2 + (miWidth - 180) / 2);
        }

        progressEffect = new DashPathEffect(new float[]{3, 5}, 2);
        progressRectF = new RectF(
                miWidth / 2 - ((miWidth - 180) / 2 - 80),
                miWidth / 2 - ((miWidth - 180) / 2 - 80),
                miWidth / 2 + (miWidth - 180) / 2 - 80,
                miWidth / 2 + (miWidth - 180) / 2 - 80);
    }

    private void initLoadingAnimator() {
        loadingAnimator = ValueAnimator.ofInt(0, 360);
        loadingAnimator.setInterpolator(new LinearInterpolator());
        loadingAnimator.setRepeatMode(ValueAnimator.RESTART);
        loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingAnimator.setDuration(2000L);
        loadingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                loadingAngle = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
    }

    private void initGetPointsAnimator() {
        getPointsAnimator = ValueAnimator.ofInt(1);
        getPointsAnimator.setInterpolator(new LinearInterpolator());
        getPointsAnimator.setRepeatMode(ValueAnimator.RESTART);
        getPointsAnimator.setRepeatCount(ValueAnimator.INFINITE);
        getPointsAnimator.setDuration(50L);
        getPointsAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                getPoints();
            }
        });
    }

    private void initProgressAnimator() {
        progressAnimator = ValueAnimator.ofInt(0, 0);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.setDuration(800L);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progressAngle = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
    }

    private void stopLoading() {
        if (loadingAnimator == null) {
            initLoadingAnimator();
        }
        if (getPointsAnimator == null) {
            initGetPointsAnimator();
        }
        if (progressAnimator == null) {
            initProgressAnimator();
        }
        loadingAnimator.cancel();
        getPointsAnimator.cancel();
        progressAnimator.cancel();
    }

    /**
     * 开始连接动画显示
     */
    public void startLoading() {
        loadingComplete = false;
        stopLoading();
        if (loadingAnimator == null) {
            initLoadingAnimator();
        }
        if (!loadingAnimator.isRunning()) {
            loadingAnimator.setDuration(2000L).start();
        }
        if (getPointsAnimator == null) {
            initGetPointsAnimator();
        }
        if (!getPointsAnimator.isRunning()) {
            getPointsAnimator.start();
        }
    }

    /**
     * 完成连接动画显示
     * @param progress 进度
     */
    public void loadingComplete(int progress) {
        loadingComplete = true;
        animate().scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animate().scaleX(1)
                                .scaleY(1)
                                .setDuration(130)
                                .setListener(null)
                                .start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();

        stopLoading();
        progress = Math.max(progress, 0);
        progress = Math.min(progress, 360);
        if (progressAnimator == null) {
            initProgressAnimator();
        }
        progressAnimator.setIntValues(0, progress);
        progressAnimator.start();
        loadingAnimator.setDuration(8000L).start();
    }

    /**
     * 记录绕X轴旋转的角度
     */
    private float degrees = 0;

    /**
     * 绕X轴旋转
     * @param toDegrees 旋转角度
     */
    public void rotateX(float toDegrees) {

        degrees = toDegrees;

        startAnimation(new Animation() {

            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                setFillAfter(true);
                setInterpolator(new LinearInterpolator());

                Camera camera = new Camera();
                Matrix matrix = t.getMatrix();
                matrix.reset();
                camera.save();

                // 绕x轴旋转
                camera.rotateX(degrees);

                camera.getMatrix(matrix);
                camera.restore();

                // 修正失真，主要修改 MPERSP_0 和 MPERSP_1
                float[] mValues = new float[9];
                //获取数值
                matrix.getValues(mValues);
                //数值修正
                mValues[6] = mValues[6] / screenScale;
                mValues[7] = mValues[7] / screenScale;
                //重新赋值
                matrix.setValues(mValues);
                // 调节中心点
                matrix.preTranslate(-miWidth / 2, -miWidth);
                matrix.postTranslate(miWidth / 2, miWidth);
            }
        });
    }

    private void getCircles() {
        for (int i = 0; i < circleCount; ++i) {
            int dir[] = {random.nextBoolean() ? 1 : -1, random.nextBoolean() ? 1 : -1};
            int rand[] = {random.nextInt(16), random.nextInt(16)};
            int randR = random.nextInt(12);

            int flag = 0;
            if (i > circleCount / 3 * 2) {
                flag = 1;
            }

            float x = miWidth / 2 + rand[0] * dir[0] * flag;
            float y = miWidth / 2 + rand[1] * dir[1] * flag;
            float r = (miWidth - 250) / 2 - randR - rand[0] - rand[1];

            circles[i] = new Circle(x, y, r);
        }

        Arrays.sort(circles, new Comparator<Circle>() {
            @Override
            public int compare(Circle o1, Circle o2) {
                return (int) (o1.r - o2.r);
            }
        });
    }

    private void getPoints() {
        int num;
        for (int i = 0; i < pointCount; ++i) {
            num = 50;
            while (num > 0) {
                num--;
                float rand[] = {(miWidth - 112) + random.nextInt(150) * (random.nextBoolean() ? 1 : -1),
                        miWidth / 2 - 100 + random.nextInt(105) * (random.nextBoolean() ? 1 : -1)};
                if (region.contains((int) rand[0], (int) rand[1])) {
                    points[i].x = rand[0];
                    points[i].y = rand[1];
                    break;
                }
            }
        }

        Arrays.sort(points, new Comparator<PointF>() {
            @Override
            public int compare(PointF o1, PointF o2) {
                return (int) (o2.y - o1.y);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.rotate(loadingAngle, miWidth / 2, miWidth / 2);
        if (!loadingComplete) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);

            for (int i = 0; i < circleCount; ++i) {
                canvas.save();
                canvas.rotate(i * -0.5f, miWidth / 2, miWidth / 2);

                mPaint.setShader(firstSweepGradients[i]);
                canvas.drawCircle(circles[i].x, circles[i].y, circles[i].r, mPaint);

                canvas.rotate(i * 0.5f, miWidth / 2, miWidth / 2);
                canvas.restore();
            }

            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < pointCount; ++i) {
                mPaint.setColor(Color.argb(255 - i * (230 / pointCount), 255, 255, 255));
                canvas.drawCircle(points[i].x, points[i].y, 8 - i * (5.0f / pointCount), mPaint);
            }
        } else {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.argb(150, 255, 255, 255));
            mPaint.setStrokeWidth(40);
            canvas.drawCircle(miWidth / 2, miWidth / 2, (miWidth - 180) / 2, mPaint);

            mPaint.setColor(Color.WHITE);
            mPaint.setShader(completeSweepGradient);
            canvas.drawCircle(miWidth / 2, miWidth / 2, (miWidth - 180) / 2, mPaint);

            mPaint.setStrokeWidth(40);
            mPaint.setColor(Color.WHITE);
            for (int i = 1; i < completeSweepGradientCount; ++i) {
                mPaint.setShader(completeSweepGradients[i]);
                canvas.drawOval(completeRectFs[i], mPaint);
            }
        }

        canvas.rotate(-loadingAngle, miWidth / 2, miWidth / 2);
        canvas.restore();

        if (loadingComplete) {
            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(8);
            mPaint.setColor(Color.WHITE);
            mPaint.setPathEffect(progressEffect);
            canvas.drawCircle(miWidth / 2, miWidth / 2, (miWidth - 180) / 2 - 80, mPaint);


            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.WHITE);
            mPaint.setStrokeWidth(10);
            canvas.drawArc(
                    progressRectF,
                    -90f,
                    progressAngle,
                    false,
                    mPaint);

            canvas.save();
            canvas.rotate(progressAngle, miWidth / 2, miWidth / 2);

            mPaint.reset();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(miWidth / 2, miWidth / 2 - ((miWidth - 180) / 2 - 80), 12, mPaint);
            canvas.rotate(-progressAngle, miWidth / 2, miWidth / 2);
            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
        init();
    }

    private int measureWidth(int measureSpec) {
        int result = miWidth;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        miWidth = Math.max(result, MIN_MI_WIDTH);
        if (!isInEditMode()) {
            miWidth = Math.min(miWidth, screenWidth);
        }
        result = miWidth;
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = miWidth;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        miWidth = Math.max(result, MIN_MI_WIDTH);
        if (!isInEditMode()) {
            miWidth = Math.min(miWidth, screenWidth);
        }
        result = miWidth;
        return result;
    }

    /**
     * 获取屏幕的宽度（单位：px）
     *
     * @param context 上下文
     *
     * @return 屏幕宽px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(dm);// 给白纸设置宽高
        return dm.widthPixels;
    }
}
