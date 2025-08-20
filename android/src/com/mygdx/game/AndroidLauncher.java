package com.mygdx.game;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.gdx.game.MyGdxGame;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.numSamples = 2; // 2x MSAA для Android
		config.useAccelerometer = false; // Отключаем акселерометр для экономии батареи
		config.useCompass = false; // Отключаем компас
		config.useWakelock = true; // Предотвращаем засыпание экрана
		config.useImmersiveMode = true; // Полноэкранный режим
		initialize(new MyGdxGame(), config);
	}
}
