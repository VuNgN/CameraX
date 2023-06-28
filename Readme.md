# CameraX

# Architecture

## CameraX structure

Các use case:

- **Preview**: Cho phép một surface hiển thị một preview như là `PreviewView`.
- **Image analysis**: cung cấp các buffer “CPU có thể truy cập” để phân tích, như là cho machine learning.
- **Image capture**: Chụp và lưu ảnh.
- Video capture: Quay video và âm thanh sử dụng `VideoCapture`.

Các use case trên có thể được sử dụng đồng thời (trộn lẫn với nhau).

## CameraX Lifecycle

CameraX quan sát (observes) một vòng đời (lifecycle) để xác định thời điểm mở Camera, thời điểm tạo một phiên chụp ảnh, và thời điểm dừng và tắt. Các use case API cung cấp các lời gọi method và các callback để theo dõi tiến trình.

Như đã được giải thích trong phần [Combine use cases](https://developer.android.com/training/camerax/architecture#combine-use-cases), bạn có thể ràng buộc (bind) một số trộn lẫn của các use case vào một vòng đời đơn (single lifecycle). Khi app của bạn cần hỗ trợ các use case mà không thể trộn lẫn với nhau, bạn có thể làm một trong những điều sau:

- Nhóm các use case có thể tương thích vào nhiều hơn một fragment và sau đó chuyển đổi giữa các fragment.
- Tạo một thành phần vòng đời tự chỉnh (custom lifecycle component) và sử dụng nó để điều khiển một cách thủ công vòng đời của camera.

Nếu bạn tách riêng view và các Lifecycle owner của các camera use case (ví dụ, nếu bạn sử dụng một custom lifecycle hoặc một [retain fragment](https://developer.android.com/reference/android/app/Fragment#setRetainInstance(boolean))), thì bạn phải chắc chắn rằng tất cả các trường hợp đều được unbound từ CameraX bằng cách sử dụng `[ProcessCameraProvider.unbindAll()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#unbindAll())` hoặc bằng cách unbind từng use case riêng lẻ. Ngoài ra, khi bạn ràng buộc các trường hợp vào một Lifecycle, bạn có thể để cameraX quản lý việc mở và đóng cái phiên chụp (capture session) và unbind các use case.

Nếu tất cả các chức năng camera của bạn tương ứng với vòng đời của một thành phần nhận biết vòng đời duy nhất (single lifecycle-aware component) như là một `[AppCompatActivity](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity)` hoặc một `AppCompat` fragment, thì sử dụng cái vòng đời của thành phần đó khi ràng buộc tất cả các use case được mong muốn sẽ chắc chắn rằng chức năng camera được sẵn sàng khi lifecycle-aware component hoạt động, và được sử lý một cách an toàn, không tiêu tốn bất kỳ tài nguyên nào.

## Custom LifecycleOwners

Cho một số trường hơp nâng cao, bạn có thể tạo một custom `LifecycleOwner` để kích hoạt app của bạn để kiểm soát rõ ràng lifecycle của CameraX session thay cho việc buộc nó vào `LifecycleOwner` tiêu chuẩn của Android.

Đoạn code dưới đây là cách để tạo một custom LifecycleOwner đơn giản:

```kotlin
class CustomLifecycle : LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry

    init {
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
    }
    ...
    fun doOnResume() {
        lifecycleRegistry.markState(State.RESUMED)
    }
    ...
    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
```

Sử dụng `LifecycleOwner` này, app của bạn có thể đặt các chuyển đổi trạng thái ở các điểm thích hợp trong code của bạn.

## Concurrent use cases

Các use case có thể chạy một cách đồng thời. Dù là các use case có thể được ràng buộc một cách tuần tự vào một lifecycle, nhưng tốt nhất là ràng buộc tất cả các trường hợp vào một lời gọi cho `CameraProcessProvider.bindToLifecycle()`.

Trong đoạn code ví dụ dưới đây, app chỉ định 2 use case được tạo và chạy đồng thời và chỉ định lifecycle để sử dụng cho cả hai use case, vì vậy cả hai sẽ start và stop theo cái lifecycle.

```kotlin
private lateinit var imageCapture: ImageCapture

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener(Runnable {
        // Camera provider is now guaranteed to be available
        val cameraProvider = cameraProviderFuture.get()

        // Set up the preview use case to display camera preview.
        val preview = Preview.Builder().build()

        // Set up the capture use case to allow users to take photos.
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

        // Choose the camera by requiring a lens facing
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

        // Attach use cases to the camera with the same lifecycle owner
        val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageCapture)

        // Connect the preview use case to the previewView
        preview.setSurfaceProvider(
                previewView.getSurfaceProvider())
    }, ContextCompat.getMainExecutor(this))
}
```

Các hỗn hợp cấu hình sau được đảm bảo để được hỗ trợ (khi Preview hoặc Video Capture là bắt buộc nhưng không phải đồng thời)

| Preview or VideoCapture | Image capture | Analysis | Descriptions |
| --- | --- | --- | --- |
| ✔ | ✔ | ✔ | Provide user a preview or record video, take a photo, and analyze the image stream.  |
|  | ✔ | ✔ | Take a photo, and analyze the image stream. |
| ✔ | ✔ |  | Provide user a preview or record video, and take a photo. |
| ✔ |  | ✔ | Provide user a preview or record video, and analyze the image stream. |

Khi cả Preview và Video Capture là bắt buộc, các hỗn hợp trường hợp dưới đây được hỗ trợ một cách có điều kiện:

| Preview | Video capture | Image capture | Analysis | Special requirement |
| --- | --- | --- | --- | --- |
| ✔ | ✔ |  |  | Guaranteed for all cameras |
| ✔ | ✔ | ✔ |  | LIMITED (or better) camera device. |
| ✔ | ✔ |  | ✔ | LEVEL_3 (or better) camera device. |

Thêm nữa,

- Tất cả use case đều tự hoạt động. Ví dụ, một app có thể quay video mà không cần preview.
- Khi các extension được kích hoạt, chỉ có tổ hợp `ImageCapture` và `Preview` được đảm bảo hoạt động. Tùy thuộc vào việc triển khai OEM, có thể không thêm `ImageAnalysis`; các extension không thể được kích hoạt cho trường hợp `VideoCapture`.
- Tùy thuộc vào khả năng của máy ảnh, một số camera có thể hỗ trợ hỗn hợp ở độ phân giải thấp, nhưng không hỗ trợ hỗn hợp tương ứng ở một số độ phân giải cao hơn.

[supported hardware level](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL) có thể được truy suất từ `Camera2CameraInfo`. Ví dụ, đoạn code dưới đây kiểm tra Camera mặt sau mặc định có phải là một `LEVEL_3` device:

```kotlin
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
fun isBackCameraLevel3Device(cameraProvider: ProcessCameraProvider) : Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return CameraSelector.DEFAULT_BACK_CAMERA
            .filter(cameraProvider.availableCameraInfos)
            .firstOrNull()
            ?.let { Camera2CameraInfo.from(it) }
            ?.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
    }
    return false
}
```

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> **Chú ý**: Nếu một hỗn hợp không tương thích của các use case được tạo, một runtime error sẽ được bắn ra trong lần đầu tiên **`[createCaptureSession()](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#createCaptureSession(android.hardware.camera2.params.SessionConfiguration))`** được gọi. .Nếu các use case bổ sung được thêm vào phiên đang chạy, một tái cấu hình có thể bị bắt buộc, khả năng gây ra một trục trặc có thể nhìn thấy.

</aside>

## Permissions

App của bạn cần `[CAMERA](https://developer.android.com/reference/android/Manifest.permission#CAMERA)` permission. Để lưu các hình ảnh vào các file, bắt buộc phải có `[WRITE_EXTERNAL_STORAGE](https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE)` permission trên các thiết bị Android 9 trở xuống.

## Declare dependencies

Để thêm một phụ thuộc trong CameraX, bạn cần phải thêm [Google Maven repository](https://developer.android.com/studio/build/dependencies#google-maven) vào trong project của bạn.

Mở `settings.gradle` file và thêm `google()`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
				google()
        mavenCentral()
    }
}
```

Thêm đoạn dưới đây vào cuối của Android block:

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    // For Kotlin projects
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

Thêm đoạn dưới đây tùy thuộc vào mỗi app:

```groovy
dependencies {
  // CameraX core library using the camera2 implementation
  def camerax_version = "1.3.0-alpha07"
  // The following line is optional, as the core library is included indirectly by camera-camera2
  implementation "androidx.camera:camera-core:${camerax_version}"
  implementation "androidx.camera:camera-camera2:${camerax_version}"
  // If you want to additionally use the CameraX Lifecycle library
  implementation "androidx.camera:camera-lifecycle:${camerax_version}"
  // If you want to additionally use the CameraX VideoCapture library
  implementation "androidx.camera:camera-video:${camerax_version}"
  // If you want to additionally use the CameraX View class
  implementation "androidx.camera:camera-view:${camerax_version}"
  // If you want to additionally add CameraX ML Kit Vision Integration
  implementation "androidx.camera:camera-mlkit-vision:${camerax_version}"
  // If you want to additionally use the CameraX Extensions library
  implementation "androidx.camera:camera-extensions:${camerax_version}"
}
```

# Configurations

## CameraXConfig

Hiểu đơn giản là CameraX có các cấu hình mặc định như là các internal executor các trình sử lý phù hợp với hầu hết các trường hợp sử dụng. Tuy nhiên, nếu app của bạn có các requirement đặc biệt hoặc bạn muốn custom đống cấu hình, `[CameraXConfig](https://developer.android.com/reference/androidx/camera/core/CameraXConfig)` là interface phục vụ cho mục đích đó.

Với `CameraXConfig`, một app có thể:

- Tối ưu hóa độ trễ khởi động với `[setAvailableCameraLimiter()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setAvailableCamerasLimiter(androidx.camera.core.CameraSelector))`.
- Cung cấp executor của app cho CameraX với `[setCameraExecutor()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setCameraExecutor(java.util.concurrent.Executor))`.
- Thay thế trình sử lý lịch biểu mặc định (default scheduler handler) với `[setSchedulerHandler()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setSchedulerHandler(android.os.Handler))`.
- Thay đổi mức độ log với `[setMinimumLoggingLevel()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setMinimumLoggingLevel(int))`.

## Usage Model

Cách dùng `CameraXConfig`:

1. Tạo một `CameraXConfig` object với các custom configuration của bạn.
2. Triển khai `[CameraXConfig.Provider](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Provider)` interface trong `[Application](https://developer.android.com/reference/android/app/Application)` của bạn, và trả về  `CameraXConfig` object của bạn trong `[getCameraXConfig()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Provider#getCameraXConfig())`.
3. Thêm `Application` class của bạn vào `AndroidManifest.xml` file.

Ví dụ dưới đây hạn chế CameraX logging chỉ còn các error messages:

```kotlin
class CameraApplication : Application(), CameraXConfig.Provider {
   override fun getCameraXConfig(): CameraXConfig {
       return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
           .setMinimumLoggingLevel(Log.ERROR).build()
   }
}
```

Giữ một bản copy của `CameraXConfig` object nếu ứng dụng của bạn cần được biết cấu hình CameraX sau khi setting nó.

## Camera Limiter

Trong suốt lời gọi đầu tiên của `[ProcessCameraProvider.getInstance()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#getInstance(android.content.Context))`, CameraX liệt kê và truy vấn các thành phần của các camera khả thi trên thiết bị. Bởi vì CameraX cần được giao tiếp với các thành phần hardware, quá trình này có thể mất thời gian với mỗi camera, đặc biệt với mỗi thiết bị cũ. Nếu app của bạn chỉ sử dụng các camera chỉ định trên thiết bị như là camera trước đặc biệt, bạn có thể set CameraX để ignore các camera khác, điều này có thể giảm độ trễ khởi động cho các camera mà app bạn sử dụng.

Nếu `[CameraSelector](https://developer.android.com/reference/androidx/camera/core/CameraSelector)` được chuyền vào `[CameraXConfig.Builder.setAvailableCamerasLimiter()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setAvailableCamerasLimiter(androidx.camera.core.CameraSelector))` lọc ra một camera, CameraX hoạt động như là camera đó không tồn tại. Ví dụ, code dưới đây hạn chế app chỉ sử dụng camera sau mặc định của thiết bị:

```kotlin
class MainApplication : Application(), CameraXConfig.Provider {
   override fun getCameraXConfig(): CameraXConfig {
       return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
              .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
              .build()
   }
}
```

## Threads

Nhiều API platform mà xây dựng lên CameraX yêu cầu chặn interprocess communication (IPC) với hardware mà có khi chạy mất hàng nghìn milli giây để phản hồi. Vì lý do này, CameraX chỉ gọi mấy cái API đó từ các background thread, vậy nên main thread không bị block và UI vẫn linh hoạt. CameraX quản lý nội bộ những cái background thread để nó hoạt động ngầm. Tuy nhiên, một số app yêu cầu kiểm soát chặt chẽ các luồng.

`CameraXConfig` cho phép app có thể set các background thread được sử dụng thông qua `[CameraXConfig.Builder.setCameraExecutor()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setCameraExecutor(java.util.concurrent.Executor))` và `[CameraXConfig.Builder.setSchedulerHandler()](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setSchedulerHandler(android.os.Handler))`.

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> **Chú ý**: Khi cung cấp một custom executor hoặc scheduler handler, sử dụng cái mà không thực thi code ở main thread.

</aside>

## Camera Executor

Camera executor được sử dụng cho tất cả các lời gọi internal Camera flatform API, như là các callback từ mấy cái API. CameraX phân bố và quản lý một internal `[Executor](https://developer.android.com/reference/java/util/concurrent/Executor)` để thực hiện các nhiệm vụ đó. Tuy nhiên, nếu app của bạn yêu cầu kiểm soát luồng chặt chẽ hơn thì sử dụng `CameraXConfig.Builder.setCameraExecutor()`.

## Scheduler Handler

Scheduler handler được sử dụng để lên lịch các task trong khoảng thời gian cố định, như là cố gắng mở camera khi nó không khả dụng. Handler này không thực thi các công việc mà chỉ gửi chúng cho camera executor. Đôi khi cũng có lúc nó được sử dụng trong các legacy API platform mà yêu cầu một `[Handler](https://developer.android.com/reference/android/os/Handler)` cho các callback. Trong các trường hợp như thế, các lời gọi thường chỉ được chuyển trực tiếp tới camera executor. CameraX phân bố và quản lý một internal `[HandlerThread](https://developer.android.com/reference/android/os/HandlerThread)` để thực hiện các task này, nhưng bạn có thể override nó với `CameraXConfig.Builder.setSchedulerHandler()`.

## Logging

CameraX logging cho phép các app lọc các logcat message, đây là một cách tốt để tránh các message dài dòng trong code của bạn. CameraX hỗ trợ 4 cấp độ của log, từ dài dòng nhất cho đến nghiêm trọng nhất:

- `Log.DEBUG` (default)
- `Log.INFO`
- `Log.WARN`
- `Log.ERROR`

Sử dụng `[CameraXConfig.Builder.setMinimumLoggingLevel(int)](https://developer.android.com/reference/androidx/camera/core/CameraXConfig.Builder#setMinimumLoggingLevel(int))` để set cấp độ log phù hợp cho app của bạn.

## Automatic selection

CameraX tự động cung cấp chức năng được chỉ định cho thiết bị đang chạy app của bạn. Ví dụ, CameraX tự động xác định độ phân giải tốt nhất để sử dụng nếu bạn không chỉ định một độ phân giải nào hoặc độ phân giải bạn chỉ định không được hỗ trợ. Tất cả các thứ đó được sử lý bởi thư viện, giúp bạn không cần phải tự viết.

Mục đích của CameraX là khởi tạo thành công một camera session. Có nghĩa là CameraX sẽ sắp xếp độ phân giải và tỉ lệ khung hình phù hợp với khả năng của thiết bị. Sự sắp xếp này sảy ra vì:

- Thiết bị không hỗ trợ độ phân giải đã được yêu cầu.
- Thiết bị có những vấn đề về khả năng tương thích, như là các thiết bị cũ yêu cầu những độ phân giải nhất định để hoạt động chính xác.
- Trên một số thiết bị, các định dạng nhất định chỉ khả dụng với các tỉ lệ khung hình nhất định.
- Thiết bị có tùy chọn “nearest mod16” để mã hóa JPEG hoặc video. For more information, see `[SCALER_STREAM_CONFIGURATION_MAP](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP)`.

Ngoài ra CameraX còn tạo và quản lý session, luôn luôn kiểm tra các kích thước ảnh được trả về trong trường hợp output trong code của bạn và điều chỉnh phù hợp.

## Rotation

Mặc định, camera rotation được set để phù hợp với rotation của hiển thị mặc định trong quá trình khởi tạo của use case. Trong trường hợp mặc định, CameraX cho các đầu ra để kiến cho app phù hợp với những gì bạn nhìn thấy trên preview. Bạn có thể thay đổi rotation thành một custom value để hỗ trợ các thiết bị đa màn hình bằng cách chuyền trong current display orientation khi cấu hình các use case object hoặc sau khi chúng đã được tạo.

App của bạn có thể set rotation mong muốn bằng cách sử dụng các configuration setting. Điều này có thể cập nhật các rotation setting bằng cách sử dụng các method từ các use case API (Như là `[ImageAnalysis.setTargetRotation()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#setTargetRotation(int))`), kể cả khi lifecycle đang trong trạng thái running. Bạn nên sử dụng nó điều này khi app bị khóa ở chế độ dọc - và sau đó không có cấu hình nào sảy ra khi rotation - nhưng cái ảnh hoặc annalysis use case cần được biết về rotation hiện tại của thiết bị. Ví dụ, rotation cần thiết cho việc nhận diện khuân mặt chính xác, hoặc các ảnh được đặt ở chế độ ngang hoặc dọc.

Dữ liệu cho các ảnh được chụp có thể được lưu mà không cần thông tin về rotation. Exif data chứa thông tin rotation nên các gallery app có thể hiển thị ảnh đúng hướng sau khi lưu.

Để hiển thị preview data đúng hướng, bạn có thể sử dụng metadata output từ `[Preview.PreviewOutput()](https://developer.android.com/reference/androidx/camera/core/Preview.PreviewOutput)` để tạo các thay đổi.

Code ví dụ về việc set cái retation trong một orientation event:

```kotlin
override fun onCreate() {
    val imageCapture = ImageCapture.Builder().build()

    val orientationEventListener = object : OrientationEventListener(this as Context) {
        override fun onOrientationChanged(orientation : Int) {
            // Monitors orientation values to determine the target rotation value
            val rotation : Int = when (orientation) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }

            imageCapture.targetRotation = rotation
        }
    }
    orientationEventListener.enable()
}
```

Dựa trên rotation đã được set, mỗi use case sẽ trực tiếp xoay ảnh hoặc cung cấp rotation metadata cho người dùng hình ảnh không được xoay.

- **Preview**: Metadata output is provided so that the rotation of the target resolution is known using `[Preview.getTargetRotation()](https://developer.android.com/reference/androidx/camera/core/Preview#getTargetRotation())`.
- **ImageAnalysis**: Metadata output is provided so that image buffer coordinates are known relative to display coordinates.
- **ImageCapture**: The image Exif metadata, buffer, or both the buffer and metadata are altered to note the rotation setting. The value altered depends upon the HAL implementation.

## Crop rect

Mặc định, crop rect là full buffer rect. Bạn có thể custom nó với `[ViewPort](https://developer.android.com/reference/androidx/camera/core/ViewPort)` và `[UseCaseGroup](https://developer.android.com/reference/androidx/camera/core/UseCaseGroup)`. Bằng cách nhóm các use case và cài đặt viewport, CameraX đảm bảo rằng crop rects của tất cả các use case trong nhóm trỏ đến cùng một khu vực trong camera censor.

Code dưới đây là cách sử dụng 2 lớp trên:

```kotlin
val viewPort =  ViewPort.Builder(Rational(width, height), display.rotation).build()
val useCaseGroup = UseCaseGroup.Builder()
    .addUseCase(preview)
    .addUseCase(imageAnalysis)
    .addUseCase(imageCapture)
    .setViewPort(viewPort)
    .build()
cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
```

`ViewPort` cung cấp buffer rect hiển thị tới các người dùng cuối. Sau đó CameraX tính toán crop rect lớn nhất có thể dựa trên các thuộc tính của viewport và các use case kèm theo. Thường thì để đạt WYSIWYG effect, bạn có thể cấu hình viewport dựa trên preview use case. Một cách đơn giản để lấy viewport là dùng `[PreviewView](https://developer.android.com/training/camerax/preview#implementation)`.

Dưới đây là cách lấy `ViewPort` object:

```kotlin
val viewport = findViewById<PreviewView>(R.id.preview_view).viewPort
```

Trong ví dụ trước, những gì app nhận được từ `ImageAnalysis` và `ImageCapture` phù hợp với những gì người dùng cuối thấy ở `PreviewView`, giả sử scale type của `PreviewView` được set là mặc định, `FILL_CENTER`. Sau khi cho phép crop rect và rotation vào output buffer, hình ảnh từ tất cả các use case là giống nhau kể cả khi chúng có thể ở các độ phân giải khác nhau.

## Camera selection

CameraX tự động chọn thiết bị camera tốt nhất cho các yêu cầu của app của bạn và các use case. Nếu bạn muốn sử dụng một thiết bị khác thì có một số các lựa chọn:

- Request camera trước mặc định với `[CameraSelector.DEFAULT_FRONT_CAMERA](https://developer.android.com/reference/androidx/camera/core/CameraSelector#DEFAULT_FRONT_CAMERA)`.
- Request camera sau mặc định với `[CameraSelector.DEFAULT_BACK_CAMERA](https://developer.android.com/reference/androidx/camera/core/CameraSelector#DEFAULT_BACK_CAMERA)`.
- Lọc danh sách các thiết bị khả dụng bằng `[CameraCharacteristics](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics)` với `[CameraSelector.Builder.addCameraFilter()](https://developer.android.com/reference/androidx/camera/core/CameraSelector.Builder#addCameraFilter(androidx.camera.core.CameraFilter))`.

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> Chú ý: các thiết bị camera phải được công nhận bởi hệ thống và xuất hiện trong **`CameraManager.getCameraIdList()`** trước khi chúng được sử dụng.
Thêm nữa, mỗi OEM chịu trách nhiệm lựa chọn xem có hỗ trợ các thiết bị camera bên ngoài không. Vì vậy, hãy chắc chắn là kiểm tra **`PackageManager.FEATURE_CAMERA_EXTERNAL`** được kích hoạt trước khi cố gắng để sử dụng camera bên ngoài.

</aside>

Ví dụ dưới đây là cách tạo một `CameraSelector` để tác động đến sự lựa chọn của thiết bị:

```kotlin
fun selectExternalOrBestCamera(provider: ProcessCameraProvider):CameraSelector? {
   val cam2Infos = provider.availableCameraInfos.map {
       Camera2CameraInfo.from(it)
   }.sortedByDescending {
       // HARDWARE_LEVEL is Int type, with the order of:
       // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
       it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
   }

   return when {
       cam2Infos.isNotEmpty() -> {
           CameraSelector.Builder()
               .addCameraFilter {
                   it.filter { camInfo ->
                       // cam2Infos[0] is either EXTERNAL or best built-in camera
                       val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
                       thisCamId == cam2Infos[0].cameraId
                   }
               }.build()
       }
       else -> null
    }
}

// create a CameraSelector for the USB camera (or highest level internal camera)
val selector = selectExternalOrBestCamera(processCameraProvider)
processCameraProvider.bindToLifecycle(this, selector, preview, analysis)
```

## Select multiple cameras concurrently

Bắt đầu từ CameraX 1.3, bạn có thể chọn nhiều camera đồng thời cùng một lúc. Ví dụ, bạn có thể ràng buộc cả camera trước và camera sau cùng chụp ảnh và quay video cùng một lúc.

Khi sử dụng chức năng Concurrent Camera, thiết bị có thể vận hành hai camera với các lense khcas nhau ở cùng một thời điểm. Đoạn code dưới đây chỉ cách cài hai camera khi gọi  `bindToLifecycle`, và cách lấy cả hai camera object từ `ConcurrentCamera` object được trả về.

```kotlin
// Build ConcurrentCameraConfig
val primary = ConcurrentCamera.SingleCameraConfig(
    primaryCameraSelector,
    useCaseGroup,
    lifecycleOwner
)

val secondary = ConcurrentCamera.SingleCameraConfig(
    secondaryCameraSelector,
    useCaseGroup,
    lifecycleOwner
)

val concurrentCamera = cameraProvider.bindToLifecycle(
    listOf(primary, secondary)
)

val primaryCamera = concurrentCamera.cameras[0]
val secondaryCamera = concurrentCamera.cameras[1]
```

## Camera resolution

Bạn có thể để CameraX đặt độ phân giải cho ảnh dựa trên các trường hợp được chỉ định trong `cameraProcessProvider.bindToLifecycle()`. Bất cứ khi nào có thể, hãy đặt tất cả các use case cần được chạy cùng một lúc trong một single session trong một lời gọi single `bindToLifecycle()`. CameraX xác định các độ phân giải dựa trên set của các use case được bind bằng cách xem xét cấp độ hardware được hỗ trợ của thiết bị và bằng cách tính device-specific variance (nơi một thiết bị vượt quá hoặc không gặp [stream configurations available](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#createCaptureSession(android.hardware.camera2.params.SessionConfiguration))). Mục đính là để app chạy trên nhiều thiết bị khác nhau mà tối thiểu số lượng dòng code.

Tỉ lệ khung hình mặc định cho các use case như image capture và image analysis là 4:3.

Các trường hợp có một tỉ lệ khung hình có thể config để cho phép app chỉ định tỉ lệ khung hình mong muốn dựa trên thiết kế UI. CameraX output được sản xuất để phù hợp với tỉ lệ khung hình được yêu cầu chặt chẽ như thiết bị hỗ trợ. Nếu không có độ phân giải phù hợp được hỗ trợ thì cái đáp ứng điều kiện nhất sẽ được chọn. Do đó, app chỉ định cách camera xuất hiện trọng app, và CameraX xác định độ phân giải tốt nhất đáp ứng điều đó trên các thiết bị khác nhau.

Ví dụ, một app có thể làm:

- Chỉ định một độ phân giải mong muốn là 4:3 hoặc 16:9 cho một use case.
- Chỉ định một custom resolution mà CameraX cố gắng tìm kiếm để phù hợp chặt chẽ nhất.
- Chỉ định một tỉ lệ khung hình cắt xén cho `ImageCapture`.

CameraX chọn các giải pháp internal Camera2 surface một cách tự động. Bảng sau là các giải pháp:

| Use case | Internal surface resolution | Output data resolution |
| --- | --- | --- |
| Preview | Aspect Ratio: The resolution that best fits the target to the setting. | Internal surface resolution. Metadata is provided to let a View crop, scale, and rotate for the target aspect ratio. |
|  | Default resolution: Highest preview resolution, or highest device-preferred resolution that matches the Preview's aspect ratio. |  |
|  | Max resolution: Preview size, which refers to the best size match to the device's screen resolution, or to 1080p (1920x1080), whichever is smaller. |  |
| Image analysis | Aspect ratio: The resolution that best fits the target to the setting. | Internal surface resolution. |
|  | Default resolution: The default target resolution setting is 640x480. Adjusting both target resolution and corresponding aspect ratio results in a best-supported resolution. |  |
|  | Max resolution: The camera device's maximum output resolution of YUV_420_888 format which is retrieved from https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap#getOutputSizes(int). The target resolution is set as 640x480 by default, so if you want a resolution larger than 640x480, you must use https://developer.android.com/reference/kotlin/androidx/camera/core/ImageAnalysis.Builder#settargetresolution and https://developer.android.com/reference/kotlin/androidx/camera/core/ImageAnalysis.Builder#settargetaspectratio to get the closest one from the supported resolutions. |  |
| Image capture | Aspect ratio: Aspect ratio that best fits the setting. | Internal surface resolution. |
|  | Default resolution: Highest resolution available, or highest device-preferred resolution that matches the ImageCapture's aspect ratio. |  |
|  | Max resolution: The camera device's maximum output resolution in a JPEG format. Use https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap#getOutputSizes(int) to retrieve this. |  |

## Specify a resolution

Bạn có thể set các độ phân giải theo ý muốn khi xây dựng các use case sử dụng phương thức `setTargetResolution(Size resolution)`, như dưới đây:

```kotlin
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(1280, 720))
    .build()
```

Bạn không thể set cả tỉ lệ khung hình và độ phân giải cùng một lúc theo ý muốn trong cùng một use case. Làm như vậy sẽ bắn ra một `IllegalArgumentException` khi xây dựng các configuration object.

Thể hiện `[Size](https://developer.android.com/reference/android/util/Size)` độ phân giải trên khung tọa độ (coordinate frame) sau khi xoay các kích thước được hỗ trợ bằng rotation mong muốn. Ví dụ, một thiết bị với hướng dọc thì retation mong muốn yêu cầu một hình ảnh 480x640, và vẫn thiết bị đó khi được quay 90 độ thì sẽ là 640x480.

Độ phân giải mong muốn sẽ cố gắng thiết lập giới hạn tối thiểu cho độ phân giải của hình ảnh. Độ phân giải hình ảnh thực tế là độ phân giải khả dụng có kích cỡ chặt chẽ nhất mà không quá nhỏ so với độ phân giải mong muốn.

Tuy nhiên, nếu không có độ phân giải nào mà bằng hoặc lơn hơn độ phân giải mong muốn tồn tại, thì độ phân giải khả dụng gần nhất nhỏ hơn độ phân giải mong muốn sẽ được chọn. Các độ phân giải có cùng một tỉ lệ khung hình của `Size` được cung cấp sẽ được ưu tiên cao hơn các độ phân giải của các tỉ lệ khung hình khác nhau.

CameraX áp dụng độ phân giải phù hợp nhất dựa trên các request. Nếu nhu cầu chính là để đáp ứng tỉ lệ khung hình, chỉ xác định `setTargetAspectRatio`, và CameraX sẽ xác định độ phân giải cụ thể dựa trên thiết bị. Nếu nhu cầu chính của app là xác định một độ phân giải để làm cho quá trình sử lý ảnh thêm hiệu quả (ví dụ một hình ảnh nhỏ hoặc trung bình dựa trên khả năng sử lý thiết bị), sử dụng `setTargetResolution(Size resolution)`.

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> Chú ý: Nếu bạn sử dụng **`setTargetResolution()`**, thì bạn có thể nhận một buffer có tỷ lệ khung hình không khớp với các use case khác. Nếu tỷ lệ khung hình phải phù hợp, kiểm tra các kích thước của các buffer được trả về bởi cả các use case và crop hoặc scale, một trong số chúng khớp với nhau.

</aside>

Nếu app của bạn yêu cầu một độ phân giải chính xác, xem bảng `[createCaptureSession()](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#regular-capture)` để xác định các độ phân giải tối đa được hỗ trợ bởi mỗi cấp độ hardware. Để kiểm tra các tỉ lệ khung hình chỉ định có được thiết bị hỗ trợ hay không thì xem `[StreamConfigurationMap.getOutputSizes(int)](https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap#getOutputSizes(int))`.

Nếu app của bạn đang chạy trên Android 10 hoặc cao hơn, bạn có thể sử dụng `[isSessionConfigurationSupported()](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#isSessionConfigurationSupported(android.hardware.camera2.params.SessionConfiguration))` để xác minh một `SessionConfiguration` cụ thể.

## Control camera output

CameraX triển khai các interface sau:

- `[CameraControl](https://developer.android.com/reference/androidx/camera/core/CameraControl)` giúp bạn config các chức năng camera phổ biến.
- `[CameraInfo](https://developer.android.com/reference/androidx/camera/core/CameraInfo)` giúp bạn query các state của các chức năng camera phổ biến.

Các chức năng camera phổ biến với CameraControl:

- Zoom
- Torch
- Focus và Metering (tap-to-focus)
- Exposure Compensation

### Get instances of CameraControl and CameraInfo

Để tạo instance của `CameraControl` và `CameraInfo`, sử dụng `[Camera](https://developer.android.com/reference/androidx/camera/core/Camera)` object được trả về từ `[ProcessCameraProvider.bindToLifecycle()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider)`.

```kotlin
val camera = processCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

// For performing operations that affect all outputs.
val cameraControl = camera.cameraControl
// For querying information and states.
val cameraInfo = camera.cameraInfo
```

Ví dụ bạn có thể zoom và các phương thức của `CameraControl` khác sau khi gọi `bindToLifecycle()`. Sau khi bạn dừng hoặc hủy activity sử dụng để bind cái camera instance, `CameraControl`  không thực thi được các phương thức đó nữa và trả về một `ListenableFuture` thất bại.

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> Chú ý: tất cả các thay đổi về trạng thái của zoom, torch, forcus và metering, và exposure compensation trở về giá trị mặc định của chúng sau khi đóng **`Camera`**, điều này sảy ra khi **`[LifecycleOwner](https://developer.android.com/reference/androidx/lifecycle/LifecycleOwner)`** bị dừng hoặc hủy.

</aside>

### Zoom

CameraControl đưa ra hai method cho việc thay đổi zoom level:

- `[setZoomRatio()](https://developer.android.com/reference/androidx/camera/core/CameraControl#setZoomRatio(float))` phóng to theo zoom ratio.
    - The ratio must be within the range of `CameraInfo.getZoomState().getValue().getMinZoomRatio()` and `CameraInfo.getZoomState().getValue().getMaxZoomRatio()`. Otherwise the function returns a failed `ListenableFuture`.
- `[setLinearZoom()](https://developer.android.com/reference/androidx/camera/core/CameraControl#setLinearZoom(float))` phóng to với một giá trị linear zoom trong khoảng từ 0 → 1.0.
    - Lợi thế của linear zoom là nó làm cho field of view (FOV) scale với các thay đổi trong zoom. Điều này là lý tưởng để sử dụng `[Slider](https://developer.android.com/reference/com/google/android/material/slider/Slider)` view.

`[CameraInfo.getZoomState()](https://developer.android.com/reference/androidx/camera/core/CameraInfo#getZoomState())` trả về một LiveData của trạng thái zoom hiện tại. Giá trị thay đổi khi camera được khởi tạo hoặc khi zoom level được thay đổi bằng `setZoomRatio()` hoặc `setLinearZoom()`. Cả hai phương thức đó đều set các giá trị sao lưu `[ZoomState.getZoomRatio()](https://developer.android.com/reference/androidx/camera/core/ZoomState#getZoomRatio())` và `[ZoomState.getLinearZoom()](https://developer.android.com/reference/androidx/camera/core/ZoomState#getLinearZoom())`. Điều này sẽ giúp ích nếu bạn muốn hiển thị tỷ lệ zoom cùng với một slider. Đơn giản observer `ZoomState` `LiveData` để update cả hai mà không cần thực hiện chuyển đổi.

`ListenableFuture` được trả về từ hai API đưa ra lựa chọn cho các app để được thông báo khi một request lặp lại với giá trị zoom chỉ định được hoàn thành. Hơn nữa, nếu bạn đặt một giá trị zoom mới khi vẫn chưa kết thúc quá trình cũ thì `ListenableFuture` của quá trình cũ sẽ thất bại ngay lập tức.

### Torch

`[CameraControl.enableTorch(boolean)](https://developer.android.com/reference/androidx/camera/core/CameraControl#enableTorch(boolean))` enable hoặc disable torch (hay còn gọi là flashlight)

`[CameraInfo.getTorchState()](https://developer.android.com/reference/androidx/camera/core/CameraInfo#getTorchState())` có thể được dùng để query trạng thái torch hiện tại. Bạn có thể kiểm tra giá trị được trả về từ `[CameraInfo.hasFlashUnit()](https://developer.android.com/reference/androidx/camera/core/CameraInfo#hasFlashUnit())` để xác định xem một torch có đang khả dụng. Nếu không thì việc gọi `CameraControl.enableTorch(boolean)` sẽ ngay lập tức trả về `ListenableFuture` với một kết quả thất bại và set trạng thái torch thành `TorchState.OFF`.

Khi torch được enable, nó sẽ được bật trong suốt quá trình chụp ảnh và quay video kể cả khi flashMode được bật. `[flashMode](https://developer.android.com/reference/android/hardware/Camera.Parameters#getFlashMode())` trong `ImageCapture` hoạt động khi và chỉ khi torch bị disable.

### Focus and Metering

`[CameraControl.startFocusAndMetering()](https://developer.android.com/reference/androidx/camera/core/CameraControl#setExposureCompensationIndex(int))` kích hoạt lấy nét tự động và đo độ sáng bằng cách thiết lập vùng đo sáng AF/AE/AWB dựa trên FocusMeteringAction đã cho. Điều này thường được dùng để triển khai chức năng “tap to focus” trong rất nhiều camera app.

**MeteringPoint**

Để bắt đầu, tạo một `[MeteringPoint](https://developer.android.com/reference/androidx/camera/core/MeteringPoint)` sử dụng  `[MeteringPointFactory.createPoint(float x, float y, float size)](https://developer.android.com/reference/androidx/camera/core/MeteringPointFactory#createPoint(float,%20float,%20float))`. Một `MeteringPoint` đại diện cho một điểm duy nhất trên camera `[Surface](https://developer.android.com/reference/android/view/Surface)`. Nó được lưu dưới dạng chuẩn hóa nên có thể dễ dàng convert thành các sensor coordinates để chỉ định các vùng AF/AE/AWB.

Size của `MeteringPoint` trong khoảng từ 0 → 1, default là 0.15f. Khi gọi `MeteringPointFactory.createPoint(float x, float y, float size)`, CameraX tạo một vùng hình chữ nhật có tâm tại `(x, y)` cho `size` được cung cấp.

Cách tạo một `MeteringPoint`:

```kotlin
// Use PreviewView.getMeteringPointFactory if PreviewView is used for preview.
previewView.setOnTouchListener((view, motionEvent) ->  {
val meteringPoint = previewView.meteringPointFactory
    .createPoint(motionEvent.x, motionEvent.y)
…
}

// Use DisplayOrientedMeteringPointFactory if SurfaceView / TextureView is used for
// preview. Please note that if the preview is scaled or cropped in the View,
// it’s the application's responsibility to transform the coordinates properly
// so that the width and height of this factory represents the full Preview FOV.
// And the (x,y) passed to create MeteringPoint might need to be adjusted with
// the offsets.
val meteringPointFactory = DisplayOrientedMeteringPointFactory(
     surfaceView.display,
     camera.cameraInfo,
     surfaceView.width,
     surfaceView.height
)

// Use SurfaceOrientedMeteringPointFactory if the point is specified in
// ImageAnalysis ImageProxy.
val meteringPointFactory = SurfaceOrientedMeteringPointFactory(
     imageWidth,
     imageHeight,
     imageAnalysis)
```

**startFocusAndMetering and FocusMeteringAction**

Để gọi `[startFocusAndMetering()](https://developer.android.com/reference/androidx/camera/core/CameraControl#startFocusAndMetering(androidx.camera.core.FocusMeteringAction))`, các app phải build một `[FocusMeteringAction](https://developer.android.com/reference/androidx/camera/core/FocusMeteringAction)`, cái mà bao gồm một hoặc nhiều `MeteringPoints` với các hỗn hợp chế độ đo sáng tùy chọn từ `[FLAG_AF](https://developer.android.com/reference/androidx/camera/core/FocusMeteringAction#FLAG_AF)`, `[FLAG_AE](https://developer.android.com/reference/androidx/camera/core/FocusMeteringAction#FLAG_AE)`, `[FLAG_AWB](https://developer.android.com/reference/androidx/camera/core/FocusMeteringAction#FLAG_AWB)`.

```kotlin
val meteringPoint1 = meteringPointFactory.createPoint(x1, x1)
val meteringPoint2 = meteringPointFactory.createPoint(x2, y2)
val action = FocusMeteringAction.Builder(meteringPoint1) // default AF|AE|AWB
      // Optionally add meteringPoint2 for AF/AE.
      .addPoint(meteringPoint2, FLAG_AF | FLAG_AE)
      // The action is canceled in 3 seconds (if not set, default is 5s).
      .setAutoCancelDuration(3, TimeUnit.SECONDS)
      .build()

val result = cameraControl.startFocusAndMetering(action)
// Adds listener to the ListenableFuture if you need to know the focusMetering result.
result.addListener({
   // result.get().isFocusSuccessful returns if the auto focus is successful or not.
}, ContextCompat.getMainExecutor(this)
```

Ở đây, hàm `[startFocusAndMetering()](https://developer.android.com/reference/androidx/camera/core/CameraControl#startFocusAndMetering(androidx.camera.core.FocusMeteringAction))` nhận một `FocusMeteringAction` được bao gồm một `MeteringPoint` cho các vùng đo sáng AF/AE/AWB và MeteringPoint khác cho chỉ AF và AE.

Bên trong, CameraX convert nó thành Camera2 `[MeteringRectangles](https://developer.android.com/reference/android/hardware/camera2/params/MeteringRectangle)` và thiết lập các parameter `[CONTROL_AF_REGIONS](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AF_REGIONS)` / `[CONTROL_AE_REGIONS](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AE_REGIONS)` / `[CONTROL_AWB_REGIONS](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AWB_REGIONS)` tương ứng cho capture request.

Vì không phải thiết bị nào cũng hỗ trợ AF/AE/AWB và nhiều vùng nên CameraX thực thi `FocusMeteringAction` với nỗ lực cao nhất. CameraX sử dụng số MeteringPoints tối đa được hỗ trợ, theo thứ tự các điểm được thêm vào. Tất cả các MeteringPoints được thêm sau điểm tối đa sẽ bị ignore. Ví dụ, nếu một `FocusMeteringAction` được cấp 3 MeteringPoints trên một nền tảng chỉ hỗ trợ 2, thì chỉ 2 cái MeteringPoints đầu được sử dụng. Cái `MeteringPoint` cuối cùng bị ignore bởi CameraX.

**Exposure Compensation**

Exposure compensation hữu dụng khi các app cần được fine-tune các exposure value (EV) vượt qua kết quả đầu ra auto exposure (AE). Các giá trị exposure compensation được gom vào để xác định độ phơi sáng cần thiết cho các điều kiện ảnh hiện tại theo cách sau:

$$
Exposure = ExposureCompensationIndex * ExposureCompensationStep
$$

CameraX cung cấp hàm `[Camera.CameraControl.setExposureCompensationIndex()](https://developer.android.com/reference/androidx/camera/core/CameraControl#setExposureCompensationIndex(int))` để thiết lập exposure compensation như một giá trị index.

Các giá trị dương làm hình ảnh sáng hơn và các giá trị âm làm mờ hình ảnh. Các app có thể query khoảng được hỗ trợ bằng `[CameraInfo.ExposureState.exposureCompensationRange()](https://developer.android.com/reference/androidx/camera/core/ExposureState#getExposureCompensationRange())`. Nếu giá trị được hỗ trợ, `ListenableFuture` được trả về sẽ hoàn thành khi giá trị được enable thành công trên capture request; Nếu index được chỉ định vượt quá khoảng hỗ trợ,  `setExposureCompensationIndex()` sẽ làm cho `ListenableFuture` mà được trả về hoàn thành ngay lập tức với một kết quả thất bại.

CameraX chỉ giữ lại `setExposureCompensationIndex()` request cuối cùng và gọi hàm nhiều lần trước khi reqest trước đó trả ra các kết quả đã được triển khai đẫn đến việc bị hủy bỏ.

Dưới đây là cách thiết lập một exposure compensation index và đăng ký một callback khi nào request thay đổi phơi sáng được thực thi:

```kotlin
camera.cameraControl.setExposureCompensationIndex(exposureCompensationIndex)
   .addListener({
      // Get the current exposure compensation index, it might be
      // different from the asked value in case this request was
      // canceled by a newer setting request.
      val currentExposureIndex = camera.cameraInfo.exposureState.exposureCompensationIndex
      …
   }, mainExecutor)
```

- `[Camera.CameraInfo.getExposureState()](https://developer.android.com/reference/androidx/camera/core/CameraInfo#getExposureState())` truy xuất `[ExposureState](https://developer.android.com/reference/androidx/camera/core/CameraInfo#getExposureState())` hiện tại bao gồm:
    - Khả năng hỗ trợ kiểm soát exposure compensation.
    - Exposure compensation index hiện tại.
    - Khoảng exposure compensation index.
    - Bước exposure compensation sử dụng trong việc tính toán exposure compensation value.

Ví dụ, dưới đây là cách khởi tạo các setting cho một `[SeekBar](https://developer.android.com/reference/android/widget/SeekBar)` với các giá `ExposureState` hiện tại:

```kotlin
val exposureState = camera.cameraInfo.exposureState
binding.seekBar.apply {
   isEnabled = exposureState.isExposureCompensationSupported
   max = exposureState.exposureCompensationRange.upper
   min = exposureState.exposureCompensationRange.lower
   progress = exposureState.exposureCompensationIndex
}
```

# Preview

Sử dụng `[PreviewView](https://developer.android.com/reference/kotlin/androidx/camera/view/PreviewView)` để thêm một preview vào trong app của bạn. `[PreviewView](https://developer.android.com/reference/kotlin/androidx/camera/view/PreviewView)` là một `View` có thể được crop, scale và rotate.

## **Use the PreviewView**

Các bước triển khai một preview cho CameraX:

1. Config một `[CameraXConfig.Provider](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraXConfig.Provider)`.
2. Thêm một `PreviewView` vào layout của bạn.
3. Request một `[ProcessCameraProvider](https://developer.android.com/reference/kotlin/androidx/camera/lifecycle/ProcessCameraProvider)`.
4. Kiểm tra `ProcessCameraProvider` trong quá trình tạo `View`.
5. Chọn một camera và ràng buộc lifecycle với các use case.

Hạn chế khi sử dụng `PreviewView`, bạn không được:

- Tạo một `SurfaceTexture` để thiết lập `TextureView` và `[Preview.SurfaceProvider](https://developer.android.com/reference/androidx/camera/core/Preview.SurfaceProvider)`.
- Truy xuất `SurfaceTexture` từ `TextureView` và thiết lập nó trong `Preview.SurfaceProvider`.
- Lấy `Surface` từ `SurfaceView` và thiết lập nó trong `Preview.SurfaceProvider`.

Nếu sảy ra các trường hợp trên,  `Preview` sẽ dừng khung hình phát trực tiếp của `PreviewView`.

### **Add a PreviewView to your layout**

```xml
<FrameLayout
    android:id="@+id/container">
        <androidx.camera.view.PreviewView
            android:id="@+id/previewView" />
</FrameLayout>
```

### **Request a CameraProvider**

```kotlin
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }
}
```

### **Check for CameraProvider availability**

Sau khi request một `CameraProvider`, xác thực xem khi view được tạo thì nó có được khởi tạo thành công hay không.

```kotlin
cameraProviderFuture.addListener(Runnable {
    val cameraProvider = cameraProviderFuture.get()
    bindPreview(cameraProvider)
}, ContextCompat.getMainExecutor(this))
```

Hàm bindPreview được triển khai bên dưới.

### **Select a camera and bind the lifecycle and use cases**

Khi `CameraProvider` đã được tạo và được xác nhận thì:

1. Tạo một `Preview`.
2. Chỉ định `LensFacing` mong muốn sử dụng.
3. Bind cái camera đã chọn với mọi use case vào lifecycle.
4. Kết nối `Preview` với `PreviewView`.

```kotlin
fun bindPreview(cameraProvider : ProcessCameraProvider) {
    var preview : Preview = Preview.Builder()
            .build()

    var cameraSelector : CameraSelector = CameraSelector.Builder()
          .requireLensFacing(CameraSelector.LENS_FACING_BACK)
          .build()

    preview.setSurfaceProvider(previewView.getSurfaceProvider())

    var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
}
```

Chú ý rằng `[bindToLifecycle()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#bindToLifecycle(androidx.lifecycle.LifecycleOwner,%20androidx.camera.core.CameraSelector,%20androidx.camera.core.UseCase...))` trả về một `[Camera](https://developer.android.com/reference/androidx/camera/core/Camera)` object.

## **Additional controls for PreviewView**

CameraX `PreviewView` cung cấp một số API để config các property như:

- The [implementation mode](https://developer.android.com/reference/androidx/camera/view/PreviewView.ImplementationMode) for rendering preview streams
- The preview image [scale type](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType)

### Implementation mode

`PreviewView` có thể sử dụng một trong các mode sau để render một preview stream vào trong `View`:

- `[PERFORMANCE](https://developer.android.com/reference/androidx/camera/view/PreviewView.ImplementationMode#PERFORMANCE)` là default mode. `PreviewView` dùng một `[SurfaceView](https://developer.android.com/reference/android/view/SurfaceView)` để hiện thị video stream, nhưng sẽ dùng `[TextureView](https://developer.android.com/reference/android/view/TextureView)` trong một số trường hợp nhất định ([certain cases](https://developer.android.com/reference/androidx/camera/view/PreviewView.ImplementationMode#PERFORMANCE)). `SurfaceView` có một bề mặt vẽ chuyên dụng dễ dàng được triển khai với một hardware overlay bởi [internal hardware compositor](https://source.android.com/devices/graphics/hwc), đặc biệt là khi không có các thành phần UI khác (như là button) trên đầu của prevew video. Bằng cách render với một hardware overlay, các video frame sẽ tránh một GPU path, điều này có thể làm giảm mức độ tiêu thụ và độ trễ của nền tảng.
- `[COMPATIBLE](https://developer.android.com/reference/androidx/camera/view/PreviewView.ImplementationMode#COMPATIBLE)` mode. Với mode này thì `PreviewView` sử dụng một `TextureView`. Không giống với `SurfaceView`,  `TextureView` không có bề mặt vẽ chuyên dụng. Kết quả là video được render với blending do đó nó có thể đươc hiển thị. Trong bước này, app có thể hiển thị một additional processing như là scaling và rotating các video mà không có hạn chế.

Sử dụng `[PreviewView.setImplementationMode()](https://developer.android.com/reference/androidx/camera/view/PreviewView#setImplementationMode(androidx.camera.view.PreviewView.ImplementationMode))` để chọn chế độ triển khai phù hợp với app của bạn. Nếu default `PERFORMANCE` mode không phù hợp với app của bạn thì dưới đây là cách triển khai `COMPATIBLE` mode:

```kotlin
// viewFinder is a PreviewView instance
viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
```

### Scale type

Khi độ phân giải của preview video khác với độ phân giải của `PreviewView` bạn mong muốn thì nội dung video cần được fit với view bằng cách cropping hoặc letterboxing (duy trì tỷ lệ khung hình ban đầu). `PreviewView` cung cấp các `[ScaleTypes](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType)` sau cho mục đích này:

- `[FIT_CENTER](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FIT_CENTER)`, `[FIT_START](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FIT_START)`, and `[FIT_END](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FIT_END)` for letterboxing. Toàn bộ nội dung video được scale (cả lên cả xuống) để tối ưu size có thể hiển thị được trong `PreviewView` mong muốn. Tuy nhiên, khi toàn bộ video frame được hiển thị, một số phần của màn hình có thể bị trống vắng. Tùy thuộc vào một trong ba loại scale mà bạn chọn, video frame sẽ được align ra center, start hoặc end của View mong muốn.
- `[FILL_CENTER](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FILL_CENTER)`, `[FILL_START](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FILL_START)`, `[FILL_END](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType#FILL_END)` cho cropping. Nếu một video không khớp với tỉ lệ khung hình của  `PreviewView`, chỉ một phần của nội dung được hiển thị, nhưng video sẽ fill trong toàn bộ `PreviewView`.

Default scale type mà CameraX sử dụng là `FILL_CENTER`. Dùng `[PreviewView.setScaleType()](https://developer.android.com/reference/androidx/camera/view/PreviewView#setScaleType(androidx.camera.view.PreviewView.ScaleType))` để thiết lập scale type phù hợp cho app của bạn. Dưới đây là cách thiết lập `FIT_CENTER` scale type:

```kotlin
// viewFinder is a PreviewView instance
viewFinder.scaleType = PreviewView.ScaleType.FIT_CENTER
```

Quá trình hiển thị một video bao gồm các bước:

1. Scale the video:
    - For `FIT_*` scale types, scale the video with `min(dst.width/src.width, dst.height/src.height)`.
    - For `FILL_*` scale types, scale the video with `max(dst.width/src.width, dst.height/src.height)`.
2. Align the scaled video with the destination `PreviewView`:
    - For `FIT_CENTER/FILL_CENTER`, center align the scaled video and the destination `PreviewView`.
    - For `FIT_START/FILL_START`, align the scaled video and the destination `PreviewView` with respect to the top-left corner of each.
    - For `FIT_END/FILL_END`, align the scaled video and the destination `PreviewView` with respect to the bottom-right corner of each.

Ví dụ, đây là một video 640x480 và một 1920x1080 destination `PreviewView`:

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F7fb6f5de-9e29-4d76-9ccd-e945ec83e2dc%2FUntitled.png?id=8c19377e-67d7-4cdd-ad02-4a5942bd7212&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

The following image shows the `FIT_START` / `FIT_CENTER` / `FIT_END` scaling process:

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F31e43a26-bad6-4aff-843e-39aa6413ec53%2FUntitled.png?id=2110825c-467c-4a6a-a458-0f6f212dd2c6&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

Process hoạt động như sau:

1. Scale the video frame (duy trì tỉ lệ khung hình ban đầu) với `min(1920/640, 1080/480) = 2.25` để có được một intermediate video frame 1440x1080.
2. Align 1440x1080 video frame với 1920x1080 `PreviewView`.
    - For `FIT_CENTER`, align the video frame with the **center** of the `PreviewView` window. The starting and ending 240 pixel columns of the `PreviewView` are blank.
    - For `FIT_START`, align the video frame with the **start** (top-left corner) of the `PreviewView` window. The ending 480 pixel columns of the `PreviewView` are blank.
    - For `FIT_END`, align the video frame with the **end** (bottom-right corner) of the `PreviewView` window. The starting 480 pixel columns of the `PreviewView` are blank.

The following image shows the `FILL_START` / `FILL_CENTER` / `FILL_END` scaling process:

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F8a7082a0-0b9a-4eb8-8156-6760319d4bae%2FUntitled.png?id=8687f7cd-5393-4154-8f04-843eddc46eb3&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

The process works like this:

1. Scale the video frame with `max(1920/640, 1080/480) = 3` to get an intermediate video frame of 1920x1440 (which is larger than the size of the `PreviewView`).
2. Crop the 1920x1440 video frame to fit the 1920x1080 `PreviewView` window.
    - For `FILL_CENTER`, crop 1920x1080 from the **center** of the 1920x1440 scaled video. The top and bottom 180 lines of video are not visible.
    - For `FILL_START`, crop 1920x1080 from the **start** of the 1920x1440 scaled video. The bottom 360 lines of video are not visible.
    - For `FILL_END`, crop 1920x1080 from the **end** of the 1920x1440 scaled video. The top 360 lines of video are not visible.

# Image capture

Image capture use case được thiết kế chụp các bức hình có độ phân giải cao, chât lượng cao và cung cấp các chức năng auto-white-balance, auto-exposure, và auto-focus (3A). Để chụp ảnh thì có các tùy chọn sau:

- `[takePicture(Executor, OnImageCapturedCallback)](https://developer.android.com/reference/androidx/camera/core/ImageCapture#takePicture(java.util.concurrent.Executor,%20androidx.camera.core.ImageCapture.OnImageCapturedCallback))`: phương thức này cung cấp một in-memory buffer của các ảnh được chụp.
- `[takePicture(OutputFileOptions, Executor, OnImageSavedCallback)](https://developer.android.com/reference/androidx/camera/core/ImageCapture#takePicture(androidx.camera.core.ImageCapture.OutputFileOptions,%20java.util.concurrent.Executor,%20androidx.camera.core.ImageCapture.OnImageSavedCallback))`: phương thức này lưu ảnh được chụp vào địa chỉ file được cung cấp.

Có hai kiểu customizable executor mà `ImageCapture` chạy trên đó là callback executor và IO executor.

- Callback executor là parameter của các phương thức `takePicture`. Nó được sử dụng để thi hành user-provided `[OnImageCapturedCallback()](https://developer.android.com/reference/androidx/camera/core/ImageCapture.OnImageCapturedCallback)`.
- Nếu người gọi chọn lưu hình ảnh vào một vị trí file, bạn có thể chỉ định một executor để làm IO. Để thiết lập IO executor, gọi `[ImageCapture.Builder.setIoExecutor(Executor)](https://developer.android.com/reference/androidx/camera/core/ImageCapture.Builder#setIoExecutor(java.util.concurrent.Executor))`. Nếu executor không hoạt động, CameraX sẽ mặc định một internal IO executor cho task.

## **Set up image capture**

Image capture cung cấp các điều khiển cơ bản để chụp ảnh như là flash, liên tục tự động lấy nét, zero-shutter lag ,v.v…

### **setCaptureMode()**

Dùng `[ImageCapture.Builder.setCaptureMode()](https://developer.android.com/reference/androidx/camera/core/ImageCapture.Builder#setCaptureMode(int))` để config chế độ chụp khi chụp ảnh:

- `[CAPTURE_MODE_MINIMIZE_LATENCY](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY())`: tối ưu hóa độ trễ chụp ảnh.
- `[CAPTURE_MODE_MAXIMIZE_QUALITY](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_MAXIMIZE_QUALITY())`: tối ưu hóa chất lượng chụp ảnh.

Chế độ mặc định là `[CAPTURE_MODE_MINIMIZE_LATENCY](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY())`.

### **Zero-Shutter Lag**

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> Đây là một tính năng thử nghiệm

</aside>

Bắt đầu từ 1.2, Zero-Shutter Lag (`[CAPTURE_MODE_ZERO_SHOT_LAG](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_ZERO_SHUTTER_LAG())`) khả dụng. Khi Zero-Shutter Lag được kích hoạt, độ trễ giảm đáng kể so với chụp ảnh mặc định `[CAPTURE_MODE_MINIMIZE_LATENCY](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_MINIMIZE_LATENCY())` nên bạn sẽ không bao giờ miss the shot.

Zero-Shutter Lag sử dụng bộ đệm vòng lưu trữ ba khung hình chụp gần đây nhất. Khi người dùng nhấn nút chụp, CameraX sẽ gọi `[takePicture()](https://developer.android.com/reference/android/hardware/Camera#takePicture(android.hardware.Camera.ShutterCallback,%20android.hardware.Camera.PictureCallback,%20android.hardware.Camera.PictureCallback,%20android.hardware.Camera.PictureCallback))`, và bộ đệm vòng sẽ truy xuất khung hình đã chụp có timestamp gần nhất với timestamp của lần nhấn nút. Sau đó, CameraX sẽ xử lý lại phiên chụp để tạo ảnh từ khung hình đó, ảnh này được lưu vào đĩa ở định dạng JPEG.

**Prerequisites**

Trước khi bật Zero-Shutter Lag, hãy sử dụng `[isZslSupported()](https://developer.android.com/reference/androidx/camera/core/CameraInfo#isZslSupported())` để xác định xem thiết bị của bạn có đáp ứng các yêu cầu sau không:

- Targets Android 6.0+ (API level 23 and higher).
- Supports `[PRIVATE` reprocessing](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING).

Zero-Shutter Lag chỉ khả dụng cho trường hợp sử dụng Chụp ảnh. Bạn không thể bật tính năng này cho trường hợp sử dụng Quay video hoặc với tiện ích Máy ảnh. Cuối cùng, vì sử dụng đèn flash dẫn đến độ trễ lớn hơn, Độ trễ màn trập không hoạt động khi đèn flash BẬT hoặc ở chế độ TỰ ĐỘNG

**Enable Zero-Shutter Lag**

Để bật Zero-Shutter Lag, hãy chuyền `[CAPTURE_MODE_ZERO_SHOT_LAG](https://developer.android.com/reference/androidx/camera/core/ImageCapture#CAPTURE_MODE_ZERO_SHUTTER_LAG())` cho `[ImageCapture.Builder.setCaptureMode()](https://developer.android.com/reference/androidx/camera/core/ImageCapture.Builder#setCaptureMode(int))`. Nếu không thành công, `setCaptureMode()` sẽ trở về `CAPTURE_MODE_MINIMIZE_LATENCY`.

### **setFlashMode()**

Chế độ flash mặc định là `[FLASH_MODE_OFF](https://developer.android.com/reference/androidx/camera/core/ImageCapture#FLASH_MODE_OFF())`. Để đặt chế độ flash, hãy sử dụng `[ImageCapture.Builder.setFlashMode()](https://developer.android.com/reference/androidx/camera/core/ImageCapture.Builder#setFlashMode(int))`:

- `[FLASH_MODE_ON](https://developer.android.com/reference/androidx/camera/core/ImageCapture#FLASH_MODE_ON())`: Flash luôn bật.
- `[FLASH_MODE_AUTO](https://developer.android.com/reference/androidx/camera/core/ImageCapture#FLASH_MODE_AUTO())`: Đèn flash tự động bật khi chụp ảnh thiếu sáng

## **Take photo**

```kotlin
val imageCapture = ImageCapture.Builder()
    .setTargetRotation(view.display.rotation)
    .build()

cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture,
    imageAnalysis, preview)
```

Lưu ý rằng `[bindToLifecycle()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#bindToLifecycle(androidx.lifecycle.LifecycleOwner,%20androidx.camera.core.CameraSelector,%20androidx.camera.core.UseCase...))` trả về một `[Camera](https://developer.android.com/reference/androidx/camera/core/Camera)` object. Xem [this guide](https://developer.android.com/training/camerax/configuration#camera-output) để biết thêm thông tin về cách kiểm soát đầu ra của máy ảnh, chẳng hạn như thu phóng và độ phơi sáng.

Khi bạn đã định cấu hình máy ảnh, đoạn mã sau sẽ chụp ảnh dựa trên hành động của người dùng:

```kotlin
fun onClick() {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(...)).build()
    imageCapture.takePicture(outputFileOptions, cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(error: ImageCaptureException)
            {
                // insert your code here.
            }
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // insert your code here.
            }
        })
}
```

Phương thức chụp ảnh hỗ trợ đầy đủ định dạng `[JPEG](https://developer.android.com/reference/android/graphics/ImageFormat#JPEG)`. Để biết mã mẫu cho biết cách chuyển đổi đối tượng `[Media.Image](https://developer.android.com/reference/android/media/Image)` từ định dạng `YUV_420_888` sang đối tượng `[Bitmap](https://developer.android.com/reference/android/graphics/Bitmap)`, hãy xem `[YuvToRgbConverter.kt](https://github.com/android/camera-samples/blob/3730442b49189f76a1083a98f3acf3f5f09222a3/CameraUtils/lib/src/main/java/com/example/android/camera/utils/YuvToRgbConverter.kt)`.

# ****Image analysis****

The [image analysis](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Analyzer#analyze(androidx.camera.core.ImageProxy)) use case cung cấp cho ứng dụng của bạn một CPU-accessible image mà có thể image processing, computer vision, hoặc machine learning. App triển khai một phương thức `[analyze()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Analyzer#analyze(androidx.camera.core.ImageProxy))` chạy trên mỗi frame.

## **Operating Modes**

Khi quy trình phân tích của ứng dụng không thể theo kịp các yêu cầu về tốc độ khung hình của CameraX, CameraX có thể được định cấu hình để giảm khung hình theo một trong các cách sau:

- ***non-blocking*** (default): Trong chế độ này, executor luôn lưu trữ hình ảnh mới nhất vào image buffer (tương tự như hàng đợi có độ sâu là một) trong khi ứng dụng phân tích hình ảnh trước đó. Nếu CameraX nhận được một hình ảnh mới trước khi ứng dụng xử lý xong, hình ảnh mới sẽ được lưu vào cùng một buffer, ghi đè lên hình ảnh trước đó. Lưu ý rằng `ImageAnalysis.Builder.setImageQueueDepth()` không có tác dụng trong trường hợp này và nội dung buffer luôn có thể bị ghi đè. Bạn có thể bật chế độ không chặn này bằng cách gọi `setBackpressureStrategy()` với `[STRATEGY_KEEP_ONLY_LATEST](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#STRATEGY_KEEP_ONLY_LATEST)`.
- ***blocking***: Trong chế độ này, internal executor có thể thêm nhiều hình ảnh vào hàng đợi hình ảnh bên trong và chỉ bắt đầu giảm khung hình khi hàng đợi đầy. Việc chặn xảy ra trên toàn bộ phạm vi thiết bị máy ảnh: nếu thiết bị máy ảnh có nhiều use case sử dụng bị ràng buộc, tất cả các use case sử dụng đó sẽ bị chặn trong khi CameraX đang xử lý những hình ảnh này. Ví dụ: khi cả preview và image analysis được liên kết với một thiết bị Máy ảnh, thì preview cũng sẽ bị chặn trong khi CameraX đang xử lý hình ảnh. Bạn có thể kích hoạt blocking mode bằng cách chuyền `[STRATEGY_BLOCK_PRODUCER](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#strategy_block_producer)` tới `[setBackpressureStrategy()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setBackpressureStrategy(int))`. Bạn cũng có thể định cấu hình độ sâu hàng đợi hình ảnh bằng cách sử dụng [ImageAnalysis.Builder.setImageQueueDepth()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setImageQueueDepth(int)).

Với độ trễ thấp và bộ phân tích hiệu suất cao, trong đó tổng thời gian để phân tích hình ảnh nhỏ hơn thời lượng của khung CameraX (ví dụ: 16 mili giây cho 60 khung hình/giây), cả hai chế độ vận hành đều mang lại trải nghiệm tổng thể mượt mà. Blocking mode vẫn có thể hữu ích trong một số trường hợp, chẳng hạn như khi xử lý sự cố hệ thống rất ngắn.

Với bộ phân tích hiệu suất cao và độ trễ cao, blocking mode với hàng đợi dài hơn là cần thiết để bù cho độ trễ. Tuy nhiên, xin lưu ý rằng ứng dụng vẫn có thể xử lý tất cả các khung hình.

Với bộ phân tích có độ trễ cao và tốn nhiều thời gian (bộ phân tích không thể xử lý tất cả các khung), non-blocking mode có thể là lựa chọn phù hợp hơn, vì các khung phải được loại bỏ cho đường dẫn phân tích, nhưng vẫn có thể thấy các trường hợp sử dụng bị ràng buộc đồng thời khác tất cả các khung.

## **Implementation**

Các bước:

- Build an `[ImageAnalysis](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis)` use case.
- Create an `[ImageAnalysis.Analyzer](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Analyzer)`.
- [Set your analyzer](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#setAnalyzer(java.util.concurrent.Executor,%20androidx.camera.core.ImageAnalysis.Analyzer)) to your `ImageAnalysis`.
- [Bind](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#bindToLifecycle(androidx.lifecycle.LifecycleOwner,%20androidx.camera.core.CameraSelector,%20androidx.camera.core.UseCase...)) your lifecycle owner, camera selector, and `ImageAnalysis` use case to the lifecycle.

Ngay sau khi liên kết, CameraX sẽ gửi hình ảnh đến analyzer đã đăng ký của bạn. Sau khi hoàn thành phân tích, hãy gọi `[ImageAnalysis.clearAnalyzer()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#clearAnalyzer())` hoặc hủy liên kết `ImageAnalysis` use case để dừng phân tích.

### **Build ImageAnalysis use case**

`[ImageAnalysis](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis)` kết nối analyzer của bạn (image consumer) với CameraX, là công cụ sản xuất hình ảnh. Các ứng dụng có thể sử dụng `ImageAnalysis` object`[ImageAnalysis.Builder](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder)` để xây dựng một `ImageAnalysis` object. Với `ImageAnalysis.Builder`, ứng dụng có thể định cấu hình như sau:

- Image output parameters:
    - Format: CameraX supports `[YUV_420_888](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#OUTPUT_IMAGE_FORMAT_YUV_420_888)` and `[RGBA_8888](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis#OUTPUT_IMAGE_FORMAT_RGBA_8888)` through `[setOutputImageFormat(int)](https://developer.android.com/reference/kotlin/androidx/camera/core/ImageAnalysis.Builder#setOutputImageFormat(kotlin.Int))`. The default format is `YUV_420_888`.
    - [Resolution](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetResolution(android.util.Size)) and [AspectRatio](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetAspectRatio(int)): You can set either of these parameters, but note that you can't set both values at the same time.
    - [Rotation](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetRotation(int)).
    - [Target Name](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetName(java.lang.String)): Use this parameter for debugging purposes.
- Image flow controls:
    - [Background executor](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setBackgroundExecutor(java.util.concurrent.Executor))
    - [Image queue (between analyzer and CamaraX) depth](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setImageQueueDepth(int))
    - [Back pressure strategy](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setBackpressureStrategy(int))

Các ứng dụng có thể đặt độ phân giải hoặc tỷ lệ khung hình, nhưng không thể đặt cả hai. Độ phân giải đầu ra chính xác tùy thuộc vào kích thước (hoặc tỷ lệ khung hình) và khả năng phần cứng được yêu cầu của ứng dụng và có thể khác với kích thước hoặc tỷ lệ được yêu cầu. Để biết thông tin về thuật toán so khớp độ phân giải, hãy xem tài liệu về `[setTargetResolution()](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetResolution(android.util.Size))`.

Một ứng dụng có thể định cấu hình các pixel hình ảnh đầu ra ở không gian màu YUV (mặc định) hoặc RGBA. Khi đặt định dạng đầu ra RGBA, CameraX sẽ chuyển đổi bên trong hình ảnh từ không gian màu YUV sang RGBA và đóng gói các bit hình ảnh vào `[ByteBuffer](https://developer.android.com/reference/androidx/camera/core/ImageProxy.PlaneProxy#getBuffer())` của mặt phẳng đầu tiên của ImageProxy (hai mặt phẳng còn lại không được sử dụng) theo trình tự sau:

```kotlin
ImageProxy.getPlanes()[0].buffer[0]: alpha
ImageProxy.getPlanes()[0].buffer[1]: red
ImageProxy.getPlanes()[0].buffer[2]: green
ImageProxy.getPlanes()[0].buffer[3]: blue
...
```

Khi thực hiện phân tích hình ảnh phức tạp mà thiết bị không thể theo kịp tốc độ khung hình, bạn có thể định cấu hình CameraX để giảm khung hình bằng các chiến lược được mô tả trong phần [Operating Modes](https://developer.android.com/training/camerax/analyze#operating_modes) của chủ đề này.

### **Create your analyzer**

Các ứng dụng có thể tạo anaylyzer bằng cách triển khai `[ImageAnalysis.Analyzer](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Analyzer)` interface và overriding `[analyze(ImageProxy image)](https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Analyzer#analyze(androidx.camera.core.ImageProxy))`. Trong mỗi analyzer, các ứng dụng nhận được một `[ImageProxy](https://developer.android.com/reference/androidx/camera/core/ImageProxy)`, đây là wrapper của [Media.Image](https://developer.android.com/reference/android/media/Image). Định dạng hình ảnh có thể được truy vấn bằng `[ImageProxy.getFormat()](https://developer.android.com/reference/androidx/camera/core/ImageProxy#getFormat())`. Định dạng là một trong những giá trị sau mà ứng dụng cung cấp với `ImageAnalysis.Builder`:

- `ImageFormat.RGBA_8888` if the app requested `OUTPUT_IMAGE_FORMAT_RGBA_8888`.
- `ImageFormat.YUV_420_888` if the app requested `OUTPUT_IMAGE_FORMAT_YUV_420_888`.

Bên trong analyzer, ứng dụng sẽ thực hiện những việc sau:

1. Phân tích một khung hình nhất định càng nhanh càng tốt, tốt nhất là trong giới hạn thời gian tốc độ khung hình nhất định (ví dụ: dưới 32 mili giây đối với trường hợp 30 khung hình/giây). Nếu ứng dụng không thể phân tích khung đủ nhanh, hãy xem xét một trong các cơ chế loại bỏ khung được hỗ trợ ([supported frame dropping mechanisms](https://developer.android.com/training/camerax/analyze#operating_modes)).
2. Phát hành `ImageProxy` cho CameraX bằng cách gọi `[ImageProxy.close()](https://developer.android.com/reference/androidx/camera/core/ImageProxy#close())`. Lưu ý rằng bạn không nên gọi hàm đóng của Media.Image (`Media.Image.close()`).

Các ứng dụng có thể sử dụng trực tiếp `Media.Image` bên trong ImageProxy. Chỉ cần không gọi `Media.Image.close()` trên hình ảnh được bao bọc vì điều này sẽ phá vỡ cơ chế chia sẻ hình ảnh bên trong CameraX; thay vào đó, hãy sử dụng `[ImageProxy.close()](https://developer.android.com/reference/androidx/camera/core/ImageProxy#close())` để giải phóng `Media.Image` bên dưới cho CameraX.

### **Configure your analyzer for ImageAnalysis**

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> **Lưu ý**: Bước này là chung cho tất cả các trường hợp sử dụng CameraX. Xem mô hình API CameraX để biết thêm thông tin về ràng buộc và tùy chỉnh vòng đời.

</aside>

Bạn nên liên kết `ImageAnalysis` của mình với vòng đời AndroidX hiện có bằng hàm `[ProcessCameraProvider.bindToLifecycle()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#bindToLifecycle(androidx.lifecycle.LifecycleOwner,%20androidx.camera.core.CameraSelector,%20androidx.camera.core.UseCase...))`. Lưu ý rằng hàm `bindToLifecycle()` trả về thiết bị `[Camera](https://developer.android.com/reference/androidx/camera/core/Camera)` đã chọn, có thể được sử dụng để tinh chỉnh các cài đặt nâng cao như độ phơi sáng và các cài đặt khác. Xem [hướng dẫn này](https://developer.android.com/training/camerax/configuration#camera-output) để biết thêm thông tin về cách kiểm soát đầu ra của camera.

Ví dụ sau đây kết hợp mọi thứ từ các bước trước đó, ràng buộc các trường hợp sử dụng CameraX `ImageAnalysis` và `Preview` với chủ sở hữu `lifeCycle`:

```kotlin
val imageAnalysis = ImageAnalysis.Builder()
    // enable the following line if RGBA output is needed.
    // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
    .setTargetResolution(Size(1280, 720))
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    // insert your code here.
    ...
    // after done, release the ImageProxy object
    imageProxy.close()
})

cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
```

# Video capture

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> **Lưu ý**: Code samples in this topic correspond with the **`camera-video:1.1.0-alpha12`** release.

</aside>

Một hệ thống chụp thường ghi lại các luồng video và âm thanh, nén chúng, kết hợp hai luồng, sau đó ghi luồng kết quả vào đĩa.

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2Fdbc7cd0a-1503-4180-adcc-4b4460b85045%2FUntitled.png?id=4e085056-38b8-4f79-9147-8e6443762b91&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

Hình 1. Sơ đồ khái niệm cho hệ thống thu video và âm thanh.

Trong CameraX, giải pháp quay video là use case sử dụng `[VideoCapture](https://developer.android.com/reference/androidx/camera/video/VideoCapture)`:

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2F23341886-0805-4a22-a765-2d550f0eb27f%2FUntitled.png?id=d6b6b568-bfa7-401a-ae21-01943277c59f&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

Hình 2. Sơ đồ khái niệm cho thấy cách CameraX xử lý trường hợp sử dụng `VideoCapture`.

Như thể hiện trong hình 2, video capture của CameraX bao gồm một số thành phần kiến trúc cấp cao:

- `SurfaceProvider` for the video source.
- `AudioSource` for audio source.
- Two encoders to encode and compress video/audio.
- A media muxer to mux the two streams.
- A file saver to write out the result.

API VideoCapture trừu tượng hóa công cụ ghi hình phức tạp và cung cấp cho các ứng dụng một API đơn giản và dễ hiểu hơn nhiều.

## **VideoCapture API overview**

`VideoCapture` là trường hợp sử dụng CameraX hoạt động tốt khi hoạt động độc lập hoặc khi được kết hợp với các use case khác. Các kết hợp cụ thể được hỗ trợ tùy thuộc vào khả năng của phần cứng máy ảnh, nhưng `Preview` và `VideoCapture` là hỗn hợp use case hợp lệ trên tất cả các thiết bị.

<aside>
<img src="https://www.notion.so/icons/star_blue.svg" alt="https://www.notion.so/icons/star_blue.svg" width="40px" /> **Lưu ý**: VideoCapture được triển khai trong thư viện camera-video bên trong gói CameraX, có sẵn trong các phiên bản 1.1.0-alpha10 trở lên.

</aside>

API VideoCapture bao gồm các đối tượng sau giao tiếp với các ứng dụng:

- `[VideoCapture](https://developer.android.com/reference/androidx/camera/video/VideoCapture)` is the top-level use case class. `VideoCapture` binds to a `LifecycleOwner` with a `CameraSelector` and other CameraX UseCases. For more information about these concepts and usages, see [CameraX Architecture](https://developer.android.com/training/camerax/architecture).
- A `[Recorder](https://developer.android.com/reference/androidx/camera/video/Recorder)` is an implementation of VideoOutput that is tightly coupled with `VideoCapture`. `Recorder` is used to perform the video and audio capturing. An application **creates** recordings from a `Recorder`.
- A `[PendingRecording](https://developer.android.com/reference/androidx/camera/video/PendingRecording)` configures a recording, providing options like enabling audio and setting an event listener. You must use a `Recorder` to create a `PendingRecording`. A `PendingRecording` does not record anything.
- A `[Recording](https://developer.android.com/reference/androidx/camera/video/Recording)` performs the actual recording. You must use a `PendingRecording` to create a `Recording`.

![Untitled](notion://www.notion.so/image/https%3A%2F%2Fs3-us-west-2.amazonaws.com%2Fsecure.notion-static.com%2Ff462b5e7-336e-4cc0-b7a4-74fe62bf0d0a%2FUntitled.png?id=fd7d0b16-b699-421b-9944-4f348edc6bbe&table=block&spaceId=4cd0a48a-ba09-4cb5-8603-7a4568ff4417&width=2000&userId=2eba6c44-7819-460d-a5d8-71580e90da06&cache=v2)

Hình 3. Sơ đồ hiển thị các tương tác xảy ra trong VideoCapture use case.

**Legend:**

1. Create a `[Recorder](https://developer.android.com/reference/androidx/camera/video/Recorder)` with `[QualitySelector](https://developer.android.com/reference/androidx/camera/video/QualitySelector)`.
2. Configure the `Recorder` with one of the `[OutputOptions](https://developer.android.com/reference/androidx/camera/video/OutputOptions)`.
3. Enable audio with `[withAudioEnabled()](https://developer.android.com/reference/androidx/camera/video/PendingRecording#withAudioEnabled())` if needed.
4. Call `[start()](https://developer.android.com/reference/androidx/camera/video/PendingRecording#start())` with a `[VideoRecordEvent](https://developer.android.com/reference/androidx/camera/video/VideoRecordEvent)` listener to begin recording.
5. Use `pause()`/`resume()`/`stop()` on the `[Recording](https://developer.android.com/reference/androidx/camera/video/Recording)` to control the recording.
6. Respond to `[VideoRecordEvents](https://developer.android.com/reference/androidx/camera/video/VideoRecordEvent)` inside your event listener.

## **Using the VideoCapture API**

Để tích hợp CameraX `VideoCapture` use case vào ứng dụng của bạn, hãy làm như sau:

1. Bind `VideoCapture`.
2. Prepare and configure recording.
3. Start and control the runtime recording.

Các phần sau đây phác thảo những gì bạn có thể làm ở mỗi bước để có được phiên ghi từ đầu đến cuối.

### **Bind VideoCapture**

To bind the `VideoCapure` use case, do the following:

1. Create a `Recorder` object.
2. Create `VideoCapture` object.
3. Bind to a `Lifecycle`.

API CameraX VideoCapture tuân theo mẫu thiết kế của trình tạo. Các ứng dụng sử dụng `Recorder.Builder` để tạo một `Recorder`. Bạn cũng có thể định cấu hình độ phân giải video cho `Recorder` thông qua đối tượng `QualitySelector`.

CameraX `Recorder` hỗ trợ các `[Qualities](https://developer.android.com/reference/androidx/camera/video/Quality)` được xác định trước sau đây cho độ phân giải video:

- `Quality.UHD` for 4K ultra HD video size (2160p)
- `Quality.FHD` for full HD video size (1080p)
- `Quality.HD` for HD video size (720p)
- `Quality.SD` for SD video size (480p)

Lưu ý rằng CameraX cũng có thể chọn các độ phân giải khác khi được ứng dụng cho phép.

Kích thước video chính xác của mỗi lựa chọn tùy thuộc vào khả năng của máy ảnh và bộ mã hóa. Để biết thêm thông tin, hãy xem tài liệu về `[CamcorderProfile](https://developer.android.com/reference/android/media/CamcorderProfile)`.

Các ứng dụng có thể định cấu hình độ phân giải bằng cách tạo `[QualitySelector](https://developer.android.com/reference/androidx/camera/video/QualitySelector)`. Bạn có thể tạo `[QualitySelector](https://developer.android.com/reference/androidx/camera/video/QualitySelector)` bằng một trong các phương pháp sau:

- Cung cấp một vài giải pháp ưu tiên bằng cách sử dụng `fromOrderedList()` và bao gồm một chiến lược dự phòng để sử dụng trong trường hợp không có giải pháp ưu tiên nào được hỗ trợ.
  CameraX có thể quyết định kết hợp dự phòng tốt nhất dựa trên khả năng của máy ảnh đã chọn, hãy tham khảo `[FallbackStrategy specification](https://developer.android.com/reference/androidx/camera/video/FallbackStrategy)` của `QualitySelector` để biết thêm chi tiết. Ví dụ: đoạn mã sau yêu cầu độ phân giải được hỗ trợ cao nhất để ghi và nếu không có độ phân giải yêu cầu nào có thể được hỗ trợ, hãy ủy quyền cho CameraX chọn độ phân giải gần nhất với độ phân giải Quality.SD:

    ```kotlin
    val qualitySelector = QualitySelector.fromOrderedList(
             listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
             FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
    ```

- Trước tiên hãy truy vấn các khả năng của máy ảnh và chọn từ các độ phân giải được hỗ trợ bằng cách sử dụng `QualitySelector::from()`:

    ```kotlin
    val cameraInfo = cameraProvider.availableCameraInfos.filter {
        Camera2CameraInfo
        .from(it)
        .getCameraCharacteristic(CameraCharacteristics.LENS\_FACING) == CameraMetadata.LENS_FACING_BACK
    }
    
    val supportedQualities = QualitySelector.getSupportedQualities(cameraInfo[0])
    val filteredQualities = arrayListOf (Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                           .filter { supportedQualities.contains(it) }
    
    // Use a simple ListView with the id of simple_quality_list_view
    viewBinding.simpleQualityListView.apply {
        adapter = ArrayAdapter(context,
                               android.R.layout.simple_list_item_1,
                               filteredQualities.map { it.qualityToString() })
    
        // Set up the user interaction to manually show or hide the system UI.
        setOnItemClickListener { _, _, position, _ ->
            // Inside View.OnClickListener,
            // convert Quality.* constant to QualitySelector
            val qualitySelector = QualitySelector.from(filteredQualities[position])
    
            // Create a new Recorder/VideoCapture for the new quality
            // and bind to lifecycle
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector).build()
    
             // ...
        }
    }
    
    // A helper function to translate Quality to a string
    fun Quality.qualityToString() : String {
        return when (this) {
            Quality.UHD -> "UHD"
            Quality.FHD -> "FHD"
            Quality.HD -> "HD"
            Quality.SD -> "SD"
            else -> throw IllegalArgumentException()
        }
    }
    ```

  Lưu ý rằng khả năng trả về từ `[QualitySelector.getSupportedQualities()](https://developer.android.com/reference/androidx/camera/video/QualitySelector#getSupportedQualities(androidx.camera.core.CameraInfo))` được đảm bảo hoạt động cho trường hợp sử dụng `VideoCapture` hoặc kết hợp các use case `VideoCapture` và `Preview`. Khi ràng buộc cùng với  `ImageCapture`hoặc `ImageAnalysis` use case, CameraX vẫn có thể không ràng buộc được khi hỗn hợp được yêu cầu không được hỗ trợ trên máy ảnh được yêu cầu.


Sau khi bạn có `QualitySelector`, ứng dụng có thể tạo đối tượng `VideoCapture` và thực hiện ràng buộc. Lưu ý rằng ràng buộc này giống như với các trường hợp sử dụng khác:

```kotlin
val recorder = Recorder.Builder()
    .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
    .build()
val videoCapture = VideoCapture.withOutput(recorder)

try {
    // Bind use cases to camera
    cameraProvider.bindToLifecycle(
            this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture)
} catch(exc: Exception) {
    Log.e(TAG, "Use case binding failed", exc)
}
```

Lưu ý rằng `[bindToLifecycle()](https://developer.android.com/reference/androidx/camera/lifecycle/ProcessCameraProvider#bindToLifecycle(androidx.lifecycle.LifecycleOwner,%20androidx.camera.core.CameraSelector,%20androidx.camera.core.UseCase...))` trả về một đối tượng `[Camera](https://developer.android.com/reference/androidx/camera/core/Camera)`. Xem [hướng dẫn này](https://developer.android.com/training/camerax/configuration#camera-output) để biết thêm thông tin về cách kiểm soát đầu ra của máy ảnh, chẳng hạn như thu phóng và độ phơi sáng.

<aside>
💡 **Lưu ý**: Định dạng vùng chứa và codec video cuối cùng hiện không thể định cấu hình được.

</aside>

`Recorder` chọn định dạng phù hợp nhất cho hệ thống. Bộ giải mã video phổ biến nhất là [H.264 AVC](https://developer.android.com/reference/android/media/MediaFormat#MIMETYPE_VIDEO_AVC)) với định dạng bộ chứa [MPEG-4](https://developer.android.com/reference/android/media/MediaFormat#MIMETYPE_VIDEO_MPEG4).

## **Configure and create recording**

Từ `Recorder`, ứng dụng có thể tạo các đối tượng ghi để thực hiện ghi video và âm thanh. Các ứng dụng tạo bản ghi bằng cách thực hiện như sau:

1. Configure `OutputOptions` with the `prepareRecording()`.
2. (Optional) Enable audio recording.
3. Use `start()` to register a `[VideoRecordEvent](https://developer.android.com/reference/androidx/camera/video/VideoRecordEvent)` listener, and begin video capturing.

`Recorder` trả về một đối tượng `Recording` khi bạn gọi hàm `start()`. Ứng dụng của bạn có thể sử dụng đối tượng `Recording` này để hoàn tất việc chụp hoặc để thực hiện các hành động khác, chẳng hạn như tạm dừng hoặc tiếp tục.

`Recorder` hỗ trợ một đối tượng `Recording` tại một thời điểm. Bạn có thể bắt đầu một bản ghi mới sau khi đã gọi `Recording.stop()` hoặc `Recording.close()` trên đối tượng `Recording` trước đó.

Hãy xem xét các bước này chi tiết hơn. Đầu tiên, ứng dụng định cấu hình `OutputOptions` cho Recorder với `Recorder.prepareRecording()`. `Recorder` hỗ trợ các loại `OutputOptions`:

- `FileDescriptorOutputOptions` for capturing into a `[FileDescriptor](https://developer.android.com/reference/java/io/FileDescriptor)`.
- `FileOutputOptions` for capturing into a `[File](https://developer.android.com/reference/java/io/File)`.
- `MediaStoreOutputOptions` for capturing into a `[MediaStore](https://developer.android.com/reference/android/provider/MediaStore)`.

Tất cả các loại `OutputOptions` cho phép bạn đặt kích thước tệp tối đa với `setFileSizeLimit()`. Các tùy chọn khác dành riêng cho loại đầu ra riêng lẻ, chẳng hạn như `ParcelFileDescriptor` cho `FileDescriptorOutputOptions`.

`prepareRecording()` trả về một đối tượng `PendingRecording`, là đối tượng trung gian được sử dụng để tạo đối tượng `Recording` tương ứng. `PendingRecording` là một lớp tạm thời nên ẩn trong hầu hết các trường hợp và hiếm khi được ứng dụng lưu vào bộ nhớ cache.

Các ứng dụng có thể định cấu hình thêm bản ghi, chẳng hạn như:

- Enable audio with `withAudioEnabled()`.
- Register a listener to receive video recording events with `start(Executor, Consumer<VideoRecordEvent>)`.
- Allow a recording to continuously record while the VideoCapture it's attached to is rebound to another camera, with `PendingRecording.asPersistentRecording()`.

Để bắt đầu ghi, hãy gọi `PendingRecording.start()`. CameraX biến `PendingRecording` thành `Recording`, xếp hàng đợi yêu cầu ghi và trả về đối tượng `Recording` mới được tạo cho ứng dụng. Sau khi quá trình ghi bắt đầu trên thiết bị Camera tương ứng, CameraX sẽ gửi sự kiện `VideoRecordEvent.EVENT_TYPE_START`.

Ví dụ sau đây cho thấy cách ghi video và âm thanh vào tệp `MediaStore`:

```kotlin
// Create MediaStoreOutputOptions for our recorder
val name = "CameraX-recording-" +
        SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
val contentValues = ContentValues().apply {
   put(MediaStore.Video.Media.DISPLAY_NAME, name)
}
val mediaStoreOutput = MediaStoreOutputOptions.Builder(this.contentResolver,
                              MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                              .setContentValues(contentValues)
                              .build()

// 2. Configure Recorder and Start recording to the mediaStoreOutput.
val recording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), captureListener)
```

Mặc dù theo mặc định, bản xem trước của máy ảnh được phản chiếu trên máy ảnh trước, nhưng các video được quay bằng VideoCapture không được phản chiếu theo mặc định. Với CameraX 1.3, giờ đây bạn có thể phản chiếu các bản ghi video để bản xem trước của camera trước và video đã ghi khớp với nhau.

Có ba tùy chọn MirrorMode: MIRROR_MODE_OFF, MIRROR_MODE_ON và MIRROR_MODE_ON_FRONT_ONLY. Để căn chỉnh với bản xem trước của máy ảnh, Google khuyên bạn nên sử dụng MIROR_MODE_ON_FRONT_ONLY, có nghĩa là tính năng phản chiếu không được bật cho máy ảnh sau nhưng được bật cho máy ảnh trước. Để biết thêm thông tin về MirrorMode, hãy xem [hằng số MirrorMode](https://developer.android.com/reference/androidx/camera/core/MirrorMode).

Đoạn mã này cho biết cách gọi `VideoCapture.Builder.setMirrorMode()` bằng `MIRROR_MODE_ON_FRONT_ONLY`. Để biết thêm thông tin, hãy xem `[setMirrorMode()](https://developer.android.com/reference/androidx/camera/video/VideoCapture.Builder#setMirrorMode(int))`.

```kotlin
val recorder = Recorder.Builder().build()

val videoCapture = VideoCapture.Builder(recorder)
    .setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)
    .build()

useCases.add(videoCapture)
```

## **Control an active recording**

Bạn có thể tạm dừng, tiếp tục và dừng `Recording` đang diễn ra bằng cách sử dụng các phương pháp sau:

- `[pause](https://developer.android.com/reference/androidx/camera/video/Recording#pause())` to pause the current active recording.
- `[resume()](https://developer.android.com/reference/androidx/camera/video/Recording#resume())` to resume a paused active recording.
- `[stop()](https://developer.android.com/reference/androidx/camera/video/Recording#stop())` to finish recording and flush any associated recording objects.
- `[mute()](https://developer.android.com/reference/androidx/camera/video/Recording#mute())` to mute or un-mute the current recording.

Lưu ý rằng bạn có thể gọi `stop()` để kết thúc một `Recording` bất kể bản ghi đang ở trạng thái ghi tạm dừng hay đang hoạt động.

Nếu bạn đã đăng ký `EventListener` với `PendingRecording.start()`, `Recording` sẽ giao tiếp bằng cách sử dụng `[VideoRecordEvent](https://developer.android.com/reference/androidx/camera/video/VideoRecordEvent)`.

- `VideoRecordEvent.EVENT_TYPE_STATUS` được sử dụng để ghi số liệu thống kê như kích thước tệp hiện tại và khoảng thời gian được ghi.
- `VideoRecordEvent.EVENT_TYPE_FINALIZE` được sử dụng cho kết quả ghi và bao gồm thông tin như URI của tệp cuối cùng cùng với mọi lỗi liên quan.

Sau khi ứng dụng của bạn nhận được `EVENT_TYPE_FINALIZE` cho biết phiên ghi thành công, thì bạn có thể truy cập video đã quay từ vị trí được chỉ định trong `OutputOptions`.