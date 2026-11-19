# Cyber Agent — تشغيل التطبيق الحقيقي

توضح هذه الوثيقة ما يعمل محلياً، وما يتطلب إعداد مشروع Firebase أو مزوداً خارجياً. نجاح بناء APK وحده لا يثبت اتصال الخدمات السحابية أو عملها على جهاز حقيقي.

## 1. إعداد Firebase

1. أنشئ مشروع Firebase تملكه.
2. أضف تطبيق Android بالمعرّف الدقيق: `com.aistudio.novaforge.kxmpzq`.
3. نزّل `google-services.json` وضعه محلياً في `app/google-services.json`. لا ترفع الملف إلى مستودع عام.
4. لتمكين CI من بناء APK مرتبط بالمشروع، أضف محتوى الملف كـGitHub Repository Secret باسم `GOOGLE_SERVICES_JSON` من Settings → Secrets and variables → Actions.
5. فعّل Google ضمن Authentication → Sign-in method.
6. أنشئ قاعدة Firestore بالمعرّف المحدد في `app/src/main/res/values/firebase_applet_config.xml`. لا تنشئ قاعدة باسم افتراضي مختلف.
7. انشر القواعد من جذر المشروع باستخدام Firebase CLI:
   `firebase deploy --only firestore:rules`
8. فعّل Firebase AI Logic واختر مزود Gemini المتاح في مشروعك. تحقق من توفر أسماء النماذج في حسابك ومن إعدادات الفوترة قبل تفعيل النماذج المدفوعة.

## 2. App Check

- في التطوير، يستخدم إصدار Debug مزود Debug App Check. شغّل التطبيق، خذ الرمز من Logcat، وسجّله في Firebase Console. لا تحفظ الرمز في Git أو داخل ملف عام.
- في الإنتاج، يستخدم التطبيق Play Integrity. سجّل بصمة SHA-256 لشهادة التوقيع الفعلية في إعدادات تطبيق Firebase.
- لا تفعّل Enforcement قبل تسجيل تطبيقك واختبار نسخة Debug ونسخة Release على جهاز حقيقي.

## 3. Gemini وOpenAI

- Gemini يتصل عبر Firebase AI Logic؛ يحتاج مشروع Firebase مضبوطاً، ومزوداً مفعلاً، وApp Check مسجلاً. قد تتطلب بعض النماذج خطة Blaze.
- لا يرسل تطبيق Android مفتاح OpenAI إلى OpenAI مباشرة. يرسل طلب ChatGPT إلى Cloud Function باسم `openaiChatCompletion` مع Firebase ID token وApp Check token. تتحقق الدالة من الاثنين، وتطبق حداً يومياً قدره 50 طلباً لكل مستخدم، ثم تستدعي OpenAI بالمفتاح المحفوظ في Firebase Secret Manager.
- عنوان الدالة يُشتق داخل التطبيق من Firebase Project ID في `google-services.json` ويُثبّت على نطاق `us-central1` الخاص بمشروع Firebase نفسه؛ لا يوجد عنوان خادم خارجي يمكن تغييره لإرسال رموز Firebase إلى مضيف غير موثوق.
- أنشئ السر من جذر المشروع باستخدام `firebase functions:secrets:set OPENAI_API_KEY`، ثم انشر الدالة باستخدام `firebase deploy --only functions:openaiChatCompletion`.
- يتطلب نشر Cloud Functions خطة Firebase Blaze مع تفعيل الفوترة؛ كما أن استخدام OpenAI نفسه يخضع لفوترة OpenAI. لا تضف `OPENAI_API_KEY` إلى `.env` أو BuildConfig أو APK. المفتاح مطلوب على الخادم فقط.
- الوضع المزدوج يرجع إلى Gemini عند فشل طلب OpenAI؛ أما اختيار ChatGPT مباشرة فيحتاج مشروع Firebase مضبوطاً والدالة المنشورة وتسجيل App Check.

## 4. حدود الوكيل الحالية

الإجراءات المحلية المسموح بها في هذا الإصدار تشمل قراءة حالة مساحة العمل، وإنشاء مشروع وحفظه، وحفظ ملفات المحرر، وإنشاء لقطة إصدار، ومراجعة المصدر بمساعدة AI، واقتراح تعديل للكود النشط.

لا ينفذ الوكيل أوامر shell عامة، ولا ينشر مواقع أو تطبيقات، ولا يشغّل اختبارات SAST/DAST حقيقية لمجرد طلب مراجعة AI. كما أن معاينة IDE ليست بيئة تشغيل Backend إنتاجية.

