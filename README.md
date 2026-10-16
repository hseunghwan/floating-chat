# floating-chat

Android 텍스트 선택 메뉴에서 AI(ChatGPT/Claude/Gemini)에 바로 질문하고, 앱 이동 없이 플로팅 창에서 답을 보는 앱.

## 상태
- `spike/` — **THROWAWAY**. WebView 플로팅 + 자동 전송 실현 가능성 확인용. 제품 코드 아님.

## spike 빌드
```sh
cd spike
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew installDebug
```
