/* * Copyright 2014 http://Bither.net * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * *    http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */package net.bither.http;import android.content.Context;import net.bither.factory.CookieFactory;import net.bither.http.HttpSetting.HttpType;import net.bither.preference.PersistentCookieStore;import org.apache.http.HttpEntity;import org.apache.http.HttpResponse;import org.apache.http.client.HttpClient;import org.apache.http.conn.ClientConnectionManager;import org.apache.http.cookie.Cookie;import org.apache.http.impl.client.BasicCookieStore;import org.apache.http.impl.client.DefaultHttpClient;import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;import org.apache.http.impl.cookie.BasicClientCookie;import org.apache.http.params.HttpConnectionParams;import org.apache.http.params.HttpParams;import java.io.BufferedReader;import java.io.InputStreamReader;import java.util.HashMap;public abstract class BaseHttpResponse<T> {    private HttpType mHttpType = HttpType.BitherApi;    protected T result;    private Context mContext;    private String mUrl;    private DefaultHttpClient mHttpClient;    public T getResult() {        return result;    }    public abstract void setResult(String response) throws Exception;    public Context getContext() {        return mContext;    }    public void setContext(Context context) {        this.mContext = context;    }    public String getUrl() {        return mUrl;    }    public void setUrl(String url) {        this.mUrl = url;    }    public DefaultHttpClient getHttpClient() {        return mHttpClient;    }    public void setHttpClient() {        this.mHttpClient = getThreadSafeHttpClient();    }    public HttpType getHttpType() {        return mHttpType;    }    public void setHttpType(HttpType mHttpType) {        this.mHttpType = mHttpType;    }    protected String getReponse(HttpResponse httpResponse) throws Exception {        HttpEntity httpEntity = httpResponse.getEntity();        String response = getResponseFromEntity(httpEntity);        int code = httpResponse.getStatusLine().getStatusCode();        String error = code + ":" + response;        switch (code) {            case 200:                break;            case 400:                throw new Http400Exception(error);            case 403:                if (!CookieFactory.isRunning()                        && getHttpType() == HttpType.BitherApi) {                    PersistentCookieStore.getInstance().clear();                }                throw new HttpAuthException(error);            case 404:                throw new Http404Exception(error);            case 500:                throw new Http500Exception(error);            default:                throw new HttpException(error);        }        return response;    }    private String getResponseFromEntity(HttpEntity entity) throws Exception {        StringBuffer buffer = new StringBuffer();        if (entity != null) {            BufferedReader reader = new BufferedReader(new InputStreamReader(                    entity.getContent(), "utf-8"), 8192);            String line = null;            while ((line = reader.readLine()) != null) {                buffer.append(line);            }            reader.close();        }        return buffer.toString();    }    private DefaultHttpClient getThreadSafeHttpClient() {        if (getHttpType() == HttpType.BitherApi) {            PersistentCookieStore persistentCookieStore = PersistentCookieStore                    .getInstance();            if (persistentCookieStore.getCookies() == null                    || persistentCookieStore.getCookies().size() == 0) {                CookieFactory.initCookie();            }        }        DefaultHttpClient httpClient = new DefaultHttpClient();        ClientConnectionManager mgr = httpClient.getConnectionManager();        HttpParams params = httpClient.getParams();        HttpConnectionParams.setConnectionTimeout(params,                HttpSetting.HTTP_CONNECTION_TIMEOUT);        HttpConnectionParams.setSoTimeout(params, HttpSetting.HTTP_SO_TIMEOUT);        httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(                params, mgr.getSchemeRegistry()), params);        setCookieStore(httpClient);        return httpClient;    }    private void setCookieStore(DefaultHttpClient httpClient) {        if (getHttpType() != HttpType.OtherApi) {            if (getHttpType() == HttpType.GetBitherCookie || getUrl().contains(BitherUrl.BITHER_DNS.BITHER_USER_DOMAIN)) {                httpClient.setCookieStore(PersistentCookieStore.getInstance());            } else {                if (getUrl().contains(BitherUrl.BITHER_DNS.BITHER_BITCOIN_DOMAIN)) {                    httpClient.setCookieStore(getCookieStore(BitherUrl.BITHER_DNS.BITHER_BITCOIN_DOMAIN));                }                if (getUrl().contains(BitherUrl.BITHER_DNS.BITHER_STATS_DOMAIN)) {                    httpClient.setCookieStore(getCookieStore(BitherUrl.BITHER_DNS.BITHER_STATS_DOMAIN));                }            }        }    }    private static HashMap<String, BasicCookieStore> cookieCache = new HashMap<String, BasicCookieStore>();    private BasicCookieStore getCookieStore(String domain) {        BasicCookieStore cookieStore = null;        if (cookieCache.containsKey(domain)) {            cookieStore = cookieCache.get(domain);        } else {            cookieStore = new BasicCookieStore();            PersistentCookieStore persistentCookieStore = PersistentCookieStore                    .getInstance();            cookieStore = new BasicCookieStore();            for (Cookie cookie : persistentCookieStore.getCookies()) {                BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());                basicClientCookie.setDomain(domain);                basicClientCookie.setExpiryDate(cookie.getExpiryDate());                basicClientCookie.setVersion(cookie.getVersion());                basicClientCookie.setPath(cookie.getPath());                cookieStore.addCookie(basicClientCookie);            }            cookieCache.put(domain, cookieStore);        }        return cookieStore;    }}