# QRScanSdk
Android扫码工具sdk，基于camera预览 + zxing解析。 
v1.2: 使用老版camera api实现; 
v2.1: 使用新版camera2 api实现。
https://juejin.cn/post/6927664078806581261

Step 1. Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.zouhecan:QRScanSdk:2.0'
	}
![](https://jitpack.io/v/zouhecan/QRScanSdk.svg)](https://jitpack.io/#zouhecan/QRScanSdk)

Step 3. Using QRScanSdk in your activity

	class MainActivity : AppCompatActivity(), QRScanResultCallback {
             fun startScan() {
                    QRScanManager.startScan(this, this)
              }

              override fun onResult(result: String?) {
                  //do something when the scan is completed
              }
        }
