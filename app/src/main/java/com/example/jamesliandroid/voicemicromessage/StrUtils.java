package com.example.jamesliandroid.voicemicromessage;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrUtils {
    public static String patternStr(String content) {
        Pattern p = Pattern.compile("[0-9]{1,3}(,[0-9]{3})*(.[0-9]{1,2})");
        Matcher m = p.matcher(content);

        List<String> rels = new ArrayList<String>();
        while (m.find()) {
            Log.d("TAG", "......." + m.group(0).trim());
            rels.add(m.group());
        }
        Log.d("TAG", "......" + rels.toString());
        return rels.get(0);
    }
}
