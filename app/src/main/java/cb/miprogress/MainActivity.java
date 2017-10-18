package cb.miprogress;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private MiProgress miProgress;
    private EditText etProgress;
    private MyScrollView scrollView;
    private View flView;
    private View rlMess;
    private TextView tvStep;

    private boolean isConn = false;

    private int height = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = (MyScrollView) findViewById(R.id.scrollView);
        flView = findViewById(R.id.fl_view);
        rlMess = findViewById(R.id.rl_mess);
        tvStep = (TextView) findViewById(R.id.tv_step);
        etProgress = (EditText) findViewById(R.id.et_progress);
        miProgress = (MiProgress) findViewById(R.id.miProgress);

        flView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(height != flView.getHeight()) {
                    height = flView.getHeight();
                }
            }
        });

        findViewById(R.id.tv_conn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConn) {
                    isConn = true;
                    miProgress.startLoading();
                }
            }
        });

        findViewById(R.id.tv_complete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConn) {
                    isConn = false;
                    int progress = Integer.parseInt("0" + etProgress.getText().toString());
                    miProgress.loadingComplete(progress);
                }
            }
        });

        scrollView.setOnScrollListener(new MyScrollView.OnScrollListener() {
            @Override
            public void onScroll(int scrollX, int scrollY) {
                if(height > 0) {
                    miProgress.rotateX(90.0f / (height - 160) * scrollY);
                    rlMess.setAlpha(1.0f - 1.0f / (tvStep.getTop()) * scrollY);
                }
            }
        });
    }
}
