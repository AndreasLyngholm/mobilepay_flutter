package simpleconcept.dk.mobilepay_flutter

import android.app.Activity
import android.content.Context
import dk.mobilepay.sdk.Country
import dk.mobilepay.sdk.MobilePay
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import dk.mobilepay.sdk.model.Payment
import java.math.BigDecimal
import android.content.Intent
import android.util.Log
import dk.mobilepay.sdk.model.FailureResult
import dk.mobilepay.sdk.model.SuccessResult
import dk.mobilepay.sdk.ResultCallback
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class MobilePayFlutterPlugin {
    companion object : FlutterPlugin, MethodCallHandler, ActivityAware {

        private lateinit var channel : MethodChannel

        private lateinit var context: Context
        private lateinit var activity: Activity

        fun getCountry(countryCode: String): Country {
            when {
                countryCode == "fi" -> {
                    return Country.FINLAND
                }
                countryCode == "dk" -> {
                    return Country.DENMARK
                }
                else -> {
                    return Country.DENMARK
                }
            }
        }

        override fun onMethodCall(call: MethodCall, result: Result) {
            when {
                call.method == "initMobilePay" -> {
                    val merchantId = call.argument<String>("merchantId")
                    val country = call.argument<String>("country")
                    MobilePay.getInstance().init(merchantId!!, getCountry(country!!))
                    result.success(null)
                }
                call.method == "setRequestCode" -> {
                    val requestCode = call.argument<Int>("requestCode")

                    this.requestCode = requestCode!!
                    result.success(null)
                }
                call.method == "createPayment" -> {
                    val price = call.argument<Double>("productPrice")
                    val orderId = call.argument<String>("orderId")

                    val payment = Payment()
                    payment.productPrice = BigDecimal(price!!)
                    payment.orderId = orderId

                    val paymentIntent = MobilePay.getInstance().createPaymentIntent(payment)

                    activity.startActivityForResult(paymentIntent, requestCode)
                    result.success(null)
                }
                call.method == "downloadMobilePay" -> {
                    val intent = MobilePay.getInstance().createDownloadMobilePayIntent(context.applicationContext!!)
                    activity.startActivity(intent)
                    result.success(null)
                }
                call.method == "isMobilePayInstalled" -> {
                    val appContext = context.applicationContext!!
                    result.success(MobilePay.getInstance().isMobilePayInstalled(appContext))
                }
                else -> result.notImplemented()
            }
        }

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
            if (requestCode == this.requestCode) {
                MobilePay.getInstance().handleResult(resultCode, data, object : ResultCallback {
                    override fun onSuccess(result: SuccessResult) {
                        val args = mapOf("requestCode" to requestCode,
                                "amountWithdrawnFromCard" to result.amountWithdrawnFromCard.toDouble(),
                                "orderId" to result.orderId,
                                "signature" to result.signature,
                                "transactionId" to result.transactionId)
                        channel.invokeMethod("mobilePaySuccess", args)
                    }

                    override fun onFailure(result: FailureResult) {
                        val args = mapOf("requestCode" to requestCode,
                                "orderId" to result.orderId,
                                "errorCode" to result.errorCode,
                                "errorMessage" to result.errorMessage
                        )
                        channel.invokeMethod("mobilePayFailure", args)
                    }

                    override fun onCancel(orderId: String) {
                        val args = mapOf("requestCode" to requestCode, "orderId" to orderId)
                        channel.invokeMethod("mobilePayCancel", args)
                    }
                })
            }
            return true
        }

        var requestCode: Int = 1337

        override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
            Log.d("***MsalFlutter***", "onAttachedToEngine")
            channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "mobilepay_flutter")
            channel.setMethodCallHandler(this);
            context = flutterPluginBinding.applicationContext
        }

        override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
            TODO("Not yet implemented")
        }

        override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
            activity = activityPluginBinding.activity;
        }

        override fun onDetachedFromActivityForConfigChanges() {
            TODO("Not yet implemented")
        }

        override fun onReattachedToActivityForConfigChanges(p0: ActivityPluginBinding) {
            TODO("Not yet implemented")
        }

        override fun onDetachedFromActivity() {
            TODO("Not yet implemented")
        }
    }
}
