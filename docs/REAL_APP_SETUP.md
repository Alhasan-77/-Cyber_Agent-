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
- أنشئ السر من جذر المشروع باستخدام `firebase functions:secrets:set OPENAI_API_KEY`، ثم انشر الدالة باستخدام `firebase deploy --only functions:openaiChatCompletion`.
- اضبط عنوان الدالة في ملف `.env` المحلي، مثلاً: `OPENAI_PROXY_BASE_URL=https://us-central1-PROJECT_ID.cloudfunctions.net/`، واستبدل `PROJECT_ID` بمعرّف مشروعك. أضف القيمة نفسها كـGitHub Actions Secret باسم `OPENAI_PROXY_BASE_URL` ليظهر اتصال ChatGPT في بناء CI.
- لا تضف `OPENAI_API_KEY` إلى `.env` أو BuildConfig أو APK. المفتاح مطلوب على الخادم فقط.
- الوضع المزدوج يرجع إلى Gemini عند عدم إعداد الخادم الوسيط أو فشل طلب OpenAI؛ أما اختيار ChatGPT مباشرة فيحتاج عنوان الخادم الوسيط وإعدادات Firebase الصحيحة.

## 4. حدود الوكيل الحالية

الإجراءات المحلية المسموح بها في هذا الإصدار تشمل قراءة حالة مساحة العمل، وإنشاء مشروع وحفظه، وحفظ ملفات المحرر، وإنشاء لقطة إصدار، ومراجعة المصدر بمساعدة AI، واقتراح تعديل للكود النشط.

لا ينفذ الوكيل أوامر shell عامة، ولا ينشر مواقع أو تطبيقات، ولا يشغّل اختبارات SAST/DAST حقيقية لمجرد طلب مراجعة AI. كما أن معاينة IDE ليست بيئة تشغيل Backend إنتاجية.

## 5. الاختبارات والبناء محلياً

المتطلبات: JDK 21، Node.js 24.12 أو أحدث، Gradle حسب Wrapper المشروع، Firebase CLI.

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
