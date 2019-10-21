## Google Mailを使ってAndroid端末からメールを送信する

GMailの認証は、Android端末で行う。

### 環境
 - Android 8.1.0（Oreo、API Level 27）
 - Anroid studio 3.5.1

#### はまったこと（その１）
```
NetHttpTransport transport = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
```
で、「JSK」がサポートされていないと実行時に怒られた。
```
HttpTransport transport = com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport();
```
としたら、非推奨とコンパイル時に取り消し線が引かれる。
```
NetHttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
```
で、上手く行った。
#### はまったこと（その２）
ユーザ認証で
```
        Collection<String> scopes = Arrays.asList(GmailScopes.GMAIL_SEND);
        this.mCredential = GoogleAccountCredential.usingOAuth2(this, scopes);
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_CHOOSER);
```
を行い、```onActivityResult()```で選択したユーザ名を```GoogleAccountCredential mCredential```に
設定するが、メールの送信で```UserRecoverableAuthIOException```が発生した。  
どうやら、GoogleでAPIサービスを作成した後の１回目は、この例外が発生するとのこと。
