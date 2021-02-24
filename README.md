# QRScanSdk
Android扫码工具sdk，基于Camera预览 + Zxing解析。 
v1.0: camera1使用老版相机API实现; 
v2.0: camera2使用新版相机API2实现。
https://juejin.cn/post/6927664078806581261

Add it in your root build.gradle at the end of repositories:

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
