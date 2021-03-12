package com.example.photo_sender_stable.models

import android.util.Log
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import okio.Buffer
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.lang.Exception

const val MTYPE_MESSAGE = 1
const val MTYPE_OFFER = 2
const val MTYPE_ANSWER = 3
const val MTYPE_CAMERA_LIST = 4
const val MTYPE_NEW_ICE_CANDIDATE = 5
const val MTYPE_SNAP = 6

@JsonClass(generateAdapter = true)
data class Messasge(
    val type: Int,
    val body: JSONObject?
)

internal object JSONObjectAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): JSONObject? {
        return (reader.readJsonValue() as? Map<*, *>)?.let { data ->
            try {
                JSONObject(data)
            } catch (exc: JSONException) {
                Log.d("JSON", "err:", exc)
                JSONObject()
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: JSONObject?) {
        value?.let {
            writer.value(Buffer().writeUtf8(value.toString()))
        }
    }
}



internal object ICECandidateAdapter {
    @ToJson
    fun toJson(value: IceCandidate?): String {
        if (value == null) {
            return ""
        }
        return value.toString()
    }

    @FromJson
    fun fromJson(candidate: JSONObject): IceCandidate {
        return IceCandidate(
            candidate.getString("sdpMid"),
            candidate.getInt("sdpMLineIndex"),
            candidate.getString("candidate")
        )
    }
}


