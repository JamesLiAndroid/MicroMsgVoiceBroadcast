package com.example.jamesliandroid.voicemicromessage;

import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

public class MainActivity extends AppCompatActivity implements SmsDatabaseChangeObserver.HandlerVoiceMessageInterface{

    public static final Uri SMS_MESSAGE_URI = Uri.parse("content://sms");
    private static SmsDatabaseChangeObserver mSmsDBChangeObserver;

    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;


    //缓冲进度
    private int mPercentForBuffering = 0;
    //播放进度
    private int mPercentForPlaying = 0;
    // 默认云端发音人
    public static String voicerCloud="xiaoyan";

    private String mainStr = "";

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      //  registerSmsDatabaseChangeObserver(this);

        final EditText etSmsNo = findViewById(R.id.et_message_num);
        Button btnStart = findViewById(R.id.btn_start);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String smsNo = etSmsNo.getText().toString().trim();
                // 注意如果是手机号码必须添加+86前缀
                registerSmsDatabaseChangeObserver(MainActivity.this, smsNo);
                Toast.makeText(MainActivity.this, "开始等待读取短信！", Toast.LENGTH_SHORT).show();
            }
        });

        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
    }

    @Override
    protected void onDestroy() {
        unregisterSmsDatabaseChangeObserver(this);
        super.onDestroy();
        if( null != mTts ){
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

//    private static void registerSmsDatabaseChangeObserver(ContextWrapper contextWrapper) {
//        //因为，某些机型修改rom导致没有getContentResolver
//        try {
//            mSmsDBChangeObserver = new SmsDatabaseChangeObserver(contextWrapper.getContentResolver(), new Handler());
//            contextWrapper.getContentResolver().registerContentObserver(SMS_MESSAGE_URI, true, mSmsDBChangeObserver);
//        } catch (Throwable b) {
//        }
//    }

    /**
     * 注册读取短信的服务
     * @param contextWrapper
     * @param smsNo
     */
    private void registerSmsDatabaseChangeObserver(ContextWrapper contextWrapper, String smsNo) {
        //因为，某些机型修改rom导致没有getContentResolver
        try {
            mSmsDBChangeObserver = new SmsDatabaseChangeObserver(contextWrapper.getContentResolver(), new Handler(), smsNo);
            mSmsDBChangeObserver.setHandlerVoiceMessageInterface(MainActivity.this);
            contextWrapper.getContentResolver().registerContentObserver(SMS_MESSAGE_URI, true, mSmsDBChangeObserver);
        } catch (Throwable b) {
            b.printStackTrace();
        }
    }

    /**
     * 注销读取短信的服务
     * @param contextWrapper
     */
    private void unregisterSmsDatabaseChangeObserver(ContextWrapper contextWrapper) {
        try {
            contextWrapper.getContentResolver().unregisterContentObserver(mSmsDBChangeObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleVoiceMessage(String content) {
        // 您注册的商户收到客户付款**元，
        // 1. 筛选主要的内容
        mainStr  = content.split("[，|,]")[0];
        Log.d("TAG", "当前获取的语音读取信息为："+mainStr);
        ((TextView) findViewById(R.id.tv_str_msg)).setText(mainStr);
        // 2. 语音播报
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, new InitListener() {
            @Override
            public void onInit(int code) {
                Log.d("TAG", "InitListener init() code = " + code);
                if (code != ErrorCode.SUCCESS) {
                    showTip("初始化失败,错误码："+code);
                } else {
                    // 初始化成功，之后可以调用startSpeaking方法
                    // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                    // 正确的做法是将onCreate中的startSpeaking调用移至这里


                }
            }
        });

        // 设置播报参数
        setParam();
        // 开始语音播报
        int codeStatus = mTts.startSpeaking(mainStr, mTtsListener);
        if( null == mTts ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            Toast.makeText(MainActivity.this, "创建对象失败，请确认 libmsc.so 放置正确，" +
                    "\n 且有调用 createUtility 进行初始化", Toast.LENGTH_SHORT ).show();
            return;
        }
        if (codeStatus != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + codeStatus);
        }
    }

    /**
     * 参数设置
     * @return
     */
    private void setParam(){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME,voicerCloud);
        }
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH,  "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME,  "80");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE,"3");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));

            SpannableStringBuilder style=new SpannableStringBuilder(mainStr);
            Log.e("TAG","beginPos = "+beginPos +"  endPos = "+endPos);
            if(!"henry".equals(voicerCloud)||!"xiaoyan".equals(voicerCloud)||
                    !"xiaoyu".equals(voicerCloud)||!"catherine".equals(voicerCloud))
                endPos++;
            style.setSpan(new BackgroundColorSpan(Color.RED),beginPos,endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) findViewById(R.id.tv_str_msg)).setText(style);
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void showTip(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }
}
