package com.esurvey.esurvey_sdk.utils

import com.blankj.utilcode.util.ToastUtils
import com.esurvey.esurvey_sdk_demo.utils.HttpClientUtils
import com.esurvey.esurvey_sdk_demo.utils.HttpClientUtils.OnRequestCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


/**
 * @author: Jokerliang
 * @Desc:
 * @create: 2024-12-26 15:46
 **/
object Api {

    private const val BASE_URL = "https://www.sobds.com"



    //=============================在这里填写你们自己申请的SDK===========================================//
    const val key = ""
    const val secret = ""
    //========================================================================//




    var appToken = ""
    var sdkToken = ""

    private var sdkTokenExpireTime = 0L;

    /**
     * 这个方法应该放到服务端去请求，为了方便，demo 直接写在了应用内，不应该在APP里面暴露secret
     * AppToken 有2小时有效期，App 应该做好缓存，避免频繁请求，2小时过期后，应该重新请求
     */
    suspend fun getAppToken(onError : (String) -> Unit) {
        withContext(Dispatchers.IO) {
            val jsonRequest =  JSONObject();
            try {
                jsonRequest.put("key", key)
                jsonRequest.put("secret", secret)

            } catch (e: Exception) {
            }

            HttpClientUtils.post("$BASE_URL/Api/Yc/getToken", jsonRequest.toString(), null, object: OnRequestCallBack {
                override fun onSuccess(json: String) {
                    val jsonObject = JSONObject(json)
                    val code = jsonObject.optInt("code")
                    if (code == 200) {
                        val data = jsonObject.optJSONObject("data")
                        val token = data!!.optString("token")
                        ToastUtils.showLong("获取应用Token成功")
                        appToken = token
                    } else {
                        ToastUtils.showShort("获取应用Token失败，无法连接caster ${jsonObject.optString("msg")}")
                        onError("获取应用Token失败，无法连接caster ${jsonObject.optString("msg")}")
                    }
                }
                override fun onError(errorMsg: String?) {
                    ToastUtils.showShort("获取应用Token失败，无法连接caster-${errorMsg}")
                    onError(errorMsg ?: "")
                }
            });
        }
    }


    /**
     * 先在服务端获取AppToken, 然后拿AppToken兑换SDKToken
     */
    suspend fun getSdkToken(userId: String, callback: (flag: Boolean) -> Unit) {
        val currentSeconds = System.currentTimeMillis() / 1000;
        if (currentSeconds < sdkTokenExpireTime) {
            // sdk暂未过去无需重新获取
            callback(true)
            return
        }

        withContext(Dispatchers.IO) {
            val jsonRequest =  JSONObject();
            try {
                jsonRequest.put("userId", userId)
            } catch (e: Exception) {
            }
            HttpClientUtils.post("$BASE_URL/Api/Yc/getSdkToken", jsonRequest.toString(), appToken,  object: OnRequestCallBack {
                override fun onSuccess(json: String) {
                    val jsonObject = JSONObject(json)
                    val code = jsonObject.optInt("code")
                    if (code == 200) {
                        val data = jsonObject.optJSONObject("data")
                        val appToken = data!!.optString("token")
                        val expireTime = data.optLong("expireTime")
                        sdkTokenExpireTime = System.currentTimeMillis() / 1000 + expireTime
                        sdkToken = appToken
                        ToastUtils.showLong("获取SDKToken成功")
                        callback(true)
                    } else {
                        ToastUtils.showShort("获取SDKToken失败，无法连接caster ${jsonObject.optString("msg")}")
                        callback(false)
                    }
                }

                override fun onError(errorMsg: String?) {
                    ToastUtils.showShort("获取SDKToken失败，无法连接caster-${errorMsg}")
                    callback(false)
                }

            });
        }
    }
}