## 5. الاختبارات والبناء محلياً

المتطلبات: JDK 21، Node.js 22، Gradle حسب Wrapper المشروع، Firebase CLI.

من جذر المستودع:

```bash
npm install
npm --prefix functions install
firebase emulators:exec --project demo-no-project --only auth,firestore "gradle --no-daemon testDebugUnitTest && npm run test:firestore-rules && npm run test:functions"
keytool -genkeypair -noprompt -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
gradle --no-daemon assembleDebug
```

إذا كان `debug.keystore` موجوداً بالفعل، لا تنشئه مرة أخرى. ناتج Debug المتوقع هو `app/build/outputs/apk/debug/app-debug.apk`.

## 6. إصدار Release

لا تستخدم مفتاح Debug للنشر. أنشئ Upload Keystore خاصاً، واحتفظ بنسخة احتياطية آمنة منه، وخزّن بياناته في GitHub Secrets مثل `KEYSTORE_PATH` و`STORE_PASSWORD` و`KEY_PASSWORD`. يجب ألا تُرفع ملفات keystore أو كلمات المرور إلى Git. بعد التوقيع، سجّل بصمات شهادة Release في Firebase وجرّب تسجيل الدخول وApp Check من جهاز حقيقي.

## 7. شروط اعتبار التطبيق جاهزاً

لا تعتبر النسخة مكتملة حتى تتحقق جميع النقاط التالية: نجاح Kotlin/Robolectric tests، نجاح Firestore Rules tests على Emulator، نجاح assembleDebug، توفر APK كأثر CI، إعداد Firebase الفعلي، نجاح Google Sign-In وFirestore وGemini وApp Check على جهاز Android، واختبار عدم تسريب الأسرار. نجاح CI لا يثبت وحده عمل خدمات الإنتاج.


## 8. إعداد بناء Release الاختياري في GitHub Actions

بعد إنشاء Upload Keystore محلياً والاحتفاظ بنسخة احتياطية آمنة منه، أضف القيم التالية في GitHub → Settings → Secrets and variables → Actions:

- `RELEASE_KEYSTORE_BASE64`: محتوى ملف JKS بعد تحويله إلى Base64.
- `RELEASE_STORE_PASSWORD`: كلمة مرور مخزن الشهادة.
- `RELEASE_KEY_PASSWORD`: كلمة مرور المفتاح.
- يجب أن يكون اسم alias داخل JKS هو `upload` كما هو محدد في إعداد Gradle.

على Linux استخدم `base64 -w 0 my-upload-key.jks`. على macOS استخدم `base64 my-upload-key.jks | tr -d '\\n'`. على Windows PowerShell استخدم `[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks"))`.

بعد إضافة الأسرار، يشغّل CI خطوة `assembleRelease` ويرفع `cyber-agent-release-apk`. لا ترسل JKS أو كلمات المرور في المحادثة أو ترفعها إلى Git. إذا لم تُضبط الأسرار، يستمر Debug بناءً وتظل خطوة Release متخطاة.

## 9. النشر اليدوي لخدمات Firebase عبر GitHub Actions

لتمكين مسار النشر اليدوي:

1. افتح المشروع الصحيح في Firebase Console، وتأكد من تفعيل Firestore وقاعدة البيانات ذات المعرّف المحدد في `firebase.json`.
2. فعّل خطة Blaze، لأن Cloud Functions تحتاج خطة فوترة مدفوعة.
3. أنشئ OpenAI API key، ثم نفّذ محلياً `firebase functions:secrets:set OPENAI_API_KEY --project PROJECT_ID`. أدخل القيمة في الطرفية فقط، ولا تضعها في GitHub أو المحادثة.
4. أنشئ حساب خدمة Google Cloud مخصصاً للنشر بصلاحيات لازمة لنشر Firebase Functions وقواعد Firestore وقراءة Secret Manager وكتابة Artifact Registry. لا تستخدم حسابك الشخصي.
5. أضف أسرار المستودع التالية: `FIREBASE_PROJECT_ID` بمعرّف مشروع Firebase، و`FIREBASE_SERVICE_ACCOUNT` بمحتوى JSON لحساب الخدمة.
6. افتح GitHub → Actions → Deploy Firebase backend → Run workflow. تتحقق المهمة من وجود سر OpenAI ثم تنشر قواعد Firestore ودالة `openaiChatCompletion`.

المهمة لا تنفذ النشر عند كل commit. يلزم تشغيلها يدوياً بعد مراجعة الفرع والتأكد من المشروع الهدف، ويجب ألا تمنح حساب الخدمة صلاحيات أوسع من اللازم.
