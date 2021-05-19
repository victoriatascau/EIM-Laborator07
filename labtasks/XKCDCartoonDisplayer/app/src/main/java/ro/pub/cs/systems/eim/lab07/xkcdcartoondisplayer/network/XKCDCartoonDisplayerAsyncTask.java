package ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.entities.XKCDCartoonInformation;
import ro.pub.cs.systems.eim.lab07.xkcdcartoondisplayer.general.Constants;

public class XKCDCartoonDisplayerAsyncTask extends AsyncTask<String, Void, XKCDCartoonInformation> {

    private TextView xkcdCartoonTitleTextView;
    private ImageView xkcdCartoonImageView;
    private TextView xkcdCartoonUrlTextView;
    private Button previousButton, nextButton;

    private class XKCDCartoonButtonClickListener implements Button.OnClickListener {

        private String xkcdComicUrl;

        public XKCDCartoonButtonClickListener(String xkcdComicUrl) {
            this.xkcdComicUrl = xkcdComicUrl;
        }

        @Override
        public void onClick(View view) {
            new XKCDCartoonDisplayerAsyncTask(xkcdCartoonTitleTextView, xkcdCartoonImageView, xkcdCartoonUrlTextView, previousButton, nextButton).execute(xkcdComicUrl);
        }

    }

    public XKCDCartoonDisplayerAsyncTask(TextView xkcdCartoonTitleTextView, ImageView xkcdCartoonImageView, TextView xkcdCartoonUrlTextView, Button previousButton, Button nextButton) {
        this.xkcdCartoonTitleTextView = xkcdCartoonTitleTextView;
        this.xkcdCartoonImageView = xkcdCartoonImageView;
        this.xkcdCartoonUrlTextView = xkcdCartoonUrlTextView;
        this.previousButton = previousButton;
        this.nextButton = nextButton;
    }

    @Override
    public XKCDCartoonInformation doInBackground(String... urls) {
        XKCDCartoonInformation xkcdCartoonInformation = new XKCDCartoonInformation();

        // TODO exercise 5a)
        // 1. obtain the content of the web page (whose Internet address is stored in urls[0])
        // - create an instance of a HttpClient object
        HttpClient httpClient = new DefaultHttpClient();
        // - create an instance of a HttpGet object
        HttpGet httpGet = new HttpGet(urls[0]);
        // - create an instance of a ResponseHandler object
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        // - execute the request, thus obtaining the web page source code
        String pageSourceCode = null;
        try {
            pageSourceCode = httpClient.execute(httpGet, responseHandler);
        } catch (Exception exception) {
            Log.e(Constants.TAG, exception.getMessage());
            if (Constants.DEBUG) {
                exception.printStackTrace();
            }
        }

        // 2. parse the web page source code
        if (pageSourceCode != null) {
            Document document = Jsoup.parse(pageSourceCode);
            Element htmlTag = document.child(0);
            // - cartoon title: get the tag whose id equals "ctitle"
            Element divTagCartoonTitle = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.CTITLE_VALUE).first();
            xkcdCartoonInformation.setCartoonTitle(divTagCartoonTitle.ownText());
            // - cartoon url
            //   * get the first tag whose id equals "comic"
            Element divTagComic = htmlTag.getElementsByAttributeValue(Constants.ID_ATTRIBUTE, Constants.COMIC_VALUE).first();
            //   * get the embedded <img> tag
            //   * get the value of the attribute "src"
            String cartoonInternetAddress = divTagComic.getElementsByTag(Constants.IMG_TAG).attr(Constants.SRC_ATTRIBUTE);
            //   * prepend the protocol: "http:"
            String cartoonUrl = Constants.HTTP_PROTOCOL + cartoonInternetAddress;
            xkcdCartoonInformation.setCartoonUrl(cartoonUrl);
            // - cartoon bitmap (only if using Apache HTTP Components)
            try {
                //   * create the HttpGet object
                HttpGet httpGetImage = new HttpGet(cartoonUrl);
                //   * execute the request and obtain the HttpResponse object
                HttpResponse httpResponse = httpClient.execute(httpGetImage);
                //   * get the HttpEntity object from the response
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    //   * get the bitmap from the HttpEntity stream (obtained by getContent()) using Bitmap.decodeStream() method
                    xkcdCartoonInformation.setCartoonBitmap(BitmapFactory.decodeStream(httpEntity.getContent()));
                }
            } catch (Exception exception) {
                Log.e(Constants.TAG, exception.getMessage());
                if (Constants.DEBUG) {
                    exception.printStackTrace();
                }
            }
            // - previous cartoon address
            //   * get the first tag whole rel attribute equals "prev"
            Element aTagPrev = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.PREVIOUS_VALUE).first();
            //   * get the href attribute of the tag
            //   * prepend the value with the base url: http://www.xkcd.com
            String previousImageInternetAddress = Constants.XKCD_INTERNET_ADDRESS + aTagPrev.attr(Constants.HREF_ATTRIBUTE);
            //   * attach the previous button a click listener with the address attached
            xkcdCartoonInformation.setPreviousCartoonUrl(previousImageInternetAddress);
            // - next cartoon address
            //   * get the first tag whole rel attribute equals "next"
            Element aTagNext = htmlTag.getElementsByAttributeValue(Constants.REL_ATTRIBUTE, Constants.NEXT_VALUE).first();
            //   * get the href attribute of the tag
            //   * prepend the value with the base url: http://www.xkcd.com
            String nextImageInternetAddress = Constants.XKCD_INTERNET_ADDRESS + aTagNext.attr(Constants.HREF_ATTRIBUTE);
            //   * attach the next button a click listener with the address attached
            xkcdCartoonInformation.setNextCartoonUrl(nextImageInternetAddress);
        }

        return  xkcdCartoonInformation;
    }

    @Override
    protected void onPostExecute(final XKCDCartoonInformation xkcdCartoonInformation) {

        // TODO exercise 5b)
        // map each member of xkcdCartoonInformation object to the corresponding widget
        if (xkcdCartoonInformation != null) {
            // cartoonTitle -> xkcdCartoonTitleTextView
            String cartoonTitle = xkcdCartoonInformation.getCartoonTitle();
            if (cartoonTitle != null) {
                xkcdCartoonTitleTextView.setText(cartoonTitle);
            }
            // cartoonBitmap -> xkcdCartoonImageView (only if using Apache HTTP Components)
            Bitmap cartoonBitmap = xkcdCartoonInformation.getCartoonBitmap();
            if (cartoonBitmap != null) {
                xkcdCartoonImageView.setImageBitmap(cartoonBitmap);
            }
            // cartoonUrl -> xkcdCartoonUrlTextView
            String cartoonUrl = xkcdCartoonInformation.getCartoonUrl();
            if (cartoonUrl != null) {
                xkcdCartoonUrlTextView.setText(cartoonUrl);
            }
            // based on cartoonUrl fetch the bitmap
            // and put it into xkcdCartoonImageView
            // previousCartoonUrl, nextCartoonUrl -> set the XKCDCartoonUrlButtonClickListener for previousButton, nextButton
            String prevCartoonUrl = xkcdCartoonInformation.getPreviousCartoonUrl();
            if (prevCartoonUrl != null) {
                previousButton.setOnClickListener(new XKCDCartoonButtonClickListener(prevCartoonUrl));
            }
            String nextCartoonUrl = xkcdCartoonInformation.getNextCartoonUrl();
            if (nextCartoonUrl != null) {
                nextButton.setOnClickListener(new XKCDCartoonButtonClickListener(nextCartoonUrl));
            }
        }
    }

}
