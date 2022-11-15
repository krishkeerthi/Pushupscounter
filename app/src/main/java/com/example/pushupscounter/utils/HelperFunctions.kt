package com.example.pushupscounter.utils

import android.content.ContentValues.TAG
import android.util.Log

var targetCount = 0
var descending = false

fun getRepCount(classificationString: String): String{
    return if(classificationString.contains("rep")){
        var count = classificationString.filter { it.isDigit() }
        count
    }
    else{
        ""
    }
}

interface TextToSpeechInterface{
    fun textToSpeech(message: String)
}

fun getCountBasedOnSelection(count: Int): Int{ //, textToSpeech: TextToSpeechInterface

        return if(targetCount == 0){
            count
        }
        else if(!descending){
//            if(count == targetCount){
//               // playSound()
//                textToSpeech.textToSpeech("Congratulations")
//                Log.d(TAG, "getCountBasedOnSelection: congratulations")
//            }
            count
        }
        else{
//            if(count == (targetCount + 1)){
//                // playSound()
//                textToSpeech.textToSpeech("Congratulations")
//            }
            (targetCount - count + 1)
        }
}

fun isTargetReached(count: Int): Boolean{
    return if(targetCount == 0){
        false
    }
    else if(!descending){
        count == targetCount
    }
    else{
        count == 0
    }
}

