package com.app.mediafly.common

import android.os.CountDownTimer
import android.widget.TextView

class MediaCountDownTimer(private var millisInFuture: Long, countDownInterval: Long, private var listener: ICompleteMediaTimerListener? = null): CountDownTimer(millisInFuture, countDownInterval) {

    override fun onTick(millisUntilFinished: Long) {
        val time = millisInFuture - millisUntilFinished
    }

    override fun onFinish() {
        listener!!.onCompleteMediaTimer("CompleteTimer")
    }

    interface ICompleteMediaTimerListener {
        fun onCompleteMediaTimer(action: String)
    }
}