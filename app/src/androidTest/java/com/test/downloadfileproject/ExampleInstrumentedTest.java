package com.test.downloadfileproject;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.test.downloadfileproject", appContext.getPackageName());
    }
    @Test
    public void useAppCdontext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.test.downloadfileproject", appContext.getPackageName());
        Map<String, String> userInfo = new HashMap<>();
        String json = "{\"level\":\"10\",\"attribute\":\"11,22,33,44\",\"equipment\":\"1001,1002,1003,1004,null\",\"spbeing\":\"null,3002,3003,null,3005,null\"}";
        try {
            JSONObject jsonObject = new JSONObject(json);
            String string2 = jsonObject.optString("level");
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                String string = jsonObject.optString(next);
                userInfo.put(next, string);
            }
            System.out.println(userInfo.size());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
