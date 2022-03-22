package simpleconcept.dk.mobilepay_flutter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import dk.mobilepay.sdk.Country;
import dk.mobilepay.sdk.MobilePay;
import dk.mobilepay.sdk.ResultCallback;
import dk.mobilepay.sdk.model.FailureResult;
import dk.mobilepay.sdk.model.Payment;
import dk.mobilepay.sdk.model.SuccessResult;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

/** MobilepayFlutterPlugin */
public class MobilePayFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Context context;
  private Activity activity;
  int requestCode = 1337;

  public final Country getCountry(String countryCode) {
    if (countryCode.equals("fi")) {
      return Country.FINLAND;
    } else if(countryCode.equals("dk")) {
      return Country.DENMARK;
    } else {
      return Country.DENMARK;
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {

    Log.d("TESTING", "HERE1");

    if(requestCode == this.requestCode) {

      Log.d("TESTING", "HERE2");

      MobilePay.getInstance().handleResult(resultCode, data, new ResultCallback() {

        @Override
        public void onSuccess(SuccessResult result) {
          Log.d("TESTING", "SUCCESS");
          Map<String, Object> args  = new HashMap<String, Object>() {{
            put("requestCode", requestCode);
            put("amountWithdrawnFromCard", result.getAmountWithdrawnFromCard().doubleValue());
            put("orderId", result.getOrderId());
            put("signature", result.getSignature());
            put("transactionId", result.getTransactionId());
          }};
          channel.invokeMethod("mobilePaySuccess", args);
        }

        @Override
        public void onFailure(FailureResult result) {
          Log.d("TESTING", "FAILURE");
          Map<String, Object> args  = new HashMap<String, Object>() {{
            put("requestCode", requestCode);
            put("errorCode", result.getErrorCode());
            put("errorMessage", result.getErrorMessage());
          }};
          channel.invokeMethod("mobilePayFailure", args);
        }

        @Override
        public void onCancel(String orderId) {
          Log.d("TESTING", "CANCEL");
          Map<String, String> args  = new HashMap<String, String>() {{
            put("orderId", orderId);
          }};
          channel.invokeMethod("mobilePayCancel", args);
        }
      });
    }
    return true;
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mobilepay_flutter");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("TESTING", call.method);
    switch (call.method) {
      case "initMobilePay":
        Log.d("TESTING", "INITING");
        String merchantId = call.argument("merchantId");
        String country = call.argument("country");
        MobilePay.getInstance().init(merchantId, getCountry(country));
        result.success(null);
        break;
      case "setRequestCode":
        requestCode = call.argument("requestCode");
        result.success(null);
        break;
      case "createPayment":
        double price = call.argument("productPrice");
        String orderId = call.argument("orderId");

        Payment payment = new Payment();
        payment.setProductPrice(BigDecimal.valueOf(price));
        payment.setOrderId(orderId);

        Intent paymentIntent = MobilePay.getInstance().createPaymentIntent(payment);

        activity.startActivityForResult(paymentIntent, requestCode);
        result.success(null);
        break;
      case "downloadMobilePay":
        Intent intent = MobilePay.getInstance().createDownloadMobilePayIntent(activity.getApplicationContext());
        activity.startActivity(intent);
        result.success(null);
        break;
      case "isMobilePayInstalled":
        Log.d("TESTING", "Got here...");
//        result.success(true);
        result.success(MobilePay.getInstance().isMobilePayInstalled(this.activity.getApplicationContext()));
        break;
      default: {
        result.notImplemented();
        break;
      }
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityBinding) {
    activity = activityBinding.getActivity();
    activityBinding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}
