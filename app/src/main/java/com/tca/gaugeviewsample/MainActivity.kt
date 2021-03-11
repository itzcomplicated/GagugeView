package com.tca.gaugeviewsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.tca.gaugeview.GaugeView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gaugeView:GaugeView = findViewById(R.id.gaugeView)
        val button20:Button = findViewById(R.id.button20)
        val button50:Button = findViewById(R.id.button50)
        val button80:Button = findViewById(R.id.button80)

        button20.setOnClickListener{gaugeView.setCurrenrValue(20F)}
        button50.setOnClickListener{gaugeView.setCurrenrValue(50F)}
        button80.setOnClickListener{gaugeView.setCurrenrValue(80F)}

    }
}
