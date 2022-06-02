package com.app.mediafly.common

import android.os.CountDownTimer
import android.widget.TextView

class NewsCountDownTimer(private var millisInFuture: Long, countDownInterval: Long, private var listener: ICompleteTimerListener? = null): CountDownTimer(millisInFuture, countDownInterval) {

    var tvTimer: TextView? = null
    override fun onTick(millisUntilFinished: Long) {
        val time = millisInFuture - millisUntilFinished
    }

    override fun onFinish() {
        listener!!.onCompleteTimer("CompleteTimer")
    }

    interface ICompleteTimerListener {
        fun onCompleteTimer(action: String)
    }
}