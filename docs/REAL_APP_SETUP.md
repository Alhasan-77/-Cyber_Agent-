# Cyber Agent — تشغيل التطبيق الحقيقي

هذه الوثيقة تفصل بين وظائف التطبيق التي تنفذها الشيفرة فعلياً وبين الخدمات التي تتطلب إعداد مشروع Firebase أو مزوداً خارجياً.

## 1. إعداد Firebase

1. أنشئ مشروع Firebase أو استخدم مشروعاً تملكه.
2. أضف تطبيق Android بالمعرّف التالي:
   `com.aistudio.novaforge.kxmpzq`
3. نزّل ملف `google-services.json` وضعه في:
   `app/google-services.json`
   لا تضع الملف داخل المستودع العام إذا كانت سياسة مشروعك تمنع نشر إعداداته.
4. فعّل Google كموفر تسجيل دخول في Firebase Authentication.
5. أضف بصمات شهادة التوقيع SHA-1 وSHA-256 لتطبيق Android في إعدادات Firebase؛ أضف بصمة debug للتطوير وبصمة release للتوزيع.
6. أنشئ قاعدة Firestore بالمعرّف الذي يحدده مورد `firestore_database_id` في التطبيق، ثم انشر قواعد `firestore.rules`:
   `firebase deploy --only firestore:rules`
7. فعّل Firebase AI Logic واختر Gemini Developer API أو Agent Platform Gemini API حسب إعداد مشروعك.

## 2. App Check

Firebase AI Logic يتطلب App Check enforcement منذ 2 نوفمبر 2026.

- التطوير: استخدم Debug provider، ثم سجّل debug token في Firebase Console. لا تضع token في Git.
- الإنتاج: سجّل تطبيق Android مع Play Integrity، وأضف بصمة SHA-256 الصحيحة، ثم فعّل enforcement بعد التأكد من نجاح التحقق.
- يجب أن يتطابق إعداد App Check مع تطبيق Firebase المسجل ونسخة التوقيع المستخدمة.

## 3. نماذج Gemini

يستخدم الوكيل أدوات Function Calling محددة لتنفيذ إجراءات مساحة العمل. النماذج المدعومة في Firebase AI Logic لهذه الوظيفة تشمل Gemini 4 Argon وGemini 3.8 Flash وGemini 3.5 Flash-Lite.

- Gemini 3.8 Flash هو المسار الافتراضي للوكيل عند اختيار الوضع المزدوج.
- Gemini 4 Argon يتطلب خطة Blaze والفوترة.
- توليد الصور عبر Nano Banana يتطلب تفعيل الفوترة.
- أسماء النماذج يجب أن تبقى مطابقة لقائمة Firebase AI Logic الرسمية.

## 4. مهام الوكيل الحالية

بعد تسجيل الدخول وإعداد Firebase، يستطيع الوكيل تنفيذ الإجراءات المسموح بها التالية:

- قراءة حالة مساحة العمل الحالية.
- إنشاء مشروع جديد من وصف المستخدم، توليد ملفاته، حفظها في Firestore، وتهيئة المحرر.
- حفظ ملفات المحرر في مشروع المستخدم على Firestore.
- إنشاء لقطة إصدار محفوظة.
- مراجعة الكود بمساعدة نموذج الذكاء الاصطناعي.
- اقتراح تعديل للكود النشط، تطبيقه في المحرر، ثم حفظ المشروع.

الوكيل لا ينفذ أوامر shell عشوائية، ولا يحذف المشاريع، ولا ينشر تطبيقات أو مواقع. لا يعتبر مراجعة النموذج بديلاً عن SAST/DAST أو اختبارات التشغيل.

## 5. OpenAI

لا تضع مفتاح OpenAI داخل APK أو `BuildConfig` في إصدار إنتاجي؛ يمكن استخراج القيم المضمنة في تطبيق Android. إذا أردت تفعيل OpenAI، اربطه بخادم وسيط موثوق يحتفظ بالمفتاح على الخادم، ثم اجعل التطبيق يتصل بنقطة النهاية الخاصة بك.

## 6. وظائف غير مفعّلة حالياً

- توليد ملفات فيديو أو موسيقى عبر مزود حقيقي غير مهيأ في هذا الإصدار.
- النشر السحابي CI/CD غير مربوط بمزود نشر.
- زر المراجعة التقليدي يقدم فحصاً أولياً للمصدر فقط؛ لا يشغّل ماسحاً أمنياً ديناميكياً.
- معاينة IDE تشغّل HTML/CSS/JavaScript داخل WebView، لكنها لا تبني حزمة Android أو backend production.

## 7. التحقق

من جذر المستودع:

```bash
firebase emulators:exec --project demo-no-project --only auth,firestore "gradle --no-daemon testDebugUnitTest"
gradle --no-daemon assembleDebug
```

اختبارات CI تستخدم Firebase Auth وFirestore Emulator. نجاح الاختبارات لا يثبت تسجيل الدخول إلى مشروع Firebase الإنتاجي أو عمل App Check على جهاز حقيقي؛ يجب إجراء اختبار جهاز بعد إدخال إعدادات المشروع الفعلية.
