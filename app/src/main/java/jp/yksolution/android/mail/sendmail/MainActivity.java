package jp.yksolution.android.mail.sendmail;

import androidx.appcompat.app.AppCompatActivity;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACCOUNT_CHOOSER = 1;
    private GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Collection<String> scopes = Arrays.asList(GmailScopes.GMAIL_SEND);
        this.mCredential = GoogleAccountCredential.usingOAuth2(this, scopes);
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_CHOOSER);

        findViewById(R.id.btnSendMail).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendMail(); }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_CHOOSER:
                if (resultCode == RESULT_OK && data != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.i("Account Name", accountName);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        ((EditText)findViewById(R.id.txtMailFm)).setText(accountName);
                    }
                }
                break;
            default:
                Log.d("onActivityResult", "unknown request Code : " + requestCode);
        }
    }

    private void sendMail() {
        String mailTo   = ((EditText)findViewById(R.id.txtMailTo)).getText().toString();
        String subject  = ((EditText)findViewById(R.id.txtSubject)).getText().toString();
        String mailBody = ((EditText)findViewById(R.id.txtMailBody)).getText().toString();

        MyGoogleMail gmail = new MyGoogleMail(mCredential);
        gmail.execute(mailTo, subject, mailBody);
    }




    private class MyGoogleMail extends android.os.AsyncTask<String, Object, MimeMessage> {
        private GoogleAccountCredential mCredential;

        public MyGoogleMail(GoogleAccountCredential credential) {
            this.mCredential = credential;
        }

        String mailTo;
        String subject;
        String mailBody;

        @Override
        protected MimeMessage doInBackground(String... vals) {
            this.mailTo = vals[0];
            this.subject = vals[1];
            this.mailBody = vals[2];

            MimeMessage msg = null;
            try {
                msg = this.createMimeMessage(this.mCredential.getSelectedAccountName(), mailTo, subject, mailBody);
// JSKがない    final NetHttpTransport transport = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
// 非推奨       final HttpTransport transport = com.google.api.client.extensions.android.http.AndroidHttp.newCompatibleTransport();
                final NetHttpTransport transport = new NetHttpTransport();
                //NetHttpTransport transport = new NetHttpTransport();
                Gmail gmail = new Gmail.Builder(transport, new GsonFactory(), this.mCredential)
                        .setApplicationName("Android de GMail").build();
                gmail.users().messages().send(this.mCredential.getSelectedAccountName(), this.createMessage(msg)).execute();
            } catch (UserRecoverableAuthIOException ex) {
                Intent intent = ex.getIntent();
                startActivityForResult(intent, REQUEST_ACCOUNT_CHOOSER);
            } catch (Exception ex) {
                msg = null;
                ex.printStackTrace();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(MimeMessage mimeMessage) {
            String msg;
            if (mimeMessage == null) {
                msg = "メールの送信に失敗しました.";
            } else {
                msg = "メールを送信しました.";
            }
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        }

        private MimeMessage createMimeMessage(String mailFrom, String mailTo, String subject, String mailBody) throws Exception {
            MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
            msg.setFrom(new InternetAddress(mailFrom));
            msg.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(mailTo));
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(mailBody);
            return msg;
        }

        private Message createMessage(MimeMessage mimeMessage) throws Exception {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            byte[] bytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
            Message message = new Message();
            message.setRaw(encodedEmail);
            return message;
        }
    }
}