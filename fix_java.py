code = open('app/src/main/java/com/films/app/MainActivity.java').read()

# 1. زيادة وقت المهلة
old_conn = 'HttpURLConnection conn = (HttpURLConnection) new URL(ngrokUrl + "?session=" + sessionId).openConnection();'
new_conn = '''HttpURLConnection conn = (HttpURLConnection) new URL(ngrokUrl + "?session=" + sessionId).openConnection();
            conn.setConnectTimeout(180000);
            conn.setReadTimeout(180000);'''
code = code.replace(old_conn, new_conn)

# 2. الاستراحة الذكية بعد التأكيد
old_send = 'conn.getResponseCode();'
new_send = '''int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Thread.sleep(2000); // استنى ثانيتين بعد ما الملف يترفع بنجاح
            }'''
code = code.replace(old_send, new_send)

open('app/src/main/java/com/films/app/MainActivity.java', 'w').write(code)
print('done')
